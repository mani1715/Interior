import { StudioRole } from './types';

/**
 * Checks if a studio role has administrative privileges.
 * Canonical admin role is DESIGNER_ADMIN. (ADMIN and OWNER accepted for backward-compatibility).
 */
export function isStudioAdmin(role?: string | null): boolean {
  if (!role) return false;
  const normalized = role.toUpperCase();
  return normalized === 'DESIGNER_ADMIN' || normalized === 'ADMIN' || normalized === 'OWNER';
}

/**
 * Checks if a studio role has active member privileges.
 */
export function isStudioMember(role?: string | null): boolean {
  if (!role) return false;
  const normalized = role.toUpperCase();
  return (
    normalized === 'DESIGNER_MEMBER' ||
    normalized === 'MEMBER' ||
    isStudioAdmin(role)
  );
}

/**
 * Authorization helper for managing team members and invitations.
 */
export function canManageTeam(role?: string | null): boolean {
  return isStudioAdmin(role);
}

/**
 * Authorization helper for managing studio profile & business settings.
 */
export function canManageBusiness(role?: string | null): boolean {
  return isStudioAdmin(role);
}

/**
 * Authorization helper for editing projects and portfolio.
 */
export function canEditProjects(role?: string | null): boolean {
  return isStudioMember(role);
}

/**
 * Authorization helper for AI visualizer generation.
 */
export function canUseAi(role?: string | null): boolean {
  return isStudioMember(role);
}

/**
 * Authorization helper for viewing and managing studio leads.
 */
export function canViewLeads(role?: string | null): boolean {
  return isStudioMember(role);
}

/**
 * Formats canonical studio role for display in the UI.
 */
export function formatStudioRoleLabel(role?: string | null): string {
  if (!role) return 'Member';
  const normalized = role.toUpperCase();
  if (normalized === 'OWNER') return 'Studio Owner';
  if (normalized === 'DESIGNER_ADMIN' || normalized === 'ADMIN') return 'Studio Admin';
  if (normalized === 'DESIGNER_MEMBER' || normalized === 'MEMBER') return 'Team Member';
  return role;
}
