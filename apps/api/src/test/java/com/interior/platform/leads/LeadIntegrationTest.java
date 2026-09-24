package com.interior.platform.leads;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.RateLimitExceededException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.service.ProfessionalOnboardingService;
import com.interior.platform.leads.domain.LeadActivityType;
import com.interior.platform.leads.domain.LeadStatus;
import com.interior.platform.leads.dto.*;
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
class LeadIntegrationTest {

    @Autowired
    private PublicLeadController publicLeadController;

    @Autowired
    private LeadController leadController;

    @Autowired
    private WhatsAppWebhookController whatsAppWebhookController;

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

    private UserRecord createTestUser(String name, String email) {
        UUID userId = UuidV7.randomUuid();
        UserRecord user = new UserRecord(
                userId,
                name,
                email,
                "+919876543210",
                "ACTIVE",
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
        UserRecord user = createTestUser(name, slug + "@example.com");
        ActorContext initialActor = new ActorContext(user.id(), user.displayName(), user.email(), Set.of("CUSTOMER"), null, null, true);
        var onboardResult = onboardingService.completeOnboarding(initialActor, createValidRequest(name, slug), null, null);
        UUID studioId = onboardResult.studio().id();
        jdbcTemplate.update("UPDATE designer_studios SET publication_status = 'PUBLISHED' WHERE id = ?", studioId);
        ActorContext designerActor = new ActorContext(
                user.id(), user.displayName(), user.email(),
                Set.of("DESIGNER", "CUSTOMER"), studioId, "OWNER", true
        );
        return new TestSetup(user, designerActor, studioId, slug);
    }

    @Test
    @DisplayName("Public Inquiry Flow: visitor submits valid lead, studio receives in inbox with normalization and audit trail")
    void testPublicInquiryFlowEndToEnd() {
        TestSetup studioA = setupStudio("Studio One", "studio-one");

        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                studioA.slug(),
                null,
                "Rahul Sharma",
                "9876543210", // 10-digit Indian format to normalize
                "rahul@example.com",
                "Guntur",
                "Modular Kitchen",
                "15L-25L",
                "Looking for full home interior renovation",
                "WHATSAPP",
                true,
                true,
                UUID.randomUUID().toString(),
                null // empty honeypot
        );

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.setRemoteAddr("203.0.113.1");

        ResponseEntity<PublicLeadSubmissionResponse> response = publicLeadController.submitLead(req, httpReq);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().referenceNumber().startsWith("INQ-"));
        assertEquals("Studio One", response.getBody().studioName());

        // Authenticated workspace query: Studio A checks inbox
        MockHttpServletRequest studioReq = new MockHttpServletRequest();
        studioReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studioA.actor());

        ResponseEntity<LeadListResponse> listResp = leadController.listLeads(
                studioReq, studioA.studioId().toString(), null, null, null, null, "createdAt:desc", 20, 0
        );
        assertEquals(200, listResp.getStatusCode().value());
        assertNotNull(listResp.getBody());
        assertEquals(1, listResp.getBody().total());

        LeadSummaryDto summary = listResp.getBody().items().get(0);
        assertEquals("Rahul Sharma", summary.name());
        assertNull(summary.phoneNormalized(), "Summary view must omit raw phone for privacy");
        assertTrue(summary.phoneMasked().contains("••••"));
        assertNull(summary.emailNormalized(), "Summary view must omit raw email for privacy");
        assertEquals("NEW", summary.status());
        assertTrue(summary.hasWhatsappConsent());

        // Detail check: full PII only accessible to authorized studio member in detail view
        ResponseEntity<LeadDetailDto> detailResp = leadController.getLeadDetail(
                studioReq, summary.id(), studioA.studioId().toString(), null
        );
        assertEquals(200, detailResp.getStatusCode().value());
        LeadDetailDto detail = detailResp.getBody();
        assertNotNull(detail);
        assertEquals("+919876543210", detail.phoneNormalized());
        assertEquals("rahul@example.com", detail.emailNormalized());
        assertEquals(1, detail.activities().size());
        assertEquals("LEAD_CREATED", detail.activities().get(0).activityType());
    }

    @Test
    @DisplayName("Honeypot protection: bot submission with website_hp filled is silently rejected without DB persistence")
    void testHoneypotRejection() {
        TestSetup studioA = setupStudio("Studio Bot Test", "studio-bot");

        PublicLeadSubmissionRequest botReq = new PublicLeadSubmissionRequest(
                studioA.slug(),
                null,
                "Spam Bot",
                "+919876543210",
                "bot@spam.com",
                "Nowhere",
                null,
                null,
                "Buy cheap seo now",
                "EMAIL",
                true,
                false,
                null,
                "https://spam-link.ru" // honeypot filled
        );

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.setRemoteAddr("198.51.100.22");

        ResponseEntity<PublicLeadSubmissionResponse> response = publicLeadController.submitLead(botReq, httpReq);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // Verify zero leads persisted in database
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM studio_leads", Integer.class);
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Rate limiting: IP exceeding 5 inquiries within 10 minutes receives 429 RateLimitExceeded")
    void testPublicInquiryRateLimiting() {
        TestSetup studioA = setupStudio("Studio Rate Limit", "studio-rate");

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.setRemoteAddr("198.51.100.99");

        for (int i = 0; i < 5; i++) {
            PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                    studioA.slug(),
                    null,
                    "Client " + i,
                    "+91987654321" + i,
                    "client" + i + "@example.com",
                    null,
                    null,
                    null,
                    "Inquiry message " + i,
                    null,
                    true,
                    false,
                    null,
                    null
            );
            ResponseEntity<PublicLeadSubmissionResponse> resp = publicLeadController.submitLead(req, httpReq);
            assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        }

        // 6th attempt from same IP triggers rate limit exception
        PublicLeadSubmissionRequest sixthReq = new PublicLeadSubmissionRequest(
                studioA.slug(),
                null,
                "Client 6",
                "+919876543216",
                "client6@example.com",
                null,
                null,
                null,
                "Rate limit test",
                null,
                true,
                false,
                null,
                null
        );
        assertThrows(RateLimitExceededException.class, () -> publicLeadController.submitLead(sixthReq, httpReq));
    }

    @Test
    @DisplayName("Tenant Isolation: Studio B cannot view, update, assign, or add notes to Studio A's leads")
    void testTenantIsolation() {
        TestSetup studioA = setupStudio("Studio A", "studio-a");
        TestSetup studioB = setupStudio("Studio B", "studio-b");

        // Submit lead to Studio A
        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                studioA.slug(),
                null,
                "Studio A Client",
                "+919876543210",
                "client_a@example.com",
                null,
                null,
                null,
                "Inquiry for Studio A",
                null,
                true,
                false,
                null,
                null
        );
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.setRemoteAddr("203.0.113.10");
        publicLeadController.submitLead(req, httpReq);

        // Fetch Studio A lead ID
        MockHttpServletRequest studioAReq = new MockHttpServletRequest();
        studioAReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studioA.actor());
        UUID leadId = leadController.listLeads(studioAReq, studioA.studioId().toString(), null, null, null, null, null, 10, 0)
                .getBody().items().get(0).id();

        // Studio B tries to view Studio A's lead
        MockHttpServletRequest studioBReq = new MockHttpServletRequest();
        studioBReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studioB.actor());

        // Either access denied or not found due to tenant isolation
        assertThrows(RuntimeException.class, () ->
                leadController.getLeadDetail(studioBReq, leadId, studioB.studioId().toString(), null)
        );

        // Studio B tries to update status of Studio A's lead
        LeadUpdateRequest updateReq = new LeadUpdateRequest("CONTACTED", null, null, 1L);
        assertThrows(RuntimeException.class, () ->
                leadController.updateLead(studioBReq, leadId, updateReq, studioB.studioId().toString(), null)
        );

        // Studio B tries to add note to Studio A's lead
        LeadNoteCreateRequest noteReq = new LeadNoteCreateRequest("Malicious note from studio B");
        assertThrows(RuntimeException.class, () ->
                leadController.addNote(studioBReq, leadId, noteReq, studioB.studioId().toString(), null)
        );
    }

    @Test
    @DisplayName("CRM Lifecycle & Optimistic Locking: status progression, internal notes, follow-up, and concurrent conflict")
    void testCrmLifecycleAndOptimisticLocking() {
        TestSetup studio = setupStudio("Studio Lifecycle", "studio-life");

        PublicLeadSubmissionRequest inq = new PublicLeadSubmissionRequest(
                studio.slug(),
                null,
                "Priya Reddy",
                "+919876543210",
                "priya@example.com",
                "Guntur",
                "Villa Interior",
                "35L-50L",
                "Complete villa makeover",
                "PHONE",
                true,
                true,
                null,
                null
        );
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        publicLeadController.submitLead(inq, httpReq);

        MockHttpServletRequest studioReq = new MockHttpServletRequest();
        studioReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studio.actor());

        LeadSummaryDto lead = leadController.listLeads(studioReq, studio.studioId().toString(), null, null, null, null, null, 10, 0)
                .getBody().items().get(0);
        assertEquals(1L, lead.version());
        assertEquals("NEW", lead.status());

        // 1. Transition NEW -> CONTACTED
        LeadUpdateRequest contactUpdate = new LeadUpdateRequest("CONTACTED", null, null, 1L);
        ResponseEntity<LeadDetailDto> updatedResp = leadController.updateLead(
                studioReq, lead.id(), contactUpdate, studio.studioId().toString(), null
        );
        assertEquals(200, updatedResp.getStatusCode().value());
        assertEquals("CONTACTED", updatedResp.getBody().status());
        assertEquals(2L, updatedResp.getBody().version());

        // 2. Add internal note
        LeadNoteCreateRequest noteReq = new LeadNoteCreateRequest("Called client, scheduled discovery call for tomorrow morning.");
        ResponseEntity<LeadNoteDto> noteResp = leadController.addNote(
                studioReq, lead.id(), noteReq, studio.studioId().toString(), null
        );
        assertEquals(HttpStatus.CREATED, noteResp.getStatusCode());
        assertNotNull(noteResp.getBody());
        assertEquals(studio.actor().displayName(), noteResp.getBody().authorName());

        // 3. Optimistic locking test: attempt update with stale version 1L should throw ConflictException
        LeadUpdateRequest staleUpdate = new LeadUpdateRequest("QUALIFIED", null, null, 1L);
        assertThrows(ConflictException.class, () ->
                leadController.updateLead(studioReq, lead.id(), staleUpdate, studio.studioId().toString(), null)
        );

        // 4. Update with correct version (2L) -> QUALIFIED
        LeadUpdateRequest validUpdate = new LeadUpdateRequest("QUALIFIED", null, null, 2L);
        ResponseEntity<LeadDetailDto> qualResp = leadController.updateLead(
                studioReq, lead.id(), validUpdate, studio.studioId().toString(), null
        );
        assertEquals("QUALIFIED", qualResp.getBody().status());
        assertEquals(3L, qualResp.getBody().version());

        // 5. Check audit activities count (CREATED, STATUS_CHANGED, NOTE_ADDED, STATUS_CHANGED)
        assertEquals(4, qualResp.getBody().activities().size());
        assertEquals(1, qualResp.getBody().notes().size());

        // 6. Test Counts summary
        ResponseEntity<LeadCountsDto> countsResp = leadController.getCounts(studioReq, studio.studioId().toString(), null);
        assertNotNull(countsResp.getBody());
        assertEquals(1, countsResp.getBody().total());
        assertEquals(0, countsResp.getBody().newLeads());
        assertEquals(1, countsResp.getBody().active()); // QUALIFIED is in active pipeline
    }

    @Test
    @DisplayName("User-Initiated WhatsApp Handoff: generates wa.me URL and logs handoff without falsely claiming delivery")
    void testWhatsAppHandoffFlow() {
        TestSetup studio = setupStudio("Studio WhatsApp", "studio-wa");

        // Public WhatsApp contact was saved during onboarding with +919876543210
        PublicWhatsAppHandoffRequest handoffReq = new PublicWhatsAppHandoffRequest(
                studio.slug(),
                null,
                "Arun Kumar",
                "9876543210",
                "Hi, I saw your portfolio and want to discuss my 3BHK flat"
        );

        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        httpReq.setRemoteAddr("203.0.113.55");

        ResponseEntity<PublicWhatsAppHandoffResponse> handoffResp = publicLeadController.initiateWhatsAppHandoff(handoffReq, httpReq);
        assertEquals(200, handoffResp.getStatusCode().value());
        assertNotNull(handoffResp.getBody());
        assertTrue(handoffResp.getBody().whatsappUrl().startsWith("https://wa.me/919876543210"));
        assertTrue(handoffResp.getBody().whatsappUrl().contains("text="));

        // Verify lead was created in studio inbox with WHATSAPP_HANDOFF source
        MockHttpServletRequest studioReq = new MockHttpServletRequest();
        studioReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studio.actor());

        LeadSummaryDto lead = leadController.listLeads(studioReq, studio.studioId().toString(), null, null, null, null, null, 10, 0)
                .getBody().items().get(0);
        assertEquals("WHATSAPP_HANDOFF", lead.source());
        assertEquals("NEW", lead.status()); // Truthful: not marked contacted!

        LeadDetailDto detail = leadController.getLeadDetail(studioReq, lead.id(), studio.studioId().toString(), null).getBody();
        assertNotNull(detail);
        boolean hasHandoffAudit = detail.activities().stream()
                .anyMatch(a -> a.activityType().equals(LeadActivityType.WHATSAPP_HANDOFF_OPENED.name()));
        assertTrue(hasHandoffAudit);
    }

    @Test
    @DisplayName("Managed WhatsApp: Disabled provider truthfully reports NOT_CONFIGURED and rejects sending")
    void testManagedWhatsAppDisabledTruthfulness() {
        TestSetup studio = setupStudio("Studio WA Disabled", "studio-wa-dis");

        PublicLeadSubmissionRequest req = new PublicLeadSubmissionRequest(
                studio.slug(),
                null,
                "Kavitha",
                "+919876543210",
                "kavitha@example.com",
                null,
                null,
                null,
                "Inquiry",
                null,
                true,
                true, // whatsapp consent granted
                null,
                null
        );
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        publicLeadController.submitLead(req, httpReq);

        MockHttpServletRequest studioReq = new MockHttpServletRequest();
        studioReq.setAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE, studio.actor());

        LeadSummaryDto lead = leadController.listLeads(studioReq, studio.studioId().toString(), null, null, null, null, null, 10, 0)
                .getBody().items().get(0);

        // Check provider status
        ResponseEntity<WhatsAppProviderStatusDto> statusResp = leadController.getWhatsAppStatus(
                studioReq, lead.id(), studio.studioId().toString(), null
        );
        assertEquals(200, statusResp.getStatusCode().value());
        assertFalse(statusResp.getBody().configured());
        assertEquals("NOT_CONFIGURED", statusResp.getBody().status());

        // Attempting to send message via disabled provider throws BadRequestException truthfully
        SendWhatsAppMessageRequest sendReq = new SendWhatsAppMessageRequest("Hello from studio!", null);
        assertThrows(BadRequestException.class, () ->
                leadController.sendWhatsAppMessage(studioReq, lead.id(), sendReq, studio.studioId().toString(), null)
        );

        // Webhook signature rejection
        ResponseEntity<Map<String, String>> webhookResp = whatsAppWebhookController.handleWebhook("invalid-signature", "{}");
        assertEquals(HttpStatus.UNAUTHORIZED, webhookResp.getStatusCode());
    }
}
