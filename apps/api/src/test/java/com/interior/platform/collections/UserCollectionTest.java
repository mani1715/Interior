package com.interior.platform.collections;

import com.interior.platform.collections.dto.*;
import com.interior.platform.collections.service.UserCollectionService;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.projects.domain.*;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.security.domain.ActorContext;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Phase 27 — User Collections & Tombstone Resolution Tests")
class UserCollectionTest {

    @Autowired
    private UserCollectionService collectionService;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userAId;
    private UUID userBId;
    private ActorContext actorA;
    private ActorContext actorB;

    private UUID studioId;
    private UUID publicProjectId;
    private UUID privateProjectId;

    @BeforeEach
    void setUp() {
        userAId = UuidV7.randomUuid();
        userBId = UuidV7.randomUuid();
        actorA = new ActorContext(userAId, "User Alpha", "alpha-" + userAId + "@example.com", Set.of("CUSTOMER"), Set.of(), null, null, "PASSKEY", true);
        actorB = new ActorContext(userBId, "User Beta", "beta-" + userBId + "@example.com", Set.of("CUSTOMER"), Set.of(), null, null, "PASSKEY", true);

        // Create Users in DB
        UUID ownerId = UuidV7.randomUuid();
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', now(), now())",
                userAId, "User Alpha", "alpha-" + userAId + "@example.com");
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', now(), now())",
                userBId, "User Beta", "beta-" + userBId + "@example.com");
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', now(), now())",
                ownerId, "Studio Owner", "owner-" + ownerId + "@example.com");

        studioId = UuidV7.randomUuid();
        String slug = "studio-" + java.util.UUID.randomUUID().toString();
        StudioDetailRecord studio = new StudioDetailRecord(
                studioId, "Urban Living Studio", slug, ownerId, "ACTIVE", "INTERIOR_STUDIO",
                "Lead Designer", "Crafted spaces", 2017, "5-10", "MID_TIER", "789 Park Rd",
                "Bengaluru", "Bengaluru Urban", "Karnataka", "560002", "India", true, true,
                "29KLMNO1234P1Q8", "PUBLISHED", Instant.now(), Instant.now(), Instant.now(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );
        studioRepository.createStudio(studio);

        // 1. Create a Public Project (READY + PORTFOLIO + unarchived)
        publicProjectId = UuidV7.randomUuid();
        StudioProjectRecord pubProject = new StudioProjectRecord(
                publicProjectId, studioId, "mod-scandi-kitchen-" + java.util.UUID.randomUUID().toString(),
                "Modern Scandinavian Kitchen", "Clean minimal open kitchen design", null,
                ProjectCategory.MODULAR_KITCHEN, ProjectStatus.READY, VisibilityStatus.PORTFOLIO,
                true, 1, "Bengaluru", "Bengaluru Urban", "Karnataka", "India",
                PropertyType.APARTMENT, ProjectScope.SINGLE_ROOM, 2024,
                BudgetVisibility.HIDDEN, null, null, "INR",
                ClientNameVisibility.HIDDEN, null, null, null, null,
                1L, userAId, Instant.now(), Instant.now(), null
        );
        projectRepository.createProject(pubProject, Collections.emptyList());

        // 2. Create a Private Project (DRAFT / PRIVATE)
        privateProjectId = UuidV7.randomUuid();
        StudioProjectRecord privProject = new StudioProjectRecord(
                privateProjectId, studioId, "secret-penthouse-" + java.util.UUID.randomUUID().toString(),
                "Secret Luxury Penthouse", "Unpublished draft", null,
                ProjectCategory.LIVING_ROOM, ProjectStatus.DRAFT, VisibilityStatus.PRIVATE,
                false, 2, "Bengaluru", "Bengaluru Urban", "Karnataka", "India",
                PropertyType.APARTMENT, ProjectScope.FULL_INTERIOR, 2024,
                BudgetVisibility.HIDDEN, null, null, "INR",
                ClientNameVisibility.HIDDEN, null, null, null, null,
                1L, userAId, Instant.now(), Instant.now(), null
        );
        projectRepository.createProject(privProject, Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM collection_items");
        jdbcTemplate.execute("DELETE FROM user_collections");
    }

    @Test
    @DisplayName("1. User collections lazy default creation ('Saved') and listing")
    void testLazyDefaultCollectionCreation() {
        List<UserCollectionDto> list = collectionService.listCollections(actorA);
        assertFalse(list.isEmpty());
        UserCollectionDto def = list.get(0);
        assertTrue(def.isDefault());
        assertEquals("Saved", def.title());
        assertEquals(userAId, def.ownerUserId());
    }

    @Test
    @DisplayName("2. User isolation: User B cannot access or modify User A's collection")
    void testCrossUserAccessDenied() {
        UserCollectionDto colA = collectionService.createCollection(
                actorA, new CreateCollectionRequest("My Dream Home", "Inspiration for 2025")
        );

        // User B attempts to access User A's collection -> AccessDeniedException
        assertThrows(com.interior.platform.common.exception.AccessDeniedException.class, () ->
                collectionService.getCollectionDetail(actorB, colA.id())
        );

        // User B attempts to update User A's collection -> AccessDeniedException
        assertThrows(com.interior.platform.common.exception.AccessDeniedException.class, () ->
                collectionService.updateCollection(actorB, colA.id(), new UpdateCollectionRequest("Hacked", null))
        );

        // User B attempts to delete User A's collection -> AccessDeniedException
        assertThrows(com.interior.platform.common.exception.AccessDeniedException.class, () ->
                collectionService.deleteCollection(actorB, colA.id())
        );
    }

    @Test
    @DisplayName("3. Saving public project succeeds; private project is rejected")
    void testSavingProjectsEligibility() {
        // Save public project to default collection -> SUCCESS
        CollectionItemDto saved = collectionService.saveProject(
                actorA, new SaveProjectRequest(publicProjectId, null, "Love this cabinetry color")
        );
        assertNotNull(saved);
        assertEquals(publicProjectId, saved.projectId());
        assertEquals("Love this cabinetry color", saved.note());
        assertTrue(saved.isAvailable());
        assertEquals("Modern Scandinavian Kitchen", saved.projectTitle());

        // Save private project -> REJECTED with IllegalStateException
        assertThrows(IllegalStateException.class, () ->
                collectionService.saveProject(actorA, new SaveProjectRequest(privateProjectId, null, null))
        );
    }

    @Test
    @DisplayName("4. Saved project becomes private/unpublished/archived -> renders as unavailable tombstone")
    void testTombstoneWhenProjectMadePrivate() {
        // 1. User saves public project
        UserCollectionDto customCol = collectionService.createCollection(
                actorA, new CreateCollectionRequest("Kitchen Ideas", "Ideas")
        );
        collectionService.saveProject(
                actorA, new SaveProjectRequest(publicProjectId, customCol.id(), "Check handles")
        );

        // Verify initial state is public
        CollectionDetailDto beforeDetail = collectionService.getCollectionDetail(actorA, customCol.id());
        assertEquals(1, beforeDetail.items().size());
        assertTrue(beforeDetail.items().get(0).isAvailable());
        assertEquals("Modern Scandinavian Kitchen", beforeDetail.items().get(0).projectTitle());

        // 2. Studio later unpublishes/makes the project PRIVATE
        jdbcTemplate.update(
                "UPDATE studio_projects SET visibility_status = 'PRIVATE', updated_at = now() WHERE id = ?",
                publicProjectId
        );

        // 3. Collection detail MUST now show unavailable tombstone with zero private data leaked!
        CollectionDetailDto afterDetail = collectionService.getCollectionDetail(actorA, customCol.id());
        assertEquals(1, afterDetail.items().size());
        CollectionItemDto item = afterDetail.items().get(0);

        assertFalse(item.isAvailable());
        assertEquals("This project is no longer available", item.projectTitle());
        assertNull(item.projectSlug());
        assertNull(item.coverImageUrl());
        assertNull(item.studioName());
        assertNull(item.studioSlug());

        // Private note remains visible to user
        assertEquals("Check handles", item.note());

        // User can remove the tombstone item
        collectionService.removeItem(actorA, item.id());
        CollectionDetailDto emptyDetail = collectionService.getCollectionDetail(actorA, customCol.id());
        assertTrue(emptyDetail.items().isEmpty());
    }

    @Test
    @DisplayName("5. Duplicate save on same collection is idempotent and updates note")
    void testDuplicateSaveIdempotency() {
        UserCollectionDto col = collectionService.createCollection(
                actorA, new CreateCollectionRequest("Wardrobes", null)
        );

        CollectionItemDto first = collectionService.saveProject(
                actorA, new SaveProjectRequest(publicProjectId, col.id(), "Note 1")
        );

        CollectionItemDto second = collectionService.saveProject(
                actorA, new SaveProjectRequest(publicProjectId, col.id(), "Note 2 Updated")
        );

        assertEquals(first.id(), second.id());
        assertEquals("Note 2 Updated", second.note());

        CollectionDetailDto detail = collectionService.getCollectionDetail(actorA, col.id());
        assertEquals(1, detail.items().size());
    }

    @Test
    @DisplayName("6. Reordering items in collection preserves sequence")
    void testReorderItems() {
        // Create second public project
        UUID project2Id = UuidV7.randomUuid();
        StudioProjectRecord project2 = new StudioProjectRecord(
                project2Id, studioId, "master-bedroom-" + java.util.UUID.randomUUID().toString(),
                "Master Bedroom Suite", "Minimal bedroom suite", null,
                ProjectCategory.BEDROOM, ProjectStatus.READY, VisibilityStatus.PORTFOLIO,
                true, 2, "Bengaluru", "Bengaluru Urban", "Karnataka", "India",
                PropertyType.APARTMENT, ProjectScope.SINGLE_ROOM, 2024,
                BudgetVisibility.HIDDEN, null, null, "INR",
                ClientNameVisibility.HIDDEN, null, null, null, null,
                1L, userAId, Instant.now(), Instant.now(), null
        );
        projectRepository.createProject(project2, Collections.emptyList());

        UserCollectionDto col = collectionService.createCollection(
                actorA, new CreateCollectionRequest("Home Renovation", null)
        );

        CollectionItemDto item1 = collectionService.saveProject(actorA, new SaveProjectRequest(publicProjectId, col.id(), null));
        CollectionItemDto item2 = collectionService.saveProject(actorA, new SaveProjectRequest(project2Id, col.id(), null));

        // Initial order: item1 (order 1), item2 (order 2)
        CollectionDetailDto detail1 = collectionService.getCollectionDetail(actorA, col.id());
        assertEquals(item1.id(), detail1.items().get(0).id());
        assertEquals(item2.id(), detail1.items().get(1).id());

        // Reorder: item2 first, then item1
        collectionService.reorderItems(actorA, col.id(), new ReorderItemsRequest(List.of(item2.id(), item1.id())));

        CollectionDetailDto detail2 = collectionService.getCollectionDetail(actorA, col.id());
        assertEquals(item2.id(), detail2.items().get(0).id());
        assertEquals(item1.id(), detail2.items().get(1).id());
    }
}
