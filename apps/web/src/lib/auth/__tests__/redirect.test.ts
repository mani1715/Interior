import { describe, it, expect } from 'vitest';
import { sanitizeRedirectUrl, resolveAuthDestination } from '../redirect';
import type { AuthUser } from '../auth-context';

describe('Authentication Redirect & Destination Resolution', () => {
  describe('sanitizeRedirectUrl', () => {
    it('allows valid internal paths with query strings', () => {
      expect(sanitizeRedirectUrl('/workspace')).toBe('/workspace');
      expect(sanitizeRedirectUrl('/workspace/projects?status=DRAFT')).toBe('/workspace/projects?status=DRAFT');
      expect(sanitizeRedirectUrl('/onboarding/professional')).toBe('/onboarding/professional');
      expect(sanitizeRedirectUrl('/admin/audit')).toBe('/admin/audit');
    });

    it('rejects external schemes and protocol-relative URLs', () => {
      expect(sanitizeRedirectUrl('https://malicious.com')).toBe('/account');
      expect(sanitizeRedirectUrl('http://malicious.com')).toBe('/account');
      expect(sanitizeRedirectUrl('//malicious.com')).toBe('/account');
      expect(sanitizeRedirectUrl('//malicious.com/test')).toBe('/account');
    });

    it('rejects Windows UNC and backslash bypass attempts', () => {
      expect(sanitizeRedirectUrl('\\malicious.com')).toBe('/account');
      expect(sanitizeRedirectUrl('\\\\malicious.com')).toBe('/account');
      expect(sanitizeRedirectUrl('/\\malicious.com')).toBe('/account');
      expect(sanitizeRedirectUrl('/workspace\\attack')).toBe('/account');
    });

    it('rejects URL encoded slash and backslash tricks', () => {
      expect(sanitizeRedirectUrl('/%2fevil.com')).toBe('/account');
      expect(sanitizeRedirectUrl('%2fevil.com')).toBe('/account');
      expect(sanitizeRedirectUrl('/%5cevil.com')).toBe('/account');
      expect(sanitizeRedirectUrl('%5cevil.com')).toBe('/account');
    });

    it('rejects script and data execution schemes', () => {
      expect(sanitizeRedirectUrl('javascript:alert(document.cookie)')).toBe('/account');
      expect(sanitizeRedirectUrl('data:text/html,<script>alert(1)</script>')).toBe('/account');
    });

    it('rejects CRLF header injection characters', () => {
      expect(sanitizeRedirectUrl('/workspace\r\nSet-Cookie: pwn=1')).toBe('/account');
      expect(sanitizeRedirectUrl('/workspace\nLocation: https://evil.com')).toBe('/account');
    });

    it('handles null, undefined, and empty inputs gracefully', () => {
      expect(sanitizeRedirectUrl(null)).toBe('/account');
      expect(sanitizeRedirectUrl(undefined)).toBe('/account');
      expect(sanitizeRedirectUrl('')).toBe('/account');
      expect(sanitizeRedirectUrl('   ')).toBe('/account');
    });

    it('respects custom fallback when provided', () => {
      expect(sanitizeRedirectUrl('https://evil.com', '/fallback')).toBe('/fallback');
      expect(sanitizeRedirectUrl(null, '/custom')).toBe('/custom');
    });
  });

  describe('resolveAuthDestination', () => {
    const baseUser: AuthUser = {
      id: 'usr-100',
      displayName: 'Test User',
      email: 'test@example.com',
      status: 'ACTIVE',
      roles: ['CUSTOMER'],
      permissions: ['project:read'],
      activeStudioId: null,
      activeStudioRole: null,
      studios: [],
      assurance: 'PASSWORD',
    };

    it('returns root / when user is null or undefined and no returnUrl', () => {
      expect(resolveAuthDestination(null)).toBe('/');
      expect(resolveAuthDestination(undefined)).toBe('/');
    });

    it('routes Platform Admin / Super Admin directly to /admin', () => {
      const adminUser: AuthUser = { ...baseUser, roles: ['ADMIN'] };
      expect(resolveAuthDestination(adminUser)).toBe('/admin');

      const superAdminUser: AuthUser = { ...baseUser, roles: ['SUPER_ADMIN'] };
      expect(resolveAuthDestination(superAdminUser)).toBe('/admin');
    });

    it('routes professional with active studio or studio memberships to /workspace', () => {
      const activeStudioUser: AuthUser = {
        ...baseUser,
        roles: ['DESIGNER'],
        activeStudioId: 'std-100',
        activeStudioRole: 'DESIGNER_ADMIN',
      };
      expect(resolveAuthDestination(activeStudioUser)).toBe('/workspace');

      const membershipUser: AuthUser = {
        ...baseUser,
        roles: ['DESIGNER'],
        activeStudioId: null,
        studios: [
          {
            studioId: 'std-200',
            studioName: 'Studio Verona',
            studioSlug: 'studio-verona',
            role: 'DESIGNER_MEMBER',
          },
        ],
      };
      expect(resolveAuthDestination(membershipUser)).toBe('/workspace');
    });

    it('routes incomplete designer without studios to /onboarding/professional', () => {
      const incompleteDesigner: AuthUser = {
        ...baseUser,
        roles: ['DESIGNER'],
        activeStudioId: null,
        studios: [],
      };
      expect(resolveAuthDestination(incompleteDesigner)).toBe('/onboarding/professional');
    });

    it('routes standard customer to /account', () => {
      const customer: AuthUser = {
        ...baseUser,
        roles: ['CUSTOMER'],
      };
      expect(resolveAuthDestination(customer)).toBe('/account');
    });

    it('preserves valid custom returnUrl over default role destination', () => {
      const designer: AuthUser = {
        ...baseUser,
        roles: ['DESIGNER'],
        activeStudioId: 'std-100',
      };
      expect(resolveAuthDestination(designer, '/workspace/projects/new')).toBe('/workspace/projects/new');
      expect(resolveAuthDestination(designer, '/catalog/furniture-item-123')).toBe('/catalog/furniture-item-123');
    });

    it('ignores generic returnUrl (/account, /sign-in, /) and applies role-based resolution', () => {
      const admin: AuthUser = { ...baseUser, roles: ['ADMIN'] };
      expect(resolveAuthDestination(admin, '/account')).toBe('/admin');
      expect(resolveAuthDestination(admin, '/sign-in')).toBe('/admin');
      expect(resolveAuthDestination(admin, '/')).toBe('/admin');
    });

    it('sanitizes malicious returnUrl and falls back to role destination', () => {
      const designer: AuthUser = {
        ...baseUser,
        roles: ['DESIGNER'],
        activeStudioId: 'std-100',
      };
      expect(resolveAuthDestination(designer, 'https://evil.com/phish')).toBe('/workspace');
      expect(resolveAuthDestination(designer, '//evil.com')).toBe('/workspace');
    });
  });
});
