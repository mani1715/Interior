package com.interior.platform.verification;

import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.verification.domain.*;
import com.interior.platform.verification.dto.*;
import com.interior.platform.verification.service.StudioVerificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Phase 27 — Studio Verification & Trust Boundary Tests")
class StudioVerificationTest {

    @Autowired
    private StudioVerificationService verificationService;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private UUID ownerId;
    private UUID studioId;
    private ActorContext ownerActor;
    private ActorContext adminActor;
    private ActorContext nonMemberActor;

    @BeforeEach
    void setUp() {
        ownerId = UuidV7.randomUuid();
        UUID adminId = UuidV7.randomUuid();
        UUID otherUserId = UuidV7.randomUuid();

        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', now(), now())",
                ownerId, "Studio Owner", "owner-" + ownerId + "@example.com");
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', now(), now())",
                adminId, "Platform Admin", "admin-" + adminId + "@example.com");
        jdbcTemplate.update("INSERT INTO users (id, display_name, email, status, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', now(), now())",
                otherUserId, "Other User", "other-" + otherUserId + "@example.com");

        studioId = UuidV7.randomUuid();
        String slug = "studio-" + java.util.UUID.randomUUID().toString();

        StudioDetailRecord studio = new StudioDetailRecord(
                studioId,
                "Studio Elegance",
                slug,
                ownerId,
                "ACTIVE",
                "INTERIOR_STUDIO",
                "Design Principal",
                "Bespoke interiors",
                2018,
                "10-20",
                "PREMIUM",
                "456 Brigade Rd",
                "Bengaluru",
                "Bengaluru Urban",
                "Karnataka",
                "560025",
                "India",
                true,
                true,
                "29XYZAB5678C1Z9",
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
        adminActor = new ActorContext(adminId, "Platform Admin", "admin-" + adminId + "@example.com", Set.of("ADMIN"), Set.of(), null, null, "PASSKEY", true);
        nonMemberActor = new ActorContext(otherUserId, "Other User", "other-" + otherUserId + "@example.com", Set.of("DESIGNER"), Set.of(), UuidV7.randomUuid(), "MEMBER", "PASSKEY", true);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM studio_verification_events");
        jdbcTemplate.execute("DELETE FROM studio_verification_documents");
        jdbcTemplate.execute("DELETE FROM studio_verifications");
    }

    @Test
    @DisplayName("1. Studio submission transitions to PENDING and records SUBMITTED event")
    void testStudioSubmission() {
        StudioVerificationDto initial = verificationService.getStudioVerification(ownerActor, studioId);
        assertEquals("NOT_SUBMITTED", initial.status());

        SubmitVerificationRequest req = new SubmitVerificationRequest(
                "Studio Elegance Pvt Ltd",
                "INTERIOR_DESIGNER",
                "CIN-U74999KA2018PTC112345",
                "29XYZAB5678C1Z9",
                "https://studioelegance.in",
                "Registered business entity operating since 2018"
        );

        StudioVerificationDto submitted = verificationService.submitVerification(ownerActor, studioId, req);
        assertEquals("PENDING", submitted.status());
        assertEquals("Studio Elegance Pvt Ltd", submitted.businessName());
        assertEquals("CIN-U74999KA2018PTC112345", submitted.registrationNumber());
        assertFalse(submitted.events().isEmpty());
        assertEquals("SUBMITTED", submitted.events().get(0).eventType());
    }

    @Test
    @DisplayName("2. Cross-tenant studio cannot access or submit verification for another studio")
    void testCrossTenantVerificationDenied() {
        assertThrows(com.interior.platform.common.exception.AccessDeniedException.class, () ->
                verificationService.getStudioVerification(nonMemberActor, studioId)
        );

        SubmitVerificationRequest req = new SubmitVerificationRequest("Malicious Studio", "INTERIOR_DESIGNER", null, null, null, null);
        assertThrows(com.interior.platform.common.exception.AccessDeniedException.class, () ->
                verificationService.submitVerification(nonMemberActor, studioId, req)
        );
    }

    @Test
    @DisplayName("3. Document upload validates allowed MIME types and file size bounds")
    void testDocumentUploadValidation() {
        // Valid PDF upload
        VerificationDocumentDto pdfDoc = verificationService.uploadDocument(
                ownerActor,
                studioId,
                VerificationDocumentType.BUSINESS_REGISTRATION,
                "incorporation.pdf",
                "application/pdf",
                1024 * 50 // 50 KB
        );
        assertNotNull(pdfDoc);
        assertEquals("BUSINESS_REGISTRATION", pdfDoc.documentType());
        assertEquals("incorporation.pdf", pdfDoc.originalFilename());

        // Invalid MIME type (e.g., executable)
        assertThrows(IllegalArgumentException.class, () ->
                verificationService.uploadDocument(
                        ownerActor,
                        studioId,
                        VerificationDocumentType.OTHER,
                        "script.sh",
                        "application/x-sh",
                        1024
                )
        );

        // Oversized file (> 10MB)
        assertThrows(IllegalArgumentException.class, () ->
                verificationService.uploadDocument(
                        ownerActor,
                        studioId,
                        VerificationDocumentType.GST_CERTIFICATE,
                        "large.pdf",
                        "application/pdf",
                        11L * 1024L * 1024L
                )
        );
    }

    @Test
    @DisplayName("4. Studio cannot self-approve; only platform admin can approve verification")
    void testStudioCannotSelfApprove() {
        SubmitVerificationRequest req = new SubmitVerificationRequest(
                "Studio Elegance", "INTERIOR_DESIGNER", "REG-123", "29XYZAB5678C1Z9", "https://elegance.in", "Notes"
        );
        verificationService.submitVerification(ownerActor, studioId, req);

        AdminVerificationDecisionRequest decision = new AdminVerificationDecisionRequest(
                VerificationStatus.VERIFIED, "Approved after verifying government portal registry"
        );

        // Studio Owner tries to approve itself -> DENIED
        assertThrows(com.interior.platform.common.exception.AccessDeniedException.class, () ->
                verificationService.adminDecision(ownerActor, studioId, decision)
        );

        // Platform Admin approves -> SUCCESS
        StudioVerificationDto approved = verificationService.adminDecision(adminActor, studioId, decision);
        assertEquals("VERIFIED", approved.status());
        assertNotNull(approved.verifiedAt());
        assertNotNull(approved.expiresAt());

        // Public badge reflects truthful verified state
        PublicVerificationBadgeDto badge = verificationService.getPublicBadge(studioId);
        assertTrue(badge.isVerified());
        assertEquals("Verified Business", badge.badgeLabel());
        assertTrue(badge.description().contains("does not guarantee service quality"));
    }

    @Test
    @DisplayName("5. Critical profile changes invalidate verification and trigger REVERIFY_REQUIRED")
    void testCriticalProfileChangeInvalidation() {
        SubmitVerificationRequest req = new SubmitVerificationRequest(
                "Studio Elegance", "INTERIOR_DESIGNER", "REG-123", "29XYZAB5678C1Z9", "https://elegance.in", "Notes"
        );
        verificationService.submitVerification(ownerActor, studioId, req);

        AdminVerificationDecisionRequest decision = new AdminVerificationDecisionRequest(
                VerificationStatus.VERIFIED, "Verified"
        );
        verificationService.adminDecision(adminActor, studioId, decision);

        PublicVerificationBadgeDto badgeBefore = verificationService.getPublicBadge(studioId);
        assertTrue(badgeBefore.isVerified());

        // Critical change: Studio changes business name to "Renovate Pro Inc"
        verificationService.handleCriticalProfileChange(studioId, "Renovate Pro Inc", "INTERIOR_DESIGNER");

        // Verification status must immediately transition to REVERIFY_REQUIRED
        StudioVerificationDto updated = verificationService.getStudioVerification(ownerActor, studioId);
        assertEquals("REVERIFY_REQUIRED", updated.status());

        // Public badge disappears immediately!
        PublicVerificationBadgeDto badgeAfter = verificationService.getPublicBadge(studioId);
        assertFalse(badgeAfter.isVerified());
        assertNull(badgeAfter.badgeLabel());
    }
}
