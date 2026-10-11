-- ============================================================================
-- Phase 9G: Support, Platform Operations, Maintenance & Incident Controls
-- ============================================================================

-- 1. Attributed Account Closure / Suspension Tracking
ALTER TABLE users ADD COLUMN IF NOT EXISTS deactivated_reason varchar(64) NULL;

-- 2. Support Requests Table
CREATE TABLE IF NOT EXISTS support_requests (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
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
CREATE INDEX IF NOT EXISTS idx_support_requests_studio ON support_requests(studio_id, created_at DESC) WHERE studio_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_support_requests_status ON support_requests(status, updated_at DESC);

-- 3. Support Messages Table
CREATE TABLE IF NOT EXISTS support_messages (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    request_id uuid NOT NULL REFERENCES support_requests(id) ON DELETE CASCADE,
    author_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    author_type varchar(32) NOT NULL CHECK (author_type IN ('USER', 'SUPPORT_ADMIN')),
    body text NOT NULL CHECK (char_length(body) <= 4000),
    is_internal boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_support_messages_request ON support_messages(request_id, created_at ASC);

-- 4. Row Level Security for Support Tables
ALTER TABLE support_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE support_requests FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS support_requests_isolation ON support_requests;
CREATE POLICY support_requests_isolation ON support_requests
    FOR ALL
    USING (
        current_setting('app.is_admin', true) = 'true'
        OR user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
        OR (studio_id IS NOT NULL AND studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid)
    );

ALTER TABLE support_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE support_messages FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS support_messages_isolation ON support_messages;
CREATE POLICY support_messages_isolation ON support_messages
    FOR ALL
    USING (
        current_setting('app.is_admin', true) = 'true'
        OR (
            is_internal = false
            AND EXISTS (
                SELECT 1 FROM support_requests r
                WHERE r.id = support_messages.request_id
                AND (
                    r.user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
                    OR (r.studio_id IS NOT NULL AND r.studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid)
                )
            )
        )
    );

-- 5. Platform Operational State
CREATE TABLE IF NOT EXISTS platform_operational_state (
    id varchar(32) NOT NULL PRIMARY KEY DEFAULT 'GLOBAL',
    maintenance_mode varchar(32) NOT NULL DEFAULT 'NORMAL' CHECK (maintenance_mode IN ('NORMAL', 'READ_ONLY')),
    maintenance_message text NULL,
    uploads_enabled boolean NOT NULL DEFAULT true,
    ai_enabled boolean NOT NULL DEFAULT true,
    registrations_enabled boolean NOT NULL DEFAULT true,
    updated_by uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO platform_operational_state (id, maintenance_mode, uploads_enabled, ai_enabled, registrations_enabled, updated_at)
VALUES ('GLOBAL', 'NORMAL', true, true, true, now())
ON CONFLICT (id) DO NOTHING;

ALTER TABLE platform_operational_state ENABLE ROW LEVEL SECURITY;
ALTER TABLE platform_operational_state FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS admin_platform_operational_state ON platform_operational_state;
CREATE POLICY admin_platform_operational_state ON platform_operational_state
    FOR ALL
    USING (current_setting('app.is_admin', true) = 'true');

-- 6. Scheduled Job Runs
CREATE TABLE IF NOT EXISTS operational_job_runs (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    job_key varchar(64) NOT NULL,
    instance_id varchar(128) NOT NULL,
    status varchar(32) NOT NULL CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED', 'PARTIAL')),
    started_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz NULL,
    processed_count integer NOT NULL DEFAULT 0 CHECK (processed_count >= 0),
    failure_code varchar(64) NULL,
    error_summary text NULL
);

CREATE INDEX IF NOT EXISTS idx_operational_job_runs_key ON operational_job_runs(job_key, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_operational_job_runs_status ON operational_job_runs(status, started_at DESC);

ALTER TABLE operational_job_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE operational_job_runs FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS admin_operational_job_runs ON operational_job_runs;
CREATE POLICY admin_operational_job_runs ON operational_job_runs
    FOR ALL
    USING (current_setting('app.is_admin', true) = 'true');
