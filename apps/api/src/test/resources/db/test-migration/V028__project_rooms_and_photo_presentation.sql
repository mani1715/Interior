-- ============================================================================
-- Phase 28 — Project Rooms & Photo Presentation (H2 Test Compatible)
-- ============================================================================

-- 1. Project Rooms Table
CREATE TABLE project_rooms (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    project_id uuid NOT NULL,
    room_type varchar(64) NOT NULL
        CHECK (room_type IN (
            'LIVING_ROOM',
            'KITCHEN',
            'BEDROOM',
            'DINING',
            'BATHROOM',
            'POOJA',
            'HOME_OFFICE',
            'BALCONY_TERRACE',
            'FOYER',
            'WARDROBE_DRESSER',
            'OTHER'
        )),
    display_name varchar(128) NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_project_rooms_project FOREIGN KEY (project_id, studio_id)
        REFERENCES studio_projects(id, studio_id) ON DELETE CASCADE,
    CONSTRAINT uq_project_rooms_id_project_studio UNIQUE (id, project_id, studio_id)
);

CREATE INDEX idx_project_rooms_project ON project_rooms (project_id, sort_order ASC);
CREATE INDEX idx_project_rooms_studio ON project_rooms (studio_id);

-- 2. Media Assets Extension for Room Association, Covers, Focal Point, and Motion Opt-Out
ALTER TABLE media_assets
    ADD COLUMN room_id uuid NULL;
ALTER TABLE media_assets
    ADD COLUMN is_room_cover boolean NOT NULL DEFAULT false;
ALTER TABLE media_assets
    ADD COLUMN focal_x numeric(5,2) NULL DEFAULT 50.00;
ALTER TABLE media_assets
    ADD COLUMN focal_y numeric(5,2) NULL DEFAULT 50.00;
ALTER TABLE media_assets
    ADD COLUMN motion_enabled boolean NOT NULL DEFAULT true;

ALTER TABLE media_assets
    ADD CONSTRAINT chk_media_assets_focal_x CHECK (focal_x IS NULL OR (focal_x >= 0 AND focal_x <= 100));
ALTER TABLE media_assets
    ADD CONSTRAINT chk_media_assets_focal_y CHECK (focal_y IS NULL OR (focal_y >= 0 AND focal_y <= 100));

ALTER TABLE media_assets
    ADD CONSTRAINT fk_media_assets_room FOREIGN KEY (room_id)
        REFERENCES project_rooms(id) ON DELETE SET NULL;

CREATE INDEX idx_media_assets_room ON media_assets (room_id);
