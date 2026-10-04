import { describe, it, expect } from 'vitest';
import {
  isStudioAdmin,
  isStudioMember,
  canManageTeam,
  canManageBusiness,
  canEditProjects,
  canUseAi,
  canViewLeads,
  formatStudioRoleLabel,
} from '../permissions';

describe('Studio Permissions Helper Functions', () => {
  it('correctly identifies administrative roles', () => {
    expect(isStudioAdmin('DESIGNER_ADMIN')).toBe(true);
    expect(isStudioAdmin('ADMIN')).toBe(true);
    expect(isStudioAdmin('OWNER')).toBe(true);
    expect(isStudioAdmin('designer_admin')).toBe(true);

    expect(isStudioAdmin('DESIGNER_MEMBER')).toBe(false);
    expect(isStudioAdmin('MEMBER')).toBe(false);
    expect(isStudioAdmin('CUSTOMER')).toBe(false);
    expect(isStudioAdmin(null)).toBe(false);
    expect(isStudioAdmin(undefined)).toBe(false);
  });

  it('correctly identifies member privileges', () => {
    expect(isStudioMember('DESIGNER_MEMBER')).toBe(true);
    expect(isStudioMember('MEMBER')).toBe(true);
    expect(isStudioMember('DESIGNER_ADMIN')).toBe(true);
    expect(isStudioMember('ADMIN')).toBe(true);
    expect(isStudioMember('OWNER')).toBe(true);

    expect(isStudioMember('CUSTOMER')).toBe(false);
    expect(isStudioMember(null)).toBe(false);
  });

  it('enforces action-specific authorization gates', () => {
    expect(canManageTeam('DESIGNER_ADMIN')).toBe(true);
    expect(canManageTeam('DESIGNER_MEMBER')).toBe(false);

    expect(canManageBusiness('DESIGNER_ADMIN')).toBe(true);
    expect(canManageBusiness('DESIGNER_MEMBER')).toBe(false);

    expect(canEditProjects('DESIGNER_ADMIN')).toBe(true);
    expect(canEditProjects('DESIGNER_MEMBER')).toBe(true);
    expect(canEditProjects('CUSTOMER')).toBe(false);

    expect(canUseAi('DESIGNER_MEMBER')).toBe(true);
    expect(canViewLeads('DESIGNER_MEMBER')).toBe(true);
  });

  it('formats display labels accurately', () => {
    expect(formatStudioRoleLabel('DESIGNER_ADMIN')).toBe('Studio Admin');
    expect(formatStudioRoleLabel('ADMIN')).toBe('Studio Admin');
    expect(formatStudioRoleLabel('OWNER')).toBe('Studio Owner');
    expect(formatStudioRoleLabel('DESIGNER_MEMBER')).toBe('Team Member');
    expect(formatStudioRoleLabel('MEMBER')).toBe('Team Member');
    expect(formatStudioRoleLabel(null)).toBe('Member');
  });
});
