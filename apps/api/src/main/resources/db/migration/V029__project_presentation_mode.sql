-- ============================================================================
-- Phase 29 — Project Presentation Mode (Elégance Portfolio Evolution Phase 3)
-- ============================================================================

-- 1. Add presentation_mode to studio_projects
ALTER TABLE studio_projects
    ADD COLUMN presentation_mode varchar(32) NOT NULL DEFAULT 'STANDARD'
    CONSTRAINT chk_studio_projects_presentation_mode CHECK (presentation_mode IN ('STANDARD', 'CINEMATIC'));

-- 2. Performance Index for presentation_mode filtered discovery/portfolio lookups
CREATE INDEX idx_studio_projects_presentation_mode ON studio_projects (studio_id, presentation_mode);
