import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import InviteSessionAcceptancePage from '@/app/invite/accept/page';
import InviteTokenPage from '@/app/invite/[token]/page';
import * as teamApi from '@/lib/team/api';
import * as authContext from '@/lib/auth/auth-context';
import * as navigation from 'next/navigation';

vi.mock('@/lib/team/api');
vi.mock('next/navigation', () => ({
  useParams: vi.fn(),
  useRouter: vi.fn(),
}));

describe('InviteSessionAcceptancePage Component (/invite/accept)', () => {
  const mockPush = vi.fn();
  const mockReplace = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(navigation.useRouter).mockReturnValue({
      push: mockPush,
      replace: mockReplace,
      prefetch: vi.fn(),
      back: vi.fn(),
      forward: vi.fn(),
      refresh: vi.fn(),
    } as any);
  });

  it('renders invitation session details with masked email and sign-in prompt when unauthenticated', async () => {
    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      refreshUser: vi.fn(),
      loginDevPersona: vi.fn(),
      logout: vi.fn(),
      revokeAllSessions: vi.fn(),
    });

    vi.spyOn(teamApi, 'getInvitationSession').mockResolvedValue({
      valid: true,
      studioName: 'Studio Aurelia',
      maskedEmail: 'n***e@studio.com',
      role: 'DESIGNER_MEMBER',
      expiresAt: '2026-10-15T00:00:00Z',
    });

    render(<InviteSessionAcceptancePage />);

    await waitFor(() => {
      expect(screen.getByText('Studio Aurelia')).toBeDefined();
    });

    expect(screen.getByText('n***e@studio.com')).toBeDefined();
    expect(screen.getByText('Team Member')).toBeDefined();
    expect(screen.getByText('Sign In to Accept')).toBeDefined();
    const link = screen.getByRole('link', { name: /Sign In to Accept/i });
    expect(link.getAttribute('href')).toBe('/sign-in?returnUrl=/invite/accept');
  });

  it('allows accepting invitation using HttpOnly cookie session when authenticated', async () => {
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

    vi.spyOn(teamApi, 'getInvitationSession').mockResolvedValue({
      valid: true,
      studioName: 'Studio Aurelia',
      maskedEmail: 'n***e@studio.com',
      role: 'DESIGNER_ADMIN',
      expiresAt: '2026-10-15T00:00:00Z',
    });

    vi.spyOn(teamApi, 'acceptInvitation').mockResolvedValue();

    render(<InviteSessionAcceptancePage />);

    await waitFor(() => {
      expect(screen.getByText('Studio Aurelia')).toBeDefined();
    });

    expect(screen.getByText('Studio Admin')).toBeDefined();

    const acceptBtn = screen.getByRole('button', { name: /Accept & Join Studio Aurelia/i });
    fireEvent.click(acceptBtn);

    await waitFor(() => {
      expect(teamApi.acceptInvitation).toHaveBeenCalled();
      expect(screen.getByText('Welcome to Studio Aurelia!')).toBeDefined();
    });
  });

  it('displays error state when invitation session is invalid or expired', async () => {
    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      refreshUser: vi.fn(),
      loginDevPersona: vi.fn(),
      logout: vi.fn(),
      revokeAllSessions: vi.fn(),
    });

    vi.spyOn(teamApi, 'getInvitationSession').mockResolvedValue({
      valid: false,
      error: 'Invitation session has expired or is invalid.',
    });

    render(<InviteSessionAcceptancePage />);

    await waitFor(() => {
      expect(screen.getByText('Invitation Unavailable')).toBeDefined();
    });

    expect(screen.getByText('Invitation session has expired or is invalid.')).toBeDefined();
  });

  it('exchanges raw token and redirects to /invite/accept on /invite/[token]', async () => {
    vi.mocked(navigation.useParams).mockReturnValue({ token: 'raw-secret-token-777' });
    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      refreshUser: vi.fn(),
      loginDevPersona: vi.fn(),
      logout: vi.fn(),
      revokeAllSessions: vi.fn(),
    });

    vi.spyOn(teamApi, 'exchangeInvitationToken').mockResolvedValue({
      valid: true,
      studioName: 'Studio Aurelia',
      maskedEmail: 'n***e@studio.com',
      role: 'DESIGNER_MEMBER',
      expiresAt: '2026-10-15T00:00:00Z',
    });
    vi.spyOn(teamApi, 'validateInvitation').mockResolvedValue({
      valid: true,
      studioName: 'Studio Aurelia',
    });

    render(<InviteTokenPage />);

    await waitFor(() => {
      expect(teamApi.exchangeInvitationToken).toHaveBeenCalledWith('raw-secret-token-777');
      expect(mockReplace).toHaveBeenCalledWith('/invite/accept');
    });
  });
});
