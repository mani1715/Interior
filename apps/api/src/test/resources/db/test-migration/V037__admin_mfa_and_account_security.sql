-- ============================================================================
-- Phase 9D: Admin MFA, Account Security (H2 Test Compatibility)
-- ============================================================================

CREATE TABLE IF NOT EXISTS identity_mfa_methods (
    id uuid NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    method_type varchar(32) NOT NULL DEFAULT 'TOTP',
    encrypted_secret text NOT NULL,
    key_version integer NOT NULL DEFAULT 1,
    status varchar(32) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'DISABLED')),
    pending_expires_at timestamptz NULL,
    confirmed_at timestamptz NULL,
    last_used_counter bigint NULL,
    failed_attempts integer NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
    locked_until timestamptz NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_identity_mfa_user_method UNIQUE (user_id, method_type)
);

CREATE INDEX IF NOT EXISTS idx_identity_mfa_methods_user ON identity_mfa_methods(user_id);
CREATE INDEX IF NOT EXISTS idx_identity_mfa_methods_status ON identity_mfa_methods(status);

CREATE TABLE IF NOT EXISTS identity_recovery_codes (
    id uuid NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash varchar(64) NOT NULL,
    used_at timestamptz NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_identity_recovery_codes_user ON identity_recovery_codes(user_id);

ALTER TABLE identity_sessions ADD COLUMN IF NOT EXISTS mfa_verified_at timestamptz NULL;
CREATE INDEX IF NOT EXISTS idx_identity_sessions_user_mfa ON identity_sessions(user_id, mfa_verified_at);
