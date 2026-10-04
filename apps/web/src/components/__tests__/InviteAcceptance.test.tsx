import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import InviteAcceptancePage from '@/app/invite/[token]/page';
import * as teamApi from '@/lib/team/api';
import * as authContext from '@/lib/auth/auth-context';
import * as navigation from 'next/navigation';

vi.mock('@/lib/team/api');
vi.mock('next/navigation', () => ({
  useParams: vi.fn(),
  useRouter: vi.fn(),
}));

describe('InviteAcceptancePage Component', () => {
  const mockPush = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(navigation.useParams).mockReturnValue({ token: 'test-tok-123' });
    vi.mocked(navigation.useRouter).mockReturnValue({
      push: mockPush,
      replace: vi.fn(),
      prefetch: vi.fn(),
      back: vi.fn(),
      forward: vi.fn(),
      refresh: vi.fn(),
    } as any);
  });

  it('renders invitation details and prompt to sign in when unauthenticated', async () => {
    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      refreshUser: vi.fn(),
      loginDevPersona: vi.fn(),
      logout: vi.fn(),
      revokeAllSessions: vi.fn(),
    });

    vi.spyOn(teamApi, 'validateInvitation').mockResolvedValue({
      valid: true,
      studioName: 'Studio Aurelia',
      invitedEmail: 'newbie@studio.com',
      role: 'DESIGNER_MEMBER',
      expiresAt: '2026-10-15T00:00:00Z',
    });

    render(<InviteAcceptancePage />);

    await waitFor(() => {
      expect(screen.getByText('Studio Aurelia')).toBeDefined();
    });

    expect(screen.getAllByText('newbie@studio.com').length).toBeGreaterThan(0);
    expect(screen.getByText('Sign In to Accept')).toBeDefined();
  });

  it('allows accepting invitation when authenticated with matching email', async () => {
    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: {
        id: 'user-new',
        email: 'newbie@studio.com',
        displayName: 'New Designer',
        roles: ['CUSTOMER'],
        status: 'ACTIVE',
        permissions: [],
        activeStudioId: null,
        activeStudioRole: null,
        studios: [],
        assurance: 'PASSWORD',
      },
      isAuthenticated: true,
      isLoading: false,
      refreshUser: vi.fn(),
      loginDevPersona: vi.fn(),
      logout: vi.fn(),
      revokeAllSessions: vi.fn(),
    });

    vi.spyOn(teamApi, 'validateInvitation').mockResolvedValue({
      valid: true,
      studioName: 'Studio Aurelia',
      invitedEmail: 'newbie@studio.com',
      role: 'DESIGNER_MEMBER',
      expiresAt: '2026-10-15T00:00:00Z',
    });

    vi.spyOn(teamApi, 'acceptInvitation').mockResolvedValue();

    render(<InviteAcceptancePage />);

    await waitFor(() => {
      expect(screen.getByText('Studio Aurelia')).toBeDefined();
    });

    const acceptBtn = screen.getByRole('button', { name: /Accept & Join/i });
    fireEvent.click(acceptBtn);

    await waitFor(() => {
      expect(teamApi.acceptInvitation).toHaveBeenCalledWith({ token: 'test-tok-123' });
      expect(screen.getByText('Welcome to Studio Aurelia!')).toBeDefined();
    });
  });

  it('shows email mismatch warning when signed in with wrong email', async () => {
    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: {
        id: 'user-wrong',
        email: 'wrong@studio.com',
        displayName: 'Wrong Account',
        roles: ['DESIGNER'],
        status: 'ACTIVE',
        permissions: [],
        activeStudioId: null,
        activeStudioRole: null,
        studios: [],
        assurance: 'PASSWORD',
      },
      isAuthenticated: true,
      isLoading: false,
      refreshUser: vi.fn(),
      loginDevPersona: vi.fn(),
      logout: vi.fn(),
      revokeAllSessions: vi.fn(),
    });

    vi.spyOn(teamApi, 'validateInvitation').mockResolvedValue({
      valid: true,
      studioName: 'Studio Aurelia',
      invitedEmail: 'newbie@studio.com',
      role: 'DESIGNER_MEMBER',
      expiresAt: '2026-10-15T00:00:00Z',
    });

    render(<InviteAcceptancePage />);

    await waitFor(() => {
      expect(screen.getByText('Email Address Mismatch')).toBeDefined();
    });

    expect(screen.getByText(/Switch Account to newbie@studio.com/i)).toBeDefined();
  });

  it('displays error state when token is invalid or expired', async () => {
    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      refreshUser: vi.fn(),
      loginDevPersona: vi.fn(),
      logout: vi.fn(),
      revokeAllSessions: vi.fn(),
    });

    vi.spyOn(teamApi, 'validateInvitation').mockResolvedValue({
      valid: false,
      error: 'Invitation token has expired.',
    });

    render(<InviteAcceptancePage />);

    await waitFor(() => {
      expect(screen.getByText('Invitation Unavailable')).toBeDefined();
    });

    expect(screen.getByText('Invitation token has expired.')).toBeDefined();
  });
});
