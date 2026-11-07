import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from '../../api-client';
import { fetchTeamOverview } from '../api';

vi.mock('../../api-client', () => ({ apiFetch: vi.fn() }));

describe('team response contract', () => {
  beforeEach(() => vi.clearAllMocks());
  it('preserves admin permissions and member names from the backend DTO', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ currentUserRole: 'Studio Admin', totalMembers: 1,
      members: [{ id: 'member', displayName: 'Test Designer', role: 'Studio Admin' }], pendingInvitations: [] });
    const team = await fetchTeamOverview('studio');
    expect(team.currentUserRole).toBe('DESIGNER_ADMIN');
    expect(team.totalActiveMembers).toBe(1);
    expect(team.members[0].fullName).toBe('Test Designer');
    expect(team.members[0].role).toBe('DESIGNER_ADMIN');
  });
});
