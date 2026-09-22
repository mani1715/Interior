-- ============================================================================
-- Phase 22 — AI Reference Image System Schema (H2 Test Parity)
-- ============================================================================

-- 1. Add preserve_structure column to ai_visualization_jobs
ALTER TABLE ai_visualization_jobs
    ADD COLUMN preserve_structure boolean NOT NULL DEFAULT true;

-- 2. AI Reference Metadata Table (Studio Reference Library)
CREATE TABLE ai_reference_metadata (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    media_id uuid NOT NULL,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    project_id uuid NULL,
    purpose text NOT NULL
        CHECK (purpose IN (
            'COLOR', 'MATERIAL', 'WOOD', 'STONE', 'TILE',
            'FABRIC', 'HARDWARE', 'FURNITURE_STYLE', 'CABINET_STYLE',
            'ROOM_STYLE', 'WALL_FINISH', 'CEILING_STYLE', 'GENERAL_STYLE'
        )),
    label varchar(100) NULL,
    default_instruction varchar(300) NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    archived_at timestamptz NULL,
    CONSTRAINT fk_ai_ref_media FOREIGN KEY (media_id, studio_id)
        REFERENCES media_assets(id, studio_id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_ref_project FOREIGN KEY (project_id, studio_id)
        REFERENCES studio_projects(id, studio_id) ON DELETE SET NULL,
    CONSTRAINT uq_ai_ref_studio_media UNIQUE (studio_id, media_id)
);

CREATE INDEX idx_ai_ref_studio ON ai_reference_metadata(studio_id);
CREATE INDEX idx_ai_ref_studio_purpose ON ai_reference_metadata(studio_id, purpose);
CREATE INDEX idx_ai_ref_studio_archived ON ai_reference_metadata(studio_id, archived_at);
CREATE INDEX idx_ai_ref_project ON ai_reference_metadata(studio_id, project_id);

-- 3. AI Job References Table (Immutable Snapshot per Generation Job)
CREATE TABLE ai_job_references (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    job_id uuid NOT NULL REFERENCES ai_visualization_jobs(id) ON DELETE CASCADE,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    media_id uuid NOT NULL,
    purpose_snapshot text NOT NULL,
    label_snapshot varchar(100) NULL,
    instruction_snapshot varchar(300) NULL,
    display_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_ai_job_ref_media FOREIGN KEY (media_id, studio_id)
        REFERENCES media_assets(id, studio_id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_job_ref_job ON ai_job_references(job_id);
CREATE INDEX idx_ai_job_ref_studio ON ai_job_references(studio_id);
