import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { sanitizeRedirectUrl } from '@/lib/auth/redirect';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { AuthContext, AuthContextType } from '@/lib/auth/auth-context';
import SignUpPage from '@/app/sign-up/page';
import AccountPage from '@/app/account/page';
import AuthErrorPage from '@/app/auth/error/page';

// Mock next/navigation
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
  }),
  useSearchParams: () => ({
    get: (key: string) => (key === 'error' ? 'EMAIL_COLLISION' : null),
  }),
}));

describe('Phase 07 — Frontend Authentication & Role Tests', () => {

  describe('Open Redirect Defense (sanitizeRedirectUrl)', () => {
    it('accepts safe internal relative paths', () => {
      expect(sanitizeRedirectUrl('/account')).toBe('/account');
      expect(sanitizeRedirectUrl('/projects/villa')).toBe('/projects/villa');
      expect(sanitizeRedirectUrl('/settings?tab=security')).toBe('/settings?tab=security');
    });

    it('sanitizes external URLs and malicious open redirect vectors to default', () => {
      expect(sanitizeRedirectUrl('https://evil.com')).toBe('/account');
      expect(sanitizeRedirectUrl('http://attacker.com')).toBe('/account');
      expect(sanitizeRedirectUrl('//evil.com')).toBe('/account');
      expect(sanitizeRedirectUrl('/\\evil.com')).toBe('/account');
      expect(sanitizeRedirectUrl('\\\\evil.com')).toBe('/account');
      expect(sanitizeRedirectUrl('\\evil.com')).toBe('/account');
      expect(sanitizeRedirectUrl('javascript:alert(1)')).toBe('/account');
      expect(sanitizeRedirectUrl('data:text/html,<script>alert(1)</script>')).toBe('/account');
      expect(sanitizeRedirectUrl('%2f%2fevil.com')).toBe('/account');
      expect(sanitizeRedirectUrl('/%2fevil.com')).toBe('/account');
      expect(sanitizeRedirectUrl('%5cevil.com')).toBe('/account');
      expect(sanitizeRedirectUrl('/%5cevil.com')).toBe('/account');
      expect(sanitizeRedirectUrl(null)).toBe('/account');
      expect(sanitizeRedirectUrl('')).toBe('/account');
      expect(sanitizeRedirectUrl('   ')).toBe('/account');
      expect(sanitizeRedirectUrl('/account\r\nSet-Cookie: evil=1')).toBe('/account');
      expect(sanitizeRedirectUrl('/account\nLocation: https://evil.com')).toBe('/account');
    });
  });

  describe('PublicHeader Auth Awareness', () => {
    it('renders unauthenticated state with Sign In and Join Atelier links by default', () => {
      render(<PublicHeader />);
      expect(screen.getByText('Sign In')).toBeDefined();
      expect(screen.getByText('Join Atelier')).toBeDefined();
    });

    it('renders authenticated state with user name, role badge, and Sign Out button', () => {
      const mockAuth: AuthContextType = {
        user: {
          id: 'user-uuid-1',
          displayName: 'Ananya Rao',
          email: 'ananya@studio.com',
          status: 'ACTIVE',
          roles: ['DESIGNER'],
          permissions: ['project:write'],
          activeStudioId: 'studio-1',
          activeStudioRole: 'OWNER',
          studios: [{ studioId: 'studio-1', studioName: 'Studio Atelier', studioSlug: 'studio-atelier', role: 'OWNER' }],
          assurance: 'MFA',
        },
        isLoading: false,
        isAuthenticated: true,
        refreshUser: async () => {},
        loginDevPersona: async () => {},
        logout: async () => {},
        revokeAllSessions: async () => {},
      };

      render(
        <AuthContext.Provider value={mockAuth}>
          <PublicHeader />
        </AuthContext.Provider>
      );
      expect(screen.getByText('Ananya Rao')).toBeDefined();
      expect(screen.getByText('DESIGNER')).toBeDefined();
      expect(screen.getByText('Sign Out')).toBeDefined();
    });
  });

  describe('SignUpPage Role Intent & Least Privilege Notice', () => {
    it('renders role choices and principle of least privilege architecture notice', () => {
      render(<SignUpPage />);

      expect(screen.getByText('Client / Homeowner')).toBeDefined();
      expect(screen.getByText('Interior Designer / Studio')).toBeDefined();
      expect(screen.getByText('Security Architecture Notice')).toBeDefined();
      expect(
        screen.getByText(/Under the principle of least privilege, initial account registration grants Customer access/i)
      ).toBeDefined();
    });

    it('allows toggling between Customer and Designer intent', () => {
      render(<SignUpPage />);
      const designerOption = screen.getByText('Interior Designer / Studio');
      fireEvent.click(designerOption);

      expect(screen.getByText('Continue as Interior Designer')).toBeDefined();
    });
  });

  describe('AccountPage Protected View', () => {
    it('renders user details, studio tenancy context, and session controls when authenticated', () => {
      const mockAuth: AuthContextType = {
        user: {
          id: 'user-uuid-99',
          displayName: 'Vikram Sharma',
          email: 'vikram@example.com',
          status: 'ACTIVE',
          roles: ['CUSTOMER'],
          permissions: ['project:read'],
          activeStudioId: null,
          activeStudioRole: null,
          studios: [],
          assurance: 'PASSWORD',
        },
        isLoading: false,
        isAuthenticated: true,
        refreshUser: async () => {},
        loginDevPersona: async () => {},
        logout: async () => {},
        revokeAllSessions: async () => {},
      };

      render(
        <AuthContext.Provider value={mockAuth}>
          <AccountPage />
        </AuthContext.Provider>
      );

      expect(screen.getByText('Vikram Sharma')).toBeDefined();
      expect(screen.getByText('vikram@example.com')).toBeDefined();
      expect(screen.getByText('CUSTOMER')).toBeDefined();
      expect(screen.getByText('PASSWORD')).toBeDefined();
      expect(screen.getByText('Sign Out')).toBeDefined();
      expect(screen.getByText('Revoke All Active Sessions')).toBeDefined();
    });
  });

  describe('AuthErrorPage', () => {
    it('renders error description for EMAIL_COLLISION error code with recovery links', () => {
      render(<AuthErrorPage />);

      expect(screen.getByText('Account Conflict')).toBeDefined();
      expect(
        screen.getByText(/An existing account matches this email address/i)
      ).toBeDefined();
      expect(screen.getByText('Try Signing In Again')).toBeDefined();
      expect(screen.getByText('Return to Homepage')).toBeDefined();
    });
  });
});
