-- ============================================================================
-- Phase 7E — Admin Controls, Moderation, Audit & Operations Hardening (V033)
-- ============================================================================

-- 1. Suspension reason columns on users and designer studios
ALTER TABLE users ADD COLUMN IF NOT EXISTS suspension_reason text NULL;
ALTER TABLE designer_studios ADD COLUMN IF NOT EXISTS suspension_reason text NULL;

-- 2. Moderation fields on studio_projects
ALTER TABLE studio_projects ADD COLUMN IF NOT EXISTS moderation_status text NOT NULL DEFAULT 'APPROVED';
ALTER TABLE studio_projects DROP CONSTRAINT IF EXISTS studio_projects_moderation_status_check;
ALTER TABLE studio_projects ADD CONSTRAINT studio_projects_moderation_status_check
    CHECK (moderation_status IN ('APPROVED', 'FLAGGED', 'HIDDEN'));

ALTER TABLE studio_projects ADD COLUMN IF NOT EXISTS moderation_reason text NULL;

-- 3. Indexes for fast administrative queries, moderation filtering, and audit log analysis
CREATE INDEX IF NOT EXISTS idx_audit_events_timestamp ON audit_events (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_action ON audit_events (action);
CREATE INDEX IF NOT EXISTS idx_studio_projects_moderation ON studio_projects (moderation_status);
CREATE INDEX IF NOT EXISTS idx_designer_studios_status ON designer_studios (status);

-- 4. Tighten public read RLS policy on studio_projects to require moderation_status = 'APPROVED'
DROP POLICY IF EXISTS public_read_studio_projects ON studio_projects;
CREATE POLICY public_read_studio_projects ON studio_projects
    FOR SELECT
    USING (
        project_status = 'READY'
        AND visibility_status = 'PORTFOLIO'
        AND archived_at IS NULL
        AND moderation_status = 'APPROVED'
        AND EXISTS (
            SELECT 1 FROM designer_studios s
            WHERE s.id = studio_projects.studio_id
              AND s.publication_status = 'PUBLISHED'
              AND s.status = 'ACTIVE'
        )
    );
