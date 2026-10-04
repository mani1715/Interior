-- ============================================================================
-- V026: Studio Team Membership & Invitations (PostgreSQL 18)
-- ============================================================================

-- 1. Studio Member Invitations Table
CREATE TABLE IF NOT EXISTS studio_member_invitations (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    invited_email text NOT NULL,
    role text NOT NULL CHECK (role IN ('ADMIN', 'MEMBER', 'DESIGNER_ADMIN', 'DESIGNER_MEMBER')),
    token_hash bytea NOT NULL UNIQUE,
    invited_by_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED')),
    expires_at timestamptz NOT NULL,
    accepted_at timestamptz NULL,
    accepted_by_user_id uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    revoked_at timestamptz NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_studio_member_invitations_studio ON studio_member_invitations (studio_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_studio_member_invitations_token ON studio_member_invitations (token_hash);
CREATE INDEX IF NOT EXISTS idx_studio_member_invitations_email ON studio_member_invitations (invited_email);

-- 2. Update role check on studio_members to support DESIGNER_ADMIN and DESIGNER_MEMBER alongside OWNER, ADMIN, MEMBER
ALTER TABLE studio_members DROP CONSTRAINT IF EXISTS studio_members_role_check;
ALTER TABLE studio_members ADD CONSTRAINT studio_members_role_check CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'DESIGNER_ADMIN', 'DESIGNER_MEMBER'));

-- 3. Row Level Security for studio_member_invitations
ALTER TABLE studio_member_invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE studio_member_invitations FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_studio_member_invitations ON studio_member_invitations;
CREATE POLICY tenant_isolation_studio_member_invitations ON studio_member_invitations
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );
