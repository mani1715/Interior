-- ============================================================================
-- Phase 7E — Admin Controls, Moderation, Audit & Operations Hardening (Test H2)
-- ============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS suspension_reason text NULL;
ALTER TABLE designer_studios ADD COLUMN IF NOT EXISTS suspension_reason text NULL;
ALTER TABLE studio_projects ADD COLUMN IF NOT EXISTS moderation_status text NOT NULL DEFAULT 'APPROVED';
ALTER TABLE studio_projects ADD COLUMN IF NOT EXISTS moderation_reason text NULL;

CREATE INDEX IF NOT EXISTS idx_audit_events_timestamp ON audit_events (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_action ON audit_events (action);
CREATE INDEX IF NOT EXISTS idx_studio_projects_moderation ON studio_projects (moderation_status);
CREATE INDEX IF NOT EXISTS idx_designer_studios_status ON designer_studios (status);
