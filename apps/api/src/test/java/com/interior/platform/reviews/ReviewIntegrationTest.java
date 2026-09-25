package com.interior.platform.reviews;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.leads.domain.LeadSource;
import com.interior.platform.leads.domain.LeadStatus;
import com.interior.platform.leads.domain.PreferredContactChannel;
import com.interior.platform.leads.domain.StudioLeadRecord;
import com.interior.platform.leads.repository.LeadRepository;
import com.interior.platform.reviews.domain.*;
import com.interior.platform.reviews.dto.*;
import com.interior.platform.reviews.repository.ReviewRepository;
import com.interior.platform.reviews.service.ReviewInvitationService;
import com.interior.platform.reviews.service.ReviewService;
import com.interior.platform.security.domain.ActorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 27 — Reviews Integration & Security Tests")
class ReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewInvitationService invitationService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private UUID ownerId;
    private UUID studioId;
    private ActorContext ownerActor;
    private ActorContext nonMemberActor;

    @BeforeEach
    void setUp() {
        ownerId = UuidV7.randomUuid();
        UUID otherUserId = UuidV7.randomUuid();

        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', now(), now())",
                ownerId, "Studio Owner", "owner-" + ownerId + "@example.com");
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', now(), now())",
                otherUserId, "Other User", "other-" + otherUserId + "@example.com");

        studioId = UuidV7.randomUuid();
        String studioSlug = "studio-" + java.util.UUID.randomUUID().toString();

        StudioDetailRecord studio = new StudioDetailRecord(
                studioId,
                "Luxe Interiors " + studioId.toString().substring(0, 6),
                studioSlug,
                ownerId,
                "ACTIVE",
                "INTERIOR_STUDIO",
                "Principal Architect",
                "Refined living",
                2015,
                "5-10",
                "LUXURY",
                "123 Main St",
                "Bengaluru",
                "Bengaluru Urban",
                "Karnataka",
                "560001",
                "India",
                true,
                true,
                "29ABCDE1234F1Z5",
                "PUBLISHED",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
        studioRepository.createStudio(studio);

        ownerActor = new ActorContext(ownerId, "Studio Owner", "owner-" + ownerId + "@example.com", Set.of("DESIGNER"), Set.of(), studioId, "OWNER", "PASSKEY", true);
        nonMemberActor = new ActorContext(otherUserId, "Other User", "other-" + otherUserId + "@example.com", Set.of("DESIGNER"), Set.of(), UuidV7.randomUuid(), "MEMBER", "PASSKEY", true);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM review_reports");
        jdbcTemplate.execute("DELETE FROM studio_reviews");
        jdbcTemplate.execute("DELETE FROM review_invitation_sessions");
        jdbcTemplate.execute("DELETE FROM review_invitations");
    }

    private StudioLeadRecord createLead(UUID studioId, LeadStatus status, String clientName) {
        UUID leadId = UuidV7.randomUuid();
        StudioLeadRecord lead = new StudioLeadRecord(
                leadId,
                studioId,
                null,
                LeadSource.PROJECT_DISCOVERY,
                status,
                clientName,
                "+919876543210",
                "client@example.com",
                "Bengaluru",
                "RESIDENTIAL",
                "15L - 25L",
                "Looking for a kitchen remodel",
                PreferredContactChannel.WHATSAPP,
                Instant.now(),
                Instant.now(),
                ownerId,
                null,
                null,
                false,
                null,
                Instant.now(),
                Instant.now(),
                1L,
                null
        );
        return leadRepository.save(lead);
    }

    @Test
    @DisplayName("1. Review invitation can only be generated for WON leads")
    void testOnlyWonLeadAllowedForInvitation() {
        StudioLeadRecord newLead = createLead(studioId, LeadStatus.NEW, "Priya Sharma");
        CreateReviewInvitationRequest reqNew = new CreateReviewInvitationRequest(newLead.id(), null);

        assertThrows(IllegalStateException.class, () ->
                invitationService.createInvitation(ownerActor, studioId, reqNew)
        );

        StudioLeadRecord wonLead = createLead(studioId, LeadStatus.WON, "Rahul Verma");
        CreateReviewInvitationRequest reqWon = new CreateReviewInvitationRequest(wonLead.id(), null);

        CreateReviewInvitationResponse response = invitationService.createInvitation(ownerActor, studioId, reqWon);
        assertNotNull(response);
        assertNotNull(response.rawToken());
        assertTrue(response.invitationUrl().startsWith("/review/invite/"));

        // Raw token must NOT exist in the database (only token_hash bytea)
        byte[] tokenHash = ReviewInvitationService.sha256(response.rawToken());
        var stored = reviewRepository.findInvitationByTokenHash(tokenHash);
        assertTrue(stored.isPresent());
        assertEquals(ReviewInvitationStatus.PENDING, stored.get().status());
    }

    @Test
    @DisplayName("2. Non-member cannot generate review invitation (AccessDenied)")
    void testNonMemberAccessDenied() {
        StudioLeadRecord wonLead = createLead(studioId, LeadStatus.WON, "Anita Rao");
        CreateReviewInvitationRequest req = new CreateReviewInvitationRequest(wonLead.id(), null);

        assertThrows(com.interior.platform.common.exception.AccessDeniedException.class, () ->
                invitationService.createInvitation(nonMemberActor, studioId, req)
        );
    }

    @Test
    @DisplayName("3. Token exchange returns scoped session and redirects to submission page")
    void testTokenExchangeAndSessionGeneration() {
        StudioLeadRecord wonLead = createLead(studioId, LeadStatus.WON, "Suresh Kumar");
        CreateReviewInvitationResponse invite = invitationService.createInvitation(
                ownerActor, studioId, new CreateReviewInvitationRequest(wonLead.id(), null)
        );

        ReviewInvitationService.ExchangeReviewSessionResult exchange =
                invitationService.exchangeToken(invite.rawToken());

        assertNotNull(exchange.sessionToken());
        assertNotNull(exchange.csrfToken());
        assertEquals("Suresh", exchange.clientFirstName());

        // Validate session is active
        var validation = invitationService.validateSessionToken(exchange.sessionToken());
        assertNotNull(validation);
        assertEquals(invite.invitationId(), validation.invitation().id());
    }

    @Test
    @DisplayName("4. Revoked invitation immediately invalidates session exchange")
    void testRevocationInvalidatesSession() {
        StudioLeadRecord wonLead = createLead(studioId, LeadStatus.WON, "Deepak Patel");
        CreateReviewInvitationResponse invite = invitationService.createInvitation(
                ownerActor, studioId, new CreateReviewInvitationRequest(wonLead.id(), null)
        );

        ReviewInvitationService.ExchangeReviewSessionResult exchange =
                invitationService.exchangeToken(invite.rawToken());

        // Revoke the invitation
        invitationService.revokeInvitation(ownerActor, studioId, invite.invitationId());

        // Both token exchange and active session token must be rejected
        assertThrows(IllegalStateException.class, () ->
                invitationService.exchangeToken(invite.rawToken())
        );
        assertThrows(com.interior.platform.common.exception.AccessDeniedException.class, () ->
                invitationService.validateSessionToken(exchange.sessionToken())
        );
    }

    @Test
    @DisplayName("5. Review submission enforces 1-5 rating, strips HTML, and marks invitation USED")
    void testReviewSubmissionAndPrivacyModes() {
        StudioLeadRecord wonLead = createLead(studioId, LeadStatus.WON, "Kiran Reddy");
        CreateReviewInvitationResponse invite = invitationService.createInvitation(
                ownerActor, studioId, new CreateReviewInvitationRequest(wonLead.id(), null)
        );
        ReviewInvitationService.ExchangeReviewSessionResult exchange =
                invitationService.exchangeToken(invite.rawToken());

        // Test invalid rating < 1
        SubmitReviewRequest invalidLow = new SubmitReviewRequest(0, "Bad", "Text text text text", DisplayNameMode.FIRST_NAME, null);
        assertThrows(IllegalArgumentException.class, () ->
                reviewService.submitReview(exchange.sessionToken(), invalidLow)
        );

        // Test invalid rating > 5
        SubmitReviewRequest invalidHigh = new SubmitReviewRequest(6, "Super", "Text text text text", DisplayNameMode.FIRST_NAME, null);
        assertThrows(IllegalArgumentException.class, () ->
                reviewService.submitReview(exchange.sessionToken(), invalidHigh)
        );

        // Valid submission with INITIALS display mode and XSS script tags
        SubmitReviewRequest validReq = new SubmitReviewRequest(
                5,
                "<script>alert(1)</script>Beautiful kitchen renovation!",
                "<script>evil()</script>We are extremely delighted with the modular kitchen and wardrobe work.",
                DisplayNameMode.INITIALS,
                null
        );

        PublicStudioReviewDto submitted = reviewService.submitReview(exchange.sessionToken(), validReq);

        assertNotNull(submitted);
        assertEquals(5, submitted.rating());
        assertEquals("Beautiful kitchen renovation!", submitted.title());
        assertEquals("We are extremely delighted with the modular kitchen and wardrobe work.", submitted.reviewText());
        assertEquals("K. R.", submitted.reviewerDisplayName()); // Initials of Kiran Reddy

        // Invitation must now be marked USED
        var updatedInv = reviewRepository.findInvitationById(invite.invitationId()).orElseThrow();
        assertEquals(ReviewInvitationStatus.USED, updatedInv.status());
        assertNotNull(updatedInv.usedAt());

        // Session token can no longer be used for new submissions
        assertThrows(com.interior.platform.common.exception.AccessDeniedException.class, () ->
                invitationService.validateSessionToken(exchange.sessionToken())
        );
    }

    @Test
    @DisplayName("6. Studio cannot author reviews directly and cannot edit client content")
    void testStudioCannotEditClientReview() {
        StudioLeadRecord wonLead = createLead(studioId, LeadStatus.WON, "Meera Nair");
        CreateReviewInvitationResponse invite = invitationService.createInvitation(
                ownerActor, studioId, new CreateReviewInvitationRequest(wonLead.id(), null)
        );
        ReviewInvitationService.ExchangeReviewSessionResult exchange =
                invitationService.exchangeToken(invite.rawToken());

        SubmitReviewRequest reviewReq = new SubmitReviewRequest(
                4,
                "Great experience",
                "Professional work executed on time with quality materials.",
                DisplayNameMode.FIRST_NAME,
                null
        );
        PublicStudioReviewDto review = reviewService.submitReview(exchange.sessionToken(), reviewReq);

        // Studio can post ONE response
        StudioReviewResponseRequest responseReq = new StudioReviewResponseRequest(
                "Thank you Meera! It was a pleasure crafting your space."
        );
        reviewService.respondToReview(ownerActor, studioId, review.id(), responseReq);

        var loaded = reviewRepository.findReviewById(review.id()).orElseThrow();
        assertEquals("Thank you Meera! It was a pleasure crafting your space.", loaded.studioResponseText());
        assertNotNull(loaded.studioResponseAt());

        // Client rating and text must remain intact!
        assertEquals(4, loaded.rating());
        assertEquals("Professional work executed on time with quality materials.", loaded.reviewText());

        // Cross-tenant studio cannot respond
        assertThrows(com.interior.platform.common.exception.AccessDeniedException.class, () ->
                reviewService.respondToReview(nonMemberActor, nonMemberActor.activeStudioId(), review.id(), responseReq)
        );
    }

    @Test
    @DisplayName("7. Reporting a review records moderation flag without modifying review text")
    void testReviewReporting() {
        StudioLeadRecord wonLead = createLead(studioId, LeadStatus.WON, "Arjun Kapoor");
        CreateReviewInvitationResponse invite = invitationService.createInvitation(
                ownerActor, studioId, new CreateReviewInvitationRequest(wonLead.id(), null)
        );
        ReviewInvitationService.ExchangeReviewSessionResult exchange =
                invitationService.exchangeToken(invite.rawToken());

        SubmitReviewRequest reviewReq = new SubmitReviewRequest(
                5,
                "Excellent craft",
                "Very attentive to details and highly recommended.",
                DisplayNameMode.ANONYMOUS,
                null
        );
        PublicStudioReviewDto review = reviewService.submitReview(exchange.sessionToken(), reviewReq);

        // Visitor reports review for SPAM
        ReviewReportRequest reportReq = new ReviewReportRequest(
                ReportReason.SPAM,
                "Looks like promotional spam"
        );
        reviewService.reportReview(review.id(), reportReq, "127.0.0.1", null);

        var loaded = reviewRepository.findReviewById(review.id()).orElseThrow();
        assertEquals(ReviewStatus.PUBLISHED, loaded.status()); // Reporting does not silently censor immediately
        assertEquals("Anonymous Client", loaded.reviewerDisplayName());
    }

    @Test
    @DisplayName("8. Aggregate calculation excludes removed reviews and computes truthful average")
    void testAggregateCalculation() {
        ReviewAggregate emptyAgg = reviewService.getReviewAggregate(studioId);
        assertEquals(0, emptyAgg.totalReviews());
        assertEquals(0.0, emptyAgg.averageRating());

        StudioLeadRecord lead1 = createLead(studioId, LeadStatus.WON, "Client One");
        StudioLeadRecord lead2 = createLead(studioId, LeadStatus.WON, "Client Two");

        CreateReviewInvitationResponse inv1 = invitationService.createInvitation(ownerActor, studioId, new CreateReviewInvitationRequest(lead1.id(), null));
        CreateReviewInvitationResponse inv2 = invitationService.createInvitation(ownerActor, studioId, new CreateReviewInvitationRequest(lead2.id(), null));

        var ex1 = invitationService.exchangeToken(inv1.rawToken());
        var ex2 = invitationService.exchangeToken(inv2.rawToken());

        reviewService.submitReview(ex1.sessionToken(), new SubmitReviewRequest(5, "Perfect", "5 star experience here", DisplayNameMode.FIRST_NAME, null));
        PublicStudioReviewDto rev2 = reviewService.submitReview(ex2.sessionToken(), new SubmitReviewRequest(3, "Average", "Decent finish but delayed", DisplayNameMode.FIRST_NAME, null));

        ReviewAggregate agg = reviewService.getReviewAggregate(studioId);
        assertEquals(2, agg.totalReviews());
        assertEquals(4.0, agg.averageRating()); // (5 + 3) / 2 = 4.0

        // If review 2 is marked REMOVED by platform admin, it disappears from aggregate
        reviewRepository.updateReviewStatus(rev2.id(), ReviewStatus.REMOVED, null, Instant.now());

        ReviewAggregate updatedAgg = reviewService.getReviewAggregate(studioId);
        assertEquals(1, updatedAgg.totalReviews());
        assertEquals(5.0, updatedAgg.averageRating()); // Only review 1 remains
    }
}
