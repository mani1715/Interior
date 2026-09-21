package com.interior.platform.seo;

import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.media.domain.MediaAssetRecord;
import com.interior.platform.media.domain.MediaProcessingStatus;
import com.interior.platform.media.domain.MediaType;
import com.interior.platform.media.domain.MediaVisibility;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.portfolio.domain.FontPairing;
import com.interior.platform.portfolio.domain.PortfolioRecord;
import com.interior.platform.portfolio.domain.PortfolioStatus;
import com.interior.platform.portfolio.domain.PortfolioTemplateKey;
import com.interior.platform.portfolio.repository.PortfolioRepository;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectStatus;
import com.interior.platform.projects.domain.StudioProjectRecord;
import com.interior.platform.projects.domain.VisibilityStatus;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.seo.domain.SeoSettingsRecord;
import com.interior.platform.seo.dto.*;
import com.interior.platform.seo.repository.SeoRepository;
import com.interior.platform.seo.service.SeoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeoServiceTest {

    @Mock private SeoRepository seoRepository;
    @Mock private StudioRepository studioRepository;
    @Mock private PortfolioRepository portfolioRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private MediaRepository mediaRepository;
    @Mock private SecurityRepository securityRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private AuditService auditService;

    private SeoService seoService;

    private final UUID studioId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private ActorContext ownerActor;
    private StudioDetailRecord sampleStudio;

    @BeforeEach
    void setUp() {
        seoService = new SeoService(
                seoRepository,
                studioRepository,
                portfolioRepository,
                projectRepository,
                mediaRepository,
                securityRepository,
                authorizationService,
                auditService
        );

        ownerActor = new ActorContext(
                userId,
                "Designer Owner",
                "owner@studio.com",
                Set.of("DESIGNER"),
                Set.of("STUDIO_MANAGE"),
                studioId,
                "OWNER",
                "PASSWORD",
                true
        );

        sampleStudio = new StudioDetailRecord(
                studioId,
                "Apex Design Studio",
                "apex-designs",
                userId,
                "ACTIVE",
                "INTERIOR_STUDIO",
                "Luxury Residential Architect",
                "Crafting refined architectural sanctuaries",
                2018,
                "5-10",
                "PREMIUM",
                "123 MG Road",
                "Bengaluru",
                "Bengaluru Urban",
                "Karnataka",
                "560001",
                "IN",
                true,
                true,
                "29ABCDE1234F1Z5",
                "UNPUBLISHED",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                List.of(
                        new StudioDetailRecord.StudioContactItem("PHONE", "+919876543210", true, 0),
                        new StudioDetailRecord.StudioContactItem("EMAIL", "private@studio.com", false, 1)
                ),
                List.of(new StudioDetailRecord.StudioServiceItem("FULL_HOME", "Full Home Interior")),
                List.of(new StudioDetailRecord.StudioSpecialtyItem("CONTEMPORARY", "Contemporary Minimalist")),
                List.of(new StudioDetailRecord.StudioServiceAreaItem("Bengaluru", "Indiranagar"))
        );
    }

    private void mockUserAndMembership(String role) {
        UserRecord user = new UserRecord(userId, "Designer Owner", "owner@studio.com", null, "ACTIVE", Instant.now(), Instant.now(), 0);
        lenient().when(securityRepository.findUserById(userId)).thenReturn(Optional.of(user));
        StudioMemberRecord member = new StudioMemberRecord(UUID.randomUUID(), studioId, "Apex Design Studio", "apex-designs", userId, role, Instant.now());
        lenient().when(securityRepository.getStudioMemberships(userId)).thenReturn(List.of(member));
    }

    @Test
    void testGetSeoStatus_EvaluatesChecklistAndBuildsCanonicalDefaults() {
        mockUserAndMembership("OWNER");
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(sampleStudio));

        PortfolioRecord portfolio = new PortfolioRecord(
                UUID.randomUUID(), studioId, PortfolioTemplateKey.BASIC, PortfolioStatus.READY, "Timeless spaces", null, null, null, null, null, null, null, FontPairing.SYSTEM_SANS, 1, Instant.now(), Instant.now()
        );
        when(portfolioRepository.findPortfolioByStudioId(studioId)).thenReturn(Optional.of(portfolio));

        SeoStatusResponse status = seoService.getSeoStatus(ownerActor, studioId);

        assertNotNull(status);
        assertEquals("UNPUBLISHED", status.publicationStatus());
        assertEquals("/professionals/apex-designs", status.canonicalUrl());
        assertTrue(status.isPublishable());
        assertTrue(status.metaTitle().contains("Apex Design Studio"));
        assertTrue(status.metaDescription().contains("Bengaluru"));
        assertEquals(6, status.checklist().size());
    }

    @Test
    void testPublishStudio_FailsWhenPortfolioNotReady() {
        mockUserAndMembership("OWNER");
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(sampleStudio));
        when(portfolioRepository.findPortfolioByStudioId(studioId)).thenReturn(Optional.empty()); // No portfolio!

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                seoService.publishStudio(ownerActor, studioId));
        assertTrue(ex.getMessage().contains("Cannot publish"));
        verify(seoRepository, never()).updatePublicationStatus(any(), any(), any());
    }

    @Test
    void testPublishStudio_SucceedsWhenRequirementsMet() {
        mockUserAndMembership("OWNER");
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(sampleStudio));

        PortfolioRecord portfolio = new PortfolioRecord(
                UUID.randomUUID(), studioId, PortfolioTemplateKey.BASIC, PortfolioStatus.READY, "Timeless spaces", null, null, null, null, null, null, null, FontPairing.SYSTEM_SANS, 1, Instant.now(), Instant.now()
        );
        when(portfolioRepository.findPortfolioByStudioId(studioId)).thenReturn(Optional.of(portfolio));

        SeoStatusResponse resp = seoService.publishStudio(ownerActor, studioId);

        assertNotNull(resp);
        verify(seoRepository).updatePublicationStatus(eq(studioId), eq("PUBLISHED"), any(Instant.class));
        verify(auditService).record(eq(userId), eq(studioId), eq("STUDIO_PUBLISHED"), anyString(), anyString(), anyMap(), isNull(), isNull());
    }

    @Test
    void testUnpublishStudio_RevertsToUnpublished() {
        mockUserAndMembership("OWNER");
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(sampleStudio));

        SeoStatusResponse resp = seoService.unpublishStudio(ownerActor, studioId);

        assertNotNull(resp);
        verify(seoRepository).updatePublicationStatus(eq(studioId), eq("UNPUBLISHED"), isNull());
        verify(auditService).record(eq(userId), eq(studioId), eq("STUDIO_UNPUBLISHED"), anyString(), anyString(), anyMap(), isNull(), isNull());
    }

    @Test
    void testUpdateSeoSettings_SanitizesHtmlOverrides() {
        mockUserAndMembership("OWNER");
        when(studioRepository.findStudioById(studioId)).thenReturn(Optional.of(sampleStudio));

        UpdateSeoSettingsRequest req = new UpdateSeoSettingsRequest(
                "<b>Custom Meta Title</b>",
                "<script>alert(1)</script>Custom Meta Description",
                "/custom-url",
                true
        );

        seoService.updateSeoSettings(ownerActor, studioId, req);

        ArgumentCaptor<SeoSettingsRecord> captor = ArgumentCaptor.forClass(SeoSettingsRecord.class);
        verify(seoRepository).upsert(captor.capture());

        SeoSettingsRecord saved = captor.getValue();
        assertEquals("Custom Meta Title", saved.metaTitleOverride());
        assertEquals("alert(1)Custom Meta Description", saved.metaDescriptionOverride());
        assertTrue(saved.indexingEnabled());
    }

    @Test
    void testGetPublicStudioBySlug_HiddenWhenUnpublished() {
        when(studioRepository.findStudioBySlug("apex-designs")).thenReturn(Optional.of(sampleStudio)); // UNPUBLISHED!

        Optional<PublicStudioDto> result = seoService.getPublicStudioBySlug("apex-designs");
        assertTrue(result.isEmpty(), "Unpublished studio must return 404 / empty");
    }

    @Test
    void testGetPublicStudioBySlug_ExposesPublicContactsOnly() {
        StudioDetailRecord publishedStudio = new StudioDetailRecord(
                studioId, sampleStudio.name(), sampleStudio.slug(), userId, "ACTIVE",
                sampleStudio.professionalType(), sampleStudio.professionalTitle(), sampleStudio.tagline(),
                sampleStudio.experienceSinceYear(), sampleStudio.teamSize(), sampleStudio.budgetRange(),
                sampleStudio.addressLine(), sampleStudio.city(), sampleStudio.district(), sampleStudio.state(),
                sampleStudio.postalCode(), sampleStudio.country(), sampleStudio.travelAvailable(),
                sampleStudio.gstRegistered(), sampleStudio.gstNumber(), "PUBLISHED",
                Instant.now(), Instant.now(), Instant.now(),
                sampleStudio.contacts(), sampleStudio.services(), sampleStudio.specialties(), sampleStudio.serviceAreas()
        );
        when(studioRepository.findStudioBySlug("apex-designs")).thenReturn(Optional.of(publishedStudio));

        Optional<PublicStudioDto> result = seoService.getPublicStudioBySlug("apex-designs");
        assertTrue(result.isPresent());

        PublicStudioDto dto = result.get();
        assertEquals(1, dto.contacts().size(), "Only public-consented contact must be exposed");
        assertEquals("+919876543210", dto.contacts().get(0).contactValue());
        assertFalse(dto.contacts().stream().anyMatch(c -> c.contactValue().contains("private")), "Private email must never leak");
    }

    @Test
    void testGetPublicProject_HiddenWhenParentStudioUnpublished() {
        when(studioRepository.findStudioBySlug("apex-designs")).thenReturn(Optional.of(sampleStudio)); // Studio UNPUBLISHED

        Optional<PublicProjectDetailDto> result = seoService.getPublicProject("apex-designs", "modern-penthouse");
        assertTrue(result.isEmpty(), "Project of unpublished studio must return 404");
    }

    @Test
    void testGetPublicProject_ExcludesPrivateAndReferenceMedia() {
        StudioDetailRecord publishedStudio = new StudioDetailRecord(
                studioId, sampleStudio.name(), sampleStudio.slug(), userId, "ACTIVE",
                sampleStudio.professionalType(), sampleStudio.professionalTitle(), sampleStudio.tagline(),
                sampleStudio.experienceSinceYear(), sampleStudio.teamSize(), sampleStudio.budgetRange(),
                sampleStudio.addressLine(), sampleStudio.city(), sampleStudio.district(), sampleStudio.state(),
                sampleStudio.postalCode(), sampleStudio.country(), sampleStudio.travelAvailable(),
                sampleStudio.gstRegistered(), sampleStudio.gstNumber(), "PUBLISHED",
                Instant.now(), Instant.now(), Instant.now(),
                sampleStudio.contacts(), sampleStudio.services(), sampleStudio.specialties(), sampleStudio.serviceAreas()
        );
        when(studioRepository.findStudioBySlug("apex-designs")).thenReturn(Optional.of(publishedStudio));

        UUID projId = UUID.randomUUID();
        StudioProjectRecord project = new StudioProjectRecord(
                projId, studioId, "modern-penthouse", "Modern Penthouse", "Short desc", "Full desc",
                ProjectCategory.LIVING_ROOM, ProjectStatus.READY, VisibilityStatus.PORTFOLIO,
                true, 0, "Bengaluru", null, "Karnataka", "IN", null, null, 2025,
                null, null, null, "INR", null, "Secret Client", null, null, "Secret Notes",
                1, userId, Instant.now(), Instant.now(), null
        );
        when(projectRepository.findProjectBySlug(studioId, "modern-penthouse")).thenReturn(Optional.of(project));

        // Mock media: 1 Real Public Photo, 1 AI Concept, 1 Confidential Spec (Private)
        MediaAssetRecord pubPhoto = new MediaAssetRecord(
                UUID.randomUUID(), studioId, projId, MediaType.REAL_PROJECT, MediaVisibility.PORTFOLIO,
                MediaProcessingStatus.READY, "storage/k1", "image/jpeg", 1000L, 1920, 1080, 0, true, "Alt 1", null, true, userId, Instant.now(), Instant.now(), null
        );
        MediaAssetRecord aiConcept = new MediaAssetRecord(
                UUID.randomUUID(), studioId, projId, MediaType.AI_CONCEPT, MediaVisibility.PORTFOLIO,
                MediaProcessingStatus.READY, "storage/k2", "image/png", 2000L, 1920, 1080, 1, false, "Alt AI", null, true, userId, Instant.now(), Instant.now(), null
        );
        MediaAssetRecord privateSpec = new MediaAssetRecord(
                UUID.randomUUID(), studioId, projId, MediaType.CLIENT_PRIVATE, MediaVisibility.PRIVATE,
                MediaProcessingStatus.READY, "storage/k3", "application/pdf", 500L, 100, 100, 2, false, "Private Spec", null, false, userId, Instant.now(), Instant.now(), null
        );
        when(mediaRepository.findMediaAssetsByProject(projId, studioId, false)).thenReturn(List.of(pubPhoto, aiConcept, privateSpec));

        Optional<PublicProjectDetailDto> result = seoService.getPublicProject("apex-designs", "modern-penthouse");
        assertTrue(result.isPresent());

        PublicProjectDetailDto detail = result.get();
        assertEquals(2, detail.media().size(), "Private spec must be filtered out");
        assertTrue(detail.media().stream().anyMatch(PublicMediaDto::isAiConcept), "AI concept must be marked truthfully");
        assertFalse(detail.metaDescription().contains("Secret Client"), "Private client name must never leak into metadata");
    }

    @Test
    void testGetSitemapData_CombinesStudioAndProjectEntries() {
        when(seoRepository.getPublishedStudioSitemapItems()).thenReturn(List.of(
                new SitemapItemDto("/professionals/apex-designs", "0.9", "weekly", Instant.now(), null, null)
        ));
        when(seoRepository.getPublicProjectSitemapItems()).thenReturn(List.of(
                new SitemapItemDto("/projects/modern-penthouse", "0.8", "monthly", Instant.now(), "https://img.com/p.webp", "Penthouse")
        ));

        List<SitemapItemDto> sitemap = seoService.getSitemapData();
        assertEquals(2, sitemap.size());
        assertEquals("/professionals/apex-designs", sitemap.get(0).path());
        assertEquals("/projects/modern-penthouse", sitemap.get(1).path());
    }
}
