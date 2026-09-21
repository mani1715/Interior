-- ============================================================================
-- Phase 20 — SEO Engine Schema (H2 Test Parity)
-- ============================================================================

-- 1. Add publication timestamp to designer_studios
ALTER TABLE designer_studios
    ADD COLUMN published_at timestamptz NULL;

-- 2. Studio SEO Settings Table
CREATE TABLE studio_seo_settings (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL UNIQUE REFERENCES designer_studios(id) ON DELETE CASCADE,
    meta_title_override text NULL CHECK (meta_title_override IS NULL OR length(meta_title_override) <= 120),
    meta_description_override text NULL CHECK (meta_description_override IS NULL OR length(meta_description_override) <= 300),
    canonical_url_override text NULL CHECK (canonical_url_override IS NULL OR length(canonical_url_override) <= 500),
    indexing_enabled boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_studio_seo_settings_studio UNIQUE (studio_id)
);

CREATE INDEX idx_studio_seo_settings_studio ON studio_seo_settings(studio_id);
