-- ============================================================================
-- Phase 9C: Production AI Provider, Usage, Cost Controls & Privacy Hardening
-- ============================================================================

-- 1. Extend ai_visualization_jobs status constraint to support TIMED_OUT
ALTER TABLE ai_visualization_jobs DROP CONSTRAINT IF EXISTS ai_visualization_jobs_status_check;
ALTER TABLE ai_visualization_jobs ADD CONSTRAINT ai_visualization_jobs_status_check
    CHECK (status IN ('QUEUED', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'TIMED_OUT'));

-- 2. Expand event_type constraint on ai_usage_events for reservation lifecycle & timeouts
ALTER TABLE ai_usage_events DROP CONSTRAINT IF EXISTS ai_usage_events_event_type_check;
ALTER TABLE ai_usage_events ADD CONSTRAINT ai_usage_events_event_type_check
    CHECK (event_type IN (
        'GENERATION_RESERVED',
        'GENERATION_ATTEMPT',
        'GENERATION_SUCCESS',
        'GENERATION_FAILED',
        'GENERATION_CANCELLED',
        'GENERATION_TIMEOUT',
        'RESERVATION_REFUNDED'
    ));

-- 3. Dedicated AI Cost Ledger for transparent multi-currency provider accounting
CREATE TABLE IF NOT EXISTS ai_cost_ledger (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    job_id uuid NOT NULL REFERENCES ai_visualization_jobs(id) ON DELETE CASCADE,
    provider_key varchar(64) NOT NULL,
    model_name varchar(128) NOT NULL,
    operation varchar(64) NOT NULL,
    input_units integer NOT NULL DEFAULT 1 CHECK (input_units >= 0),
    output_units integer NOT NULL DEFAULT 1 CHECK (output_units >= 0),
    amount_minor bigint NOT NULL DEFAULT 0 CHECK (amount_minor >= 0),
    currency varchar(3) NOT NULL DEFAULT 'USD',
    cost_source varchar(32) NOT NULL CHECK (cost_source IN ('PROVIDER_REPORTED', 'CALCULATED', 'UNKNOWN')),
    pricing_version varchar(32) NOT NULL DEFAULT 'v1',
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Indexes for studio querying, admin diagnostics, and monthly aggregation
CREATE INDEX IF NOT EXISTS idx_ai_cost_ledger_studio_created ON ai_cost_ledger(studio_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_cost_ledger_job ON ai_cost_ledger(job_id);
CREATE INDEX IF NOT EXISTS idx_ai_cost_ledger_created ON ai_cost_ledger(created_at DESC);

-- Enable RLS on ai_cost_ledger (tenant isolation: studio members can view their studio's costs, platform admins can view all)
ALTER TABLE ai_cost_ledger ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_cost_ledger FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_ai_cost_ledger ON ai_cost_ledger;
CREATE POLICY tenant_isolation_ai_cost_ledger ON ai_cost_ledger
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

-- 4. Studio AI controls (kill-switch per studio, custom studio budget limit)
ALTER TABLE designer_studios ADD COLUMN IF NOT EXISTS ai_enabled boolean NOT NULL DEFAULT true;
ALTER TABLE designer_studios ADD COLUMN IF NOT EXISTS ai_monthly_budget_minor bigint NULL;
ALTER TABLE designer_studios ADD COLUMN IF NOT EXISTS ai_budget_currency varchar(3) NOT NULL DEFAULT 'USD';
