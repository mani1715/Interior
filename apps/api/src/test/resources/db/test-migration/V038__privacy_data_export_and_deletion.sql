-- ============================================================================
-- Phase 9E: Privacy, Data Export, Account Closure (H2 Test Compatibility)
-- ============================================================================

CREATE TABLE IF NOT EXISTS privacy_requests (
    id uuid NOT NULL DEFAULT random_uuid() PRIMARY KEY,
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

CREATE INDEX IF NOT EXISTS idx_privacy_requests_user ON privacy_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_privacy_requests_status ON privacy_requests(status);

CREATE TABLE IF NOT EXISTS data_export_jobs (
    id uuid NOT NULL DEFAULT random_uuid() PRIMARY KEY,
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

CREATE INDEX IF NOT EXISTS idx_data_export_jobs_user ON data_export_jobs(user_id);
CREATE INDEX IF NOT EXISTS idx_data_export_jobs_status ON data_export_jobs(status);

CREATE TABLE IF NOT EXISTS privacy_legal_holds (
    id uuid NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    subject_type varchar(32) NOT NULL CHECK (subject_type IN ('USER', 'STUDIO')),
    subject_id uuid NOT NULL,
    reason text NOT NULL,
    placed_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    placed_at timestamptz NOT NULL DEFAULT now(),
    released_at timestamptz NULL,
    active boolean NOT NULL DEFAULT true,
    metadata jsonb NULL
);

CREATE INDEX IF NOT EXISTS idx_privacy_legal_holds_subject ON privacy_legal_holds(subject_type, subject_id);
