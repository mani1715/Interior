export type StudioRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'DESIGNER_ADMIN' | 'DESIGNER_MEMBER';

export interface TeamMember {
  id: string;
  userId: string;
  email: string;
  fullName: string;
  role: StudioRole;
  joinedAt: string;
}

export interface PendingInvitation {
  id: string;
  invitedEmail: string;
  role: StudioRole;
  invitedByEmail: string;
  invitedAt: string;
  expiresAt: string;
}

export interface TeamOverview {
  studioId: string;
  studioName: string;
  currentUserRole: StudioRole;
  totalActiveMembers: number;
  members: TeamMember[];
  pendingInvitations: PendingInvitation[];
}

export interface CreateInvitationRequest {
  email: string;
  role: 'DESIGNER_ADMIN' | 'DESIGNER_MEMBER' | 'ADMIN' | 'MEMBER';
}

export interface CreateInvitationResponse {
  invitationId: string;
  invitedEmail: string;
  role: string;
  inviteLink: string;
  expiresAt: string;
}

export interface ValidateInvitationResponse {
  valid: boolean;
  studioName?: string;
  invitedEmail?: string;
  role?: string;
  expiresAt?: string;
  error?: string;
}

export interface AcceptInvitationRequest {
  token: string;
}

export interface UpdateRoleRequest {
  role: 'DESIGNER_ADMIN' | 'DESIGNER_MEMBER' | 'ADMIN' | 'MEMBER';
}
