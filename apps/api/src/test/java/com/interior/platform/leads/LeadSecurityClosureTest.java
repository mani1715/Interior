package com.interior.platform.leads;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.RateLimitExceededException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.service.ProfessionalOnboardingService;
import com.interior.platform.discovery.dto.DiscoverySearchResponse;
import com.interior.platform.discovery.dto.DiscoverySuggestionsResponse;
import com.interior.platform.discovery.web.DiscoveryController;
import com.interior.platform.leads.domain.LeadActivityType;
import com.interior.platform.leads.domain.LeadStatus;
import com.interior.platform.leads.domain.WhatsAppMessageStatus;
import com.interior.platform.leads.dto.*;
import com.interior.platform.leads.repository.LeadRepository;
import com.interior.platform.leads.service.WhatsAppService;
import com.interior.platform.leads.web.LeadController;
import com.interior.platform.leads.web.PublicLeadController;
import com.interior.platform.leads.web.WhatsAppWebhookController;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.RateLimiterService;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Phase 26.1 Leads PII, RLS, and WhatsApp Security Closure Integration Tests")
class LeadSecurityClosureTest {

    @Autowired
    private PublicLeadController publicLeadController;

    @Autowired
    private LeadController leadController;

    @Autowired
    private WhatsAppWebhookController whatsAppWebhookController;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private DiscoveryController discoveryController;

    @Autowired
    private ProfessionalOnboardingService onboardingService;

    @Autowired
    private SecurityRepository securityRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        rateLimiterService.reset();
        jdbcTemplate.execute("DELETE FROM lead_activities");
        jdbcTemplate.execute("DELETE FROM lead_notes");
        jdbcTemplate.execute("DELETE FROM lead_whatsapp_messages");
        jdbcTemplate.execute("DELETE FROM studio_leads");
        jdbcTemplate.execute("DELETE FROM audit_events");
        jdbcTemplate.execute("DELETE FROM designer_onboarding_completions");
        jdbcTemplate.execute("DELETE FROM designer_onboarding_drafts");
        jdbcTemplate.execute("DELETE FROM studio_projects");
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
    }

    private UserRecord createTestUser(String name, String email, String status) {
        UUID userId = UuidV7.randomUuid();
        UserRecord user = new UserRecord(
                userId,
                name,
                email,
                "+919876543210",
                status,
                Instant.now(),
                Instant.now(),
                0L
        );
        securityRepository.createUser(user);
        securityRepository.assignUserRole(UuidV7.randomUuid(), userId, "CUSTOMER", Instant.now());
        return user;
    }

    private OnboardingCompletionRequest createValidRequest(String studioName, String slug) {
        return new OnboardingCompletionRequest(
                "INTERIOR_STUDIO",
                studioName,
                slug,
                "Principal Architect",
                "Crafting soulful spaces",
                2018,
                "5-10",
                "15L-35L",
                "Brodipet 4th Line",
                "Guntur",
                "Guntur",
                "Andhra Pradesh",
                "522002",
                "IN",
                true,
                true,
                "37AAAAA0000A1Z5",
                List.of("Modular Kitchen", "Living Room"),
                List.of("Warm Contemporary"),
                List.of("Guntur"),
                "+919876543210",
                "+919876543210",
                "contact@" + slug + ".com",
                "https://" + slug + ".com",
                "https://instagram.com/" + slug,
                true,
                true
        );
    }

    private record TestSetup(UserRecord user, ActorContext actor, UUID studioId, String slug) {}

    private TestSetup setupStudio(String name, String slug) {
        UserRecord user = createTestUser(name, slug + "@example.com", "ACTIVE");
        ActorContext initialActor = new ActorContext(user.id(), user.displayName(), user.email(), Set.of("CUSTOMER"), null, null, true);
        var onboardResult = onboardingService.completeOnboarding(initialActor, createValidRequest(name, slug), null, null);
        UUID studioId = onboardResult.studio().id();
        jdbcTemplate.update("UPDATE designer_studios SET publication_status = 'PUBLISHED', status = 'ACTIVE' WHERE id = ?", studioId);
        ActorContext designerActor = new ActorContext(
                user.id(), user.displayName(), user.email(),
                Set.of("DESIGNER", "CUSTOMER"), studioId, "OWNER", true
        );
        return new TestSetup(user, designerActor, studioId, slug);
    }

    // =========================================================================
    // 1. PUBLIC LEAD INSERTION & ATTRIBUTION HARDENING
    // =========================================================================

    @Test
    @DisplayName("Public submission rejects non-existent studio slug")
    void testPublicLeadRejectsNonExistentStudio() {
        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                "non-existent-studio",
                null,
                "Anil Verma",
                "+919876543210",
                "anil@example.com",
                "Hyderabad",
                "Residential",
                "10L-20L",
                "Looking for 3BHK interior design",
                "PHONE",
                true,
                false,
                UUID.randomUUID().toString(),
                null
        );

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        assertThrows(BadRequestException.class, () -> publicLeadController.submitLead(req, httpReq));
    }

    @Test
    @DisplayName("Public submission rejects unpublished (UNPUBLISHED) studio")
    void testPublicLeadRejectsDraftStudio() {
        TestSetup studio = setupStudio("Draft Studio", "draft-studio");
        jdbcTemplate.update("UPDATE designer_studios SET publication_status = 'UNPUBLISHED' WHERE id = ?", studio.studioId());

        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                studio.slug(),
                null,
                "Anil Verma",
                "+919876543210",
                "anil@example.com",
                "Hyderabad",
                "Residential",
                "10L-20L",
                "Looking for 3BHK interior design",
                "PHONE",
                true,
                false,
                UUID.randomUUID().toString(),
                null
        );

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        assertThrows(BadRequestException.class, () -> publicLeadController.submitLead(req, httpReq));
    }

    @Test
    @DisplayName("Public submission rejects suspended studio")
    void testPublicLeadRejectsSuspendedStudio() {
        TestSetup studio = setupStudio("Suspended Studio", "suspended-studio");
        jdbcTemplate.update("UPDATE designer_studios SET status = 'SUSPENDED' WHERE id = ?", studio.studioId());

        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                studio.slug(),
                null,
                "Anil Verma",
                "+919876543210",
                "anil@example.com",
                "Hyderabad",
                "Residential",
                "10L-20L",
                "Looking for 3BHK interior design",
                "PHONE",
                true,
                false,
                UUID.randomUUID().toString(),
                null
        );

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        assertThrows(BadRequestException.class, () -> publicLeadController.submitLead(req, httpReq));
    }

    @Test
    @DisplayName("Public submission rejects project attribution if project belongs to another studio")
    void testPublicLeadRejectsCrossStudioProjectAttribution() {
        TestSetup studioA = setupStudio("Studio A", "studio-a");
        TestSetup studioB = setupStudio("Studio B", "studio-b");

        // Insert project for Studio B
        UUID projectBId = UuidV7.randomUuid();
        jdbcTemplate.update(
                "INSERT INTO studio_projects (id, studio_id, title, slug, category_code, project_status, visibility_status, created_at, updated_at, version) " +
                "VALUES (?, ?, 'Villa Project', 'villa-project', 'LIVING_ROOM', 'READY', 'PORTFOLIO', now(), now(), 1)",
                projectBId, studioB.studioId()
        );

        // Attempt submission to Studio A targeting Studio B's project slug
        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                studioA.slug(),
                "villa-project",
                "Pooja Hegde",
                "+919876543210",
                "pooja@example.com",
                "Mumbai",
                "Villa",
                "50L+",
                "Interested in your villa project",
                "ANY",
                true,
                false,
                UUID.randomUUID().toString(),
                null
        );

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        assertThrows(BadRequestException.class, () -> publicLeadController.submitLead(req, httpReq));
    }

    @Test
    @DisplayName("Public submission rejects project attribution if project is not READY/PORTFOLIO or is archived")
    void testPublicLeadRejectsUnreadyProjectAttribution() {
        TestSetup studio = setupStudio("Studio C", "studio-c");

        // Insert draft project
        UUID draftProjectId = UuidV7.randomUuid();
        jdbcTemplate.update(
                "INSERT INTO studio_projects (id, studio_id, title, slug, category_code, project_status, visibility_status, created_at, updated_at, version) " +
                "VALUES (?, ?, 'Draft Project', 'draft-project', 'LIVING_ROOM', 'DRAFT', 'PORTFOLIO', now(), now(), 1)",
                draftProjectId, studio.studioId()
        );

        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                studio.slug(),
                "draft-project",
                "Vikram Roy",
                "+919876543210",
                "vikram@example.com",
                "Kolkata",
                "Apartment",
                "15L-25L",
                "Inquiring on draft project",
                "ANY",
                true,
                false,
                UUID.randomUUID().toString(),
                null
        );

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        assertThrows(BadRequestException.class, () -> publicLeadController.submitLead(req, httpReq));
    }

    // =========================================================================
    // 2. PRIVACY & DATA MINIMIZATION
    // =========================================================================

    @Test
    @DisplayName("Data minimization: list leads returns masked phone and null raw phone/email; detail view returns full PII")
    void testDataMinimizationOnListAndDetail() {
        TestSetup studio = setupStudio("Privacy Studio", "privacy-studio");

        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                studio.slug(),
                null,
                "Suresh Raina",
                "9876543210",
                "suresh.raina@example.com",
                "Chennai",
                "Penthouse",
                "40L-60L",
                "Looking for luxury penthouse interior",
                "WHATSAPP",
                true,
                true,
                UUID.randomUUID().toString(),
                null
        );

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        publicLeadController.submitLead(req, httpReq);

        // Authenticated workspace call
        MockHttpServletRequest authReq = new MockHttpServletRequest();
        authReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studio.actor());

        ResponseEntity<LeadListResponse> listResp = leadController.listLeads(
                authReq, studio.studioId().toString(), null, null, null, null, "createdAt:desc", 20, 0
        );
        assertEquals(1, listResp.getBody().total());
        LeadSummaryDto summary = listResp.getBody().items().get(0);

        // Verification of data minimization
        assertNull(summary.phoneNormalized(), "Summary MUST omit phoneNormalized for data minimization");
        assertNull(summary.emailNormalized(), "Summary MUST omit emailNormalized for data minimization");
        assertTrue(summary.phoneMasked().contains("••••"), "Summary MUST return masked phone");
        assertTrue(summary.phoneMasked().startsWith("+91"));
        assertTrue(summary.phoneMasked().endsWith("3210"));

        // Detail view exposes full PII strictly to authorized member
        ResponseEntity<LeadDetailDto> detailResp = leadController.getLeadDetail(
                authReq, summary.id(), studio.studioId().toString(), null
        );
        LeadDetailDto detail = detailResp.getBody();
        assertNotNull(detail);
        assertEquals("+919876543210", detail.phoneNormalized());
        assertEquals("suresh.raina@example.com", detail.emailNormalized());
    }

    @Test
    @DisplayName("Privacy regression: submitted lead PII is strictly excluded from Discovery search, suggestions, and public index")
    void testLeadsExcludedFromDiscoveryAndSearch() {
        TestSetup studio = setupStudio("Discovery Studio", "discovery-studio");
        String secretToken = "SECRET_TOKEN_LEAD_PII_9876";

        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                studio.slug(),
                null,
                secretToken, // Visitor name containing secret token
                "+919876543210",
                "secret@example.com",
                "Delhi",
                "Commercial",
                "30L",
                "Inquiry with " + secretToken,
                "PHONE",
                true,
                false,
                UUID.randomUUID().toString(),
                null
        );

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        publicLeadController.submitLead(req, httpReq);

        // Test Unified Discovery Search: must return 0 results
        ResponseEntity<DiscoverySearchResponse> searchResp = discoveryController.searchAll(
                secretToken, null, null, null, null, null, null, null, null, 10, 0, null
        );
        assertNotNull(searchResp.getBody());
        assertEquals(0, searchResp.getBody().totalProjects(), "Discovery search must never index or return studio leads");
        assertEquals(0, searchResp.getBody().totalProfessionals(), "Discovery search must never index or return studio leads");

        // Test Discovery Suggestions: must return 0 suggestions
        MockHttpServletRequest sugReq = new MockHttpServletRequest();
        ResponseEntity<DiscoverySuggestionsResponse> sugResp = discoveryController.getSuggestions(
                secretToken, sugReq
        );
        assertNotNull(sugResp.getBody());
        assertTrue(sugResp.getBody().studios().isEmpty(), "Suggestions must never return studio lead data");
        assertTrue(sugResp.getBody().projects().isEmpty(), "Suggestions must never return studio lead data");
    }

    // =========================================================================
    // 3. CROSS-TENANT ISOLATION
    // =========================================================================

    @Test
    @DisplayName("Cross-tenant isolation: Studio A cannot view or manipulate Studio B's leads")
    void testCrossTenantIsolation() {
        TestSetup studioA = setupStudio("Studio Alpha", "studio-alpha");
        TestSetup studioB = setupStudio("Studio Beta", "studio-beta");

        // Visitor submits lead to Studio B
        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                studioB.slug(),
                null,
                "Client of Studio B",
                "+919876543210",
                "clientb@example.com",
                "Pune",
                "Villa",
                "25L-40L",
                "Inquiry for Studio B",
                "ANY",
                true,
                false,
                UUID.randomUUID().toString(),
                null
        );
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        publicLeadController.submitLead(req, httpReq);

        // Studio B queries their lead
        MockHttpServletRequest studioBReq = new MockHttpServletRequest();
        studioBReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studioB.actor());
        LeadListResponse bLeads = leadController.listLeads(
                studioBReq, studioB.studioId().toString(), null, null, null, null, "createdAt:desc", 20, 0
        ).getBody();
        assertEquals(1, bLeads.total());
        UUID leadBId = bLeads.items().get(0).id();

        // Studio A tries to access Studio B's lead detail -> AccessDeniedException
        MockHttpServletRequest studioAReq = new MockHttpServletRequest();
        studioAReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studioA.actor());

        assertThrows(AccessDeniedException.class, () ->
                leadController.getLeadDetail(studioAReq, leadBId, studioB.studioId().toString(), null)
        );

        // Studio A tries to update Studio B's lead status -> AccessDeniedException or not found in studio A
        LeadUpdateRequest updateReq = new LeadUpdateRequest("CONTACTED", null, null, 1L);
        assertThrows(Exception.class, () ->
                leadController.updateLead(studioAReq, leadBId, updateReq, studioA.studioId().toString(), null)
        );

        // Studio A tries to add note to Studio B's lead -> AccessDeniedException or not found in studio A
        LeadNoteCreateRequest noteReq = new LeadNoteCreateRequest("Hacked note from Studio A");
        assertThrows(Exception.class, () ->
                leadController.addNote(studioAReq, leadBId, noteReq, studioA.studioId().toString(), null)
        );

        // Studio A tries to archive Studio B's lead -> AccessDeniedException or not found in studio A
        assertThrows(Exception.class, () ->
                leadController.archiveLead(studioAReq, leadBId, 1L, studioA.studioId().toString(), null)
        );
    }

    // =========================================================================
    // 4. STUDIO MEMBER ASSIGNMENT SECURITY
    // =========================================================================

    @Test
    @DisplayName("Assignment security: cannot assign lead to user from another studio or inactive user")
    void testAssignmentSecurity() {
        TestSetup studioA = setupStudio("Studio One", "studio-one-assign");
        TestSetup studioB = setupStudio("Studio Two", "studio-two-assign");

        // Submit lead to Studio A
        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                studioA.slug(),
                null,
                "Client One",
                "+919876543210",
                "client1@example.com",
                "Bangalore",
                "Apartment",
                "20L",
                "Inquiry message",
                "ANY",
                true,
                false,
                UUID.randomUUID().toString(),
                null
        );
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        publicLeadController.submitLead(req, httpReq);

        MockHttpServletRequest studioAReq = new MockHttpServletRequest();
        studioAReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studioA.actor());
        UUID leadId = leadController.listLeads(
                studioAReq, studioA.studioId().toString(), null, null, null, null, "createdAt:desc", 20, 0
        ).getBody().items().get(0).id();

        // 1. Assigning Studio B's user to Studio A's lead throws BadRequestException
        LeadAssignmentRequest crossAssignReq = new LeadAssignmentRequest(studioB.user().id(), 1L);
        BadRequestException ex1 = assertThrows(BadRequestException.class, () ->
                leadController.assignLead(studioAReq, leadId, crossAssignReq, studioA.studioId().toString(), null)
        );
        assertTrue(ex1.getMessage().contains("active member of this studio"));

        // 2. Creating an inactive user in Studio A and attempting assignment throws BadRequestException
        UserRecord inactiveUser = createTestUser("Inactive Designer", "inactive@studio-a.com", "SUSPENDED");
        jdbcTemplate.update(
                "INSERT INTO studio_members (id, studio_id, user_id, role, granted_at) VALUES (?, ?, ?, 'MEMBER', now())",
                UuidV7.randomUuid(), studioA.studioId(), inactiveUser.id()
        );

        LeadAssignmentRequest inactiveAssignReq = new LeadAssignmentRequest(inactiveUser.id(), 1L);
        BadRequestException ex2 = assertThrows(BadRequestException.class, () ->
                leadController.assignLead(studioAReq, leadId, inactiveAssignReq, studioA.studioId().toString(), null)
        );
        assertTrue(ex2.getMessage().contains("active member of this studio"));
    }

    // =========================================================================
    // 5. OPTIMISTIC CONCURRENCY CONTROL
    // =========================================================================

    @Test
    @DisplayName("Optimistic concurrency: updating or archiving lead with stale version throws ConflictException (409)")
    void testOptimisticConcurrencyControl() {
        TestSetup studio = setupStudio("OCC Studio", "occ-studio");

        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                studio.slug(),
                null,
                "OCC Client",
                "+919876543210",
                "occ@example.com",
                "Coimbatore",
                "Apartment",
                "15L",
                "Testing OCC",
                "ANY",
                true,
                false,
                UUID.randomUUID().toString(),
                null
        );
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        publicLeadController.submitLead(req, httpReq);

        MockHttpServletRequest studioReq = new MockHttpServletRequest();
        studioReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studio.actor());
        UUID leadId = leadController.listLeads(
                studioReq, studio.studioId().toString(), null, null, null, null, "createdAt:desc", 20, 0
        ).getBody().items().get(0).id();

        // First update: from version 1 -> version 2 (succeeds)
        LeadUpdateRequest update1 = new LeadUpdateRequest("CONTACTED", null, null, 1L);
        leadController.updateLead(studioReq, leadId, update1, studio.studioId().toString(), null);

        // Stale update: trying to update with version 1 again -> ConflictException (409)
        LeadUpdateRequest staleUpdate = new LeadUpdateRequest("QUALIFIED", null, null, 1L);
        assertThrows(ConflictException.class, () ->
                leadController.updateLead(studioReq, leadId, staleUpdate, studio.studioId().toString(), null)
        );

        // Stale archive: trying to archive with version 1 -> ConflictException (409)
        assertThrows(ConflictException.class, () ->
                leadController.archiveLead(studioReq, leadId, 1L, studio.studioId().toString(), null)
        );
    }

    // =========================================================================
    // 6. STATUS TRANSITION STATE MACHINE HARDENING
    // =========================================================================

    @Test
    @DisplayName("State machine hardening: ARCHIVED cannot jump directly to WON, WON cannot jump to NEW, LOST cannot jump directly to WON")
    void testHardenedStatusTransitions() {
        TestSetup studio = setupStudio("SM Studio", "sm-studio");

        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                studio.slug(),
                null,
                "SM Client",
                "+919876543210",
                "sm@example.com",
                "Hyderabad",
                "Apartment",
                "20L",
                "Testing SM",
                "ANY",
                true,
                false,
                UUID.randomUUID().toString(),
                null
        );
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        publicLeadController.submitLead(req, httpReq);

        MockHttpServletRequest studioReq = new MockHttpServletRequest();
        studioReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studio.actor());
        UUID leadId = leadController.listLeads(
                studioReq, studio.studioId().toString(), null, null, null, null, "createdAt:desc", 20, 0
        ).getBody().items().get(0).id();

        // 1. Move to ARCHIVED (v1 -> v2)
        leadController.archiveLead(studioReq, leadId, 1L, studio.studioId().toString(), null);

        // 2. Try to jump directly from ARCHIVED to WON -> BadRequestException
        assertThrows(BadRequestException.class, () ->
                leadController.updateLead(studioReq, leadId, new LeadUpdateRequest("WON", null, null, 2L), studio.studioId().toString(), null)
        );

        // 3. ARCHIVED can safely reopen to IN_DISCUSSION (v2 -> v3)
        leadController.updateLead(studioReq, leadId, new LeadUpdateRequest("IN_DISCUSSION", null, null, 2L), studio.studioId().toString(), null);

        // 4. Mark WON (v3 -> v4)
        leadController.updateLead(studioReq, leadId, new LeadUpdateRequest("WON", null, null, 3L), studio.studioId().toString(), null);

        // 5. WON cannot jump back to NEW -> BadRequestException
        assertThrows(BadRequestException.class, () ->
                leadController.updateLead(studioReq, leadId, new LeadUpdateRequest("NEW", null, null, 4L), studio.studioId().toString(), null)
        );

        // 6. Move WON to IN_DISCUSSION (v4 -> v5) then to LOST (v5 -> v6)
        leadController.updateLead(studioReq, leadId, new LeadUpdateRequest("IN_DISCUSSION", null, null, 4L), studio.studioId().toString(), null);
        leadController.updateLead(studioReq, leadId, new LeadUpdateRequest("LOST", "Budget mismatch", null, 5L), studio.studioId().toString(), null);

        // 7. LOST cannot jump directly to WON -> BadRequestException
        assertThrows(BadRequestException.class, () ->
                leadController.updateLead(studioReq, leadId, new LeadUpdateRequest("WON", null, null, 6L), studio.studioId().toString(), null)
        );
    }

    // =========================================================================
    // 7. MONOTONIC WHATSAPP TRANSITIONS & IDEMPOTENCY
    // =========================================================================

    @Test
    @DisplayName("WhatsApp webhook monotonicity and status transition safety")
    void testWhatsAppWebhookMonotonicityAndIdempotency() {
        // 1. Unconfigured provider rejects webhook signatures
        ResponseEntity<Map<String, String>> invalidSigResp = whatsAppWebhookController.handleWebhook("invalid-signature", "{}");
        assertEquals(HttpStatus.UNAUTHORIZED, invalidSigResp.getStatusCode());

        // 2. Monotonic transition checks in domain state machine
        assertTrue(WhatsAppMessageStatus.QUEUED.canTransitionTo(WhatsAppMessageStatus.SUBMITTED));
        assertTrue(WhatsAppMessageStatus.SUBMITTED.canTransitionTo(WhatsAppMessageStatus.SENT));
        assertTrue(WhatsAppMessageStatus.SENT.canTransitionTo(WhatsAppMessageStatus.DELIVERED));
        assertTrue(WhatsAppMessageStatus.DELIVERED.canTransitionTo(WhatsAppMessageStatus.READ));

        // Regressions and duplicate events are strictly rejected:
        assertFalse(WhatsAppMessageStatus.DELIVERED.canTransitionTo(WhatsAppMessageStatus.DELIVERED),
                "Duplicate status event must be treated as false to prevent duplicate audit logs");
        assertFalse(WhatsAppMessageStatus.READ.canTransitionTo(WhatsAppMessageStatus.READ),
                "Duplicate read event must not re-trigger transition");
        assertFalse(WhatsAppMessageStatus.DELIVERED.canTransitionTo(WhatsAppMessageStatus.FAILED),
                "DELIVERED message cannot regress to FAILED");
        assertFalse(WhatsAppMessageStatus.READ.canTransitionTo(WhatsAppMessageStatus.FAILED),
                "READ message cannot regress to FAILED");
        assertFalse(WhatsAppMessageStatus.READ.canTransitionTo(WhatsAppMessageStatus.QUEUED),
                "READ message cannot regress to QUEUED");
    }

    // =========================================================================
    // 8. RATE LIMITING PROTECTION
    // =========================================================================

    @Test
    @DisplayName("Rate limiting: contact flood limit triggers after 3 submissions from same phone within 15 minutes")
    void testContactFloodRateLimit() {
        TestSetup studio = setupStudio("Rate Limit Studio", "rl-studio");
        String phone = "+919876543299";

        MockHttpServletRequest httpReq = new MockHttpServletRequest();

        // 3 submissions succeed
        for (int i = 0; i < 3; i++) {
            PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                    studio.slug(),
                    null,
                    "Client " + i,
                    phone,
                    "client" + i + "@example.com",
                    "Vijayawada",
                    "Commercial",
                    "10L",
                    "Message " + i,
                    "PHONE",
                    true,
                    false,
                    UUID.randomUUID().toString(),
                    null
            );
            ResponseEntity<PublicLeadSubmissionResponse> resp = publicLeadController.submitLead(req, httpReq);
            assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        }

        // 4th submission from same phone throws RateLimitExceededException
        PublicLeadSubmissionRequest req4 = new PublicLeadSubmissionRequest(
                studio.slug(),
                null,
                "Client 4",
                phone,
                "client4@example.com",
                "Vijayawada",
                "Commercial",
                "10L",
                "Message 4",
                "PHONE",
                true,
                false,
                UUID.randomUUID().toString(),
                null
        );
        assertThrows(RateLimitExceededException.class, () -> publicLeadController.submitLead(req4, httpReq));
    }
}
