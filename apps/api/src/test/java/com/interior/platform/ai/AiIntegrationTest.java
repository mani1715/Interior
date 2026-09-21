package com.interior.platform.ai;

import com.interior.platform.ai.dto.AiJobListResponse;
import com.interior.platform.ai.dto.AiStudioStatusResponse;
import com.interior.platform.ai.dto.CreateAiJobRequest;
import com.interior.platform.ai.web.AiController;
import com.interior.platform.common.exception.AiProviderNotConfiguredException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.service.ProfessionalOnboardingService;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.dto.CreateProjectRequest;
import com.interior.platform.projects.dto.ProjectDetailResponse;
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

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AiIntegrationTest {

    @Autowired
    private AiController aiController;

    @Autowired
    private ProjectController projectController;

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
        jdbcTemplate.execute("DELETE FROM ai_usage_events");
        jdbcTemplate.execute("DELETE FROM ai_visualization_jobs");
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
        jdbcTemplate.execute("DELETE FROM studio_slug_claims");
        jdbcTemplate.execute("DELETE FROM designer_studios");
        jdbcTemplate.execute("DELETE FROM identity_user_roles");
        jdbcTemplate.execute("DELETE FROM identity_sessions");
        jdbcTemplate.execute("DELETE FROM users");

        userId = UuidV7.randomUuid();
        UserRecord user = new UserRecord(
                userId, "Aura Studio", "aura@aurastudio.in", "+919876543210",
                "ACTIVE", Instant.now(), Instant.now(), 0L
        );
        securityRepository.createUser(user);
        securityRepository.assignUserRole(UuidV7.randomUuid(), userId, "CUSTOMER", Instant.now());

        ActorContext preActor = new ActorContext(
                userId, "Aura Studio", "aura@aurastudio.in",
                Set.of("CUSTOMER"), Set.of(), null, null, "PASSWORD", true
        );

        OnboardingCompletionRequest req = new OnboardingCompletionRequest(
                "INTERIOR_STUDIO",
                "Aura Living",
                "aura-living",
                "Design Director",
                "Curated residential interiors",
                2020,
                "STUDIO_2_5",
                "LUXURY",
                "Indiranagar",
                "Bengaluru",
                "Bengaluru Urban",
                "Karnataka",
                "560038",
                "IN",
                true,
                false,
                null,
                List.of("Modular Kitchen", "Living Room"),
                List.of("Modern Minimalist", "Warm Contemporary"),
                List.of("Bengaluru", "Indiranagar"),
                "+919876543210",
                "+919876543210",
                "hello@auraliving.in",
                "https://auraliving.in",
                "https://instagram.com/auraliving",
                true,
                true
        );

        var completion = onboardingService.completeOnboarding(preActor, req, null, null);
        studioId = completion.studio().id();

        designerActor = new ActorContext(
                userId, "Aura Living", "aura@aurastudio.in",
                Set.of("DESIGNER", "CUSTOMER"), Set.of(), studioId, "OWNER", "PASSWORD", true
        );

        MockHttpServletRequest projectReq = new MockHttpServletRequest();
        projectReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, designerActor);

        CreateProjectRequest createProj = new CreateProjectRequest(
                "Penthouse Remodel",
                ProjectCategory.LIVING_ROOM,
                "A penthouse living space revamp",
                null, null, null, null,
                "Bengaluru", null, null, null,
                2024, null, null, null, "INR", null, null, null, null, null, false, null
        );
        ResponseEntity<ProjectDetailResponse> projResp = projectController.createProject(projectReq, studioId.toString(), null, createProj);
        projectId = projResp.getBody().id();
    }

    private MockHttpServletRequest mockRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, designerActor);
        return request;
    }

    @Test
    @DisplayName("Should return studio AI status with isConfigured=false by default")
    void testGetStudioStatusIntegration() {
        ResponseEntity<AiStudioStatusResponse> response = aiController.getStudioStatus(mockRequest(), studioId.toString(), null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isConfigured());
        assertEquals(50, response.getBody().dailyQuota());
        assertEquals(0, response.getBody().usedToday());
        assertEquals(50, response.getBody().remainingToday());
    }

    @Test
    @DisplayName("Should throw AiProviderNotConfiguredException when submitting job without configured provider")
    void testSubmitJobUnconfiguredThrowsException() {
        CreateAiJobRequest req = new CreateAiJobRequest(
                UuidV7.randomUuid(),
                projectId,
                "Add bespoke teak cabinetry and warm spotlights",
                null
        );

        assertThrows(AiProviderNotConfiguredException.class, () ->
                aiController.createGenerationJob(mockRequest(), studioId.toString(), null, req)
        );
    }

    @Test
    @DisplayName("Should list studio jobs cleanly")
    void testListJobsEmpty() {
        ResponseEntity<AiJobListResponse> response = aiController.listJobs(mockRequest(), null, 20, 0, studioId.toString(), null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().total());
        assertTrue(response.getBody().items().isEmpty());
    }
}
