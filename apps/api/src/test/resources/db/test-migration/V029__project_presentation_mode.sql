-- ============================================================================
-- Phase 29 — Project Presentation Mode (H2 Test Compatible)
-- ============================================================================

-- 1. Add presentation_mode to studio_projects
ALTER TABLE studio_projects
    ADD COLUMN presentation_mode varchar(32) NOT NULL DEFAULT 'STANDARD';

ALTER TABLE studio_projects
    ADD CONSTRAINT chk_studio_projects_presentation_mode CHECK (presentation_mode IN ('STANDARD', 'CINEMATIC'));

CREATE INDEX idx_studio_projects_presentation_mode ON studio_projects (studio_id, presentation_mode);
