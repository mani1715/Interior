-- ============================================================================
-- Phase 19 — Media Engine & Watermarks Schema (H2 Test Compatible)
-- ============================================================================

-- 1. Studio Watermark Settings Table
CREATE TABLE studio_watermark_settings (
    studio_id uuid NOT NULL PRIMARY KEY REFERENCES designer_studios(id) ON DELETE CASCADE,
    enabled boolean NOT NULL DEFAULT true,
    position text NOT NULL DEFAULT 'BOTTOM_RIGHT'
        CHECK (position IN ('TOP_LEFT', 'TOP_RIGHT', 'BOTTOM_LEFT', 'BOTTOM_RIGHT', 'CENTER')),
    opacity numeric(3,2) NOT NULL DEFAULT 0.60
        CHECK (opacity >= 0.10 AND opacity <= 1.00),
    size_percentage integer NOT NULL DEFAULT 15
        CHECK (size_percentage >= 5 AND size_percentage <= 30),
    use_logo boolean NOT NULL DEFAULT false,
    fallback_text text NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- 2. Upload Intents Table (Direct storage upload contract)
CREATE TABLE upload_intents (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    project_id uuid NOT NULL,
    media_type text NOT NULL
        CHECK (media_type IN ('REAL_PROJECT', 'BEFORE', 'AFTER', 'AI_CONCEPT', 'REFERENCE', 'CLIENT_PRIVATE')),
    expected_content_type text NOT NULL
        CHECK (expected_content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    expected_size_bytes bigint NOT NULL
        CHECK (expected_size_bytes > 0 AND expected_size_bytes <= 26214400),
    quarantine_key text NOT NULL,
    status text NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'COMMITTED', 'EXPIRED', 'FAILED')),
    expires_at timestamptz NOT NULL,
    created_by uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_upload_intents_project FOREIGN KEY (project_id, studio_id)
        REFERENCES studio_projects(id, studio_id) ON DELETE CASCADE
);

-- 3. Media Assets Table (Private canonical originals)
CREATE TABLE media_assets (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    project_id uuid NOT NULL,
    media_type text NOT NULL
        CHECK (media_type IN ('REAL_PROJECT', 'BEFORE', 'AFTER', 'AI_CONCEPT', 'REFERENCE', 'CLIENT_PRIVATE')),
    visibility text NOT NULL DEFAULT 'PORTFOLIO'
        CHECK (visibility IN ('PRIVATE', 'PORTFOLIO', 'PUBLIC')),
    processing_status text NOT NULL DEFAULT 'PENDING_UPLOAD'
        CHECK (processing_status IN ('PENDING_UPLOAD', 'UPLOADED', 'PROCESSING', 'READY', 'FAILED', 'QUARANTINED', 'DELETED')),
    original_storage_key text NOT NULL,
    content_type text NOT NULL,
    file_size bigint NOT NULL CHECK (file_size >= 0),
    width integer NOT NULL CHECK (width >= 0),
    height integer NOT NULL CHECK (height >= 0),
    sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_cover boolean NOT NULL DEFAULT false,
    alt_text text NULL,
    caption text NULL,
    watermark_enabled boolean NOT NULL DEFAULT true,
    created_by uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz NULL,
    CONSTRAINT fk_media_assets_project FOREIGN KEY (project_id, studio_id)
        REFERENCES studio_projects(id, studio_id) ON DELETE CASCADE,
    CONSTRAINT uq_media_assets_id_studio UNIQUE (id, studio_id),
    CONSTRAINT chk_media_privacy CHECK (
        NOT (media_type IN ('REFERENCE', 'CLIENT_PRIVATE') AND visibility IN ('PORTFOLIO', 'PUBLIC'))
    )
);

-- 4. Media Derivatives Table (Optimized, watermarked public variants)
CREATE TABLE media_derivatives (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    media_id uuid NOT NULL,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    variant_name text NOT NULL
        CHECK (variant_name IN ('THUMBNAIL', 'MEDIUM', 'LARGE')),
    width integer NOT NULL CHECK (width > 0),
    height integer NOT NULL CHECK (height > 0),
    format text NOT NULL,
    file_size bigint NOT NULL CHECK (file_size >= 0),
    storage_key text NOT NULL,
    public_url text NOT NULL,
    is_watermarked boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_media_derivatives_media FOREIGN KEY (media_id, studio_id)
        REFERENCES media_assets(id, studio_id) ON DELETE CASCADE,
    CONSTRAINT uq_media_derivatives_variant UNIQUE (media_id, variant_name)
);

-- Indexes
CREATE INDEX idx_studio_watermark_settings_studio ON studio_watermark_settings(studio_id);
CREATE INDEX idx_upload_intents_studio_project ON upload_intents(studio_id, project_id);
CREATE INDEX idx_upload_intents_status ON upload_intents(status);
CREATE INDEX idx_media_assets_studio_project ON media_assets(studio_id, project_id);
CREATE INDEX idx_media_assets_type ON media_assets(studio_id, media_type);
CREATE INDEX idx_media_assets_sort ON media_assets(project_id, sort_order);
CREATE INDEX idx_media_assets_cover ON media_assets(project_id, is_cover);
CREATE INDEX idx_media_derivatives_media ON media_derivatives(media_id);
