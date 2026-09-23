package com.interior.platform.seo.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.media.domain.MediaAssetRecord;
import com.interior.platform.media.domain.MediaDerivativeRecord;
import com.interior.platform.media.domain.MediaType;
import com.interior.platform.media.domain.MediaVisibility;
import com.interior.platform.media.dto.MediaDerivativeDto;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.portfolio.domain.PortfolioRecord;
import com.interior.platform.portfolio.domain.PortfolioSectionRecord;
import com.interior.platform.portfolio.domain.PortfolioStatus;
import com.interior.platform.portfolio.repository.PortfolioRepository;
import com.interior.platform.projects.domain.ProjectStyle;
import com.interior.platform.projects.domain.StudioProjectRecord;
import com.interior.platform.projects.dto.ProjectPresentationDto;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.seo.domain.SeoChecklistItem;
import com.interior.platform.seo.domain.SeoSettingsRecord;
import com.interior.platform.seo.dto.*;
import com.interior.platform.seo.repository.SeoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeoService {

    private final SeoRepository seoRepository;
    private final StudioRepository studioRepository;
    private final PortfolioRepository portfolioRepository;
    private final ProjectRepository projectRepository;
    private final MediaRepository mediaRepository;
    private final SecurityRepository securityRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public SeoService(
            SeoRepository seoRepository,
            StudioRepository studioRepository,
            PortfolioRepository portfolioRepository,
            ProjectRepository projectRepository,
            MediaRepository mediaRepository,
            SecurityRepository securityRepository,
            AuthorizationService authorizationService,
            AuditService auditService
    ) {
        this.seoRepository = seoRepository;
        this.studioRepository = studioRepository;
        this.portfolioRepository = portfolioRepository;
        this.projectRepository = projectRepository;
        this.mediaRepository = mediaRepository;
        this.securityRepository = securityRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public SeoStatusResponse getSeoStatus(ActorContext actor, UUID requestedStudioId) {
        authorizationService.requireAuthenticated(actor);
        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);

        StudioDetailRecord studio = studioRepository.findStudioById(context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Optional<SeoSettingsRecord> settingsOpt = seoRepository.findByStudioId(context.studioId());
        Optional<PortfolioRecord> portfolioOpt = portfolioRepository.findPortfolioByStudioId(context.studioId());
        List<StudioProjectRecord> projects = projectRepository.listProjects(context.studioId(), null, null, null, null, false);
        List<MediaAssetRecord> allMedia = mediaRepository.findMediaAssetsByStudio(context.studioId(), null, null, null, null, false);

        int totalProjects = projects.size();
        int publicProjects = (int) projects.stream()
                .filter(p -> "READY".equalsIgnoreCase(p.projectStatus().name()) &&
                             ("PORTFOLIO".equalsIgnoreCase(p.visibilityStatus().name()) || "PUBLIC".equalsIgnoreCase(p.visibilityStatus().name())))
                .count();

        int totalMedia = allMedia.size();
        int missingAltText = (int) allMedia.stream()
                .filter(m -> m.altText() == null || m.altText().trim().isEmpty())
                .count();

        // Evaluate SEO Checklist
        List<SeoChecklistItem> checklist = new ArrayList<>();

        boolean profileComplete = studio.name() != null && !studio.name().isBlank() &&
                                  studio.city() != null && !studio.city().isBlank() &&
                                  studio.professionalType() != null;
        checklist.add(new SeoChecklistItem(
                "BUSINESS_PROFILE",
                "Studio Business Profile",
                "Studio name, primary city, and professional classification configured.",
                profileComplete,
                true
        ));

        boolean portfolioReady = portfolioOpt.isPresent() && portfolioOpt.get().status() == PortfolioStatus.READY;
        checklist.add(new SeoChecklistItem(
                "PORTFOLIO_READY",
                "Portfolio Engine Ready",
                "Portfolio content and theme configured and marked READY.",
                portfolioReady,
                true
        ));

        boolean hasPublicContact = studio.contacts() != null && studio.contacts().stream().anyMatch(StudioDetailRecord.StudioContactItem::publicConsent);
        checklist.add(new SeoChecklistItem(
                "PUBLIC_CONTACT",
                "Public Contact Channel",
                "At least one public-consented contact method (Phone, WhatsApp, or Email) available.",
                hasPublicContact,
                true
        ));

        boolean hasProjects = publicProjects > 0;
        checklist.add(new SeoChecklistItem(
                "PUBLIC_PROJECTS",
                "Public Project Stories",
                "At least one ready project story with portfolio visibility.",
                hasProjects,
                false
        ));

        boolean hasCoverMedia = allMedia.stream().anyMatch(MediaAssetRecord::isCover);
        checklist.add(new SeoChecklistItem(
                "COVER_PHOTOGRAPHY",
                "Cover Photography",
                "At least one project has an assigned cover photograph.",
                hasCoverMedia,
                false
        ));

        boolean altTextHealthy = totalMedia == 0 || missingAltText == 0;
        checklist.add(new SeoChecklistItem(
                "IMAGE_ALT_TEXT",
                "Accessible Image Alt Text",
                "All project visuals have descriptive alt text for search engine accessibility.",
                altTextHealthy,
                false
        ));

        boolean isPublishable = profileComplete && portfolioReady && hasPublicContact;

        // Canonical Defaults
        String defaultTitle = buildDefaultStudioTitle(studio);
        String defaultDescription = buildDefaultStudioDescription(studio);

        String metaTitleOverride = settingsOpt.map(SeoSettingsRecord::metaTitleOverride).orElse(null);
        String metaDescOverride = settingsOpt.map(SeoSettingsRecord::metaDescriptionOverride).orElse(null);
        String canonicalOverride = settingsOpt.map(SeoSettingsRecord::canonicalUrlOverride).orElse(null);
        boolean indexingEnabled = settingsOpt.map(SeoSettingsRecord::indexingEnabled).orElse(true);

        String canonicalUrl = canonicalOverride != null && !canonicalOverride.isBlank()
                ? canonicalOverride
                : "/professionals/" + studio.slug();

        return new SeoStatusResponse(
                studio.id(),
                studio.name(),
                studio.slug(),
                studio.publicationStatus(),
                null,
                canonicalUrl,
                indexingEnabled,
                metaTitleOverride != null && !metaTitleOverride.isBlank() ? metaTitleOverride : defaultTitle,
                metaDescOverride != null && !metaDescOverride.isBlank() ? metaDescOverride : defaultDescription,
                metaTitleOverride,
                metaDescOverride,
                canonicalOverride,
                checklist,
                isPublishable,
                totalProjects,
                publicProjects,
                totalMedia,
                missingAltText
        );
    }

    @Transactional
    public SeoStatusResponse publishStudio(ActorContext actor, UUID requestedStudioId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        StudioDetailRecord studio = studioRepository.findStudioById(context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Optional<PortfolioRecord> portfolioOpt = portfolioRepository.findPortfolioByStudioId(context.studioId());
        boolean hasPublicContact = studio.contacts() != null && studio.contacts().stream().anyMatch(StudioDetailRecord.StudioContactItem::publicConsent);
        boolean profileComplete = studio.name() != null && !studio.name().isBlank() &&
                                  studio.city() != null && !studio.city().isBlank();
        boolean portfolioReady = portfolioOpt.isPresent() && portfolioOpt.get().status() == PortfolioStatus.READY;

        if (!profileComplete || !portfolioReady || !hasPublicContact) {
            throw new IllegalStateException("Cannot publish: Studio must have complete business profile, READY portfolio, and at least one public contact.");
        }

        Instant now = Instant.now();
        seoRepository.updatePublicationStatus(context.studioId(), "PUBLISHED", now);

        auditService.record(
                actor.userId(),
                context.studioId(),
                "STUDIO_PUBLISHED",
                "designer_studios",
                context.studioId().toString(),
                Map.of("publicationStatus", "PUBLISHED", "publishedAt", now.toString()),
                null,
                null
        );

        return getSeoStatus(actor, context.studioId());
    }

    @Transactional
    public SeoStatusResponse unpublishStudio(ActorContext actor, UUID requestedStudioId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        seoRepository.updatePublicationStatus(context.studioId(), "UNPUBLISHED", null);

        auditService.record(
                actor.userId(),
                context.studioId(),
                "STUDIO_UNPUBLISHED",
                "designer_studios",
                context.studioId().toString(),
                Map.of("publicationStatus", "UNPUBLISHED"),
                null,
                null
        );

        return getSeoStatus(actor, context.studioId());
    }

    @Transactional
    public SeoStatusResponse updateSeoSettings(
            ActorContext actor,
            UUID requestedStudioId,
            UpdateSeoSettingsRequest request
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        String titleOverride = sanitizePlainString(request.metaTitleOverride());
        String descOverride = sanitizePlainString(request.metaDescriptionOverride());
        String canonicalOverride = sanitizePlainString(request.canonicalUrlOverride());
        boolean indexingEnabled = request.indexingEnabled() == null || request.indexingEnabled();

        SeoSettingsRecord settings = new SeoSettingsRecord(
                com.interior.platform.common.util.UuidV7.randomUuid(),
                context.studioId(),
                titleOverride,
                descOverride,
                canonicalOverride,
                indexingEnabled,
                Instant.now(),
                Instant.now()
        );
        seoRepository.upsert(settings);

        auditService.record(
                actor.userId(),
                context.studioId(),
                "SEO_SETTINGS_UPDATED",
                "studio_seo_settings",
                context.studioId().toString(),
                Map.of("indexingEnabled", indexingEnabled),
                null,
                null
        );

        return getSeoStatus(actor, context.studioId());
    }

    // ========================================================================
    // Public Endpoints (No Auth Required)
    // ========================================================================

    @Transactional(readOnly = true)
    public Optional<PublicStudioDto> getPublicStudioBySlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<StudioDetailRecord> studioOpt = studioRepository.findStudioBySlug(slug.trim());
        if (studioOpt.isEmpty()) {
            return Optional.empty();
        }

        StudioDetailRecord studio = studioOpt.get();
        // GATING: Only PUBLISHED studios are public!
        if (!"PUBLISHED".equalsIgnoreCase(studio.publicationStatus())) {
            return Optional.empty();
        }

        Optional<SeoSettingsRecord> seoOpt = seoRepository.findByStudioId(studio.id());
        Optional<PortfolioRecord> portfolioOpt = portfolioRepository.findPortfolioByStudioId(studio.id());

        // Contacts: strictly public consented!
        List<PublicContactDto> publicContacts = studio.contacts() != null
                ? studio.contacts().stream()
                        .filter(StudioDetailRecord.StudioContactItem::publicConsent)
                        .map(c -> new PublicContactDto(c.kind(), c.value()))
                        .toList()
                : List.of();

        List<String> services = studio.services() != null
                ? studio.services().stream().map(StudioDetailRecord.StudioServiceItem::serviceName).toList()
                : List.of();

        List<String> specialties = studio.specialties() != null
                ? studio.specialties().stream().map(StudioDetailRecord.StudioSpecialtyItem::specialtyName).toList()
                : List.of();

        List<String> serviceAreas = studio.serviceAreas() != null
                ? studio.serviceAreas().stream().map(StudioDetailRecord.StudioServiceAreaItem::cityName).toList()
                : List.of();

        // Portfolio representation
        PublicPortfolioDto portfolioDto = null;
        if (portfolioOpt.isPresent()) {
            PortfolioRecord p = portfolioOpt.get();
            List<PortfolioSectionRecord> sections = portfolioRepository.findSectionsByPortfolioId(p.id());
            List<PublicPortfolioDto.PublicSectionDto> publicSections = sections.stream()
                    .filter(PortfolioSectionRecord::isVisible)
                    .map(s -> new PublicPortfolioDto.PublicSectionDto(
                            s.sectionType().name(),
                            s.displayOrder(),
                            s.content()
                    ))
                    .toList();

            portfolioDto = new PublicPortfolioDto(
                    p.templateKey().name(),
                    p.headline(),
                    p.subheadline(),
                    p.bio(),
                    p.designPhilosophy(),
                    p.yearsOfExperience(),
                    p.primaryColor(),
                    p.secondaryColor(),
                    p.accentColor(),
                    p.fontPairing() != null ? p.fontPairing().name() : "SYSTEM_SANS",
                    publicSections
            );
        }

        // Projects representation (portfolio visible projects)
        List<StudioProjectRecord> readyProjects = projectRepository.findPortfolioProjects(studio.id());
        List<ProjectPresentationDto> projectDtos = readyProjects.stream()
                .map(this::toProjectPresentation)
                .toList();

        // SEO meta
        String defaultTitle = buildDefaultStudioTitle(studio);
        String defaultDesc = buildDefaultStudioDescription(studio);
        String title = seoOpt.map(SeoSettingsRecord::metaTitleOverride).filter(s -> !s.isBlank()).orElse(defaultTitle);
        String desc = seoOpt.map(SeoSettingsRecord::metaDescriptionOverride).filter(s -> !s.isBlank()).orElse(defaultDesc);
        String canonical = seoOpt.map(SeoSettingsRecord::canonicalUrlOverride).filter(s -> !s.isBlank()).orElse("/professionals/" + studio.slug());
        boolean indexingEnabled = seoOpt.map(SeoSettingsRecord::indexingEnabled).orElse(true);

        return Optional.of(new PublicStudioDto(
                studio.id(),
                studio.slug(),
                studio.name(),
                studio.professionalType(),
                studio.professionalTitle(),
                studio.tagline(),
                studio.experienceSinceYear(),
                studio.city(),
                studio.district(),
                studio.state(),
                studio.country(),
                null,
                title,
                desc,
                canonical,
                indexingEnabled,
                publicContacts,
                services,
                specialties,
                serviceAreas,
                portfolioDto,
                projectDtos
        ));
    }

    @Transactional(readOnly = true)
    public Optional<PublicProjectDetailDto> getPublicProject(String studioSlug, String projectSlug) {
        if (studioSlug == null || projectSlug == null) {
            return Optional.empty();
        }

        Optional<StudioDetailRecord> studioOpt = studioRepository.findStudioBySlug(studioSlug.trim());
        if (studioOpt.isEmpty()) {
            return Optional.empty();
        }

        StudioDetailRecord studio = studioOpt.get();
        // Parent must be PUBLISHED!
        if (!"PUBLISHED".equalsIgnoreCase(studio.publicationStatus())) {
            return Optional.empty();
        }

        Optional<StudioProjectRecord> projectOpt = projectRepository.findProjectBySlug(studio.id(), projectSlug.trim());
        if (projectOpt.isEmpty()) {
            return Optional.empty();
        }

        StudioProjectRecord project = projectOpt.get();
        // GATING: project must be READY, PORTFOLIO/PUBLIC visible, and not archived!
        if (!"READY".equalsIgnoreCase(project.projectStatus().name()) ||
            "PRIVATE".equalsIgnoreCase(project.visibilityStatus().name()) ||
            project.archivedAt() != null) {
            return Optional.empty();
        }

        // Public contacts only
        List<PublicContactDto> publicContacts = studio.contacts() != null
                ? studio.contacts().stream()
                        .filter(StudioDetailRecord.StudioContactItem::publicConsent)
                        .map(c -> new PublicContactDto(c.kind(), c.value()))
                        .toList()
                : List.of();

        PublicStudioSummaryDto studioSummary = new PublicStudioSummaryDto(
                studio.id(),
                studio.slug(),
                studio.name(),
                studio.professionalType(),
                studio.professionalTitle(),
                studio.city(),
                studio.state(),
                publicContacts
        );

        // Fetch public-safe media ONLY (exclude PRIVATE, REFERENCE, CLIENT_PRIVATE)
        List<MediaAssetRecord> assets = mediaRepository.findMediaAssetsByProject(project.id(), studio.id(), false);
        List<PublicMediaDto> publicMedia = assets.stream()
                .filter(m -> m.visibility() != MediaVisibility.PRIVATE &&
                             m.mediaType() != MediaType.REFERENCE &&
                             m.mediaType() != MediaType.CLIENT_PRIVATE)
                .map(m -> {
                    List<MediaDerivativeRecord> derivatives = mediaRepository.findDerivativesByMediaId(m.id(), studio.id());
                    List<MediaDerivativeDto> derivativeDtos = derivatives.stream()
                            .map(d -> new MediaDerivativeDto(
                                    d.id(),
                                    d.variantName(),
                                    d.width(),
                                    d.height(),
                                    d.format(),
                                    d.fileSize(),
                                    d.publicUrl(),
                                    d.isWatermarked()
                            ))
                            .toList();

                    return new PublicMediaDto(
                            m.id(),
                            m.mediaType().name(),
                            m.mediaType().getDisplayName(),
                            m.isCover(),
                            m.altText(),
                            m.caption(),
                            m.mediaType() == MediaType.AI_CONCEPT,
                            derivativeDtos
                    );
                })
                .toList();

        List<ProjectStyle> styles = projectRepository.findStylesByProjectId(project.id());
        List<String> styleCodes = styles.stream().map(Enum::name).toList();
        List<String> styleDisplayNames = styles.stream().map(ProjectStyle::getDisplayName).toList();

        // Related projects (up to 3 other portfolio projects from same studio)
        List<StudioProjectRecord> otherProjects = projectRepository.findPortfolioProjects(studio.id()).stream()
                .filter(p -> !p.id().equals(project.id()))
                .limit(3)
                .toList();
        List<ProjectPresentationDto> relatedDtos = otherProjects.stream()
                .map(this::toProjectPresentation)
                .toList();

        // SEO title and description (Truthful, zero private client name, zero budget)
        String title = project.title() + (project.city() != null ? " in " + project.city() : "") + " | " + studio.name();
        String desc = project.shortDescription() != null && !project.shortDescription().isBlank()
                ? project.shortDescription()
                : project.title() + " by " + studio.name() + (project.city() != null ? " in " + project.city() : "") + ".";

        String canonicalUrl = "/projects/" + project.slug();

        String budgetFormatted = null;
        if (project.budgetVisibility() != null && !"HIDDEN".equalsIgnoreCase(project.budgetVisibility().name())) {
            budgetFormatted = project.budgetMin() != null ? "₹" + project.budgetMin().toPlainString() : null;
        }

        String areaFormatted = project.areaValue() != null
                ? project.areaValue().toPlainString() + " " + (project.areaUnit() != null ? project.areaUnit().name() : "SQ_FT")
                : null;

        return Optional.of(new PublicProjectDetailDto(
                project.id(),
                project.slug(),
                project.title(),
                project.shortDescription(),
                project.fullDescription(),
                project.categoryCode().name(),
                project.categoryCode().getDisplayName(),
                styleCodes,
                styleDisplayNames,
                project.city(),
                project.district(),
                project.state(),
                project.country(),
                project.propertyType() != null ? project.propertyType().getDisplayName() : null,
                project.projectScope() != null ? project.projectScope().getDisplayName() : null,
                project.completionYear(),
                budgetFormatted,
                areaFormatted,
                project.updatedAt(),
                title,
                desc,
                canonicalUrl,
                studioSummary,
                publicMedia,
                relatedDtos
        ));
    }

    @Transactional(readOnly = true)
    public List<SitemapItemDto> getSitemapData() {
        List<SitemapItemDto> result = new ArrayList<>();
        result.addAll(seoRepository.getPublishedStudioSitemapItems());
        result.addAll(seoRepository.getPublicProjectSitemapItems());
        return result;
    }

    // ========================================================================
    // Internal Helpers
    // ========================================================================

    private ProjectPresentationDto toProjectPresentation(StudioProjectRecord project) {
        String coverUrl = null;
        Optional<MediaAssetRecord> coverOpt = mediaRepository.findCoverMedia(project.id(), project.studioId());
        if (coverOpt.isPresent()) {
            List<MediaDerivativeRecord> derivs = mediaRepository.findDerivativesByMediaId(coverOpt.get().id(), project.studioId());
            coverUrl = derivs.stream()
                    .filter(d -> "MEDIUM".equalsIgnoreCase(d.variantName().name()) || "LARGE".equalsIgnoreCase(d.variantName().name()))
                    .map(MediaDerivativeRecord::publicUrl)
                    .findFirst()
                    .orElse(null);
        }

        List<ProjectStyle> styles = projectRepository.findStylesByProjectId(project.id());
        List<String> styleCodes = styles.stream().map(Enum::name).toList();
        List<String> styleDisplayNames = styles.stream().map(ProjectStyle::getDisplayName).toList();

        return new ProjectPresentationDto(
                project.id(),
                project.slug(),
                project.title(),
                project.shortDescription(),
                project.fullDescription(),
                project.categoryCode().name(),
                project.categoryCode().getDisplayName(),
                styleCodes,
                styleDisplayNames,
                project.city(),
                project.propertyType() != null ? project.propertyType().getDisplayName() : null,
                project.projectScope() != null ? project.projectScope().getDisplayName() : null,
                project.completionYear(),
                project.featured(),
                project.displayOrder(),
                null, // No private client name in public presentation!
                null, // No private budget in public presentation!
                project.areaValue() != null ? project.areaValue().toPlainString() + " " + project.areaUnit() : null,
                coverUrl
        );
    }

    private String buildDefaultStudioTitle(StudioDetailRecord studio) {
        String typeLabel = studio.professionalTitle() != null && !studio.professionalTitle().isBlank()
                ? studio.professionalTitle()
                : studio.professionalType().replace("_", " ");
        return studio.name() + " | " + typeLabel + (studio.city() != null ? " in " + studio.city() : "");
    }

    private String buildDefaultStudioDescription(StudioDetailRecord studio) {
        StringBuilder sb = new StringBuilder();
        if (studio.tagline() != null && !studio.tagline().isBlank()) {
            sb.append(studio.tagline()).append(". ");
        }
        sb.append(studio.name()).append(" offers professional interior architecture and design services");
        if (studio.city() != null && !studio.city().isBlank()) {
            sb.append(" based in ").append(studio.city());
            if (studio.state() != null && !studio.state().isBlank()) {
                sb.append(", ").append(studio.state());
            }
        }
        sb.append(".");
        return sb.toString();
    }

    private String sanitizePlainString(String input) {
        if (input == null) return null;
        String clean = input.replaceAll("<[^>]*>", "").trim();
        return clean.isEmpty() ? null : clean;
    }

    private UserRecord validateActiveUser(UUID userId) {
        UserRecord user = securityRepository.findUserById(userId)
                .orElseThrow(() -> new AccessDeniedException("User account not found"));
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new AccessDeniedException("Account is suspended or inactive");
        }
        return user;
    }

    private ResolvedStudioContext resolveStudioContext(ActorContext actor, UUID requestedStudioId) {
        List<StudioMemberRecord> memberships = securityRepository.getStudioMemberships(actor.userId());
        if (memberships == null || memberships.isEmpty()) {
            throw new AccessDeniedException("No studio membership found for account");
        }

        if (requestedStudioId != null) {
            Optional<StudioMemberRecord> match = memberships.stream()
                    .filter(m -> m.studioId().equals(requestedStudioId))
                    .findFirst();
            if (match.isEmpty()) {
                throw new AccessDeniedException("Access denied: you are not a member of the requested studio");
            }
            return new ResolvedStudioContext(match.get().studioId(), match.get().role());
        }

        if (actor.activeStudioId() != null) {
            Optional<StudioMemberRecord> match = memberships.stream()
                    .filter(m -> m.studioId().equals(actor.activeStudioId()))
                    .findFirst();
            if (match.isPresent()) {
                return new ResolvedStudioContext(match.get().studioId(), match.get().role());
            }
        }

        StudioMemberRecord primary = memberships.get(0);
        return new ResolvedStudioContext(primary.studioId(), primary.role());
    }

    private void requireStudioManagePermission(ResolvedStudioContext context, ActorContext actor) {
        if (actor.hasRole("SUPER_ADMIN") || actor.hasRole("ADMIN")) {
            return;
        }
        if (!"OWNER".equalsIgnoreCase(context.role()) && !"ADMIN".equalsIgnoreCase(context.role())) {
            throw new AccessDeniedException("Access denied: elevated studio role (OWNER or ADMIN) required for SEO management");
        }
    }

    private record ResolvedStudioContext(UUID studioId, String role) {}
}
