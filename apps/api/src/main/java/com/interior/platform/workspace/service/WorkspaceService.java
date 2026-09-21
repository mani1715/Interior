package com.interior.platform.workspace.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.workspace.dto.WorkspaceBusinessProfileResponse;
import com.interior.platform.workspace.dto.WorkspaceSummaryResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkspaceService {

    private final SecurityRepository securityRepository;
    private final StudioRepository studioRepository;
    private final AuthorizationService authorizationService;
    private final com.interior.platform.portfolio.service.PortfolioService portfolioService;
    private final com.interior.platform.projects.repository.ProjectRepository projectRepository;

    public WorkspaceService(
            SecurityRepository securityRepository,
            StudioRepository studioRepository,
            AuthorizationService authorizationService
    ) {
        this(securityRepository, studioRepository, authorizationService, null, null);
    }

    public WorkspaceService(
            SecurityRepository securityRepository,
            StudioRepository studioRepository,
            AuthorizationService authorizationService,
            com.interior.platform.portfolio.service.PortfolioService portfolioService
    ) {
        this(securityRepository, studioRepository, authorizationService, portfolioService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public WorkspaceService(
            SecurityRepository securityRepository,
            StudioRepository studioRepository,
            AuthorizationService authorizationService,
            com.interior.platform.portfolio.service.PortfolioService portfolioService,
            com.interior.platform.projects.repository.ProjectRepository projectRepository
    ) {
        this.securityRepository = securityRepository;
        this.studioRepository = studioRepository;
        this.authorizationService = authorizationService;
        this.portfolioService = portfolioService;
        this.projectRepository = projectRepository;
    }

    /**
     * Retrieves the aggregate workspace summary for the authenticated professional actor.
     * Enforces active account status, DESIGNER/DESIGNER_TEAM role verification, and membership-backed active studio resolution.
     */
    public WorkspaceSummaryResponse getWorkspaceSummary(ActorContext actor, UUID requestedStudioId) {
        authorizationService.requireAuthenticated(actor);

        UserRecord user = validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext studioContext = resolveActiveStudio(actor, requestedStudioId);
        StudioDetailRecord studio = studioRepository.findStudioById(studioContext.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        // User summary
        WorkspaceSummaryResponse.WorkspaceUserDto userDto = new WorkspaceSummaryResponse.WorkspaceUserDto(
                user.displayName(),
                user.email(),
                new ArrayList<>(actor.platformRoles())
        );

        // Studio summary
        WorkspaceSummaryResponse.WorkspaceStudioDto studioDto = new WorkspaceSummaryResponse.WorkspaceStudioDto(
                studio.id(),
                studio.name(),
                studio.slug(),
                studio.professionalType(),
                studio.professionalTitle(),
                studio.tagline(),
                studioContext.role(),
                studio.status(),
                studio.publicationStatus(),
                studio.city(),
                studio.state(),
                studio.services() != null ? studio.services().size() : 0,
                studio.specialties() != null ? studio.specialties().size() : 0,
                studio.serviceAreas() != null ? studio.serviceAreas().size() : 0,
                studio.contacts() != null ? studio.contacts().size() : 0
        );

        // Available studio memberships
        List<WorkspaceSummaryResponse.StudioMembershipSummaryDto> availableStudios = studioContext.allMemberships().stream()
                .map(m -> new WorkspaceSummaryResponse.StudioMembershipSummaryDto(
                        m.studioId(),
                        m.studioName(),
                        m.studioSlug(),
                        m.role()
                ))
                .toList();

        // Portfolio readiness
        String portfolioStatus = portfolioService != null
                ? portfolioService.getPortfolioReadinessStatus(studio.id())
                : "NOT_CONFIGURED";

        // Projects readiness & count
        int projectCount = projectRepository != null ? projectRepository.countProjects(studio.id()) : 0;
        int readyProjectCount = projectRepository != null ? projectRepository.countReadyProjects(studio.id()) : 0;

        // Completeness calculation
        WorkspaceSummaryResponse.CompletenessDto completeness = calculateCompleteness(studio, portfolioStatus, readyProjectCount);

        // Dynamic setup checklist
        List<WorkspaceSummaryResponse.SetupChecklistItemDto> checklist = buildSetupChecklist(studio, portfolioStatus, projectCount);

        // Module readiness
        List<WorkspaceSummaryResponse.ModuleReadinessDto> modules = deriveModuleReadiness(studio, portfolioStatus, projectCount);

        // Truthful activity feed (derived strictly from business lifecycle timestamps; NEVER from raw audit events)
        List<WorkspaceSummaryResponse.ActivityItemDto> activityFeed = buildActivityFeed(studio);

        return new WorkspaceSummaryResponse(
                userDto,
                studioDto,
                availableStudios,
                completeness,
                checklist,
                modules,
                activityFeed
        );
    }

    /**
     * Retrieves the detailed business profile for the authenticated professional.
     */
    public WorkspaceBusinessProfileResponse getBusinessProfile(ActorContext actor, UUID requestedStudioId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext studioContext = resolveActiveStudio(actor, requestedStudioId);
        StudioDetailRecord studio = studioRepository.findStudioById(studioContext.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found"));

        List<WorkspaceBusinessProfileResponse.BusinessContactItemDto> contactDtos = studio.contacts() != null
                ? studio.contacts().stream().map(c -> new WorkspaceBusinessProfileResponse.BusinessContactItemDto(
                        c.kind(),
                        c.value(),
                        c.publicConsent(),
                        c.publicConsent() ? "Public when portfolio published" : "Private / Internal only",
                        c.sortOrder()
                )).toList()
                : List.of();

        List<WorkspaceBusinessProfileResponse.BusinessServiceItemDto> serviceDtos = studio.services() != null
                ? studio.services().stream().map(s -> new WorkspaceBusinessProfileResponse.BusinessServiceItemDto(
                        s.serviceCode(), s.serviceName()
                )).toList()
                : List.of();

        List<WorkspaceBusinessProfileResponse.BusinessSpecialtyItemDto> specialtyDtos = studio.specialties() != null
                ? studio.specialties().stream().map(sp -> new WorkspaceBusinessProfileResponse.BusinessSpecialtyItemDto(
                        sp.specialtyCode(), sp.specialtyName()
                )).toList()
                : List.of();

        List<WorkspaceBusinessProfileResponse.BusinessServiceAreaItemDto> areaDtos = studio.serviceAreas() != null
                ? studio.serviceAreas().stream().map(a -> new WorkspaceBusinessProfileResponse.BusinessServiceAreaItemDto(
                        a.cityName(), a.locality()
                )).toList()
                : List.of();

        // GSTIN is sensitive commercial data: exposed only to authenticated studio owner
        String gstNumber = "OWNER".equalsIgnoreCase(studioContext.role()) ? studio.gstNumber() : null;

        return new WorkspaceBusinessProfileResponse(
                studio.id(),
                studio.name(),
                studio.slug(),
                studio.professionalType(),
                studio.professionalTitle(),
                studio.tagline(),
                studio.experienceSinceYear(),
                studio.teamSize(),
                studio.budgetRange(),
                studio.addressLine(),
                studio.city(),
                studio.district(),
                studio.state(),
                studio.postalCode(),
                studio.country(),
                studio.travelAvailable(),
                studio.gstRegistered(),
                gstNumber,
                studio.status(),
                studio.publicationStatus(),
                studioContext.role(),
                studio.onboardingCompletedAt(),
                contactDtos,
                serviceDtos,
                specialtyDtos,
                areaDtos
        );
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
            throw new AccessDeniedException("Professional onboarding required to access workspace");
        }
    }

    /**
     * Resolves the active studio context strictly through server-validated memberships.
     * Never falls back to designer_onboarding_completions as authority.
     */
    private ResolvedStudioContext resolveActiveStudio(ActorContext actor, UUID requestedStudioId) {
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
            return new ResolvedStudioContext(match.get().studioId(), match.get().role(), memberships);
        }

        if (actor.activeStudioId() != null) {
            Optional<StudioMemberRecord> match = memberships.stream()
                    .filter(m -> m.studioId().equals(actor.activeStudioId()))
                    .findFirst();
            if (match.isPresent()) {
                return new ResolvedStudioContext(match.get().studioId(), match.get().role(), memberships);
            }
        }

        // Default to primary (first) active membership
        StudioMemberRecord primary = memberships.get(0);
        return new ResolvedStudioContext(primary.studioId(), primary.role(), memberships);
    }

    /**
     * Computes professional profile completeness and platform readiness deterministically.
     * Core profile completeness consists of 5 equal dimensions (20% each, total 100%):
     * 1. Identity (name, slug, professionalType, professionalTitle) -> 20 pts
     * 2. Location (city, state, postalCode + at least 1 service area) -> 20 pts (10 for address, 10 for area)
     * 3. Services (at least 1 service selected) -> 20 pts
     * 4. Specialties (at least 1 specialty selected) -> 20 pts
     * 5. Contacts (at least 1 contact channel provided) -> 20 pts
     * Optional fields (GST, Instagram, Website, Team size, Budget range) do NOT penalize this score.
     */
    public WorkspaceSummaryResponse.CompletenessDto calculateCompleteness(StudioDetailRecord studio) {
        int identityScore = 0;
        if (studio.name() != null && !studio.name().isBlank() &&
            studio.slug() != null && !studio.slug().isBlank() &&
            studio.professionalType() != null && !studio.professionalType().isBlank() &&
            studio.professionalTitle() != null && !studio.professionalTitle().isBlank()) {
            identityScore = 20;
        }

        int locationScore = 0;
        if (studio.city() != null && !studio.city().isBlank() &&
            studio.state() != null && !studio.state().isBlank()) {
            locationScore += 10;
        }
        if (studio.serviceAreas() != null && !studio.serviceAreas().isEmpty()) {
            locationScore += 10;
        }

        int servicesScore = (studio.services() != null && !studio.services().isEmpty()) ? 20 : 0;
        int specialtiesScore = (studio.specialties() != null && !studio.specialties().isEmpty()) ? 20 : 0;
        int contactsScore = (studio.contacts() != null && !studio.contacts().isEmpty()) ? 20 : 0;

        int profileScore = identityScore + locationScore + servicesScore + specialtiesScore + contactsScore;

        return calculateCompletenessWithScores(studio, profileScore, identityScore, locationScore, servicesScore, specialtiesScore, contactsScore, "NOT_CONFIGURED", 0);
    }

    public WorkspaceSummaryResponse.CompletenessDto calculateCompleteness(StudioDetailRecord studio, String portfolioStatus) {
        int identityScore = 0;
        if (studio.name() != null && !studio.name().isBlank() &&
            studio.slug() != null && !studio.slug().isBlank() &&
            studio.professionalType() != null && !studio.professionalType().isBlank() &&
            studio.professionalTitle() != null && !studio.professionalTitle().isBlank()) {
            identityScore = 20;
        }

        int locationScore = 0;
        if (studio.city() != null && !studio.city().isBlank() &&
            studio.state() != null && !studio.state().isBlank()) {
            locationScore += 10;
        }
        if (studio.serviceAreas() != null && !studio.serviceAreas().isEmpty()) {
            locationScore += 10;
        }

        int servicesScore = (studio.services() != null && !studio.services().isEmpty()) ? 20 : 0;
        int specialtiesScore = (studio.specialties() != null && !studio.specialties().isEmpty()) ? 20 : 0;
        int contactsScore = (studio.contacts() != null && !studio.contacts().isEmpty()) ? 20 : 0;

        int profileScore = identityScore + locationScore + servicesScore + specialtiesScore + contactsScore;

        return calculateCompletenessWithScores(studio, profileScore, identityScore, locationScore, servicesScore, specialtiesScore, contactsScore, portfolioStatus, 0);
    }

    public WorkspaceSummaryResponse.CompletenessDto calculateCompleteness(StudioDetailRecord studio, String portfolioStatus, int readyProjectCount) {
        int identityScore = 0;
        if (studio.name() != null && !studio.name().isBlank() &&
            studio.slug() != null && !studio.slug().isBlank() &&
            studio.professionalType() != null && !studio.professionalType().isBlank() &&
            studio.professionalTitle() != null && !studio.professionalTitle().isBlank()) {
            identityScore = 20;
        }

        int locationScore = 0;
        if (studio.city() != null && !studio.city().isBlank() &&
            studio.state() != null && !studio.state().isBlank()) {
            locationScore += 10;
        }
        if (studio.serviceAreas() != null && !studio.serviceAreas().isEmpty()) {
            locationScore += 10;
        }

        int servicesScore = (studio.services() != null && !studio.services().isEmpty()) ? 20 : 0;
        int specialtiesScore = (studio.specialties() != null && !studio.specialties().isEmpty()) ? 20 : 0;
        int contactsScore = (studio.contacts() != null && !studio.contacts().isEmpty()) ? 20 : 0;

        int profileScore = identityScore + locationScore + servicesScore + specialtiesScore + contactsScore;

        return calculateCompletenessWithScores(studio, profileScore, identityScore, locationScore, servicesScore, specialtiesScore, contactsScore, portfolioStatus, readyProjectCount);
    }

    private WorkspaceSummaryResponse.CompletenessDto calculateCompletenessWithScores(
            StudioDetailRecord studio,
            int profileScore,
            int identityScore,
            int locationScore,
            int servicesScore,
            int specialtiesScore,
            int contactsScore,
            String portfolioStatus,
            int readyProjectCount
    ) {
        // Platform Launch Readiness (Portfolio contributes 25% when READY; Projects contributes 25% when ready)
        int portfolioScore = "READY".equalsIgnoreCase(portfolioStatus) ? 25 : 0;
        int projectsScore = (readyProjectCount > 0) ? 25 : 0;
        int platformReadiness = Math.round(profileScore * 0.5f) + portfolioScore + projectsScore;

        String status = (profileScore == 100) ? "PROFILE_COMPLETED" : "IN_PROGRESS";

        return new WorkspaceSummaryResponse.CompletenessDto(
                profileScore,
                platformReadiness,
                status,
                new WorkspaceSummaryResponse.CompletenessBreakdownDto(
                        identityScore,
                        locationScore,
                        servicesScore,
                        specialtiesScore,
                        contactsScore,
                        portfolioScore,
                        projectsScore
                )
        );
    }

    private List<WorkspaceSummaryResponse.SetupChecklistItemDto> buildSetupChecklist(StudioDetailRecord studio, String portfolioStatus) {
        return buildSetupChecklist(studio, portfolioStatus, 0);
    }

    private List<WorkspaceSummaryResponse.SetupChecklistItemDto> buildSetupChecklist(StudioDetailRecord studio, String portfolioStatus, int projectCount) {
        List<WorkspaceSummaryResponse.SetupChecklistItemDto> items = new ArrayList<>();

        items.add(new WorkspaceSummaryResponse.SetupChecklistItemDto(
                "registration",
                "Professional Registration",
                "Account authenticated and professional workspace activated.",
                true,
                "View",
                "/workspace"
        ));

        items.add(new WorkspaceSummaryResponse.SetupChecklistItemDto(
                "identity",
                "Business Identity",
                "Studio handle, professional classification, and title defined.",
                studio.name() != null && !studio.name().isBlank(),
                "View Profile",
                "/workspace/business"
        ));

        items.add(new WorkspaceSummaryResponse.SetupChecklistItemDto(
                "location",
                "Location & Service Coverage",
                "Primary operational city and service coverage localities.",
                studio.serviceAreas() != null && !studio.serviceAreas().isEmpty(),
                "View Coverage",
                "/workspace/business"
        ));

        items.add(new WorkspaceSummaryResponse.SetupChecklistItemDto(
                "services",
                "Services Offered",
                "Residential, commercial, and turnkey services catalog.",
                studio.services() != null && !studio.services().isEmpty(),
                "View Services",
                "/workspace/business"
        ));

        items.add(new WorkspaceSummaryResponse.SetupChecklistItemDto(
                "specialties",
                "Design Specialties",
                "Aesthetic and architectural design styles configured.",
                studio.specialties() != null && !studio.specialties().isEmpty(),
                "View Specialties",
                "/workspace/business"
        ));

        items.add(new WorkspaceSummaryResponse.SetupChecklistItemDto(
                "contacts",
                "Contact Channels",
                "Direct phone, WhatsApp, and email channels configured.",
                studio.contacts() != null && !studio.contacts().isEmpty(),
                "View Contacts",
                "/workspace/business"
        ));

        boolean portfolioReady = "READY".equalsIgnoreCase(portfolioStatus);
        items.add(new WorkspaceSummaryResponse.SetupChecklistItemDto(
                "portfolio",
                "Configure Portfolio Website",
                portfolioReady ? "Portfolio configured with required structural sections." : "Select portfolio theme layout and structure in Portfolio Builder.",
                portfolioReady,
                portfolioReady ? "Manage Portfolio" : "Prepare Portfolio",
                "/workspace/portfolio"
        ));

        boolean projectsReady = projectCount > 0;
        items.add(new WorkspaceSummaryResponse.SetupChecklistItemDto(
                "projects",
                "Publish First Project Story",
                projectsReady ? "Projects configured and managed in Project CMS." : "Create and organize your residential or commercial interior projects.",
                projectsReady,
                projectsReady ? "Manage Projects" : "Create Project",
                "/workspace/projects"
        ));

        return items;
    }

    private List<WorkspaceSummaryResponse.ModuleReadinessDto> deriveModuleReadiness(StudioDetailRecord studio, String portfolioStatus) {
        return deriveModuleReadiness(studio, portfolioStatus, 0);
    }

    private List<WorkspaceSummaryResponse.ModuleReadinessDto> deriveModuleReadiness(StudioDetailRecord studio, String portfolioStatus, int projectCount) {
        String portfolioAction = "READY".equalsIgnoreCase(portfolioStatus) ? "Manage Portfolio" : "Configure Portfolio";
        return List.of(
                new WorkspaceSummaryResponse.ModuleReadinessDto(
                        "portfolio",
                        "Portfolio",
                        "Build your professional portfolio website to showcase your style and identity.",
                        portfolioStatus,
                        "/workspace/portfolio",
                        portfolioAction
                ),
                new WorkspaceSummaryResponse.ModuleReadinessDto(
                        "projects",
                        "Projects",
                        "Showcase completed residential and commercial interior projects and case studies.",
                        "READY",
                        "/workspace/projects",
                        projectCount > 0 ? "Manage Projects" : "Add Project"
                ),
                new WorkspaceSummaryResponse.ModuleReadinessDto(
                        "media",
                        "Media Library",
                        "Centralized storage for high-resolution project photography, plans, and brand assets.",
                        "READY",
                        "/workspace/media",
                        "Manage Media"
                ),
                new WorkspaceSummaryResponse.ModuleReadinessDto(
                        "ai",
                        "AI Studio",
                        "Transform site photos and room dimensions into conceptual spatial renders.",
                        "COMING_SOON",
                        "/workspace/ai",
                        "Learn About This Feature"
                ),
                new WorkspaceSummaryResponse.ModuleReadinessDto(
                        "leads",
                        "Leads & Clients",
                        "Manage client inquiries, consultation requests, and project briefs.",
                        "COMING_SOON",
                        "/workspace/leads",
                        "View Overview"
                ),
                new WorkspaceSummaryResponse.ModuleReadinessDto(
                        "seo",
                        "SEO Center",
                        "Optimize search appearance, local keywords, and portfolio discoverability.",
                        "READY",
                        "/workspace/seo",
                        "Manage SEO"
                ),
                new WorkspaceSummaryResponse.ModuleReadinessDto(
                        "analytics",
                        "Analytics",
                        "Track portfolio engagement, project impressions, and visitor inquiries once live.",
                        "COMING_SOON",
                        "/workspace/analytics",
                        "Learn About This Feature"
                ),
                new WorkspaceSummaryResponse.ModuleReadinessDto(
                        "notifications",
                        "Notifications",
                        "Platform updates, system notices, and client communication alerts.",
                        "COMING_SOON",
                        "/workspace/notifications",
                        "View Overview"
                ),
                new WorkspaceSummaryResponse.ModuleReadinessDto(
                        "business",
                        "Business Profile",
                        "View and manage your registered professional details, services, and contacts.",
                        "READY",
                        "/workspace/business",
                        "View Business Profile"
                )
        );
    }

    private List<WorkspaceSummaryResponse.ActivityItemDto> buildActivityFeed(StudioDetailRecord studio) {
        List<WorkspaceSummaryResponse.ActivityItemDto> feed = new ArrayList<>();

        if (studio.onboardingCompletedAt() != null) {
            feed.add(new WorkspaceSummaryResponse.ActivityItemDto(
                    "act-1",
                    "Professional Onboarding Completed",
                    "Studio profile, services, and commercial details successfully registered. Professional workspace enabled.",
                    studio.onboardingCompletedAt(),
                    "ONBOARDING"
            ));
        }

        if (studio.createdAt() != null) {
            feed.add(new WorkspaceSummaryResponse.ActivityItemDto(
                    "act-2",
                    "Studio Workspace Initialized",
                    "Professional workspace initialized with operational status ACTIVE.",
                    studio.createdAt(),
                    "SYSTEM"
            ));
        }

        return feed;
    }

    private record ResolvedStudioContext(
            UUID studioId,
            String role,
            List<StudioMemberRecord> allMemberships
    ) {}
}
