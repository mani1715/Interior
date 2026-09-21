package com.interior.platform.portfolio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.portfolio.domain.FontPairing;
import com.interior.platform.portfolio.domain.PortfolioRecord;
import com.interior.platform.portfolio.domain.PortfolioSectionRecord;
import com.interior.platform.portfolio.domain.PortfolioStatus;
import com.interior.platform.portfolio.domain.PortfolioTemplateKey;
import com.interior.platform.portfolio.domain.PortfolioVersionRecord;
import com.interior.platform.portfolio.domain.SectionType;
import com.interior.platform.portfolio.dto.CreateVersionSnapshotRequest;
import com.interior.platform.portfolio.dto.InitializePortfolioRequest;
import com.interior.platform.portfolio.dto.PortfolioDetailResponse;
import com.interior.platform.portfolio.dto.PortfolioPreviewResponse;
import com.interior.platform.portfolio.dto.PortfolioSectionDto;
import com.interior.platform.portfolio.dto.PortfolioVersionDto;
import com.interior.platform.portfolio.dto.ReorderSectionsRequest;
import com.interior.platform.portfolio.dto.RestoreVersionRequest;
import com.interior.platform.portfolio.dto.SwitchTemplateRequest;
import com.interior.platform.portfolio.dto.UpdatePortfolioRequest;
import com.interior.platform.portfolio.dto.UpdateSectionRequest;
import com.interior.platform.portfolio.repository.PortfolioRepository;
import com.interior.platform.portfolio.validation.PortfolioSectionValidator;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private static final int MAX_SNAPSHOT_BYTES = 64 * 1024; // 64 KB
    private static final int MAX_RETAINED_VERSIONS = 10;

    private final PortfolioRepository portfolioRepository;
    private final StudioRepository studioRepository;
    private final SecurityRepository securityRepository;
    private final AuthorizationService authorizationService;
    private final PortfolioSectionValidator sectionValidator;
    private final ObjectMapper objectMapper;

    private final com.interior.platform.projects.service.ProjectService projectService;

    public PortfolioService(
            PortfolioRepository portfolioRepository,
            StudioRepository studioRepository,
            SecurityRepository securityRepository,
            AuthorizationService authorizationService,
            PortfolioSectionValidator sectionValidator,
            ObjectMapper objectMapper
    ) {
        this(portfolioRepository, studioRepository, securityRepository, authorizationService, sectionValidator, objectMapper, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PortfolioService(
            PortfolioRepository portfolioRepository,
            StudioRepository studioRepository,
            SecurityRepository securityRepository,
            AuthorizationService authorizationService,
            PortfolioSectionValidator sectionValidator,
            ObjectMapper objectMapper,
            com.interior.platform.projects.service.ProjectService projectService
    ) {
        this.portfolioRepository = portfolioRepository;
        this.studioRepository = studioRepository;
        this.securityRepository = securityRepository;
        this.authorizationService = authorizationService;
        this.sectionValidator = sectionValidator;
        this.objectMapper = objectMapper;
        this.projectService = projectService;
    }

    @Transactional(readOnly = true)
    public PortfolioDetailResponse getPortfolio(ActorContext actor, UUID requestedStudioId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        PortfolioRecord portfolio = portfolioRepository.findPortfolioByStudioId(context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not configured for this studio"));

        StudioDetailRecord studio = studioRepository.findStudioById(context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        return buildPortfolioDetailResponse(portfolio, studio);
    }

    @Transactional
    public PortfolioDetailResponse initializePortfolio(ActorContext actor, UUID requestedStudioId, InitializePortfolioRequest request) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        StudioDetailRecord studio = studioRepository.findStudioById(context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        Optional<PortfolioRecord> existing = portfolioRepository.findPortfolioByStudioId(context.studioId());
        if (existing.isPresent()) {
            return buildPortfolioDetailResponse(existing.get(), studio);
        }

        PortfolioTemplateKey templateKey = (request != null && request.templateKey() != null)
                ? request.templateKey()
                : PortfolioTemplateKey.BASIC;

        UUID portfolioId = UuidV7.randomUuid();
        PortfolioRecord newPortfolio = new PortfolioRecord(
                portfolioId,
                studio.id(),
                templateKey,
                PortfolioStatus.DRAFT,
                studio.name() != null ? studio.name() : "Interior Design Studio",
                studio.tagline() != null ? studio.tagline() : "Professional Interior Design & Spatial Architecture",
                studio.tagline() != null ? studio.tagline() : "",
                "Crafting functional, harmonious environments tailored to how you live.",
                studio.experienceSinceYear() != null ? (int) Math.max(0, 2026 - studio.experienceSinceYear()) : null,
                "#2C3E50",
                "#E8DCC4",
                "#D4AF37",
                FontPairing.SYSTEM_SANS,
                1L,
                Instant.now(),
                Instant.now()
        );

        portfolioRepository.createPortfolio(newPortfolio);

        List<PortfolioSectionRecord> defaultSections = List.of(
                new PortfolioSectionRecord(
                        UuidV7.randomUuid(), portfolioId, studio.id(), SectionType.HERO, 0, true, 1,
                        "{\"badgeText\": \"Interior Architecture\", \"ctaText\": \"Inquire for Projects\"}",
                        Instant.now(), Instant.now()
                ),
                new PortfolioSectionRecord(
                        UuidV7.randomUuid(), portfolioId, studio.id(), SectionType.ABOUT, 1, true, 1,
                        "{\"narrativeOverride\": \"\", \"philosophyOverride\": \"\"}",
                        Instant.now(), Instant.now()
                ),
                new PortfolioSectionRecord(
                        UuidV7.randomUuid(), portfolioId, studio.id(), SectionType.SERVICES, 2, true, 1,
                        "{\"sectionHeadline\": \"Our Services\", \"sectionDescription\": \"End-to-end interior design services.\"}",
                        Instant.now(), Instant.now()
                ),
                new PortfolioSectionRecord(
                        UuidV7.randomUuid(), portfolioId, studio.id(), SectionType.CONTACT, 3, true, 1,
                        "{\"contactIntro\": \"Reach out to schedule an initial design consultation.\", \"inquiryFormEnabled\": true}",
                        Instant.now(), Instant.now()
                )
        );
        portfolioRepository.createSections(defaultSections);

        createSnapshotInternal(newPortfolio, defaultSections, "Initial Portfolio Creation", actor.userId());

        return buildPortfolioDetailResponse(newPortfolio, studio);
    }

    @Transactional
    public PortfolioDetailResponse updatePortfolio(ActorContext actor, UUID requestedStudioId, UpdatePortfolioRequest request) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        if (request.status() != null && "PUBLISHED".equalsIgnoreCase(request.status().trim())) {
            throw new BadRequestException("Setting status to PUBLISHED is not permitted in Phase 10");
        }

        PortfolioRecord current = portfolioRepository.findPortfolioByStudioId(context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not configured for this studio"));

        if (current.version() != request.version()) {
            throw new ConflictException("Portfolio has been modified by another operation (expected version " +
                    request.version() + ", but found " + current.version() + ")");
        }

        PortfolioStatus targetStatus = current.status();
        if (request.status() != null && !request.status().isBlank()) {
            try {
                targetStatus = PortfolioStatus.valueOf(request.status().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid portfolio status: " + request.status());
            }
        }

        PortfolioRecord updated = new PortfolioRecord(
                current.id(),
                current.studioId(),
                current.templateKey(),
                targetStatus,
                request.headline() != null ? request.headline() : current.headline(),
                request.subheadline() != null ? request.subheadline() : current.subheadline(),
                request.bio() != null ? request.bio() : current.bio(),
                request.designPhilosophy() != null ? request.designPhilosophy() : current.designPhilosophy(),
                request.yearsOfExperience() != null ? request.yearsOfExperience() : current.yearsOfExperience(),
                request.primaryColor() != null ? request.primaryColor() : current.primaryColor(),
                request.secondaryColor() != null ? request.secondaryColor() : current.secondaryColor(),
                request.accentColor() != null ? request.accentColor() : current.accentColor(),
                request.fontPairing() != null ? request.fontPairing() : current.fontPairing(),
                current.version(),
                current.createdAt(),
                Instant.now()
        );

        boolean ok = portfolioRepository.updatePortfolio(updated, request.version());
        if (!ok) {
            throw new ConflictException("Portfolio version conflict. Please reload and retry.");
        }

        StudioDetailRecord studio = studioRepository.findStudioById(context.studioId()).orElseThrow();
        PortfolioRecord refreshed = portfolioRepository.findPortfolioById(current.id()).orElseThrow();
        return buildPortfolioDetailResponse(refreshed, studio);
    }

    @Transactional
    public PortfolioDetailResponse updateSection(
            ActorContext actor,
            UUID requestedStudioId,
            UUID sectionId,
            UpdateSectionRequest request
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        PortfolioRecord portfolio = portfolioRepository.findPortfolioByStudioId(context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not configured for this studio"));

        if (portfolio.version() != request.version()) {
            throw new ConflictException("Portfolio version conflict (expected " + request.version() +
                    ", current " + portfolio.version() + ")");
        }

        PortfolioSectionRecord section = portfolioRepository.findSectionById(sectionId, portfolio.id())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found in portfolio"));

        if (request.content() != null) {
            sectionValidator.validateSectionContent(section.sectionType(), request.content());
        }

        boolean ok = portfolioRepository.incrementVersion(portfolio.id(), request.version());
        if (!ok) {
            throw new ConflictException("Portfolio version conflict. Please reload and retry.");
        }

        boolean isVisible = request.isVisible() != null ? request.isVisible() : section.isVisible();
        String contentJson = request.content() != null ? request.content().toString() : section.content();

        PortfolioSectionRecord updatedSection = new PortfolioSectionRecord(
                section.id(),
                section.portfolioId(),
                section.studioId(),
                section.sectionType(),
                section.displayOrder(),
                isVisible,
                section.schemaVersion(),
                contentJson,
                section.createdAt(),
                Instant.now()
        );

        portfolioRepository.updateSection(updatedSection);

        StudioDetailRecord studio = studioRepository.findStudioById(context.studioId()).orElseThrow();
        PortfolioRecord refreshed = portfolioRepository.findPortfolioById(portfolio.id()).orElseThrow();
        return buildPortfolioDetailResponse(refreshed, studio);
    }

    @Transactional
    public PortfolioDetailResponse reorderSections(
            ActorContext actor,
            UUID requestedStudioId,
            ReorderSectionsRequest request
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        PortfolioRecord portfolio = portfolioRepository.findPortfolioByStudioId(context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not configured for this studio"));

        if (portfolio.version() != request.version()) {
            throw new ConflictException("Portfolio version conflict (expected " + request.version() +
                    ", current " + portfolio.version() + ")");
        }

        List<PortfolioSectionRecord> existing = portfolioRepository.findSectionsByPortfolioId(portfolio.id());
        Set<UUID> existingIds = existing.stream().map(PortfolioSectionRecord::id).collect(Collectors.toSet());

        if (request.sectionIds().size() != existingIds.size() || !existingIds.containsAll(request.sectionIds())) {
            throw new BadRequestException("Section reorder list must contain all existing section IDs exactly once");
        }

        boolean ok = portfolioRepository.incrementVersion(portfolio.id(), request.version());
        if (!ok) {
            throw new ConflictException("Portfolio version conflict. Please reload and retry.");
        }

        portfolioRepository.reorderSections(portfolio.id(), request.sectionIds());

        StudioDetailRecord studio = studioRepository.findStudioById(context.studioId()).orElseThrow();
        PortfolioRecord refreshed = portfolioRepository.findPortfolioById(portfolio.id()).orElseThrow();
        return buildPortfolioDetailResponse(refreshed, studio);
    }

    @Transactional
    public PortfolioDetailResponse switchTemplate(
            ActorContext actor,
            UUID requestedStudioId,
            SwitchTemplateRequest request
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        PortfolioRecord portfolio = portfolioRepository.findPortfolioByStudioId(context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not configured for this studio"));

        if (portfolio.version() != request.version()) {
            throw new ConflictException("Portfolio version conflict (expected " + request.version() +
                    ", current " + portfolio.version() + ")");
        }

        boolean ok = portfolioRepository.updateTemplateKey(portfolio.id(), request.templateKey().name(), request.version());
        if (!ok) {
            throw new ConflictException("Portfolio version conflict. Please reload and retry.");
        }

        StudioDetailRecord studio = studioRepository.findStudioById(context.studioId()).orElseThrow();
        PortfolioRecord refreshed = portfolioRepository.findPortfolioById(portfolio.id()).orElseThrow();
        return buildPortfolioDetailResponse(refreshed, studio);
    }

    @Transactional
    public PortfolioDetailResponse createVersionSnapshot(
            ActorContext actor,
            UUID requestedStudioId,
            CreateVersionSnapshotRequest request
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        PortfolioRecord portfolio = portfolioRepository.findPortfolioByStudioId(context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not configured for this studio"));

        if (portfolio.version() != request.version()) {
            throw new ConflictException("Portfolio version conflict (expected " + request.version() +
                    ", current " + portfolio.version() + ")");
        }

        List<PortfolioSectionRecord> sections = portfolioRepository.findSectionsByPortfolioId(portfolio.id());
        createSnapshotInternal(portfolio, sections, request.label(), actor.userId());

        StudioDetailRecord studio = studioRepository.findStudioById(context.studioId()).orElseThrow();
        return buildPortfolioDetailResponse(portfolio, studio);
    }

    @Transactional
    public PortfolioDetailResponse restoreVersion(
            ActorContext actor,
            UUID requestedStudioId,
            RestoreVersionRequest request
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        PortfolioRecord portfolio = portfolioRepository.findPortfolioByStudioId(context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not configured for this studio"));

        if (portfolio.version() != request.version()) {
            throw new ConflictException("Portfolio version conflict (expected " + request.version() +
                    ", current " + portfolio.version() + ")");
        }

        PortfolioVersionRecord historical = portfolioRepository.findVersionByNumber(portfolio.id(), request.targetVersionNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Version snapshot #" + request.targetVersionNumber() + " not found"));

        try {
            JsonNode snapshot = objectMapper.readTree(historical.snapshotPayload());
            JsonNode root = snapshot.get("portfolio");
            JsonNode sectionsNode = snapshot.get("sections");

            PortfolioRecord restored = new PortfolioRecord(
                    portfolio.id(),
                    portfolio.studioId(),
                    PortfolioTemplateKey.valueOf(root.get("templateKey").asText()),
                    portfolio.status(),
                    root.hasNonNull("headline") ? root.get("headline").asText() : null,
                    root.hasNonNull("subheadline") ? root.get("subheadline").asText() : null,
                    root.hasNonNull("bio") ? root.get("bio").asText() : null,
                    root.hasNonNull("designPhilosophy") ? root.get("designPhilosophy").asText() : null,
                    root.hasNonNull("yearsOfExperience") ? root.get("yearsOfExperience").asInt() : null,
                    root.hasNonNull("primaryColor") ? root.get("primaryColor").asText() : null,
                    root.hasNonNull("secondaryColor") ? root.get("secondaryColor").asText() : null,
                    root.hasNonNull("accentColor") ? root.get("accentColor").asText() : null,
                    FontPairing.valueOf(root.get("fontPairing").asText()),
                    portfolio.version(),
                    portfolio.createdAt(),
                    Instant.now()
            );

            boolean ok = portfolioRepository.updatePortfolio(restored, request.version());
            if (!ok) {
                throw new ConflictException("Portfolio version conflict during restore. Please reload.");
            }

            List<PortfolioSectionRecord> newSections = new ArrayList<>();
            if (sectionsNode != null && sectionsNode.isArray()) {
                for (JsonNode sn : sectionsNode) {
                    newSections.add(new PortfolioSectionRecord(
                            UuidV7.randomUuid(),
                            portfolio.id(),
                            portfolio.studioId(),
                            SectionType.valueOf(sn.get("sectionType").asText()),
                            sn.get("displayOrder").asInt(),
                            sn.get("isVisible").asBoolean(),
                            sn.get("schemaVersion").asInt(),
                            sn.get("content").toString(),
                            Instant.now(),
                            Instant.now()
                    ));
                }
            }

            portfolioRepository.replaceAllSections(portfolio.id(), portfolio.studioId(), newSections);

            createSnapshotInternal(restored, newSections, "Restored from version #" + request.targetVersionNumber(), actor.userId());

            StudioDetailRecord studio = studioRepository.findStudioById(context.studioId()).orElseThrow();
            PortfolioRecord refreshed = portfolioRepository.findPortfolioById(portfolio.id()).orElseThrow();
            return buildPortfolioDetailResponse(refreshed, studio);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Corrupt snapshot payload: cannot restore version");
        }
    }

    @Transactional(readOnly = true)
    public PortfolioPreviewResponse getPortfolioPreview(ActorContext actor, UUID requestedStudioId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        PortfolioRecord portfolio = portfolioRepository.findPortfolioByStudioId(context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not configured for this studio"));

        StudioDetailRecord studio = studioRepository.findStudioById(context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        List<PortfolioSectionRecord> sections = portfolioRepository.findSectionsByPortfolioId(portfolio.id());

        List<PortfolioPreviewResponse.PreviewContactDto> publicContacts = studio.contacts() != null
                ? studio.contacts().stream()
                .filter(c -> Boolean.TRUE.equals(c.publicConsent()))
                .map(c -> new PortfolioPreviewResponse.PreviewContactDto(c.kind(), c.value()))
                .toList()
                : List.of();

        List<PortfolioPreviewResponse.PreviewServiceDto> canonicalServices = studio.services() != null
                ? studio.services().stream()
                .map(s -> new PortfolioPreviewResponse.PreviewServiceDto(s.serviceCode(), s.serviceName()))
                .toList()
                : List.of();

        List<PortfolioPreviewResponse.PreviewSpecialtyDto> canonicalSpecialties = studio.specialties() != null
                ? studio.specialties().stream()
                .map(sp -> new PortfolioPreviewResponse.PreviewSpecialtyDto(sp.specialtyCode(), sp.specialtyName()))
                .toList()
                : List.of();

        List<PortfolioPreviewResponse.PreviewServiceAreaDto> canonicalAreas = studio.serviceAreas() != null
                ? studio.serviceAreas().stream()
                .map(a -> new PortfolioPreviewResponse.PreviewServiceAreaDto(a.cityName(), a.locality()))
                .toList()
                : List.of();

        List<PortfolioPreviewResponse.PreviewSectionDto> visibleSections = sections.stream()
                .filter(PortfolioSectionRecord::isVisible)
                .map(s -> new PortfolioPreviewResponse.PreviewSectionDto(
                        s.id(),
                        s.sectionType(),
                        s.displayOrder(),
                        s.schemaVersion(),
                        parseJsonContent(s.content())
                ))
                .toList();

        List<com.interior.platform.projects.dto.ProjectPresentationDto> portfolioProjects = projectService != null
                ? projectService.getPortfolioProjects(studio.id())
                : List.of();

        return new PortfolioPreviewResponse(
                portfolio.id(),
                studio.id(),
                studio.name(),
                studio.slug(),
                studio.professionalType(),
                studio.professionalTitle(),
                studio.city(),
                studio.state(),
                portfolio.templateKey(),
                portfolio.status(),
                portfolio.headline(),
                portfolio.subheadline(),
                portfolio.bio(),
                portfolio.designPhilosophy(),
                portfolio.yearsOfExperience(),
                portfolio.primaryColor(),
                portfolio.secondaryColor(),
                portfolio.accentColor(),
                portfolio.fontPairing(),
                publicContacts,
                canonicalServices,
                canonicalSpecialties,
                canonicalAreas,
                visibleSections,
                portfolioProjects,
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public String getPortfolioReadinessStatus(UUID studioId) {
        Optional<PortfolioRecord> opt = portfolioRepository.findPortfolioByStudioId(studioId);
        if (opt.isEmpty()) {
            return "NOT_CONFIGURED";
        }
        PortfolioRecord portfolio = opt.get();
        Optional<StudioDetailRecord> studioOpt = studioRepository.findStudioById(studioId);
        if (studioOpt.isEmpty()) {
            return "NOT_CONFIGURED";
        }
        List<String> missing = checkMissingRequirements(portfolio, studioOpt.get());
        return missing.isEmpty() ? "READY" : "IN_PROGRESS";
    }

    private void createSnapshotInternal(
            PortfolioRecord portfolio,
            List<PortfolioSectionRecord> sections,
            String label,
            UUID actorUserId
    ) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode portfolioNode = root.putObject("portfolio");
            portfolioNode.put("id", portfolio.id().toString());
            portfolioNode.put("templateKey", portfolio.templateKey().name());
            portfolioNode.put("status", portfolio.status().name());
            portfolioNode.put("headline", portfolio.headline() != null ? portfolio.headline() : "");
            portfolioNode.put("subheadline", portfolio.subheadline() != null ? portfolio.subheadline() : "");
            portfolioNode.put("bio", portfolio.bio() != null ? portfolio.bio() : "");
            portfolioNode.put("designPhilosophy", portfolio.designPhilosophy() != null ? portfolio.designPhilosophy() : "");
            portfolioNode.put("yearsOfExperience", portfolio.yearsOfExperience() != null ? portfolio.yearsOfExperience() : 0);
            portfolioNode.put("primaryColor", portfolio.primaryColor() != null ? portfolio.primaryColor() : "");
            portfolioNode.put("secondaryColor", portfolio.secondaryColor() != null ? portfolio.secondaryColor() : "");
            portfolioNode.put("accentColor", portfolio.accentColor() != null ? portfolio.accentColor() : "");
            portfolioNode.put("fontPairing", portfolio.fontPairing().name());

            List<Map<String, Object>> sectionMaps = new ArrayList<>();
            for (PortfolioSectionRecord s : sections) {
                sectionMaps.add(Map.of(
                        "sectionType", s.sectionType().name(),
                        "displayOrder", s.displayOrder(),
                        "isVisible", s.isVisible(),
                        "schemaVersion", s.schemaVersion(),
                        "content", parseJsonContent(s.content())
                ));
            }
            root.putPOJO("sections", sectionMaps);

            String serialized = objectMapper.writeValueAsString(root);
            if (serialized.getBytes().length > MAX_SNAPSHOT_BYTES) {
                throw new BadRequestException("Snapshot payload exceeds maximum size of 64 KB");
            }

            int nextVersionNumber = portfolioRepository.getNextVersionNumber(portfolio.id());
            PortfolioVersionRecord versionRecord = new PortfolioVersionRecord(
                    UuidV7.randomUuid(),
                    portfolio.id(),
                    portfolio.studioId(),
                    nextVersionNumber,
                    label,
                    serialized,
                    actorUserId,
                    Instant.now()
            );

            portfolioRepository.createVersionSnapshot(versionRecord);
            portfolioRepository.pruneOldVersions(portfolio.id(), MAX_RETAINED_VERSIONS);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Failed to serialize portfolio snapshot: " + e.getMessage());
        }
    }

    private PortfolioDetailResponse buildPortfolioDetailResponse(PortfolioRecord portfolio, StudioDetailRecord studio) {
        List<PortfolioSectionRecord> sectionRecords = portfolioRepository.findSectionsByPortfolioId(portfolio.id());
        List<PortfolioVersionRecord> versionRecords = portfolioRepository.findVersionsByPortfolioId(portfolio.id(), 10);

        List<PortfolioSectionDto> sectionDtos = sectionRecords.stream()
                .map(s -> new PortfolioSectionDto(
                        s.id(),
                        s.sectionType(),
                        s.displayOrder(),
                        s.isVisible(),
                        s.schemaVersion(),
                        parseJsonContent(s.content()),
                        s.updatedAt()
                ))
                .toList();

        List<PortfolioVersionDto> versionDtos = versionRecords.stream()
                .map(v -> new PortfolioVersionDto(
                        v.id(),
                        v.versionNumber(),
                        v.label(),
                        v.createdBy(),
                        v.createdAt()
                ))
                .toList();

        List<String> missing = checkMissingRequirements(portfolio, studio);
        boolean isReady = missing.isEmpty();

        return new PortfolioDetailResponse(
                portfolio.id(),
                portfolio.studioId(),
                portfolio.templateKey(),
                portfolio.status(),
                portfolio.headline(),
                portfolio.subheadline(),
                portfolio.bio(),
                portfolio.designPhilosophy(),
                portfolio.yearsOfExperience(),
                portfolio.primaryColor(),
                portfolio.secondaryColor(),
                portfolio.accentColor(),
                portfolio.fontPairing(),
                portfolio.version(),
                isReady,
                missing,
                sectionDtos,
                versionDtos,
                portfolio.createdAt(),
                portfolio.updatedAt()
        );
    }

    private List<String> checkMissingRequirements(PortfolioRecord portfolio, StudioDetailRecord studio) {
        List<String> missing = new ArrayList<>();
        List<PortfolioSectionRecord> sections = portfolioRepository.findSectionsByPortfolioId(portfolio.id());

        Set<SectionType> visibleTypes = sections.stream()
                .filter(PortfolioSectionRecord::isVisible)
                .map(PortfolioSectionRecord::sectionType)
                .collect(Collectors.toSet());

        for (SectionType required : SectionType.REQUIRED_SECTION_TYPES) {
            if (!visibleTypes.contains(required)) {
                missing.add("Required section '" + required.name() + "' must be visible");
            }
        }

        if (portfolio.headline() == null || portfolio.headline().isBlank()) {
            missing.add("Portfolio headline is required");
        }

        if (studio.services() == null || studio.services().isEmpty()) {
            missing.add("At least one studio service must be configured");
        }

        if (studio.contacts() == null || studio.contacts().stream().noneMatch(c -> Boolean.TRUE.equals(c.publicConsent()))) {
            missing.add("At least one contact channel with public consent must be configured");
        }

        return missing;
    }

    private JsonNode parseJsonContent(String content) {
        if (content == null || content.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(content);
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode();
        }
    }

    private UserRecord validateActiveUser(UUID userId) {
        UserRecord user = securityRepository.findUserById(userId)
                .orElseThrow(() -> new AccessDeniedException("User account not found"));
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new AccessDeniedException("Account is suspended or inactive");
        }
        return user;
    }

    private void validateProfessionalRole(ActorContext actor) {
        if (!actor.hasRole("DESIGNER") && !actor.hasRole("DESIGNER_TEAM") &&
            !actor.hasRole("SUPER_ADMIN") && !actor.hasRole("ADMIN")) {
            throw new AccessDeniedException("Professional onboarding required to access portfolio");
        }
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
            throw new AccessDeniedException("Access denied: elevated studio role (OWNER or ADMIN) required for portfolio modifications");
        }
    }

    private record ResolvedStudioContext(UUID studioId, String role) {}
}
