package com.interior.platform.projects.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.projects.domain.AreaUnit;
import com.interior.platform.projects.domain.BudgetVisibility;
import com.interior.platform.projects.domain.ClientNameVisibility;
import com.interior.platform.projects.domain.ProjectCategory;
import com.interior.platform.projects.domain.ProjectScope;
import com.interior.platform.projects.domain.ProjectStatus;
import com.interior.platform.projects.domain.ProjectStyle;
import com.interior.platform.projects.domain.PropertyType;
import com.interior.platform.projects.domain.StudioProjectRecord;
import com.interior.platform.projects.domain.VisibilityStatus;
import com.interior.platform.projects.dto.CreateProjectRequest;
import com.interior.platform.projects.dto.ProjectDetailResponse;
import com.interior.platform.projects.dto.ProjectPresentationDto;
import com.interior.platform.projects.dto.ProjectSummaryResponse;
import com.interior.platform.projects.dto.ReorderProjectsRequest;
import com.interior.platform.projects.dto.UpdateProjectRequest;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SecurityRepository securityRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final com.interior.platform.media.service.MediaService mediaService;

    public ProjectService(
            ProjectRepository projectRepository,
            SecurityRepository securityRepository,
            AuthorizationService authorizationService,
            AuditService auditService
    ) {
        this(projectRepository, securityRepository, authorizationService, auditService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ProjectService(
            ProjectRepository projectRepository,
            SecurityRepository securityRepository,
            AuthorizationService authorizationService,
            AuditService auditService,
            @org.springframework.context.annotation.Lazy com.interior.platform.media.service.MediaService mediaService
    ) {
        this.projectRepository = projectRepository;
        this.securityRepository = securityRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.mediaService = mediaService;
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> listProjects(
            ActorContext actor,
            UUID requestedStudioId,
            ProjectStatus status,
            ProjectCategory category,
            VisibilityStatus visibility,
            Boolean featured,
            boolean includeArchived
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        List<StudioProjectRecord> projects = projectRepository.listProjects(
                context.studioId(),
                status,
                category,
                visibility,
                featured,
                includeArchived
        );

        if (projects.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> projectIds = projects.stream().map(StudioProjectRecord::id).toList();
        Map<UUID, List<ProjectStyle>> stylesMap = projectRepository.findStylesByProjectIds(projectIds);

        return projects.stream()
                .map(p -> toSummaryResponse(p, stylesMap.getOrDefault(p.id(), Collections.emptyList())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProject(ActorContext actor, UUID requestedStudioId, UUID projectId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        StudioProjectRecord project = projectRepository.findProjectById(context.studioId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        List<ProjectStyle> styles = projectRepository.findStylesByProjectId(project.id());
        return toDetailResponse(project, styles);
    }

    @Transactional
    public ProjectDetailResponse createProject(
            ActorContext actor,
            UUID requestedStudioId,
            CreateProjectRequest request
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        validateBudgetAndClient(
                request.budgetMin(),
                request.budgetMax(),
                request.clientNameVisibility(),
                request.clientDisplayName()
        );

        String slug = generateUniqueSlug(context.studioId(), request.title(), null);
        int displayOrder = projectRepository.getNextDisplayOrder(context.studioId());

        ReadinessEvaluation readiness = evaluateReadiness(
                request.title(),
                request.categoryCode(),
                request.shortDescription(),
                request.city(),
                request.state()
        );

        ProjectStatus projectStatus = readiness.isReady() ? ProjectStatus.READY : ProjectStatus.DRAFT;
        VisibilityStatus visibilityStatus = request.visibilityStatus() != null ? request.visibilityStatus() : VisibilityStatus.PRIVATE;
        BudgetVisibility budgetVisibility = request.budgetVisibility() != null ? request.budgetVisibility() : BudgetVisibility.HIDDEN;
        ClientNameVisibility clientNameVisibility = request.clientNameVisibility() != null ? request.clientNameVisibility() : ClientNameVisibility.HIDDEN;
        String country = (request.country() != null && !request.country().isBlank()) ? request.country().toUpperCase(Locale.ROOT) : "IN";
        String currency = (request.currency() != null && !request.currency().isBlank()) ? request.currency().toUpperCase(Locale.ROOT) : "INR";
        boolean featured = Boolean.TRUE.equals(request.featured());

        UUID projectId = UuidV7.randomUuid();
        Instant now = Instant.now();

        StudioProjectRecord newProject = new StudioProjectRecord(
                projectId,
                context.studioId(),
                slug,
                request.title().trim(),
                request.shortDescription() != null ? request.shortDescription().trim() : null,
                request.fullDescription() != null ? request.fullDescription().trim() : null,
                request.categoryCode(),
                projectStatus,
                visibilityStatus,
                featured,
                displayOrder,
                request.city() != null ? request.city().trim() : null,
                request.district() != null ? request.district().trim() : null,
                request.state() != null ? request.state().trim() : null,
                country,
                request.propertyType(),
                request.projectScope(),
                request.completionYear(),
                budgetVisibility,
                request.budgetMin(),
                request.budgetMax(),
                currency,
                clientNameVisibility,
                request.clientDisplayName() != null ? request.clientDisplayName().trim() : null,
                request.areaValue(),
                request.areaUnit(),
                request.internalNotes() != null ? request.internalNotes().trim() : null,
                1L,
                actor.userId(),
                now,
                now,
                null
        );

        List<ProjectStyle> styles = request.styleCodes() != null ? request.styleCodes() : Collections.emptyList();
        StudioProjectRecord created = projectRepository.createProject(newProject, styles);

        auditService.record(
                actor.userId(),
                context.studioId(),
                "PROJECT_CREATED",
                "PROJECT",
                projectId.toString(),
                Map.of("title", created.title(), "category", created.categoryCode().name(), "status", created.projectStatus().name()),
                null,
                null
        );

        return toDetailResponse(created, styles);
    }

    @Transactional
    public ProjectDetailResponse updateProject(
            ActorContext actor,
            UUID requestedStudioId,
            UUID projectId,
            UpdateProjectRequest request
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        StudioProjectRecord existing = projectRepository.findProjectById(context.studioId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (existing.projectStatus() == ProjectStatus.ARCHIVED) {
            throw new BadRequestException("Archived projects cannot be edited. Please restore the project first.");
        }

        validateBudgetAndClient(
                request.budgetMin(),
                request.budgetMax(),
                request.clientNameVisibility(),
                request.clientDisplayName()
        );

        String slug = existing.slug();
        if (!existing.title().equalsIgnoreCase(request.title().trim())) {
            slug = generateUniqueSlug(context.studioId(), request.title(), existing.id());
        }

        ReadinessEvaluation readiness = evaluateReadiness(
                request.title(),
                request.categoryCode(),
                request.shortDescription(),
                request.city(),
                request.state()
        );

        ProjectStatus projectStatus = readiness.isReady() ? ProjectStatus.READY : ProjectStatus.DRAFT;
        VisibilityStatus visibilityStatus = request.visibilityStatus() != null ? request.visibilityStatus() : existing.visibilityStatus();
        BudgetVisibility budgetVisibility = request.budgetVisibility() != null ? request.budgetVisibility() : existing.budgetVisibility();
        ClientNameVisibility clientNameVisibility = request.clientNameVisibility() != null ? request.clientNameVisibility() : existing.clientNameVisibility();
        String country = (request.country() != null && !request.country().isBlank()) ? request.country().toUpperCase(Locale.ROOT) : existing.country();
        String currency = (request.currency() != null && !request.currency().isBlank()) ? request.currency().toUpperCase(Locale.ROOT) : existing.currency();
        boolean featured = request.featured() != null ? request.featured() : existing.featured();

        StudioProjectRecord updatedRecord = new StudioProjectRecord(
                existing.id(),
                existing.studioId(),
                slug,
                request.title().trim(),
                request.shortDescription() != null ? request.shortDescription().trim() : null,
                request.fullDescription() != null ? request.fullDescription().trim() : null,
                request.categoryCode(),
                projectStatus,
                visibilityStatus,
                featured,
                existing.displayOrder(),
                request.city() != null ? request.city().trim() : null,
                request.district() != null ? request.district().trim() : null,
                request.state() != null ? request.state().trim() : null,
                country,
                request.propertyType(),
                request.projectScope(),
                request.completionYear(),
                budgetVisibility,
                request.budgetMin(),
                request.budgetMax(),
                currency,
                clientNameVisibility,
                request.clientDisplayName() != null ? request.clientDisplayName().trim() : null,
                request.areaValue(),
                request.areaUnit(),
                request.internalNotes() != null ? request.internalNotes().trim() : null,
                existing.version(),
                existing.createdBy(),
                existing.createdAt(),
                Instant.now(),
                existing.archivedAt()
        );

        List<ProjectStyle> styles = request.styleCodes() != null ? request.styleCodes() : Collections.emptyList();
        int rows = projectRepository.updateProject(updatedRecord, styles, request.version());
        if (rows == 0) {
            throw new ConflictException("Project was modified concurrently. Please reload and try again.");
        }

        auditService.record(
                actor.userId(),
                context.studioId(),
                "PROJECT_UPDATED",
                "PROJECT",
                projectId.toString(),
                Map.of("title", updatedRecord.title(), "status", updatedRecord.projectStatus().name(), "version", existing.version() + 1),
                null,
                null
        );

        StudioProjectRecord reloaded = projectRepository.findProjectById(context.studioId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found after update"));

        return toDetailResponse(reloaded, styles);
    }

    @Transactional
    public List<ProjectSummaryResponse> reorderProjects(
            ActorContext actor,
            UUID requestedStudioId,
            ReorderProjectsRequest request
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        projectRepository.updateDisplayOrders(context.studioId(), request.orderedProjectIds());

        auditService.record(
                actor.userId(),
                context.studioId(),
                "PROJECTS_REORDERED",
                "PROJECT",
                null,
                Map.of("count", request.orderedProjectIds().size()),
                null,
                null
        );

        return listProjects(actor, requestedStudioId, null, null, null, null, false);
    }

    @Transactional
    public ProjectDetailResponse archiveProject(
            ActorContext actor,
            UUID requestedStudioId,
            UUID projectId,
            long expectedVersion
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        int rows = projectRepository.archiveProject(context.studioId(), projectId, expectedVersion);
        if (rows == 0) {
            throw new ConflictException("Project was modified concurrently. Please reload and try again.");
        }

        auditService.record(
                actor.userId(),
                context.studioId(),
                "PROJECT_ARCHIVED",
                "PROJECT",
                projectId.toString(),
                Map.of("archived", true),
                null,
                null
        );

        StudioProjectRecord reloaded = projectRepository.findProjectById(context.studioId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found after archive"));
        List<ProjectStyle> styles = projectRepository.findStylesByProjectId(projectId);

        return toDetailResponse(reloaded, styles);
    }

    @Transactional
    public ProjectDetailResponse restoreProject(
            ActorContext actor,
            UUID requestedStudioId,
            UUID projectId,
            long expectedVersion
    ) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        int rows = projectRepository.restoreProject(context.studioId(), projectId, expectedVersion);
        if (rows == 0) {
            throw new ConflictException("Project was modified concurrently. Please reload and try again.");
        }

        // Re-evaluate readiness upon restore
        StudioProjectRecord reloaded = projectRepository.findProjectById(context.studioId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found after restore"));

        ReadinessEvaluation readiness = evaluateReadiness(
                reloaded.title(),
                reloaded.categoryCode(),
                reloaded.shortDescription(),
                reloaded.city(),
                reloaded.state()
        );
        ProjectStatus newStatus = readiness.isReady() ? ProjectStatus.READY : ProjectStatus.DRAFT;

        if (newStatus != reloaded.projectStatus()) {
            List<ProjectStyle> styles = projectRepository.findStylesByProjectId(projectId);
            projectRepository.updateProject(
                    new StudioProjectRecord(
                            reloaded.id(), reloaded.studioId(), reloaded.slug(), reloaded.title(),
                            reloaded.shortDescription(), reloaded.fullDescription(), reloaded.categoryCode(),
                            newStatus, reloaded.visibilityStatus(), reloaded.featured(), reloaded.displayOrder(),
                            reloaded.city(), reloaded.district(), reloaded.state(), reloaded.country(),
                            reloaded.propertyType(), reloaded.projectScope(), reloaded.completionYear(),
                            reloaded.budgetVisibility(), reloaded.budgetMin(), reloaded.budgetMax(),
                            reloaded.currency(), reloaded.clientNameVisibility(), reloaded.clientDisplayName(),
                            reloaded.areaValue(), reloaded.areaUnit(), reloaded.internalNotes(),
                            reloaded.version(), reloaded.createdBy(), reloaded.createdAt(), Instant.now(), null
                    ),
                    styles,
                    reloaded.version()
            );
            reloaded = projectRepository.findProjectById(context.studioId(), projectId).orElse(reloaded);
        }

        auditService.record(
                actor.userId(),
                context.studioId(),
                "PROJECT_RESTORED",
                "PROJECT",
                projectId.toString(),
                Map.of("status", newStatus.name()),
                null,
                null
        );

        List<ProjectStyle> styles = projectRepository.findStylesByProjectId(projectId);
        return toDetailResponse(reloaded, styles);
    }

    @Transactional(readOnly = true)
    public List<ProjectPresentationDto> getPortfolioProjects(UUID studioId) {
        List<StudioProjectRecord> projects = projectRepository.findPortfolioProjects(studioId);
        if (projects.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> projectIds = projects.stream().map(StudioProjectRecord::id).toList();
        Map<UUID, List<ProjectStyle>> stylesMap = projectRepository.findStylesByProjectIds(projectIds);

        return projects.stream()
                .map(p -> toPresentationDto(p, stylesMap.getOrDefault(p.id(), Collections.emptyList())))
                .toList();
    }

    public ReadinessEvaluation evaluateReadiness(
            String title,
            ProjectCategory categoryCode,
            String shortDescription,
            String city,
            String state
    ) {
        List<String> missing = new ArrayList<>();
        if (title == null || title.trim().length() < 3) {
            missing.add("Title must be at least 3 characters");
        }
        if (categoryCode == null) {
            missing.add("Category is required");
        }
        if (shortDescription == null || shortDescription.trim().length() < 10) {
            missing.add("Short summary must be at least 10 characters");
        }
        if (city == null || city.trim().isBlank()) {
            missing.add("City is required");
        }
        if (state == null || state.trim().isBlank()) {
            missing.add("State is required");
        }
        return new ReadinessEvaluation(missing.isEmpty(), missing);
    }

    public String generateUniqueSlug(UUID studioId, String title, UUID excludeProjectId) {
        String baseSlug = toSlug(title);
        String candidate = baseSlug;
        int counter = 2;
        while (projectRepository.existsBySlug(studioId, candidate, excludeProjectId)) {
            candidate = baseSlug + "-" + counter;
            counter++;
        }
        return candidate;
    }

    private String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "project";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "project" : slug;
    }

    private void validateBudgetAndClient(
            BigDecimal budgetMin,
            BigDecimal budgetMax,
            ClientNameVisibility clientNameVisibility,
            String clientDisplayName
    ) {
        if (budgetMin != null && budgetMax != null && budgetMin.compareTo(budgetMax) > 0) {
            throw new BadRequestException("Minimum budget cannot exceed maximum budget");
        }
        if (clientNameVisibility == ClientNameVisibility.DISPLAY && (clientDisplayName == null || clientDisplayName.isBlank())) {
            throw new BadRequestException("Client name must be provided when client name visibility is set to DISPLAY");
        }
    }

    private ProjectDetailResponse toDetailResponse(StudioProjectRecord p, List<ProjectStyle> styles) {
        ReadinessEvaluation eval = evaluateReadiness(p.title(), p.categoryCode(), p.shortDescription(), p.city(), p.state());
        List<ProjectStyle> safeStyles = styles != null ? styles : Collections.emptyList();
        List<String> styleNames = safeStyles.stream().map(ProjectStyle::getDisplayName).toList();

        return new ProjectDetailResponse(
                p.id(),
                p.studioId(),
                p.slug(),
                p.title(),
                p.shortDescription(),
                p.fullDescription(),
                p.categoryCode(),
                p.categoryCode().getDisplayName(),
                p.projectStatus(),
                p.visibilityStatus(),
                p.featured(),
                p.displayOrder(),
                p.city(),
                p.district(),
                p.state(),
                p.country(),
                p.propertyType(),
                p.projectScope(),
                safeStyles,
                styleNames,
                p.completionYear(),
                p.budgetVisibility(),
                p.budgetMin(),
                p.budgetMax(),
                p.currency(),
                p.clientNameVisibility(),
                p.clientDisplayName(),
                p.areaValue(),
                p.areaUnit(),
                p.internalNotes(),
                p.version(),
                p.createdBy(),
                p.createdAt(),
                p.updatedAt(),
                p.archivedAt(),
                eval.isReady(),
                eval.missingFields()
        );
    }

    private ProjectSummaryResponse toSummaryResponse(StudioProjectRecord p, List<ProjectStyle> styles) {
        List<ProjectStyle> safeStyles = styles != null ? styles : Collections.emptyList();
        List<String> styleNames = safeStyles.stream().map(ProjectStyle::getDisplayName).toList();

        return new ProjectSummaryResponse(
                p.id(),
                p.slug(),
                p.title(),
                p.categoryCode(),
                p.categoryCode().getDisplayName(),
                p.projectStatus(),
                p.visibilityStatus(),
                p.featured(),
                p.displayOrder(),
                p.city(),
                p.state(),
                p.propertyType(),
                p.projectScope(),
                safeStyles,
                styleNames,
                p.completionYear(),
                p.version(),
                p.createdAt(),
                p.updatedAt(),
                p.archivedAt()
        );
    }

    private ProjectPresentationDto toPresentationDto(StudioProjectRecord p, List<ProjectStyle> styles) {
        String clientName = (p.clientNameVisibility() == ClientNameVisibility.DISPLAY) ? p.clientDisplayName() : null;

        String budgetFormatted = null;
        if (p.budgetVisibility() == BudgetVisibility.RANGE && p.budgetMin() != null && p.budgetMax() != null) {
            budgetFormatted = formatCurrency(p.currency(), p.budgetMin()) + " - " + formatCurrency(p.currency(), p.budgetMax());
        } else if (p.budgetVisibility() == BudgetVisibility.STARTING_FROM && p.budgetMin() != null) {
            budgetFormatted = "From " + formatCurrency(p.currency(), p.budgetMin());
        }

        String areaFormatted = null;
        if (p.areaValue() != null) {
            String unit = (p.areaUnit() == AreaUnit.SQ_M) ? "sq m" : "sq ft";
            areaFormatted = String.format(Locale.ROOT, "%,.0f %s", p.areaValue(), unit);
        }

        String location = null;
        if (p.city() != null && p.state() != null) {
            location = p.city() + ", " + p.state();
        } else if (p.city() != null) {
            location = p.city();
        } else if (p.state() != null) {
            location = p.state();
        }

        List<String> styleCodes = styles != null ? styles.stream().map(Enum::name).toList() : Collections.emptyList();
        List<String> styleNames = styles != null ? styles.stream().map(ProjectStyle::getDisplayName).toList() : Collections.emptyList();

        String coverUrl = mediaService != null ? mediaService.getProjectCoverImageUrl(p.studioId(), p.id()).orElse(null) : null;

        return new ProjectPresentationDto(
                p.id(),
                p.slug(),
                p.title(),
                p.shortDescription(),
                p.fullDescription(),
                p.categoryCode().name(),
                p.categoryCode().getDisplayName(),
                styleCodes,
                styleNames,
                location,
                p.propertyType() != null ? p.propertyType().getDisplayName() : null,
                p.projectScope() != null ? p.projectScope().getDisplayName() : null,
                p.completionYear(),
                p.featured(),
                p.displayOrder(),
                clientName,
                budgetFormatted,
                areaFormatted,
                coverUrl
        );
    }

    private String formatCurrency(String currency, BigDecimal amount) {
        if (amount == null) return null;
        if ("INR".equalsIgnoreCase(currency)) {
            double val = amount.doubleValue();
            if (val >= 10_000_000) {
                return String.format(Locale.ROOT, "₹%.2f Cr", val / 10_000_000);
            } else if (val >= 100_000) {
                return String.format(Locale.ROOT, "₹%.1f L", val / 100_000);
            } else {
                return String.format(Locale.ROOT, "₹%,.0f", val);
            }
        }
        return currency + " " + String.format(Locale.ROOT, "%,.0f", amount);
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
            throw new AccessDeniedException("Professional onboarding required to access project CMS");
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
            throw new AccessDeniedException("Access denied: elevated studio role (OWNER or ADMIN) required for project management");
        }
    }

    public record ReadinessEvaluation(boolean isReady, List<String> missingFields) {}
    private record ResolvedStudioContext(UUID studioId, String role) {}
}
