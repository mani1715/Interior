-- ============================================================================
-- Phase 9E: Privacy, Data Export, Account Closure, Anonymization & Retention
-- ============================================================================

-- 1. Privacy Requests Table
-- Tracks lifecycle of user/studio data export, account/studio closure and erasure requests
CREATE TABLE IF NOT EXISTS privacy_requests (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    studio_id uuid NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    request_type varchar(32) NOT NULL CHECK (request_type IN (
        'USER_DATA_EXPORT',
        'STUDIO_DATA_EXPORT',
        'ACCOUNT_CLOSURE',
        'STUDIO_CLOSURE',
        'ACCOUNT_DELETION'
    )),
    status varchar(32) NOT NULL DEFAULT 'REQUESTED' CHECK (status IN (
        'REQUESTED',
        'PROCESSING',
        'COMPLETED',
        'FAILED',
        'CANCELLED'
    )),
    confirmed_at timestamptz NULL,
    completed_at timestamptz NULL,
    failure_reason text NULL,
    metadata jsonb NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_privacy_requests_user ON privacy_requests(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_privacy_requests_studio ON privacy_requests(studio_id, created_at DESC) WHERE studio_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_privacy_requests_status ON privacy_requests(status);

-- 2. Data Export Jobs Table
-- Manages export compilation, download tokens, size, expiry and access counter
CREATE TABLE IF NOT EXISTS data_export_jobs (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    privacy_request_id uuid NOT NULL REFERENCES privacy_requests(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    studio_id uuid NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    export_type varchar(32) NOT NULL CHECK (export_type IN ('USER', 'STUDIO')),
    storage_key varchar(512) NULL,
    file_size_bytes bigint NULL CHECK (file_size_bytes IS NULL OR file_size_bytes >= 0),
    download_token_hash varchar(64) NULL,
    download_count integer NOT NULL DEFAULT 0 CHECK (download_count >= 0),
    status varchar(32) NOT NULL DEFAULT 'QUEUED' CHECK (status IN (
        'QUEUED',
        'PROCESSING',
        'READY',
        'EXPIRED',
        'FAILED'
    )),
    expires_at timestamptz NULL,
    completed_at timestamptz NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_data_export_jobs_user ON data_export_jobs(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_data_export_jobs_studio ON data_export_jobs(studio_id, created_at DESC) WHERE studio_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_data_export_jobs_status ON data_export_jobs(status);
CREATE INDEX IF NOT EXISTS idx_data_export_jobs_expires ON data_export_jobs(expires_at) WHERE expires_at IS NOT NULL;

-- 3. Privacy Legal Holds Table
-- Blocks destructive erasure/anonymization for audit, financial dispute, or active litigation
CREATE TABLE IF NOT EXISTS privacy_legal_holds (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    subject_type varchar(32) NOT NULL CHECK (subject_type IN ('USER', 'STUDIO')),
    subject_id uuid NOT NULL,
    reason text NOT NULL,
    placed_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    placed_at timestamptz NOT NULL DEFAULT now(),
    released_at timestamptz NULL,
    active boolean NOT NULL DEFAULT true,
    metadata jsonb NULL
);

CREATE INDEX IF NOT EXISTS idx_privacy_legal_holds_subject ON privacy_legal_holds(subject_type, subject_id) WHERE active = true;
CREATE INDEX IF NOT EXISTS idx_privacy_legal_holds_placed_by ON privacy_legal_holds(placed_by);

-- 4. Row Level Security Policies
ALTER TABLE privacy_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy_requests FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS user_isolation_privacy_requests ON privacy_requests;
CREATE POLICY user_isolation_privacy_requests ON privacy_requests
    FOR ALL
    USING (
        user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
        OR (studio_id IS NOT NULL AND studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid)
        OR current_setting('app.is_admin', true) = 'true'
    );

ALTER TABLE data_export_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE data_export_jobs FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS user_isolation_data_export_jobs ON data_export_jobs;
CREATE POLICY user_isolation_data_export_jobs ON data_export_jobs
    FOR ALL
    USING (
        user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
        OR (studio_id IS NOT NULL AND studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid)
        OR current_setting('app.is_admin', true) = 'true'
    );

ALTER TABLE privacy_legal_holds ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy_legal_holds FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS admin_isolation_privacy_legal_holds ON privacy_legal_holds;
CREATE POLICY admin_isolation_privacy_legal_holds ON privacy_legal_holds
    FOR ALL
    USING (
        current_setting('app.is_admin', true) = 'true'
    );
