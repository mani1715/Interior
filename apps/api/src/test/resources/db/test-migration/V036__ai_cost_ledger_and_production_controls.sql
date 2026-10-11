-- ============================================================================
-- Phase 9C: Production AI Provider, Usage, Cost Controls (H2 Test Compatibility)
-- ============================================================================

-- 1. Dedicated AI Cost Ledger for H2
CREATE TABLE IF NOT EXISTS ai_cost_ledger (
    id uuid NOT NULL DEFAULT random_uuid() PRIMARY KEY,
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

CREATE INDEX IF NOT EXISTS idx_ai_cost_ledger_studio_created ON ai_cost_ledger(studio_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_cost_ledger_job ON ai_cost_ledger(job_id);
CREATE INDEX IF NOT EXISTS idx_ai_cost_ledger_created ON ai_cost_ledger(created_at DESC);

-- 2. Studio AI controls
ALTER TABLE designer_studios ADD COLUMN IF NOT EXISTS ai_enabled boolean NOT NULL DEFAULT true;
ALTER TABLE designer_studios ADD COLUMN IF NOT EXISTS ai_monthly_budget_minor bigint NULL;
ALTER TABLE designer_studios ADD COLUMN IF NOT EXISTS ai_budget_currency varchar(3) NOT NULL DEFAULT 'USD';
