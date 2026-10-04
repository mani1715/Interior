-- ============================================================================
-- V027: Canonicalize Identity and Studio Roles (PostgreSQL 18)
-- Canonical Studio Roles: DESIGNER_ADMIN, DESIGNER_MEMBER
-- Global Platform Identity: CUSTOMER (baseline), ADMIN, SUPER_ADMIN
-- ============================================================================

-- 1. Canonicalize studio_members roles
UPDATE studio_members SET role = 'DESIGNER_ADMIN' WHERE role IN ('OWNER', 'ADMIN');
UPDATE studio_members SET role = 'DESIGNER_MEMBER' WHERE role = 'MEMBER';

ALTER TABLE studio_members DROP CONSTRAINT IF EXISTS studio_members_role_check;
ALTER TABLE studio_members ADD CONSTRAINT studio_members_role_check 
    CHECK (role IN ('DESIGNER_ADMIN', 'DESIGNER_MEMBER'));

-- 2. Canonicalize studio_member_invitations roles
UPDATE studio_member_invitations SET role = 'DESIGNER_ADMIN' WHERE role = 'ADMIN';
UPDATE studio_member_invitations SET role = 'DESIGNER_MEMBER' WHERE role = 'MEMBER';

ALTER TABLE studio_member_invitations DROP CONSTRAINT IF EXISTS studio_member_invitations_role_check;
ALTER TABLE studio_member_invitations ADD CONSTRAINT studio_member_invitations_role_check 
    CHECK (role IN ('DESIGNER_ADMIN', 'DESIGNER_MEMBER'));
