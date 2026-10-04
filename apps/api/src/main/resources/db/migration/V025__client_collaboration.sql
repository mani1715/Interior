-- ============================================================================
-- V025: Client Collaboration Annotations & Revision Workflow
-- Canonical: Normalized Image Coordinates, Threaded Pin Feedback & Bundle Revisions
-- ============================================================================

-- 1. Extend ai_client_reviews with revision round and preferred job
ALTER TABLE ai_client_reviews
    ADD COLUMN IF NOT EXISTS revision_round integer NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS preferred_job_id uuid NULL REFERENCES ai_visualization_jobs(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_ai_reviews_preferred_job ON ai_client_reviews(preferred_job_id);

-- 2. Client Concept Annotations (Numbered Pin-Point Spatial Notes)
CREATE TABLE IF NOT EXISTS ai_client_review_annotations (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    review_id uuid NOT NULL REFERENCES ai_client_reviews(id) ON DELETE CASCADE,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    job_id uuid NOT NULL REFERENCES ai_visualization_jobs(id) ON DELETE CASCADE,
    media_id uuid NOT NULL,
    pin_number integer NOT NULL DEFAULT 1,
    coord_x double precision NOT NULL CHECK (coord_x >= 0.0 AND coord_x <= 1.0),
    coord_y double precision NOT NULL CHECK (coord_y >= 0.0 AND coord_y <= 1.0),
    author_type text NOT NULL CHECK (author_type IN ('CLIENT', 'STUDIO')),
    author_name varchar(100) NOT NULL,
    comment_text varchar(1000) NOT NULL,
    is_change_request boolean NOT NULL DEFAULT false,
    resolved_at timestamptz NULL,
    resolved_by varchar(100) NULL,
    parent_annotation_id uuid NULL REFERENCES ai_client_review_annotations(id) ON DELETE CASCADE,
    revision_round integer NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_ai_annotation_media FOREIGN KEY (media_id, studio_id)
        REFERENCES media_assets(id, studio_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_annotations_review ON ai_client_review_annotations(review_id);
CREATE INDEX IF NOT EXISTS idx_ai_annotations_job ON ai_client_review_annotations(job_id);
CREATE INDEX IF NOT EXISTS idx_ai_annotations_studio ON ai_client_review_annotations(studio_id);
CREATE INDEX IF NOT EXISTS idx_ai_annotations_parent ON ai_client_review_annotations(parent_annotation_id);

-- 3. Row-Level Security (RLS) for Tenant Isolation
ALTER TABLE ai_client_review_annotations ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_client_review_annotations FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_ai_client_review_annotations ON ai_client_review_annotations;
CREATE POLICY tenant_isolation_ai_client_review_annotations ON ai_client_review_annotations
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );
