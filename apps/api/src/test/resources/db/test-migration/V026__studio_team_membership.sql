-- ============================================================================
-- V026: Studio Team Membership & Invitations (H2 Test Database)
-- ============================================================================

-- 1. Studio Member Invitations Table
CREATE TABLE IF NOT EXISTS studio_member_invitations (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    invited_email text NOT NULL,
    role text NOT NULL CONSTRAINT studio_member_invitations_role_check CHECK (role IN ('ADMIN', 'MEMBER', 'DESIGNER_ADMIN', 'DESIGNER_MEMBER')),
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

-- 2. Update role check on studio_members
ALTER TABLE studio_members DROP CONSTRAINT IF EXISTS studio_members_role_check;
ALTER TABLE studio_members ADD CONSTRAINT studio_members_role_check CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'DESIGNER_ADMIN', 'DESIGNER_MEMBER'));
