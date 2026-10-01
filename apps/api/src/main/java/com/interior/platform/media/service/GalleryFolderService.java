package com.interior.platform.media.service;

import com.interior.platform.common.exception.*;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.service.AuditService;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class GalleryFolderService {
    public record Folder(UUID id, String name, String description, boolean published, int version, List<UUID> mediaIds) {}
    public record Save(@NotBlank @Size(max=80) String name, @NotNull @Size(max=500) String description,
                       boolean published, @Min(0) int version, @NotNull @Size(max=200) List<@NotNull UUID> mediaIds) {}
    public record Photo(String id, String src, String alt, String caption, String kind) {}
    public record PublicFolder(String id, String name, String description, List<Photo> photos) {}
    private final JdbcTemplate db;
    private final MediaService media;
    private final AuditService audit;
    public GalleryFolderService(JdbcTemplate db, MediaService media, AuditService audit) {this.db=db;this.media=media;this.audit=audit;}

    @Transactional(readOnly=true)
    public List<Folder> list(ActorContext actor, UUID requested) {
        UUID studio=media.authorizeGalleryStudio(actor,requested,false);
        return folders(studio);
    }
    private List<Folder> folders(UUID studio) {
        return db.query("SELECT * FROM gallery_folders WHERE studio_id=? ORDER BY name,id",(r,n)->
            new Folder(r.getObject("id",UUID.class),r.getString("name"),r.getString("description"),r.getBoolean("published"),r.getInt("version"),
                db.query("SELECT media_id FROM gallery_folder_media WHERE studio_id=? AND folder_id=? ORDER BY position",(m,i)->m.getObject(1,UUID.class),studio,r.getObject("id",UUID.class))),studio);
    }
    @Transactional
    public Folder save(ActorContext actor, UUID requested, UUID id, Save input) {
        UUID studio=media.authorizeGalleryStudio(actor,requested,true);
        if(input.name()==null || input.name().isBlank() || input.name().trim().length()>80 || input.description()==null || input.description().length()>500 || input.mediaIds()==null || input.mediaIds().size()>200 || new HashSet<>(input.mediaIds()).size()!=input.mediaIds().size())
            throw new BadRequestException("Provide a folder name and up to 200 distinct photos");
        for(UUID photo:input.mediaIds()) {
            Integer count=db.queryForObject("SELECT count(*) FROM media_assets WHERE id=? AND studio_id=? AND deleted_at IS NULL AND processing_status='READY'",Integer.class,photo,studio);
            if(count==null || count!=1) throw new BadRequestException("A selected photo is unavailable in this studio");
        }
        boolean creating=id==null;
        if(creating) {
            if(db.queryForObject("SELECT count(*) FROM gallery_folders WHERE studio_id=?",Integer.class,studio)>=100) throw new BadRequestException("A studio can have up to 100 folders");
            id=UuidV7.randomUuid();
            db.update("INSERT INTO gallery_folders(id,studio_id,name,description,published,version) VALUES(?,?,?,?,?,0)",id,studio,input.name().trim(),input.description().trim(),input.published());
        } else {
            if(db.queryForObject("SELECT count(*) FROM gallery_folders WHERE id=? AND studio_id=?",Integer.class,id,studio)==0) throw new ResourceNotFoundException("Folder not found");
            if(db.update("UPDATE gallery_folders SET name=?,description=?,published=?,version=version+1 WHERE id=? AND studio_id=? AND version=?",input.name().trim(),input.description().trim(),input.published(),id,studio,input.version())!=1)
                throw new ConflictException("Folder changed. Reload before saving again.");
            db.update("DELETE FROM gallery_folder_media WHERE folder_id=? AND studio_id=?",id,studio);
        }
        for(int i=0;i<input.mediaIds().size();i++) db.update("INSERT INTO gallery_folder_media(folder_id,studio_id,media_id,position) VALUES(?,?,?,?)",id,studio,input.mediaIds().get(i),i);
        audit.record(actor.userId(),studio,creating?"GALLERY_FOLDER_CREATED":"GALLERY_FOLDER_UPDATED","GALLERY_FOLDER",id.toString(),Map.of("published",input.published(),"photoCount",input.mediaIds().size()),null,null);
        return new Folder(id,input.name().trim(),input.description().trim(),input.published(),creating?0:input.version()+1,List.copyOf(input.mediaIds()));
    }
    @Transactional(readOnly=true)
    public List<PublicFolder> publicFolders(String slug) {
        return db.query("SELECT f.id,f.studio_id,f.name,f.description FROM gallery_folders f JOIN designer_studios s ON s.id=f.studio_id WHERE s.slug=? AND s.status='ACTIVE' AND s.publication_status='PUBLISHED' AND f.published=true ORDER BY f.name,f.id",(r,n)-> {
            List<Photo> photos=db.query("""
                SELECT m.id,d.public_url,m.alt_text,m.caption,m.media_type FROM gallery_folder_media fm
                JOIN media_assets m ON m.id=fm.media_id AND m.studio_id=fm.studio_id
                JOIN media_derivatives d ON d.media_id=m.id AND d.studio_id=m.studio_id AND d.variant_name='LARGE'
                JOIN studio_projects p ON p.id=m.project_id AND p.studio_id=m.studio_id
                WHERE fm.folder_id=? AND fm.studio_id=? AND m.processing_status='READY' AND m.deleted_at IS NULL
                AND m.visibility IN ('PUBLIC','PORTFOLIO') AND m.media_type IN ('REAL_PROJECT','BEFORE','AFTER','AI_CONCEPT')
                AND p.project_status='READY' AND p.visibility_status='PORTFOLIO' AND p.archived_at IS NULL
                ORDER BY fm.position
                """,(m,i)->new Photo(m.getString("id"),m.getString("public_url"),m.getString("alt_text"),m.getString("caption"),m.getString("media_type")),r.getObject("id",UUID.class),r.getObject("studio_id",UUID.class));
            return new PublicFolder(r.getString("id"),r.getString("name"),r.getString("description"),photos);
        },slug).stream().filter(f->!f.photos().isEmpty()).toList();
    }
}
