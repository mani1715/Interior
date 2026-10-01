-- Custom studio folders reference media without copying it or changing visibility.
CREATE TABLE gallery_folders (
 id uuid PRIMARY KEY, studio_id uuid NOT NULL REFERENCES designer_studios(id),
 name varchar(80) NOT NULL, description varchar(500) NOT NULL DEFAULT '',
 published boolean NOT NULL DEFAULT false, version integer NOT NULL DEFAULT 0,
 UNIQUE(id, studio_id)
);
CREATE TABLE gallery_folder_media (
 folder_id uuid NOT NULL, studio_id uuid NOT NULL, media_id uuid NOT NULL,
 position integer NOT NULL CHECK(position >= 0), PRIMARY KEY(folder_id, media_id),
 FOREIGN KEY(folder_id,studio_id) REFERENCES gallery_folders(id,studio_id) ON DELETE CASCADE,
 FOREIGN KEY(media_id,studio_id) REFERENCES media_assets(id,studio_id) ON DELETE CASCADE
);
CREATE INDEX idx_gallery_studio ON gallery_folders(studio_id);
ALTER TABLE gallery_folders ENABLE ROW LEVEL SECURITY;
ALTER TABLE gallery_folders FORCE ROW LEVEL SECURITY;
ALTER TABLE gallery_folder_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE gallery_folder_media FORCE ROW LEVEL SECURITY;
CREATE POLICY gallery_folder_tenant ON gallery_folders FOR ALL USING (
 studio_id = NULLIF(current_setting('app.current_studio_id',true),'')::uuid OR current_setting('app.is_admin',true)='true'
);
CREATE POLICY gallery_media_tenant ON gallery_folder_media FOR ALL USING (
 studio_id = NULLIF(current_setting('app.current_studio_id',true),'')::uuid OR current_setting('app.is_admin',true)='true'
);
CREATE POLICY gallery_folder_public ON gallery_folders FOR SELECT USING (
 published AND EXISTS (SELECT 1 FROM designer_studios s WHERE s.id=studio_id AND s.status='ACTIVE' AND s.publication_status='PUBLISHED')
);
CREATE POLICY gallery_media_public ON gallery_folder_media FOR SELECT USING (
 EXISTS (SELECT 1 FROM gallery_folders f WHERE f.id=folder_id AND f.studio_id=gallery_folder_media.studio_id AND f.published)
 AND EXISTS (SELECT 1 FROM media_assets m JOIN studio_projects p ON p.id=m.project_id AND p.studio_id=m.studio_id
 JOIN designer_studios s ON s.id=p.studio_id WHERE m.id=media_id AND m.studio_id=gallery_folder_media.studio_id
 AND m.deleted_at IS NULL AND m.processing_status='READY' AND m.visibility IN ('PUBLIC','PORTFOLIO')
 AND m.media_type IN ('REAL_PROJECT','BEFORE','AFTER','AI_CONCEPT')
 AND p.project_status='READY' AND p.visibility_status='PORTFOLIO' AND p.archived_at IS NULL
 AND s.status='ACTIVE' AND s.publication_status='PUBLISHED')
);
