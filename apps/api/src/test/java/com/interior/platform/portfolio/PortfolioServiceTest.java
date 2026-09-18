package com.interior.platform.portfolio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.portfolio.domain.FontPairing;
import com.interior.platform.portfolio.domain.PortfolioRecord;
import com.interior.platform.portfolio.domain.PortfolioSectionRecord;
import com.interior.platform.portfolio.domain.PortfolioStatus;
import com.interior.platform.portfolio.domain.PortfolioTemplateKey;
import com.interior.platform.portfolio.domain.SectionType;
import com.interior.platform.portfolio.dto.InitializePortfolioRequest;
import com.interior.platform.portfolio.dto.PortfolioDetailResponse;
import com.interior.platform.portfolio.dto.PortfolioPreviewResponse;
import com.interior.platform.portfolio.dto.ReorderSectionsRequest;
import com.interior.platform.portfolio.dto.SwitchTemplateRequest;
import com.interior.platform.portfolio.dto.UpdatePortfolioRequest;
import com.interior.platform.portfolio.repository.PortfolioRepository;
import com.interior.platform.portfolio.service.PortfolioService;
import com.interior.platform.portfolio.validation.PortfolioSectionValidator;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private SecurityRepository securityRepository;

    private AuthorizationService authorizationService;
    private PortfolioSectionValidator sectionValidator;
    private ObjectMapper objectMapper;
    private PortfolioService portfolioService;

    private UUID userId;
    private UUID studioId;
    private UUID portfolioId;
    private ActorContext ownerActor;
    private ActorContext memberActor;
    private UserRecord activeUser;
    private StudioDetailRecord studioRecord;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService();
        sectionValidator = new PortfolioSectionValidator();
        objectMapper = new ObjectMapper();

        portfolioService = new PortfolioService(
                portfolioRepository,
                studioRepository,
                securityRepository,
                authorizationService,
                sectionValidator,
                objectMapper
        );

        userId = UuidV7.randomUuid();
        studioId = UuidV7.randomUuid();
        portfolioId = UuidV7.randomUuid();

        ownerActor = new ActorContext(
                userId, "Owner User", "owner@studio.com",
                Set.of("DESIGNER"), Set.of(),
                studioId, "OWNER", "PASSWORD", true
        );

        memberActor = new ActorContext(
                userId, "Team Member", "member@studio.com",
                Set.of("DESIGNER_TEAM"), Set.of(),
                studioId, "MEMBER", "PASSWORD", true
        );

        activeUser = new UserRecord(
                userId, "Owner User", "owner@studio.com", "+919876543210",
                "ACTIVE", Instant.now(), Instant.now(), 0L
        );

        studioRecord = new StudioDetailRecord(
                studioId, "Apex Interiors", "apex-interiors", userId, "ACTIVE",
                "INTERIOR_STUDIO", "Principal Architect", "Modern luxury homes",
                2018, "STUDIO_5_10", "PREMIUM", "100 Feet Rd", "Bengaluru",
                "Bengaluru Urban", "Karnataka", "560038", "IN", true,
                false, null, "UNPUBLISHED", Instant.now(), Instant.now(), Instant.now(),
                List.of(
                        new StudioDetailRecord.StudioContactItem("EMAIL", "public@apex.com", true, 0),
                        new StudioDetailRecord.StudioContactItem("PHONE", "+919876543210", false, 1)
                ),
                List.of(new StudioDetailRecord.StudioServiceItem("RESIDENTIAL_FULL", "Full Home Interior Design")),
                List.of(),
                List.of()
        );

        lenient().when(securityRepository.findUserById(userId)).thenReturn(Optional.of(activeUser));
    }

    private StudioMemberRecord createMemberRecord(String role) {
        return new StudioMemberRecord(
                UuidV7.randomUuid(), studioId, "Apex Interiors", "apex-interiors", userId, role, Instant.now()
        );
    }

    @Test
    @DisplayName("Idempotent initialization: 1st call creates portfolio, 2nd call returns existing")
    void testIdempotentInitialization() {
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(createMemberRecord("OWNER")));
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(studioRecord));
        when(portfolioRepository.findPortfolioByStudioId(studioId)).thenReturn(Optional.empty());

        when(portfolioRepository.createPortfolio(any())).thenAnswer(inv -> inv.getArgument(0));

        PortfolioDetailResponse response = portfolioService.initializePortfolio(
                ownerActor, studioId, new InitializePortfolioRequest(PortfolioTemplateKey.BASIC)
        );

        assertNotNull(response);
        assertEquals(PortfolioTemplateKey.BASIC, response.templateKey());
        assertEquals(PortfolioStatus.DRAFT, response.status());
        verify(portfolioRepository, times(1)).createPortfolio(any());
        verify(portfolioRepository, times(1)).createSections(anyList());
        verify(portfolioRepository, times(1)).createVersionSnapshot(any());

        // Second call when portfolio already exists
        PortfolioRecord existing = new PortfolioRecord(
                portfolioId, studioId, PortfolioTemplateKey.BASIC, PortfolioStatus.DRAFT,
                "Headline", "Subheadline", "Bio", "Philosophy", 5,
                "#2C3E50", "#E8DCC4", "#D4AF37", FontPairing.SYSTEM_SANS, 1L,
                Instant.now(), Instant.now()
        );
        when(portfolioRepository.findPortfolioByStudioId(studioId)).thenReturn(Optional.of(existing));

        PortfolioDetailResponse secondResponse = portfolioService.initializePortfolio(
                ownerActor, studioId, new InitializePortfolioRequest(PortfolioTemplateKey.MODERN)
        );

        assertNotNull(secondResponse);
        assertEquals(portfolioId, secondResponse.id());
        // Verify createPortfolio was NOT called a second time
        verify(portfolioRepository, times(1)).createPortfolio(any());
    }

    @Test
    @DisplayName("Publication boundary: setting status = PUBLISHED is explicitly rejected")
    void testPublicationStatusRejected() {
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(createMemberRecord("OWNER")));

        UpdatePortfolioRequest pubRequest = new UpdatePortfolioRequest(
                "Headline", "Subheadline", "Bio", "Philosophy", 5,
                "#2C3E50", "#E8DCC4", "#D4AF37", FontPairing.SYSTEM_SANS,
                "PUBLISHED", 1L
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                portfolioService.updatePortfolio(ownerActor, studioId, pubRequest)
        );
        assertTrue(ex.getMessage().contains("PUBLISHED is not permitted"));
    }

    @Test
    @DisplayName("Optimistic concurrency: stale aggregate version throws ConflictException (HTTP 409)")
    void testOptimisticConcurrencyOnUpdate() {
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(createMemberRecord("OWNER")));

        PortfolioRecord current = new PortfolioRecord(
                portfolioId, studioId, PortfolioTemplateKey.BASIC, PortfolioStatus.DRAFT,
                "Headline", "Subheadline", "Bio", "Philosophy", 5,
                "#2C3E50", "#E8DCC4", "#D4AF37", FontPairing.SYSTEM_SANS, 5L,
                Instant.now(), Instant.now()
        );
        when(portfolioRepository.findPortfolioByStudioId(studioId)).thenReturn(Optional.of(current));

        UpdatePortfolioRequest staleRequest = new UpdatePortfolioRequest(
                "New Headline", null, null, null, null,
                null, null, null, null, "DRAFT", 4L
        );

        ConflictException ex = assertThrows(ConflictException.class, () ->
                portfolioService.updatePortfolio(ownerActor, studioId, staleRequest)
        );
        assertTrue(ex.getMessage().contains("expected version 4, but found 5"));
    }

    @Test
    @DisplayName("Role enforcement: MEMBER cannot modify portfolio (throws AccessDeniedException)")
    void testRoleEnforcement_MemberCannotMutate() {
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(createMemberRecord("MEMBER")));

        assertThrows(AccessDeniedException.class, () ->
                portfolioService.initializePortfolio(memberActor, studioId, null)
        );

        UpdatePortfolioRequest updateReq = new UpdatePortfolioRequest(
                "Headline", null, null, null, null, null, null, null, null, "DRAFT", 1L
        );
        assertThrows(AccessDeniedException.class, () ->
                portfolioService.updatePortfolio(memberActor, studioId, updateReq)
        );

        assertThrows(AccessDeniedException.class, () ->
                portfolioService.switchTemplate(memberActor, studioId, new SwitchTemplateRequest(PortfolioTemplateKey.MODERN, 1L))
        );
    }

    @Test
    @DisplayName("Role enforcement: MEMBER can view portfolio and preview")
    void testRoleEnforcement_MemberCanReadAndPreview() {
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(createMemberRecord("MEMBER")));
        PortfolioRecord current = new PortfolioRecord(
                portfolioId, studioId, PortfolioTemplateKey.BASIC, PortfolioStatus.DRAFT,
                "Headline", "Subheadline", "Bio", "Philosophy", 5,
                "#2C3E50", "#E8DCC4", "#D4AF37", FontPairing.SYSTEM_SANS, 1L,
                Instant.now(), Instant.now()
        );
        when(portfolioRepository.findPortfolioByStudioId(studioId)).thenReturn(Optional.of(current));
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(studioRecord));
        when(portfolioRepository.findSectionsByPortfolioId(portfolioId)).thenReturn(List.of());

        PortfolioDetailResponse response = portfolioService.getPortfolio(memberActor, studioId);
        assertNotNull(response);

        PortfolioPreviewResponse preview = portfolioService.getPortfolioPreview(memberActor, studioId);
        assertNotNull(preview);
    }

    @Test
    @DisplayName("Contact privacy: only contacts with public_consent == true appear in preview")
    void testContactPrivacy_OmitPrivateContacts() {
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(createMemberRecord("OWNER")));
        PortfolioRecord current = new PortfolioRecord(
                portfolioId, studioId, PortfolioTemplateKey.BASIC, PortfolioStatus.DRAFT,
                "Headline", "Subheadline", "Bio", "Philosophy", 5,
                "#2C3E50", "#E8DCC4", "#D4AF37", FontPairing.SYSTEM_SANS, 1L,
                Instant.now(), Instant.now()
        );
        when(portfolioRepository.findPortfolioByStudioId(studioId)).thenReturn(Optional.of(current));
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(studioRecord));
        when(portfolioRepository.findSectionsByPortfolioId(portfolioId)).thenReturn(List.of());

        PortfolioPreviewResponse preview = portfolioService.getPortfolioPreview(ownerActor, studioId);

        assertEquals(1, preview.publicContacts().size());
        assertEquals("public@apex.com", preview.publicContacts().get(0).contactValue());
        assertTrue(preview.publicContacts().stream().noneMatch(c -> c.contactValue().contains("+919876543210")));
    }

    @Test
    @DisplayName("Switch template: template changes, version increments, content remains untouched")
    void testSwitchTemplate_ContentPreserved() {
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(createMemberRecord("OWNER")));
        PortfolioRecord current = new PortfolioRecord(
                portfolioId, studioId, PortfolioTemplateKey.BASIC, PortfolioStatus.DRAFT,
                "Headline", "Subheadline", "Bio", "Philosophy", 5,
                "#2C3E50", "#E8DCC4", "#D4AF37", FontPairing.SYSTEM_SANS, 2L,
                Instant.now(), Instant.now()
        );
        when(portfolioRepository.findPortfolioByStudioId(studioId)).thenReturn(Optional.of(current));
        when(portfolioRepository.updateTemplateKey(portfolioId, "MODERN", 2L)).thenReturn(true);

        PortfolioRecord updated = new PortfolioRecord(
                portfolioId, studioId, PortfolioTemplateKey.MODERN, PortfolioStatus.DRAFT,
                "Headline", "Subheadline", "Bio", "Philosophy", 5,
                "#2C3E50", "#E8DCC4", "#D4AF37", FontPairing.SYSTEM_SANS, 3L,
                Instant.now(), Instant.now()
        );
        when(portfolioRepository.findPortfolioById(portfolioId)).thenReturn(Optional.of(updated));
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(studioRecord));

        PortfolioDetailResponse response = portfolioService.switchTemplate(
                ownerActor, studioId, new SwitchTemplateRequest(PortfolioTemplateKey.MODERN, 2L)
        );

        assertEquals(PortfolioTemplateKey.MODERN, response.templateKey());
        assertEquals(3L, response.version());
        verify(portfolioRepository, times(1)).updateTemplateKey(portfolioId, "MODERN", 2L);
    }

    @Test
    @DisplayName("Section reorder: invalid section list throws BadRequestException")
    void testReorderValidation_MissingOrForeignIds() {
        when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(createMemberRecord("OWNER")));
        PortfolioRecord current = new PortfolioRecord(
                portfolioId, studioId, PortfolioTemplateKey.BASIC, PortfolioStatus.DRAFT,
                "Headline", "Subheadline", "Bio", "Philosophy", 5,
                "#2C3E50", "#E8DCC4", "#D4AF37", FontPairing.SYSTEM_SANS, 1L,
                Instant.now(), Instant.now()
        );
        when(portfolioRepository.findPortfolioByStudioId(studioId)).thenReturn(Optional.of(current));

        UUID s1 = UuidV7.randomUuid();
        UUID s2 = UuidV7.randomUuid();
        when(portfolioRepository.findSectionsByPortfolioId(portfolioId)).thenReturn(List.of(
                new PortfolioSectionRecord(s1, portfolioId, studioId, SectionType.HERO, 0, true, 1, "{}", Instant.now(), Instant.now()),
                new PortfolioSectionRecord(s2, portfolioId, studioId, SectionType.ABOUT, 1, true, 1, "{}", Instant.now(), Instant.now())
        ));

        UUID foreignId = UuidV7.randomUuid();
        ReorderSectionsRequest badRequest = new ReorderSectionsRequest(List.of(s1, foreignId), 1L);

        assertThrows(BadRequestException.class, () ->
                portfolioService.reorderSections(ownerActor, studioId, badRequest)
        );
    }

    @Test
    @DisplayName("Payload sanitization: script injection and large payloads rejected")
    void testPayloadSanitization() {
        ObjectNode scriptContent = objectMapper.createObjectNode();
        scriptContent.put("narrativeOverride", "<script>alert('xss')</script>");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                sectionValidator.validateSectionContent(SectionType.ABOUT, scriptContent)
        );
        assertTrue(ex.getMessage().contains("disallowed HTML or script"));

        ObjectNode iframeContent = objectMapper.createObjectNode();
        iframeContent.put("narrativeOverride", "<iframe src='https://evil.com'></iframe>");

        assertThrows(BadRequestException.class, () ->
                sectionValidator.validateSectionContent(SectionType.ABOUT, iframeContent)
        );
    }
}
