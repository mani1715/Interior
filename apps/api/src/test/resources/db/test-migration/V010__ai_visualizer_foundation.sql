-- ============================================================================
-- Phase 21 — AI Visualizer Foundation Schema (H2 Test Parity)
-- ============================================================================

-- 1. AI Visualization Jobs Table
CREATE TABLE ai_visualization_jobs (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    project_id uuid NOT NULL,
    input_media_id uuid NOT NULL,
    output_media_id uuid NULL,
    provider_key text NOT NULL,
    provider_job_id text NULL,
    prompt text NOT NULL,
    system_prompt text NULL,
    status text NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    error_code text NULL,
    error_message_safe text NULL,
    attempt_count integer NOT NULL DEFAULT 0,
    idempotency_key text NULL,
    created_by uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz NULL,
    completed_at timestamptz NULL,
    failed_at timestamptz NULL,
    usage_metadata text NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_ai_jobs_project FOREIGN KEY (project_id, studio_id)
        REFERENCES studio_projects(id, studio_id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_jobs_input_media FOREIGN KEY (input_media_id, studio_id)
        REFERENCES media_assets(id, studio_id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_jobs_output_media FOREIGN KEY (output_media_id, studio_id)
        REFERENCES media_assets(id, studio_id) ON DELETE SET NULL,
    CONSTRAINT uq_ai_jobs_studio_idempotency UNIQUE (studio_id, idempotency_key)
);

CREATE INDEX idx_ai_jobs_studio ON ai_visualization_jobs(studio_id);
CREATE INDEX idx_ai_jobs_studio_status ON ai_visualization_jobs(studio_id, status);
CREATE INDEX idx_ai_jobs_studio_created ON ai_visualization_jobs(studio_id, created_at DESC);
CREATE INDEX idx_ai_jobs_project ON ai_visualization_jobs(studio_id, project_id);
CREATE INDEX idx_ai_jobs_input ON ai_visualization_jobs(input_media_id);

-- 2. AI Usage Events Table
CREATE TABLE ai_usage_events (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    job_id uuid NOT NULL REFERENCES ai_visualization_jobs(id) ON DELETE CASCADE,
    event_type text NOT NULL
        CHECK (event_type IN ('GENERATION_ATTEMPT', 'GENERATION_SUCCESS', 'GENERATION_FAILED', 'GENERATION_CANCELLED')),
    provider_key text NOT NULL,
    units_consumed integer NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_usage_studio ON ai_usage_events(studio_id);
CREATE INDEX idx_ai_usage_created ON ai_usage_events(studio_id, created_at DESC);
