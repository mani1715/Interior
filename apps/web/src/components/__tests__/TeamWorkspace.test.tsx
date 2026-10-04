import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { TeamWorkspace } from '../team/TeamWorkspace';
import * as teamApi from '@/lib/team/api';
import * as authContext from '@/lib/auth/auth-context';
import { TeamOverview } from '@/lib/team/types';

// Mock dependencies
vi.mock('@/lib/team/api');
vi.mock('@/lib/realtime/RealtimeProvider', () => ({
  useRealtimeSubscription: vi.fn(),
}));

const mockTeamOverview: TeamOverview = {
  studioId: 'studio-111',
  studioName: 'Studio Aurelia',
  currentUserRole: 'DESIGNER_ADMIN',
  totalActiveMembers: 2,
  members: [
    {
      id: 'mem-1',
      userId: 'user-1',
      email: 'admin@aurelia.com',
      fullName: 'Elena Rostova',
      role: 'DESIGNER_ADMIN',
      joinedAt: '2026-01-15T10:00:00Z',
    },
    {
      id: 'mem-2',
      userId: 'user-2',
      email: 'junior@aurelia.com',
      fullName: 'Marcus Vance',
      role: 'DESIGNER_MEMBER',
      joinedAt: '2026-02-20T14:30:00Z',
    },
  ],
  pendingInvitations: [
    {
      id: 'inv-1',
      invitedEmail: 'cad_expert@test.com',
      role: 'DESIGNER_MEMBER',
      invitedByEmail: 'admin@aurelia.com',
      invitedAt: '2026-10-01T09:00:00Z',
      expiresAt: '2026-10-08T09:00:00Z',
    },
  ],
};

const mockSoloTeamOverview: TeamOverview = {
  studioId: 'studio-solo',
  studioName: 'Solo Atelier',
  currentUserRole: 'DESIGNER_ADMIN',
  totalActiveMembers: 1,
  members: [
    {
      id: 'mem-solo',
      userId: 'user-solo',
      email: 'admin@aurelia.com',
      fullName: 'Elena Rostova',
      role: 'DESIGNER_ADMIN',
      joinedAt: '2026-01-15T10:00:00Z',
    },
  ],
  pendingInvitations: [],
};

describe('TeamWorkspace Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(authContext, 'useAuth').mockReturnValue({
      user: {
        id: 'user-1',
        email: 'admin@aurelia.com',
        displayName: 'Elena Rostova',
        roles: ['DESIGNER'],
        status: 'ACTIVE',
        permissions: [],
        activeStudioId: 'studio-111',
        activeStudioRole: 'DESIGNER_ADMIN',
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
  });

  it('renders studio team overview with members and pending invitations', async () => {
    vi.spyOn(teamApi, 'fetchTeamOverview').mockResolvedValue(mockTeamOverview);

    render(<TeamWorkspace />);

    expect(screen.getByText('Loading studio team workspace...')).toBeDefined();

    await waitFor(() => {
      expect(screen.getByText('Studio Aurelia')).toBeDefined();
    });

    expect(screen.getByText('2 Active Members')).toBeDefined();
    expect(screen.getAllByText('Elena Rostova').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Marcus Vance').length).toBeGreaterThan(0);
    expect(screen.getByText('Pending Invitations (1)')).toBeDefined();
    expect(screen.getByText('cad_expert@test.com')).toBeDefined();
  });

  it('displays solo member empty state guidance when only one member exists', async () => {
    vi.spyOn(teamApi, 'fetchTeamOverview').mockResolvedValue(mockSoloTeamOverview);

    render(<TeamWorkspace />);

    await waitFor(() => {
      expect(screen.getByText('Solo Atelier')).toBeDefined();
    });

    expect(screen.getByText('1 Active Member')).toBeDefined();
    expect(
      screen.getByText('You are currently the only member of this studio')
    ).toBeDefined();
  });

  it('opens invite modal, submits invitation and displays copyable link with local disclaimer', async () => {
    vi.spyOn(teamApi, 'fetchTeamOverview').mockResolvedValue(mockTeamOverview);
    vi.spyOn(teamApi, 'createInvitation').mockResolvedValue({
      invitationId: 'inv-new',
      invitedEmail: 'newbie@studio.com',
      role: 'DESIGNER_MEMBER',
      inviteLink: 'http://localhost:3000/invite/tok123',
      expiresAt: '2026-10-11T12:00:00Z',
    });

    render(<TeamWorkspace />);

    await waitFor(() => {
      expect(screen.getByText('Studio Aurelia')).toBeDefined();
    });

    // Click Invite Member button
    const inviteBtn = screen.getByRole('button', { name: /Invite Member/i });
    fireEvent.click(inviteBtn);

    // Modal opens
    expect(screen.getByText('Invite Studio Member')).toBeDefined();
    expect(
      screen.getByText(/Email delivery is not configured in this local environment/i)
    ).toBeDefined();

    // Fill in email
    const emailInput = screen.getByPlaceholderText('designer@studio.com');
    fireEvent.change(emailInput, { target: { value: 'newbie@studio.com' } });

    // Submit form
    const submitBtn = screen.getByRole('button', { name: /Generate Invite Link/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(teamApi.createInvitation).toHaveBeenCalledWith({
        email: 'newbie@studio.com',
        role: 'DESIGNER_MEMBER',
      });
      expect(screen.getByText('Invitation Link Created!')).toBeDefined();
    });

    expect(screen.getByDisplayValue('http://localhost:3000/invite/tok123')).toBeDefined();
    expect(screen.getByRole('button', { name: /Copy/i })).toBeDefined();
  });

  it('revokes a pending invitation', async () => {
    vi.spyOn(teamApi, 'fetchTeamOverview').mockResolvedValue(mockTeamOverview);
    vi.spyOn(teamApi, 'revokeInvitation').mockResolvedValue();
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    render(<TeamWorkspace />);

    await waitFor(() => {
      expect(screen.getByText('cad_expert@test.com')).toBeDefined();
    });

    const revokeBtn = screen.getByRole('button', { name: /Revoke/i });
    fireEvent.click(revokeBtn);

    await waitFor(() => {
      expect(teamApi.revokeInvitation).toHaveBeenCalledWith('inv-1');
    });
  });

  it('blocks demoting or removing the sole admin (last-admin protection)', async () => {
    vi.spyOn(teamApi, 'fetchTeamOverview').mockResolvedValue(mockSoloTeamOverview);

    render(<TeamWorkspace />);

    await waitFor(() => {
      expect(screen.getByText('Solo Atelier')).toBeDefined();
    });

    // In desktop and mobile views, Sole Admin indicator is present instead of change/delete buttons
    expect(screen.getAllByText('Sole Admin').length).toBeGreaterThan(0);

    // Click Leave Studio
    const leaveBtn = screen.getByRole('button', { name: /Leave Studio/i });
    fireEvent.click(leaveBtn);

    // Leave modal opens showing lockout warning
    expect(
      screen.getByText(
        /Cannot leave studio: You are the sole administrator/i
      )
    ).toBeDefined();

    const leaveBtns = screen.getAllByRole('button', { name: /Leave Studio/i });
    expect(leaveBtns[1].hasAttribute('disabled')).toBe(true);
  });

  it('allows changing role for another member', async () => {
    vi.spyOn(teamApi, 'fetchTeamOverview').mockResolvedValue(mockTeamOverview);
    vi.spyOn(teamApi, 'updateMemberRole').mockResolvedValue({
      id: 'mem-2',
      userId: 'user-2',
      email: 'junior@aurelia.com',
      fullName: 'Marcus Vance',
      role: 'DESIGNER_ADMIN',
      joinedAt: '2026-02-20T14:30:00Z',
    });

    render(<TeamWorkspace />);

    await waitFor(() => {
      expect(screen.getByText('Studio Aurelia')).toBeDefined();
    });

    // Find Change Role button for Marcus Vance
    const changeRoleBtns = screen.getAllByRole('button', { name: /Change Role/i });
    fireEvent.click(changeRoleBtns[0]);

    expect(screen.getByText('Update Member Role')).toBeDefined();

    const saveBtn = screen.getByRole('button', { name: /Save Role/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(teamApi.updateMemberRole).toHaveBeenCalledWith('mem-2', {
        role: 'DESIGNER_ADMIN',
      });
    });
  });

  it('allows removing a member when multiple members exist', async () => {
    vi.spyOn(teamApi, 'fetchTeamOverview').mockResolvedValue(mockTeamOverview);
    vi.spyOn(teamApi, 'removeMember').mockResolvedValue();

    render(<TeamWorkspace />);

    await waitFor(() => {
      expect(screen.getByText('Studio Aurelia')).toBeDefined();
    });

    const removeBtns = screen.getAllByTitle('Remove member');
    fireEvent.click(removeBtns[0]);

    expect(screen.getByText('Remove Studio Member?')).toBeDefined();

    const confirmBtn = screen.getByRole('button', { name: /Confirm Removal/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(teamApi.removeMember).toHaveBeenCalledWith('mem-2');
    });
  });
});
