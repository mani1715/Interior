package com.interior.platform.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.analytics.domain.AnalyticsEventType;
import com.interior.platform.analytics.dto.PublicAnalyticsEventRequest;
import com.interior.platform.analytics.dto.StudioAnalyticsSummaryDto;
import com.interior.platform.analytics.repository.AnalyticsRepository;
import com.interior.platform.analytics.service.AnalyticsService;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.leads.domain.LeadSource;
import com.interior.platform.leads.domain.LeadStatus;
import com.interior.platform.leads.domain.PreferredContactChannel;
import com.interior.platform.leads.domain.StudioLeadRecord;
import com.interior.platform.leads.repository.LeadRepository;
import com.interior.platform.leads.service.LeadService;
import com.interior.platform.security.domain.ActorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 28 — Analytics Subsystem Integration Tests")
class AnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private LeadService leadService;

    private UUID userAId;
    private UUID userBId;
    private UUID studioAId;
    private UUID studioBId;
    private UUID projectAId;

    private final String studioASlug = "studio-analytics-alpha";
    private final String studioBSlug = "studio-analytics-beta";
    private final String projectASlug = "modern-living-room";

    @BeforeEach
    void setUp() {
        cleanUp();

        userAId = UuidV7.randomUuid();
        userBId = UuidV7.randomUuid();
        studioAId = UuidV7.randomUuid();
        studioBId = UuidV7.randomUuid();
        projectAId = UuidV7.randomUuid();

        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status) VALUES (?, 'Designer A', 'alpha@example.com', 'ACTIVE')", userAId);
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status) VALUES (?, 'Designer B', 'beta@example.com', 'ACTIVE')", userBId);

        jdbcTemplate.update("""
            INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status, city, state, country, created_at, updated_at)
            VALUES (?, 'Studio Alpha', ?, ?, 'ACTIVE', 'PUBLISHED', 'Mumbai', 'Maharashtra', 'IN', now(), now())
        """, studioAId, studioASlug, userAId);

        jdbcTemplate.update("""
            INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status, city, state, country, created_at, updated_at)
            VALUES (?, 'Studio Beta', ?, ?, 'ACTIVE', 'PUBLISHED', 'Delhi', 'Delhi', 'IN', now(), now())
        """, studioBId, studioBSlug, userBId);

        jdbcTemplate.update("""
            INSERT INTO studio_members (id, studio_id, user_id, role, granted_at)
            VALUES (?, ?, ?, 'OWNER', now())
        """, UuidV7.randomUuid(), studioAId, userAId);

        jdbcTemplate.update("""
            INSERT INTO studio_members (id, studio_id, user_id, role, granted_at)
            VALUES (?, ?, ?, 'OWNER', now())
        """, UuidV7.randomUuid(), studioBId, userBId);

        jdbcTemplate.update("""
            INSERT INTO studio_projects (
                id, studio_id, slug, title, short_description, full_description, category_code, project_status, visibility_status,
                featured, display_order, city, district, state, country, property_type, project_scope, completion_year,
                budget_visibility, budget_min, budget_max, currency, client_name_visibility, client_display_name,
                area_value, area_unit, internal_notes, version, created_by, created_at, updated_at
            ) VALUES (?, ?, ?, 'Modern Living Room', 'Short desc', 'Full desc', 'LIVING_ROOM', 'READY', 'PORTFOLIO',
                true, 0, 'Mumbai', 'Mumbai', 'MH', 'IN', 'APARTMENT', 'FULL_INTERIOR', 2024,
                'RANGE', 500000, 800000, 'INR', 'HIDDEN', 'Mr. Sharma', 1200, 'SQ_FT', null, 1, ?, now(), now())
        """, projectAId, studioAId, projectASlug, userAId);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("DELETE FROM analytics_events");
        jdbcTemplate.execute("DELETE FROM studio_daily_metrics");
        jdbcTemplate.execute("DELETE FROM lead_activities");
        jdbcTemplate.execute("DELETE FROM studio_leads");
        jdbcTemplate.execute("DELETE FROM project_styles");
        jdbcTemplate.execute("DELETE FROM studio_projects");
        jdbcTemplate.execute("DELETE FROM studio_members");
        jdbcTemplate.execute("DELETE FROM audit_events");
        jdbcTemplate.execute("DELETE FROM designer_studios");
        jdbcTemplate.execute("DELETE FROM identity_user_roles");
        jdbcTemplate.execute("DELETE FROM identity_sessions");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    @DisplayName("Public ingestion: records low-trust profile and project views with deduplication")
    void testPublicIngestionAndDeduplication() throws Exception {
        PublicAnalyticsEventRequest profileReq = new PublicAnalyticsEventRequest(
                AnalyticsEventType.PUBLIC_PROFILE_VIEW,
                "STUDIO",
                studioASlug,
                "session-hash-123",
                "https://example.com/referrals?token=secret123&utm_source=ad",
                "DESKTOP",
                Map.of("categoryCode", "LIVING_ROOM")
        );

        mockMvc.perform(post("/public/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileReq)))
                .andExpect(status().isAccepted());

        // Deduplication: sending identical event with same session within same hour should succeed with 202 but not duplicate daily metrics
        mockMvc.perform(post("/public/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileReq)))
                .andExpect(status().isAccepted());

        // Public project view
        PublicAnalyticsEventRequest projectReq = new PublicAnalyticsEventRequest(
                AnalyticsEventType.PUBLIC_PROJECT_VIEW,
                "PROJECT",
                projectASlug,
                "session-hash-123",
                "https://google.com/search?q=interior+design",
                "MOBILE",
                Map.of("sourceRoute", "/projects")
        );

        mockMvc.perform(post("/public/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectReq)))
                .andExpect(status().isAccepted());

        // Verify summary
        LocalDate today = LocalDate.now();
        StudioAnalyticsSummaryDto summary = analyticsRepository.getSummary(studioAId, today.minusDays(1), today.plusDays(1));

        assertEquals(1, summary.totalProfileViews());
        assertEquals(1, summary.totalProjectViews());
        assertEquals(2, summary.funnel().publicViews());
        assertFalse(summary.topProjects().isEmpty());
        assertEquals(projectAId, summary.topProjects().get(0).projectId());
        assertEquals(1, summary.topProjects().get(0).viewCount());
    }

    @Test
    @DisplayName("Public ingestion: rejects high-trust server event types from public endpoint")
    void testPublicRejectsHighTrustEvents() throws Exception {
        PublicAnalyticsEventRequest leadReq = new PublicAnalyticsEventRequest(
                AnalyticsEventType.LEAD_CREATED,
                "STUDIO",
                studioASlug,
                "session-spoof",
                null,
                null,
                null
        );

        mockMvc.perform(post("/public/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leadReq)))
                .andExpect(status().isAccepted()); // Controller gracefully consumes, service drops

        StudioAnalyticsSummaryDto summary = analyticsRepository.getSummary(studioAId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        assertEquals(0, summary.totalLeadsCreated());
    }

    @Test
    @DisplayName("Server events: lead creation and status transition to WON correctly update daily metrics and funnel")
    void testServerEventsAndFunnel() {
        UUID leadId = UuidV7.randomUuid();
        Instant now = Instant.now();

        StudioLeadRecord lead = new StudioLeadRecord(
                leadId, studioAId, projectAId, LeadSource.PROJECT_DISCOVERY,
                LeadStatus.NEW, "Test Client", "+919876543210", "client@example.com",
                "Mumbai", "LIVING_ROOM", "5-10L", "Looking for renovation",
                PreferredContactChannel.WHATSAPP, now, now, null, null,
                null, false, null, now, now, 1L, null
        );
        leadRepository.save(lead);

        // Record server event LEAD_CREATED
        analyticsService.recordServerEvent(
                studioAId,
                AnalyticsEventType.LEAD_CREATED,
                "LEAD",
                leadId,
                "PUBLIC_PROJECT",
                Map.of("projectSlug", projectASlug),
                "lead_created:" + leadId
        );

        // Verify metrics
        LocalDate today = LocalDate.now();
        StudioAnalyticsSummaryDto summary = analyticsRepository.getSummary(studioAId, today.minusDays(1), today.plusDays(1));
        assertEquals(1, summary.totalLeadsCreated());
        assertEquals(0, summary.totalLeadsWon());

        // Transition lead to WON
        ActorContext actor = new ActorContext(userAId, "Designer A", "alpha@example.com", Set.of("DESIGNER"), Set.of(), studioAId, "OWNER", "PASSKEY", true);
        leadService.updateLead(actor, studioAId, leadId, new com.interior.platform.leads.dto.LeadUpdateRequest(
                "WON", null, null, 1L
        ));

        // Verify updated metrics
        StudioAnalyticsSummaryDto updatedSummary = analyticsRepository.getSummary(studioAId, today.minusDays(1), today.plusDays(1));
        assertEquals(1, updatedSummary.totalLeadsCreated());
        assertEquals(1, updatedSummary.totalLeadsWon());
        assertEquals(100.0, updatedSummary.funnel().leadWinRatePercent());
    }

    @Test
    @DisplayName("Studio analytics workspace: enforces tenant isolation")
    void testTenantIsolationOnWorkspaceAnalytics() throws Exception {
        // Record event for Studio A
        analyticsService.recordServerEvent(
                studioAId,
                AnalyticsEventType.LEAD_CREATED,
                "LEAD",
                UuidV7.randomUuid(),
                "test",
                null,
                null
        );

        // Studio A member accessing Studio A analytics -> 200 OK
        mockMvc.perform(get("/studio/analytics")
                        .header("X-Studio-Id", studioAId.toString())
                        .requestAttr(com.interior.platform.security.interceptor.SecurityInterceptor.ACTOR_ATTRIBUTE,
                                new ActorContext(userAId, "User A", "alpha@example.com", Set.of("DESIGNER"), Set.of(), studioAId, "OWNER", "PASSKEY", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studioId").value(studioAId.toString()))
                .andExpect(jsonPath("$.totalLeadsCreated").value(1));

        // Studio B member trying to access Studio A analytics -> 403 Forbidden
        mockMvc.perform(get("/studio/analytics")
                        .header("X-Studio-Id", studioAId.toString())
                        .requestAttr(com.interior.platform.security.interceptor.SecurityInterceptor.ACTOR_ATTRIBUTE,
                                new ActorContext(userBId, "User B", "beta@example.com", Set.of("DESIGNER"), Set.of(), studioBId, "OWNER", "PASSKEY", true)))
                .andExpect(status().isForbidden());
    }
}
