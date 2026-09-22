-- ============================================================================
-- Phase 23 — AI Precision Editing Schema (PostgreSQL 18)
-- ============================================================================

-- 1. Extend ai_visualization_jobs with editing_mode and mask_storage_key
ALTER TABLE ai_visualization_jobs
    ADD COLUMN editing_mode text NOT NULL DEFAULT 'FULL_IMAGE'
        CHECK (editing_mode IN ('FULL_IMAGE', 'PRECISION_MASK')),
    ADD COLUMN mask_storage_key text NULL;

-- 2. Index for studio job querying by editing mode
CREATE INDEX idx_ai_jobs_studio_mode ON ai_visualization_jobs(studio_id, editing_mode);
