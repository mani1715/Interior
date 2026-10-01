package com.interior.platform.media.web;

import com.interior.platform.media.service.GalleryFolderService;
import com.interior.platform.media.service.GalleryFolderService.*;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class GalleryFolderController {
    private final GalleryFolderService service;
    public GalleryFolderController(GalleryFolderService service){this.service=service;}
    private ActorContext actor(HttpServletRequest r){Object a=r.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);return a instanceof ActorContext c?c:ActorContext.anonymous();}
    private <T> ResponseEntity<T> response(T value){return ResponseEntity.ok().header("Cache-Control","private, no-store").body(value);}
    @GetMapping("/gallery-folders")
    public ResponseEntity<List<Folder>> list(HttpServletRequest r,@RequestParam(required=false) UUID studioId){return response(service.list(actor(r),studioId));}
    @PostMapping("/gallery-folders")
    public ResponseEntity<Folder> create(HttpServletRequest r,@RequestParam(required=false) UUID studioId,@Valid @RequestBody Save data){return response(service.save(actor(r),studioId,null,data));}
    @PutMapping("/gallery-folders/{id}")
    public ResponseEntity<Folder> update(HttpServletRequest r,@RequestParam(required=false) UUID studioId,@PathVariable UUID id,@Valid @RequestBody Save data){return response(service.save(actor(r),studioId,id,data));}
    @GetMapping("/public/studios/{slug}/gallery")
    public ResponseEntity<List<PublicFolder>> publicGallery(@PathVariable String slug){return response(service.publicFolders(slug));}
}
