-- ============================================================================
-- Phase 28 — Project Rooms & Photo Presentation (Elégance Portfolio Evolution)
-- ============================================================================

-- 1. Project Rooms Table (Spatial grouping within projects)
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

-- 2. Row Level Security for Project Rooms
ALTER TABLE project_rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_rooms FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_project_rooms ON project_rooms
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

-- 3. Media Assets Extension for Room Association, Covers, Focal Point, and Motion Opt-Out
ALTER TABLE media_assets
    ADD COLUMN room_id uuid NULL,
    ADD COLUMN is_room_cover boolean NOT NULL DEFAULT false,
    ADD COLUMN focal_x numeric(5,2) NULL DEFAULT 50.00,
    ADD COLUMN focal_y numeric(5,2) NULL DEFAULT 50.00,
    ADD COLUMN motion_enabled boolean NOT NULL DEFAULT true;

ALTER TABLE media_assets
    ADD CONSTRAINT chk_media_assets_focal_x CHECK (focal_x IS NULL OR (focal_x >= 0 AND focal_x <= 100)),
    ADD CONSTRAINT chk_media_assets_focal_y CHECK (focal_y IS NULL OR (focal_y >= 0 AND focal_y <= 100));

ALTER TABLE media_assets
    ADD CONSTRAINT fk_media_assets_room FOREIGN KEY (room_id, project_id, studio_id)
        REFERENCES project_rooms(id, project_id, studio_id) ON DELETE SET NULL (room_id);

CREATE UNIQUE INDEX idx_media_single_room_cover ON media_assets (room_id)
    WHERE is_room_cover = true AND room_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_media_assets_room ON media_assets (room_id)
    WHERE room_id IS NOT NULL AND deleted_at IS NULL;
