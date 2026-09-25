package com.interior.platform.collections.web;

import com.interior.platform.collections.dto.*;
import com.interior.platform.collections.service.UserCollectionService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/account/collections")
@Tag(name = "User Inspiration Collections", description = "Endpoints for logged-in user private saved project boards and inspiration collections")
public class UserCollectionController {

    private final UserCollectionService collectionService;

    public UserCollectionController(UserCollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping
    @Operation(summary = "List user collections", description = "Lists personal inspiration collections for the authenticated user.")
    public ResponseEntity<List<UserCollectionDto>> listCollections(HttpServletRequest request) {
        ActorContext actor = extractActor(request);
        List<UserCollectionDto> list = collectionService.listCollections(actor);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(list);
    }

    @PostMapping
    @Operation(summary = "Create custom collection", description = "Creates a new named inspiration board for the authenticated user.")
    public ResponseEntity<UserCollectionDto> createCollection(
            HttpServletRequest request,
            @Valid @RequestBody CreateCollectionRequest body
    ) {
        ActorContext actor = extractActor(request);
        UserCollectionDto created = collectionService.createCollection(actor, body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get collection details", description = "Returns collection metadata and resolved project item cards with tombstones for private/unpublished projects.")
    public ResponseEntity<CollectionDetailDto> getCollectionDetail(
            HttpServletRequest request,
            @PathVariable("id") UUID id
    ) {
        ActorContext actor = extractActor(request);
        CollectionDetailDto detail = collectionService.getCollectionDetail(actor, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(detail);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update collection metadata", description = "Updates title or description of a personal collection.")
    public ResponseEntity<UserCollectionDto> updateCollection(
            HttpServletRequest request,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCollectionRequest body
    ) {
        ActorContext actor = extractActor(request);
        UserCollectionDto updated = collectionService.updateCollection(actor, id, body);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete custom collection", description = "Deletes a custom collection and its saved items.")
    public ResponseEntity<Void> deleteCollection(
            HttpServletRequest request,
            @PathVariable("id") UUID id
    ) {
        ActorContext actor = extractActor(request);
        collectionService.deleteCollection(actor, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items")
    @Operation(summary = "Save public project to collection", description = "Saves an active public portfolio project to the default or chosen user collection.")
    public ResponseEntity<CollectionItemDto> saveProject(
            HttpServletRequest request,
            @Valid @RequestBody SaveProjectRequest body
    ) {
        ActorContext actor = extractActor(request);
        CollectionItemDto item = collectionService.saveProject(actor, body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(item);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove project from collection", description = "Removes a saved project item from the user's collection.")
    public ResponseEntity<Void> removeItem(
            HttpServletRequest request,
            @PathVariable("itemId") UUID itemId
    ) {
        ActorContext actor = extractActor(request);
        collectionService.removeItem(actor, itemId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/items/{itemId}/note")
    @Operation(summary = "Update private note on saved item", description = "Updates personal private note on a saved project card.")
    public ResponseEntity<Void> updateNote(
            HttpServletRequest request,
            @PathVariable("itemId") UUID itemId,
            @Valid @RequestBody UpdateItemNoteRequest body
    ) {
        ActorContext actor = extractActor(request);
        collectionService.updateNote(actor, itemId, body);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reorder")
    @Operation(summary = "Reorder items in collection", description = "Reorders collection item display sequence.")
    public ResponseEntity<Void> reorderItems(
            HttpServletRequest request,
            @PathVariable("id") UUID id,
            @Valid @RequestBody ReorderItemsRequest body
    ) {
        ActorContext actor = extractActor(request);
        collectionService.reorderItems(actor, id, body);
        return ResponseEntity.noContent().build();
    }

    private ActorContext extractActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return actor != null ? actor : ActorContext.anonymous();
    }
}
