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

