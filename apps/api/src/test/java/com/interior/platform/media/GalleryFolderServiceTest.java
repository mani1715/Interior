package com.interior.platform.media;

import com.interior.platform.media.service.*;
import com.interior.platform.media.service.GalleryFolderService.*;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.common.exception.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class GalleryFolderServiceTest {
    EmbeddedDatabase database; JdbcTemplate db; GalleryFolderService service;
    MediaService media; UUID studio=UUID.randomUUID(),other=UUID.randomUUID(),project=UUID.randomUUID(),photo=UUID.randomUUID();
    ActorContext actor=ActorContext.anonymous();
    @BeforeEach void setup(){
        database=new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();db=new JdbcTemplate(database);
        db.execute("CREATE TABLE designer_studios(id uuid PRIMARY KEY,slug varchar,status varchar,publication_status varchar)");
        db.execute("CREATE TABLE studio_projects(id uuid PRIMARY KEY,studio_id uuid,project_status varchar,visibility_status varchar,archived_at timestamp)");
        db.execute("CREATE TABLE media_assets(id uuid,studio_id uuid,project_id uuid,processing_status varchar,deleted_at timestamp,visibility varchar,media_type varchar,alt_text varchar,caption varchar,UNIQUE(id,studio_id))");
        db.execute("CREATE TABLE media_derivatives(media_id uuid,studio_id uuid,variant_name varchar,public_url varchar)");
        new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new org.springframework.core.io.ClassPathResource("db/test-migration/V022__gallery_folders.sql")).execute(database);
        db.update("INSERT INTO designer_studios VALUES(?,?,?,?)",studio,"studio","ACTIVE","PUBLISHED");
        db.update("INSERT INTO designer_studios VALUES(?,?,?,?)",other,"other","ACTIVE","PUBLISHED");
        db.update("INSERT INTO studio_projects VALUES(?,?,?,?,null)",project,studio,"READY","PORTFOLIO");
        db.update("INSERT INTO media_assets VALUES(?,?,?,?,null,?,?,?,?)",photo,studio,project,"READY","PORTFOLIO","REAL_PROJECT","A room","Finished room");
        db.update("INSERT INTO media_derivatives VALUES(?,?,?,?)",photo,studio,"LARGE","https://example.com/watermarked.jpg");
        media=mock(MediaService.class);when(media.authorizeGalleryStudio(any(),any(),anyBoolean())).thenReturn(studio);
        service=new GalleryFolderService(db,media,mock(AuditService.class));
    }
    @AfterEach void close(){database.shutdown();}
    Save input(boolean published,int version,List<UUID> ids){return new Save("Kitchens","Material details",published,version,ids);}
    @Test void storesFoldersAndRejectsStaleEdits(){
        Folder f=service.save(actor,null,null,input(false,0,List.of(photo)));
        assertEquals(List.of(photo),service.list(actor,null).get(0).mediaIds());
        Folder updated=service.save(actor,null,f.id(),input(true,0,List.of(photo)));
        assertEquals(1,updated.version());
        assertThrows(ConflictException.class,()->service.save(actor,null,f.id(),input(false,0,List.of())));
        assertEquals(List.of(photo),service.list(actor,null).get(0).mediaIds());
    }
    @Test void rejectsForeignMediaAndForeignFolder(){
        UUID foreign=UUID.randomUUID();db.update("INSERT INTO media_assets VALUES(?,?,?,?,null,?,?,?,?)",foreign,other,project,"READY","PORTFOLIO","REAL_PROJECT","Other","Other");
        assertThrows(BadRequestException.class,()->service.save(actor,null,null,input(false,0,List.of(foreign))));
        UUID folder=UUID.randomUUID();db.update("INSERT INTO gallery_folders VALUES(?,?,?,?,?,?)",folder,other,"Other","",false,0);
        assertThrows(ResourceNotFoundException.class,()->service.save(actor,null,folder,input(false,0,List.of(photo))));
    }
    @Test void publicGalleryRequiresEveryPublicationGate(){
        Folder f=service.save(actor,null,null,input(false,0,List.of(photo)));
        assertTrue(service.publicFolders("studio").isEmpty());
        service.save(actor,null,f.id(),input(true,0,List.of(photo)));
        assertEquals("https://example.com/watermarked.jpg",service.publicFolders("studio").get(0).photos().get(0).src());
        for(String gate:List.of("visibility='PRIVATE'","processing_status='FAILED'","media_type='CLIENT_PRIVATE'","deleted_at=CURRENT_TIMESTAMP")){
            db.update("UPDATE media_assets SET "+gate+" WHERE id=?",photo);
            assertTrue(service.publicFolders("studio").isEmpty(),gate);
            db.update("UPDATE media_assets SET visibility='PORTFOLIO',processing_status='READY',media_type='REAL_PROJECT',deleted_at=null WHERE id=?",photo);
        }
        db.update("UPDATE studio_projects SET project_status='DRAFT'");assertTrue(service.publicFolders("studio").isEmpty());
        db.update("UPDATE studio_projects SET project_status='READY',visibility_status='PRIVATE'");assertTrue(service.publicFolders("studio").isEmpty());
        db.update("UPDATE studio_projects SET visibility_status='PORTFOLIO',archived_at=CURRENT_TIMESTAMP");assertTrue(service.publicFolders("studio").isEmpty());
        db.update("UPDATE studio_projects SET archived_at=null");db.update("UPDATE designer_studios SET publication_status='DRAFT'");assertTrue(service.publicFolders("studio").isEmpty());
    }
    @Test void removingMembershipKeepsOriginal(){Folder f=service.save(actor,null,null,input(false,0,List.of(photo)));service.save(actor,null,f.id(),input(false,0,List.of()));assertEquals(1,db.queryForObject("SELECT count(*) FROM media_assets",Integer.class));}
}
