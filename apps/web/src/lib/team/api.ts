import { apiFetch } from '../api-client';
import {
  TeamOverview,
  CreateInvitationRequest,
  CreateInvitationResponse,
  UpdateRoleRequest,
  TeamMember,
  ValidateInvitationResponse,
  ExchangeInvitationResponse,
  AcceptInvitationRequest,
} from './types';

export async function fetchTeamOverview(studioId?: string): Promise<TeamOverview> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  const data = await apiFetch<Omit<TeamOverview, 'members'> & {
    totalMembers: number;
    members: (TeamMember & { displayName?: string })[];
  }>(`/workspace/team${query}`);
  const role = (value: string) => value === 'Studio Admin' ? 'DESIGNER_ADMIN' as const
    : value === 'Team Member' ? 'DESIGNER_MEMBER' as const : value as TeamMember['role'];
  return {
    ...data,
    currentUserRole: role(data.currentUserRole),
    totalActiveMembers: data.totalMembers ?? data.totalActiveMembers,
    members: data.members.map(member => ({ ...member, role: role(member.role), fullName: member.displayName ?? member.fullName })),
    pendingInvitations: data.pendingInvitations.map(invite => ({ ...invite, role: role(invite.role) })),
  };
}

export async function createInvitation(
  request: CreateInvitationRequest,
  studioId?: string
): Promise<CreateInvitationResponse> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<CreateInvitationResponse>(`/workspace/team/invitations${query}`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function revokeInvitation(invitationId: string, studioId?: string): Promise<void> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<void>(`/workspace/team/invitations/${invitationId}${query}`, {
    method: 'DELETE',
  });
}

export async function updateMemberRole(
  memberId: string,
  request: UpdateRoleRequest,
  studioId?: string
): Promise<TeamMember> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<TeamMember>(`/workspace/team/members/${memberId}/role${query}`, {
    method: 'PATCH',
    body: JSON.stringify(request),
  });
}

export async function removeMember(memberId: string, studioId?: string): Promise<void> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<void>(`/workspace/team/members/${memberId}${query}`, {
    method: 'DELETE',
  });
}

export async function leaveStudio(studioId?: string): Promise<void> {
  const query = studioId ? `?studioId=${encodeURIComponent(studioId)}` : '';
  return apiFetch<void>(`/workspace/team/leave${query}`, {
    method: 'POST',
  });
}

export async function validateInvitation(token: string): Promise<ValidateInvitationResponse> {
  return apiFetch<ValidateInvitationResponse>(
    `/workspace/invitations/validate?token=${encodeURIComponent(token)}`
  );
}

export async function exchangeInvitationToken(token: string): Promise<ExchangeInvitationResponse> {
  return apiFetch<ExchangeInvitationResponse>('/workspace/invitations/exchange', {
    method: 'POST',
    body: JSON.stringify({ token }),
  });
}

export async function getInvitationSession(): Promise<ValidateInvitationResponse> {
  return apiFetch<ValidateInvitationResponse>('/workspace/invitations/session');
}

export async function acceptInvitation(request?: AcceptInvitationRequest): Promise<void> {
  return apiFetch<void>('/workspace/invitations/accept', {
    method: 'POST',
    body: JSON.stringify(request || {}),
  });
}
