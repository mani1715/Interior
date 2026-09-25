package com.interior.platform.collections.service;

import com.interior.platform.collections.domain.CollectionItemRecord;
import com.interior.platform.collections.domain.UserCollectionRecord;
import com.interior.platform.collections.dto.*;
import com.interior.platform.collections.repository.UserCollectionRepository;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.security.domain.ActorContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class UserCollectionService {

    private static final int MAX_COLLECTIONS_PER_USER = 50;
    private static final int MAX_ITEMS_PER_COLLECTION = 500;
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    private final UserCollectionRepository collectionRepository;
    private final JdbcTemplate jdbcTemplate;

    public UserCollectionService(UserCollectionRepository collectionRepository, JdbcTemplate jdbcTemplate) {
        this.collectionRepository = collectionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public List<UserCollectionDto> listCollections(ActorContext actor) {
        UUID userId = requireAuthenticatedUserId(actor);
        ensureDefaultCollection(userId);

        List<UserCollectionRecord> records = collectionRepository.listCollections(userId);
        return records.stream()
                .map(r -> {
                    int count = collectionRepository.countItems(r.id());
                    return toDto(r, count);
                })
                .toList();
    }

    @Transactional
    public UserCollectionDto createCollection(ActorContext actor, CreateCollectionRequest req) {
        UUID userId = requireAuthenticatedUserId(actor);

        int currentCount = collectionRepository.countCollections(userId);
        if (currentCount >= MAX_COLLECTIONS_PER_USER) {
            throw new IllegalStateException("Maximum limit of " + MAX_COLLECTIONS_PER_USER + " collections reached.");
        }

        String cleanTitle = stripHtml(req.title());
        if (cleanTitle.isEmpty() || cleanTitle.length() > 100) {
            throw new IllegalArgumentException("Collection title must be between 1 and 100 characters.");
        }

        String cleanDesc = req.description() != null ? stripHtml(req.description()).trim() : null;
        if (cleanDesc != null && cleanDesc.length() > 500) {
            cleanDesc = cleanDesc.substring(0, 500);
        }

        Instant now = Instant.now();
        UserCollectionRecord record = new UserCollectionRecord(
                UuidV7.randomUuid(),
                userId,
                cleanTitle,
                cleanDesc,
                false,
                now,
                now,
                1L
        );

        collectionRepository.save(record);
        return toDto(record, 0);
    }

    public CollectionDetailDto getCollectionDetail(ActorContext actor, UUID collectionId) {
        UUID userId = requireAuthenticatedUserId(actor);

        UserCollectionRecord collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found: " + collectionId));

        if (!collection.ownerUserId().equals(userId)) {
            throw new AccessDeniedException("Access to this collection is denied");
        }

        List<CollectionItemDto> items = collectionRepository.listItemsWithProjection(collectionId);
        int count = items.size();

        return new CollectionDetailDto(toDto(collection, count), items);
    }

    @Transactional
    public UserCollectionDto updateCollection(ActorContext actor, UUID collectionId, UpdateCollectionRequest req) {
        UUID userId = requireAuthenticatedUserId(actor);

        UserCollectionRecord collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found: " + collectionId));

        if (!collection.ownerUserId().equals(userId)) {
            throw new AccessDeniedException("Access to this collection is denied");
        }

        String cleanTitle = stripHtml(req.title());
        if (cleanTitle.isEmpty() || cleanTitle.length() > 100) {
            throw new IllegalArgumentException("Collection title must be between 1 and 100 characters.");
        }

        String cleanDesc = req.description() != null ? stripHtml(req.description()).trim() : null;
        if (cleanDesc != null && cleanDesc.length() > 500) {
            cleanDesc = cleanDesc.substring(0, 500);
        }

        collectionRepository.updateCollection(collectionId, userId, cleanTitle, cleanDesc);

        UserCollectionRecord updated = collectionRepository.findById(collectionId).orElseThrow();
        int count = collectionRepository.countItems(collectionId);
        return toDto(updated, count);
    }

    @Transactional
    public void deleteCollection(ActorContext actor, UUID collectionId) {
        UUID userId = requireAuthenticatedUserId(actor);

        UserCollectionRecord collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found: " + collectionId));

        if (!collection.ownerUserId().equals(userId)) {
            throw new AccessDeniedException("Access to this collection is denied");
        }

        if (collection.isDefault()) {
            throw new IllegalStateException("The default 'Saved' collection cannot be deleted.");
        }

        collectionRepository.deleteCollection(collectionId, userId);
    }

    @Transactional
    public CollectionItemDto saveProject(ActorContext actor, SaveProjectRequest req) {
        UUID userId = requireAuthenticatedUserId(actor);

        // Verify project is currently public and eligible
        validateProjectIsPublic(req.projectId());

        // Resolve collection (target or default)
        UserCollectionRecord targetCollection;
        if (req.collectionId() != null) {
            targetCollection = collectionRepository.findById(req.collectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Collection not found: " + req.collectionId()));
            if (!targetCollection.ownerUserId().equals(userId)) {
                throw new AccessDeniedException("Access to this collection is denied");
            }
        } else {
            targetCollection = ensureDefaultCollection(userId);
        }

        // Check if project already in this collection
        Optional<CollectionItemRecord> existing = collectionRepository.findItemByProject(targetCollection.id(), req.projectId());
        if (existing.isPresent()) {
            CollectionItemRecord item = existing.get();
            if (req.note() != null && !req.note().isBlank()) {
                String cleanNote = stripHtml(req.note());
                collectionRepository.updateItemNote(item.id(), userId, cleanNote);
            }
            List<CollectionItemDto> list = collectionRepository.listItemsWithProjection(targetCollection.id());
            return list.stream().filter(i -> i.id().equals(item.id())).findFirst().orElseThrow();
        }

        int currentItemCount = collectionRepository.countItems(targetCollection.id());
        if (currentItemCount >= MAX_ITEMS_PER_COLLECTION) {
            throw new IllegalStateException("Maximum limit of " + MAX_ITEMS_PER_COLLECTION + " items reached for this collection.");
        }

        String cleanNote = req.note() != null ? stripHtml(req.note()).trim() : null;
        if (cleanNote != null && cleanNote.length() > 500) {
            cleanNote = cleanNote.substring(0, 500);
        }

        UUID itemId = UuidV7.randomUuid();
        CollectionItemRecord newItem = new CollectionItemRecord(
                itemId,
                targetCollection.id(),
                userId,
                req.projectId(),
                cleanNote,
                currentItemCount + 1,
                Instant.now()
        );

        collectionRepository.saveItem(newItem);

        List<CollectionItemDto> list = collectionRepository.listItemsWithProjection(targetCollection.id());
        return list.stream().filter(i -> i.id().equals(itemId)).findFirst().orElseThrow();
    }

    @Transactional
    public void removeItem(ActorContext actor, UUID itemId) {
        UUID userId = requireAuthenticatedUserId(actor);

        CollectionItemRecord item = collectionRepository.findItemById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection item not found: " + itemId));

        if (!item.ownerUserId().equals(userId)) {
            throw new AccessDeniedException("Access to this item is denied");
        }

        collectionRepository.deleteItem(itemId, userId);
    }

    @Transactional
    public void updateNote(ActorContext actor, UUID itemId, UpdateItemNoteRequest req) {
        UUID userId = requireAuthenticatedUserId(actor);

        CollectionItemRecord item = collectionRepository.findItemById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection item not found: " + itemId));

        if (!item.ownerUserId().equals(userId)) {
            throw new AccessDeniedException("Access to this item is denied");
        }

        String cleanNote = req.note() != null ? stripHtml(req.note()).trim() : null;
        if (cleanNote != null && cleanNote.length() > 500) {
            cleanNote = cleanNote.substring(0, 500);
        }

        collectionRepository.updateItemNote(itemId, userId, cleanNote);
    }

    @Transactional
    public void reorderItems(ActorContext actor, UUID collectionId, ReorderItemsRequest req) {
        UUID userId = requireAuthenticatedUserId(actor);

        UserCollectionRecord collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found: " + collectionId));

        if (!collection.ownerUserId().equals(userId)) {
            throw new AccessDeniedException("Access to this collection is denied");
        }

        List<UUID> itemIds = req.itemIds();
        for (int i = 0; i < itemIds.size(); i++) {
            collectionRepository.updateItemDisplayOrder(itemIds.get(i), collectionId, i + 1);
        }
    }

    public UserCollectionRecord ensureDefaultCollection(UUID userId) {
        return collectionRepository.findDefaultCollection(userId).orElseGet(() -> {
            Instant now = Instant.now();
            UserCollectionRecord def = new UserCollectionRecord(
                    UuidV7.randomUuid(),
                    userId,
                    "Saved",
                    "Default collection for saved projects and design ideas.",
                    true,
                    now,
                    now,
                    1L
            );
            return collectionRepository.save(def);
        });
    }

    private void validateProjectIsPublic(UUID projectId) {
        String sql = """
            SELECT count(*)
            FROM studio_projects p
            JOIN designer_studios s ON p.studio_id = s.id
            WHERE p.id = ?
              AND p.project_status = 'READY'
              AND p.visibility_status = 'PORTFOLIO'
              AND p.archived_at IS NULL
              AND s.publication_status = 'PUBLISHED'
              AND s.status = 'ACTIVE'
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, projectId);
        if (count == null || count == 0) {
            throw new IllegalStateException("Only currently public and active portfolio projects can be saved to collections.");
        }
    }

    private UUID requireAuthenticatedUserId(ActorContext actor) {
        if (actor == null || !actor.isAuthenticated() || actor.userId() == null) {
            throw new AccessDeniedException("Authentication required to access user collections");
        }
        return actor.userId();
    }

    private String stripHtml(String input) {
        if (input == null) return "";
        return HTML_TAG_PATTERN.matcher(input).replaceAll("").trim();
    }

    private UserCollectionDto toDto(UserCollectionRecord r, int itemCount) {
        return new UserCollectionDto(
                r.id(),
                r.ownerUserId(),
                r.title(),
                r.description(),
                r.isDefault(),
                itemCount,
                r.createdAt(),
                r.updatedAt()
        );
    }
}
