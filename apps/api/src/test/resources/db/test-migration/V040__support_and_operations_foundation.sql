-- ============================================================================
-- Phase 9G: Support, Platform Operations, Maintenance & Incident Controls (H2 Test Compatibility)
-- ============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS deactivated_reason varchar(64) NULL;

CREATE TABLE IF NOT EXISTS support_requests (
    id uuid NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    studio_id uuid NULL REFERENCES designer_studios(id) ON DELETE SET NULL,
    category varchar(32) NOT NULL CHECK (category IN ('ACCOUNT', 'BILLING', 'TECHNICAL', 'PRIVACY', 'OTHER')),
    subject varchar(255) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'IN_PROGRESS', 'WAITING_USER', 'RESOLVED', 'CLOSED')),
    priority varchar(32) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    resolved_at timestamptz NULL
);

CREATE INDEX IF NOT EXISTS idx_support_requests_user ON support_requests(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_support_requests_studio ON support_requests(studio_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_support_requests_status ON support_requests(status, updated_at DESC);

CREATE TABLE IF NOT EXISTS support_messages (
    id uuid NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    request_id uuid NOT NULL REFERENCES support_requests(id) ON DELETE CASCADE,
    author_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    author_type varchar(32) NOT NULL CHECK (author_type IN ('USER', 'SUPPORT_ADMIN')),
    body text NOT NULL,
    is_internal boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_support_messages_request ON support_messages(request_id, created_at ASC);

CREATE TABLE IF NOT EXISTS platform_operational_state (
    id varchar(32) NOT NULL DEFAULT 'GLOBAL' PRIMARY KEY,
    maintenance_mode varchar(32) NOT NULL DEFAULT 'NORMAL' CHECK (maintenance_mode IN ('NORMAL', 'READ_ONLY')),
    maintenance_message text NULL,
    uploads_enabled boolean NOT NULL DEFAULT true,
    ai_enabled boolean NOT NULL DEFAULT true,
    registrations_enabled boolean NOT NULL DEFAULT true,
    updated_by uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);

MERGE INTO platform_operational_state (id, maintenance_mode, uploads_enabled, ai_enabled, registrations_enabled, updated_at)
KEY (id)
VALUES ('GLOBAL', 'NORMAL', true, true, true, now());

CREATE TABLE IF NOT EXISTS operational_job_runs (
    id uuid NOT NULL DEFAULT random_uuid() PRIMARY KEY,
    job_key varchar(64) NOT NULL,
    instance_id varchar(128) NOT NULL,
    status varchar(32) NOT NULL CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED', 'PARTIAL')),
    started_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz NULL,
    processed_count integer NOT NULL DEFAULT 0,
    failure_code varchar(64) NULL,
    error_summary text NULL
);

CREATE INDEX IF NOT EXISTS idx_operational_job_runs_key ON operational_job_runs(job_key, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_operational_job_runs_status ON operational_job_runs(status, started_at DESC);
