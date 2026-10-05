package com.interior.platform.projects;

import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.service.ProfessionalOnboardingService;
import com.interior.platform.media.domain.MediaAssetRecord;
import com.interior.platform.media.domain.MediaProcessingStatus;
import com.interior.platform.media.domain.MediaType;
import com.interior.platform.media.domain.MediaVisibility;
import com.interior.platform.media.dto.UpdateMediaRequest;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.media.service.MediaService;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.RoomType;
import com.interior.platform.projects.dto.CreateProjectRequest;
import com.interior.platform.projects.dto.CreateRoomRequest;
import com.interior.platform.projects.dto.ProjectDetailResponse;
import com.interior.platform.projects.dto.ProjectRoomDto;
import com.interior.platform.projects.dto.ReorderRoomsRequest;
import com.interior.platform.projects.dto.UpdateRoomRequest;
import com.interior.platform.projects.web.ProjectController;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Project Rooms & Photo Organization Integration Tests")
class ProjectRoomIntegrationTest {

    @Autowired
    private ProjectController projectController;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private ProfessionalOnboardingService onboardingService;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID studioId;
    private UUID projectId;
    private ActorContext designerActor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM media_derivatives");
        jdbcTemplate.execute("DELETE FROM media_assets");
        jdbcTemplate.execute("DELETE FROM project_rooms");
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
        jdbcTemplate.execute("DELETE FROM studio_slug_claims");
        jdbcTemplate.execute("DELETE FROM designer_studios");
        jdbcTemplate.execute("DELETE FROM identity_user_roles");
        jdbcTemplate.execute("DELETE FROM identity_sessions");
        jdbcTemplate.execute("DELETE FROM users");

        userId = UuidV7.randomUuid();
        UserRecord user = new UserRecord(
                userId, "Astra Studio Designer", "astra@studio.in", "+919876543210",
                "ACTIVE", Instant.now(), Instant.now(), 0L
        );
        securityRepository.createUser(user);
        securityRepository.assignUserRole(UuidV7.randomUuid(), userId, "CUSTOMER", Instant.now());

        ActorContext preActor = new ActorContext(
                userId, "Astra Studio Designer", "astra@studio.in",
                Set.of("CUSTOMER"), Set.of(), null, null, "PASSWORD", true
        );

        OnboardingCompletionRequest req = new OnboardingCompletionRequest(
                "INTERIOR_STUDIO",
                "Astra Atelier",
                "astra-atelier",
                "Principal Designer",
                "Modern luxury residential design in Mumbai",
                2020,
                "STUDIO_2_5",
                "LUXURY",
                "Bandra West",
                "Mumbai",
                "Mumbai Suburban",
                "Maharashtra",
                "400050",
                "IN",
                true,
                false,
                null,
                List.of("Modular Kitchen", "Living Room"),
                List.of("Modern Minimalist", "Warm Contemporary"),
                List.of("Mumbai", "Bandra"),
                "+919876543210",
                "+919876543210",
                "hello@astra.in",
                "https://astra.in",
                "https://instagram.com/astra",
                true,
                true
        );

        var completion = onboardingService.completeOnboarding(preActor, req, null, null);
        studioId = completion.studio().id();

        designerActor = new ActorContext(
                userId, "Astra Studio Designer", "astra@studio.in",
                Set.of("DESIGNER", "CUSTOMER"), Set.of("DESIGNER_ADMIN"), studioId, "OWNER", "PASSWORD", true
        );

        // Create a project
        CreateProjectRequest projectReq = new CreateProjectRequest(
                "Penthouse Suite",
                ProjectCategory.COMPLETE_HOME_INTERIOR,
                "Luxury 4BHK Penthouse in Bandra",
                "Full interior architecture overhaul.",
                null, null, List.of(), "Mumbai", null, "Maharashtra", "IN",
                2026, null, null, null, "INR", null, null, null, null, null, false, null
        );
        MockHttpServletRequest mockReq = createMockRequest(designerActor);
        ResponseEntity<ProjectDetailResponse> projectRes = projectController.createProject(mockReq, null, studioId, projectReq);
        projectId = projectRes.getBody().id();
    }

    private MockHttpServletRequest createMockRequest(ActorContext actor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, actor);
        return request;
    }

    @Test
    @DisplayName("Create room defaults to standard display name when null")
    void testCreateRoomDefaultDisplayName() {
        MockHttpServletRequest req = createMockRequest(designerActor);
        CreateRoomRequest roomReq = new CreateRoomRequest(RoomType.LIVING_ROOM, null);
        ResponseEntity<ProjectRoomDto> resp = projectController.createRoom(
                req, null, studioId, projectId, roomReq);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(RoomType.LIVING_ROOM, resp.getBody().roomType());
        assertEquals("Living Room", resp.getBody().displayName());
        assertEquals(0, resp.getBody().sortOrder());
        assertEquals(0, resp.getBody().photoCount());
    }

    @Test
    @DisplayName("Create room preserves custom display name")
    void testCreateRoomCustomDisplayName() {
        MockHttpServletRequest req = createMockRequest(designerActor);
        CreateRoomRequest roomReq = new CreateRoomRequest(RoomType.BEDROOM, "Master Bedroom Suite 1");
        ResponseEntity<ProjectRoomDto> resp = projectController.createRoom(
                req, null, studioId, projectId, roomReq);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(RoomType.BEDROOM, resp.getBody().roomType());
        assertEquals("Master Bedroom Suite 1", resp.getBody().displayName());
    }

    @Test
    @DisplayName("List rooms returns sorted rooms with photo counts and cover URLs")
    void testListRooms() {
        MockHttpServletRequest req = createMockRequest(designerActor);
        projectController.createRoom(
                req, null, studioId, projectId,
                new CreateRoomRequest(RoomType.FOYER, "Foyer"));
        projectController.createRoom(
                req, null, studioId, projectId,
                new CreateRoomRequest(RoomType.KITCHEN, "Modular Kitchen"));

        ResponseEntity<List<ProjectRoomDto>> listResp = projectController.listRooms(
                req, null, studioId, projectId);
        assertEquals(HttpStatus.OK, listResp.getStatusCode());
        assertNotNull(listResp.getBody());
        assertEquals(2, listResp.getBody().size());
        assertEquals("Foyer", listResp.getBody().get(0).displayName());
        assertEquals(0, listResp.getBody().get(0).sortOrder());
        assertEquals("Modular Kitchen", listResp.getBody().get(1).displayName());
        assertEquals(1, listResp.getBody().get(1).sortOrder());
    }

    @Test
    @DisplayName("Update room display name and type")
    void testUpdateRoom() {
        MockHttpServletRequest req = createMockRequest(designerActor);
        ResponseEntity<ProjectRoomDto> created = projectController.createRoom(
                req, null, studioId, projectId,
                new CreateRoomRequest(RoomType.OTHER, "Pantry"));
        UUID roomId = created.getBody().id();

        UpdateRoomRequest updateReq = new UpdateRoomRequest(RoomType.KITCHEN, "Spice Kitchen");
        ResponseEntity<ProjectRoomDto> updated = projectController.updateRoom(
                req, null, studioId, projectId, roomId, updateReq);

        assertEquals(HttpStatus.OK, updated.getStatusCode());
        assertEquals(RoomType.KITCHEN, updated.getBody().roomType());
        assertEquals("Spice Kitchen", updated.getBody().displayName());
    }

    @Test
    @DisplayName("Reorder rooms updates sort order")
    void testReorderRooms() {
        MockHttpServletRequest req = createMockRequest(designerActor);
        ResponseEntity<ProjectRoomDto> r1 = projectController.createRoom(
                req, null, studioId, projectId,
                new CreateRoomRequest(RoomType.LIVING_ROOM, "Living Room"));
        ResponseEntity<ProjectRoomDto> r2 = projectController.createRoom(
                req, null, studioId, projectId,
                new CreateRoomRequest(RoomType.DINING, "Dining Room"));

        UUID r1Id = r1.getBody().id();
        UUID r2Id = r2.getBody().id();

        // Reorder to [r2, r1]
        ResponseEntity<Void> reordered = projectController.reorderRooms(
                req, null, studioId, projectId,
                new ReorderRoomsRequest(List.of(r2Id, r1Id)));

        assertEquals(HttpStatus.NO_CONTENT, reordered.getStatusCode());

        List<ProjectRoomDto> rooms = projectController.listRooms(
                req, null, studioId, projectId).getBody();
        assertEquals(2, rooms.size());
        assertEquals(r2Id, rooms.get(0).id());
        assertEquals(0, rooms.get(0).sortOrder());
        assertEquals(r1Id, rooms.get(1).id());
        assertEquals(1, rooms.get(1).sortOrder());
    }

    @Test
    @DisplayName("Safe room deletion never deletes media assets; resets room_id to null and is_room_cover to false")
    void testSafeRoomDeletion() {
        MockHttpServletRequest req = createMockRequest(designerActor);
        ResponseEntity<ProjectRoomDto> r = projectController.createRoom(
                req, null, studioId, projectId,
                new CreateRoomRequest(RoomType.BEDROOM, "Kids Bedroom"));
        UUID roomId = r.getBody().id();

        // Create media asset assigned to this room and marked as room cover
        UUID mediaId = UuidV7.randomUuid();
        MediaAssetRecord media = new MediaAssetRecord(
                mediaId, studioId, projectId, MediaType.REAL_PROJECT,
                MediaVisibility.PORTFOLIO, MediaProcessingStatus.READY,
                "raw/kids.jpg", "image/jpeg", 2048L, 1920, 1080,
                0, false, "Kids bed", "Scandinavian styling", false,
                userId, Instant.now(), Instant.now(), null,
                roomId, true, new BigDecimal("50.00"), new BigDecimal("50.00"), true
        );
        mediaRepository.createMediaAsset(media);

        // Verify photo count is 1
        List<ProjectRoomDto> roomsBefore = projectController.listRooms(
                req, null, studioId, projectId).getBody();
        assertEquals(1, roomsBefore.get(0).photoCount());

        // Delete room
        ResponseEntity<Void> deleteResp = projectController.deleteRoom(
                req, null, studioId, projectId, roomId);
        assertEquals(HttpStatus.NO_CONTENT, deleteResp.getStatusCode());

        // Verify room is deleted
        List<ProjectRoomDto> roomsAfter = projectController.listRooms(
                req, null, studioId, projectId).getBody();
        assertEquals(0, roomsAfter.size());

        // Verify media still exists, but room_id is null and is_room_cover is false
        MediaAssetRecord preservedMedia = mediaRepository.findMediaAsset(mediaId, studioId).orElseThrow();
        assertNull(preservedMedia.roomId());
        assertFalse(preservedMedia.isRoomCover());
        assertEquals(projectId, preservedMedia.projectId());
        assertEquals(studioId, preservedMedia.studioId());
    }

    @Test
    @DisplayName("Marking a photo as room cover unsets the previous room cover in the same room")
    void testRoomCoverSingleActive() {
        MockHttpServletRequest req = createMockRequest(designerActor);
        ResponseEntity<ProjectRoomDto> r = projectController.createRoom(
                req, null, studioId, projectId,
                new CreateRoomRequest(RoomType.LIVING_ROOM, "Formal Living"));
        UUID roomId = r.getBody().id();

        UUID media1Id = UuidV7.randomUuid();
        MediaAssetRecord m1 = new MediaAssetRecord(
                media1Id, studioId, projectId, MediaType.REAL_PROJECT,
                MediaVisibility.PORTFOLIO, MediaProcessingStatus.READY,
                "raw/m1.jpg", "image/jpeg", 2048L, 1920, 1080,
                0, false, null, null, false,
                userId, Instant.now(), Instant.now(), null,
                roomId, true, new BigDecimal("50.00"), new BigDecimal("50.00"), true
        );
        mediaRepository.createMediaAsset(m1);

        UUID media2Id = UuidV7.randomUuid();
        MediaAssetRecord m2 = new MediaAssetRecord(
                media2Id, studioId, projectId, MediaType.REAL_PROJECT,
                MediaVisibility.PORTFOLIO, MediaProcessingStatus.READY,
                "raw/m2.jpg", "image/jpeg", 2048L, 1920, 1080,
                1, false, null, null, false,
                userId, Instant.now(), Instant.now(), null,
                roomId, false, new BigDecimal("50.00"), new BigDecimal("50.00"), true
        );
        mediaRepository.createMediaAsset(m2);

        // Update media 2 to be the room cover
        UpdateMediaRequest updateReq = new UpdateMediaRequest(
                null, null, false, MediaVisibility.PORTFOLIO, false, 1,
                roomId, false, true,
                new BigDecimal("35.00"), new BigDecimal("65.00"), false
        );
        mediaService.updateMedia(designerActor, studioId, media2Id, updateReq);

        // Verify media 1 is no longer room cover, and media 2 is room cover
        MediaAssetRecord check1 = mediaRepository.findMediaAsset(media1Id, studioId).orElseThrow();
        MediaAssetRecord check2 = mediaRepository.findMediaAsset(media2Id, studioId).orElseThrow();

        assertFalse(check1.isRoomCover());
        assertTrue(check2.isRoomCover());
        assertEquals(new BigDecimal("35.00"), check2.focalX());
        assertEquals(new BigDecimal("65.00"), check2.focalY());
        assertFalse(check2.motionEnabled());
    }
}
