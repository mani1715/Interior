package com.interior.platform.discovery;

import com.interior.platform.common.util.UuidV7;
import com.interior.platform.discovery.dto.*;
import com.interior.platform.discovery.web.DiscoveryController;
import com.interior.platform.media.domain.DerivativeVariant;
import com.interior.platform.media.domain.MediaType;
import com.interior.platform.media.domain.MediaVisibility;
import com.interior.platform.projects.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DiscoveryIntegrationTest {

    @Autowired
    private DiscoveryController discoveryController;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID studioAId;
    private UUID studioBId;
    private UUID project1Id;
    private UUID project2Id;
    private UUID project3PrivateId;
    private UUID project4DraftId;
    private UUID project5StudioBId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM studio_seo_settings");
        jdbcTemplate.execute("DELETE FROM media_derivatives");
        jdbcTemplate.execute("DELETE FROM media_assets");
        jdbcTemplate.execute("DELETE FROM upload_intents");
        jdbcTemplate.execute("DELETE FROM studio_watermark_settings");
        jdbcTemplate.execute("DELETE FROM project_styles");
        jdbcTemplate.execute("DELETE FROM studio_projects");
        jdbcTemplate.execute("DELETE FROM portfolio_versions");
        jdbcTemplate.execute("DELETE FROM portfolio_sections");
        jdbcTemplate.execute("DELETE FROM portfolios");
        jdbcTemplate.execute("DELETE FROM audit_events");
        jdbcTemplate.execute("DELETE FROM designer_onboarding_completions");
        jdbcTemplate.execute("DELETE FROM designer_onboarding_drafts");
        jdbcTemplate.execute("DELETE FROM studio_specialties");
        jdbcTemplate.execute("DELETE FROM studio_service_areas");
        jdbcTemplate.execute("DELETE FROM studio_services");
        jdbcTemplate.execute("DELETE FROM studio_contacts");
        jdbcTemplate.execute("DELETE FROM studio_members");
        jdbcTemplate.execute("DELETE FROM studio_slug_claims WHERE studio_id IS NOT NULL");
        jdbcTemplate.execute("DELETE FROM designer_studios");
        jdbcTemplate.execute("DELETE FROM identity_user_roles");
        jdbcTemplate.execute("DELETE FROM identity_sessions");
        jdbcTemplate.execute("DELETE FROM users");

        UUID userAId = UuidV7.randomUuid();
        UUID userBId = UuidV7.randomUuid();

        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', now(), now())",
                userAId, "Aarav Mehta", "aarav@mehta.com");
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', now(), now())",
                userBId, "Pooja Hegde", "pooja@hegde.com");

        studioAId = UuidV7.randomUuid();
        studioBId = UuidV7.randomUuid();

        // Studio A: Published & Active
        jdbcTemplate.update("INSERT INTO designer_studios (" +
                "id, name, slug, owner_id, status, publication_status, professional_type, professional_title, tagline, city, state, country, created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, 'ACTIVE', 'PUBLISHED', 'INTERIOR_STUDIO', 'Principal Designer', 'Crafting spaces', 'Bengaluru', 'Karnataka', 'IN', now(), now())",
                studioAId, "Mehta Design Studio", "mehta-design", userAId);

        // Studio B: Unpublished initially
        jdbcTemplate.update("INSERT INTO designer_studios (" +
                "id, name, slug, owner_id, status, publication_status, professional_type, professional_title, tagline, city, state, country, created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, 'ACTIVE', 'UNPUBLISHED', 'ARCHITECTURE_STUDIO', 'Lead Architect', 'Modern forms', 'Mumbai', 'Maharashtra', 'IN', now(), now())",
                studioBId, "Hegde Architects", "hegde-architects", userBId);

        // Studio A services & specialties
        jdbcTemplate.update("INSERT INTO studio_services (id, studio_id, service_code, service_name) VALUES (?, ?, ?, ?)",
                UuidV7.randomUuid(), studioAId, "RESIDENTIAL", "Residential Design");
        jdbcTemplate.update("INSERT INTO studio_specialties (id, studio_id, specialty_code, specialty_name) VALUES (?, ?, ?, ?)",
                UuidV7.randomUuid(), studioAId, "MODERN_MINIMALIST", "Modern Minimalist");

        // Studio B services & specialties
        jdbcTemplate.update("INSERT INTO studio_services (id, studio_id, service_code, service_name) VALUES (?, ?, ?, ?)",
                UuidV7.randomUuid(), studioBId, "ARCHITECTURE", "Architectural Planning");
        jdbcTemplate.update("INSERT INTO studio_specialties (id, studio_id, specialty_code, specialty_name) VALUES (?, ?, ?, ?)",
                UuidV7.randomUuid(), studioBId, "WARM_CONTEMPORARY", "Warm Contemporary");

        // Project 1: Studio A, READY, PORTFOLIO, Real Project Cover
        project1Id = UuidV7.randomUuid();
        insertProject(project1Id, studioAId, "indiranagar-penthouse", "Indiranagar Penthouse",
                "A luxurious minimalist penthouse in the heart of Indiranagar", ProjectCategory.LIVING_ROOM.name(),
                ProjectStatus.READY.name(), VisibilityStatus.PORTFOLIO.name(), "Bengaluru", "Karnataka", true, userAId);
        insertProjectStyle(project1Id, studioAId, ProjectStyle.MODERN_MINIMALIST.name());
        insertMediaWithDerivative(studioAId, project1Id, MediaType.REAL_PROJECT.name(), "https://cdn.platform.local/real-penthouse-thumb.webp");

        // Project 2: Studio A, READY, PORTFOLIO, AI Concept Cover
        project2Id = UuidV7.randomUuid();
        insertProject(project2Id, studioAId, "whitefield-nordic-villa", "Whitefield Nordic Villa",
                "Scandinavian kitchen and dining concept", ProjectCategory.MODULAR_KITCHEN.name(),
                ProjectStatus.READY.name(), VisibilityStatus.PORTFOLIO.name(), "Bengaluru", "Karnataka", false, userAId);
        insertProjectStyle(project2Id, studioAId, ProjectStyle.SCANDINAVIAN.name());
        insertMediaWithDerivative(studioAId, project2Id, MediaType.AI_CONCEPT.name(), "https://cdn.platform.local/ai-kitchen-thumb.webp");

        // Project 3: Studio A, READY, PRIVATE
        project3PrivateId = UuidV7.randomUuid();
        insertProject(project3PrivateId, studioAId, "private-client-residence", "Private Client Residence",
                "Confidential residence", ProjectCategory.BEDROOM.name(),
                ProjectStatus.READY.name(), VisibilityStatus.PRIVATE.name(), "Bengaluru", "Karnataka", false, userAId);

        // Project 4: Studio A, DRAFT, PORTFOLIO
        project4DraftId = UuidV7.randomUuid();
        insertProject(project4DraftId, studioAId, "draft-work-in-progress", "Draft Work In Progress",
                "Draft project", ProjectCategory.LIVING_ROOM.name(),
                ProjectStatus.DRAFT.name(), VisibilityStatus.PORTFOLIO.name(), "Bengaluru", "Karnataka", false, userAId);

        // Project 5: Studio B, READY, PORTFOLIO
        project5StudioBId = UuidV7.randomUuid();
        insertProject(project5StudioBId, studioBId, "bandra-contemporary-loft", "Bandra Contemporary Loft",
                "Seaside contemporary apartment", ProjectCategory.LIVING_ROOM.name(),
                ProjectStatus.READY.name(), VisibilityStatus.PORTFOLIO.name(), "Mumbai", "Maharashtra", true, userBId);
        insertProjectStyle(project5StudioBId, studioBId, ProjectStyle.WARM_CONTEMPORARY.name());
        insertMediaWithDerivative(studioBId, project5StudioBId, MediaType.REAL_PROJECT.name(), "https://cdn.platform.local/real-loft-thumb.webp");
    }

    private void insertProject(UUID id, UUID studioId, String slug, String title, String shortDesc,
                               String categoryCode, String status, String visibility, String city, String state,
                               boolean featured, UUID userId) {
        jdbcTemplate.update("INSERT INTO studio_projects (" +
                "id, studio_id, slug, title, short_description, full_description, category_code, project_status, visibility_status, " +
                "featured, display_order, city, district, state, country, property_type, project_scope, completion_year, " +
                "budget_visibility, budget_min, budget_max, currency, client_name_visibility, client_display_name, " +
                "area_value, area_unit, internal_notes, version, created_by, created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, 'IN', 'APARTMENT', 'FULL_INTERIOR', 2025, " +
                "'RANGE', 2000000, 3000000, 'INR', 'HIDDEN', 'Private Client', 2500, 'SQ_FT', null, 1, ?, now(), now())",
                id, studioId, slug, title, shortDesc, shortDesc, categoryCode, status, visibility, featured,
                city, city, state, userId);
    }

    private void insertProjectStyle(UUID projectId, UUID studioId, String styleCode) {
        jdbcTemplate.update("INSERT INTO project_styles (id, project_id, studio_id, style_code, created_at) VALUES (?, ?, ?, ?, now())",
                UuidV7.randomUuid(), projectId, studioId, styleCode);
    }

    private void insertMediaWithDerivative(UUID studioId, UUID projectId, String mediaType, String publicUrl) {
        UUID mediaId = UuidV7.randomUuid();
        UUID derivativeId = UuidV7.randomUuid();

        jdbcTemplate.update("INSERT INTO media_assets (" +
                "id, studio_id, project_id, original_storage_key, content_type, file_size, width, height, media_type, visibility, " +
                "processing_status, is_cover, sort_order, watermark_enabled, created_by, created_at, updated_at" +
                ") VALUES (?, ?, ?, 'storage/private/original.jpg', 'image/jpeg', 1024, 800, 600, ?, 'PUBLIC', 'READY', true, 0, false, null, now(), now())",
                mediaId, studioId, projectId, mediaType);

        jdbcTemplate.update("INSERT INTO media_derivatives (" +
                "id, media_id, studio_id, variant_name, width, height, format, file_size, storage_key, public_url, is_watermarked, created_at" +
                ") VALUES (?, ?, ?, 'MEDIUM', 800, 600, 'WEBP', 512, 'storage/public/thumb.webp', ?, false, now())",
                derivativeId, mediaId, studioId, publicUrl);
    }

    @Test
    @DisplayName("Public eligibility gate: Unpublished studio projects are excluded from public search")
    void testPublicEligibilityGate_UnpublishedStudioHidden() {
        ResponseEntity<DiscoverySearchResponse> response = discoveryController.searchProjects(
                null, null, null, null, null, null, null, null, null, 20, 0, null
        );

        assertNotNull(response.getBody());
        DiscoverySearchResponse body = response.getBody();
        // Studio A projects (Project 1, Project 2) are present; Project 5 from unpublished Studio B is NOT
        assertEquals(2, body.totalProjects());
        assertTrue(body.projects().stream().anyMatch(p -> p.id().equals(project1Id)));
        assertTrue(body.projects().stream().anyMatch(p -> p.id().equals(project2Id)));
        assertFalse(body.projects().stream().anyMatch(p -> p.id().equals(project5StudioBId)));
    }

    @Test
    @DisplayName("Project privacy: PRIVATE and DRAFT projects are never discoverable")
    void testProjectPrivacy_PrivateAndDraftHidden() {
        ResponseEntity<DiscoverySearchResponse> response = discoveryController.searchProjects(
                null, null, null, null, null, null, null, null, null, 20, 0, null
        );

        assertNotNull(response.getBody());
        DiscoverySearchResponse body = response.getBody();
        assertFalse(body.projects().stream().anyMatch(p -> p.id().equals(project3PrivateId)), "Private project must not be discoverable");
        assertFalse(body.projects().stream().anyMatch(p -> p.id().equals(project4DraftId)), "Draft project must not be discoverable");
    }

    @Test
    @DisplayName("Immediate unpublish removal: publishing Studio B exposes its project; unpublishing removes it immediately")
    void testImmediateUnpublishRemoval() {
        // 1. Publish Studio B
        jdbcTemplate.update("UPDATE designer_studios SET publication_status = 'PUBLISHED' WHERE id = ?", studioBId);

        ResponseEntity<DiscoverySearchResponse> publishedRes = discoveryController.searchProjects(
                null, null, null, null, null, null, null, null, null, 20, 0, null
        );
        assertEquals(3, publishedRes.getBody().totalProjects());
        assertTrue(publishedRes.getBody().projects().stream().anyMatch(p -> p.id().equals(project5StudioBId)));

        // 2. Unpublish Studio B
        jdbcTemplate.update("UPDATE designer_studios SET publication_status = 'UNPUBLISHED' WHERE id = ?", studioBId);

        ResponseEntity<DiscoverySearchResponse> unpublishedRes = discoveryController.searchProjects(
                null, null, null, null, null, null, null, null, null, 20, 0, null
        );
        assertEquals(2, unpublishedRes.getBody().totalProjects());
        assertFalse(unpublishedRes.getBody().projects().stream().anyMatch(p -> p.id().equals(project5StudioBId)));
    }

    @Test
    @DisplayName("Immediate archive removal: archiving a project immediately excludes it from search")
    void testImmediateArchiveRemoval() {
        jdbcTemplate.update("UPDATE studio_projects SET archived_at = now(), project_status = 'ARCHIVED' WHERE id = ?", project1Id);

        ResponseEntity<DiscoverySearchResponse> response = discoveryController.searchProjects(
                null, null, null, null, null, null, null, null, null, 20, 0, null
        );

        assertEquals(1, response.getBody().totalProjects());
        assertFalse(response.getBody().projects().stream().anyMatch(p -> p.id().equals(project1Id)));
    }

    @Test
    @DisplayName("SEO indexing preference: indexing_enabled = false excludes studio from discovery")
    void testIndexingDisabled_Excluded() {
        jdbcTemplate.update("INSERT INTO studio_seo_settings (id, studio_id, indexing_enabled) VALUES (?, ?, false)",
                UuidV7.randomUuid(), studioAId);

        ResponseEntity<DiscoverySearchResponse> response = discoveryController.searchProjects(
                null, null, null, null, null, null, null, null, null, 20, 0, null
        );

        assertEquals(0, response.getBody().totalProjects());
    }

    @Test
    @DisplayName("Text relevance and search filters work accurately")
    void testTextRelevanceAndFilters() {
        // Query by title keyword "Penthouse"
        ResponseEntity<DiscoverySearchResponse> searchRes = discoveryController.searchProjects(
                "Penthouse", null, null, null, null, null, null, null, null, 20, 0, null
        );
        assertEquals(1, searchRes.getBody().totalProjects());
        assertEquals("Indiranagar Penthouse", searchRes.getBody().projects().get(0).title());

        // Category filter: MODULAR_KITCHEN
        ResponseEntity<DiscoverySearchResponse> catRes = discoveryController.searchProjects(
                null, "MODULAR_KITCHEN", null, null, null, null, null, null, null, 20, 0, null
        );
        assertEquals(1, catRes.getBody().totalProjects());
        assertEquals("Whitefield Nordic Villa", catRes.getBody().projects().get(0).title());

        // Style filter: SCANDINAVIAN
        ResponseEntity<DiscoverySearchResponse> styleRes = discoveryController.searchProjects(
                null, null, "SCANDINAVIAN", null, null, null, null, null, null, 20, 0, null
        );
        assertEquals(1, styleRes.getBody().totalProjects());
        assertEquals("Whitefield Nordic Villa", styleRes.getBody().projects().get(0).title());
    }

    @Test
    @DisplayName("AI concept badge: real work returns isAiConceptCover = false; AI concept returns true")
    void testAiConceptCoverBadge() {
        ResponseEntity<DiscoverySearchResponse> response = discoveryController.searchProjects(
                null, null, null, null, null, null, null, null, null, 20, 0, null
        );

        var p1 = response.getBody().projects().stream().filter(p -> p.id().equals(project1Id)).findFirst().orElseThrow();
        assertFalse(p1.isAiConceptCover());
        assertEquals("https://cdn.platform.local/real-penthouse-thumb.webp", p1.coverImageUrl());

        var p2 = response.getBody().projects().stream().filter(p -> p.id().equals(project2Id)).findFirst().orElseThrow();
        assertTrue(p2.isAiConceptCover());
        assertEquals("https://cdn.platform.local/ai-kitchen-thumb.webp", p2.coverImageUrl());
    }

    @Test
    @DisplayName("Professional search: Published studio is found with specialties and project count")
    void testProfessionalSearch() {
        ResponseEntity<DiscoverySearchResponse> res = discoveryController.searchProfessionals(
                "Mehta", null, null, null, null, null, null, null, null, 20, 0, null
        );

        assertEquals(1, res.getBody().totalProfessionals());
        DiscoveryProfessionalCardDto studio = res.getBody().professionals().get(0);
        assertEquals("Mehta Design Studio", studio.name());
        assertEquals(2, studio.projectCount());
        assertTrue(studio.services().contains("Residential Design"));
        assertTrue(studio.specialties().contains("Modern Minimalist"));
    }

    @Test
    @DisplayName("Suggestions endpoint returns matching categories, styles, studios, projects")
    void testSuggestionsEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        ResponseEntity<DiscoverySuggestionsResponse> res = discoveryController.getSuggestions("living", request);
        assertNotNull(res.getBody());
        assertTrue(res.getBody().categories().stream().anyMatch(c -> c.text().contains("Living Room")));

        ResponseEntity<DiscoverySuggestionsResponse> studioRes = discoveryController.getSuggestions("mehta", request);
        assertNotNull(studioRes.getBody());
        assertTrue(studioRes.getBody().studios().stream().anyMatch(s -> s.text().equals("Mehta Design Studio")));
    }

    @Test
    @DisplayName("Facets endpoint accurately aggregates published portfolio data without leaking private items")
    void testFacetsEndpoint() {
        ResponseEntity<DiscoveryFacetsDto> res = discoveryController.getFacets(null, null);
        assertNotNull(res.getBody());

        var livingRoomFacet = res.getBody().categories().stream().filter(c -> "LIVING_ROOM".equals(c.code())).findFirst().orElseThrow();
        // Project 1 only (Project 4 draft and Project 5 unpublished studio B are excluded)
        assertEquals(1, livingRoomFacet.count());

        var kitchenFacet = res.getBody().categories().stream().filter(c -> "MODULAR_KITCHEN".equals(c.code())).findFirst().orElseThrow();
        assertEquals(1, kitchenFacet.count());

        var bedroomFacet = res.getBody().categories().stream().filter(c -> "BEDROOM".equals(c.code())).findFirst();
        assertTrue(bedroomFacet.isEmpty(), "Private project category must not be in facets");
    }
}
