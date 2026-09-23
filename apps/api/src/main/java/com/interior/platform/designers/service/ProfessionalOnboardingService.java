package com.interior.platform.designers.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.OnboardingDraftRecord;
import com.interior.platform.designers.domain.ProfessionalType;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.dto.OnboardingDraftDto;
import com.interior.platform.designers.dto.OnboardingStatusResponse;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.SessionSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ProfessionalOnboardingService {

    private static final int MAX_DRAFT_BYTES = 65536; // 64 KB limit
    private static final Pattern GST_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{7,15}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern SAFE_URL_PATTERN = Pattern.compile("^https?://.*$", Pattern.CASE_INSENSITIVE);

    public record CompletionResult(
            OnboardingStatusResponse.StudioSummary studio,
            String newCsrfToken,
            String message
    ) {}

    private final StudioRepository studioRepository;
    private final SecurityRepository securityRepository;
    private final SlugValidationService slugValidationService;
    private final SessionSecurityService sessionSecurityService;
    private final AuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProfessionalOnboardingService(
            StudioRepository studioRepository,
            SecurityRepository securityRepository,
            SlugValidationService slugValidationService,
            SessionSecurityService sessionSecurityService,
            AuditService auditService
    ) {
        this.studioRepository = studioRepository;
        this.securityRepository = securityRepository;
        this.slugValidationService = slugValidationService;
        this.sessionSecurityService = sessionSecurityService;
        this.auditService = auditService;
    }

    /**
     * Authoritatively retrieves onboarding status for the authenticated user.
     */
    public OnboardingStatusResponse getStatus(ActorContext actor) {
        validateAuthenticatedActor(actor);

        UserRecord user = securityRepository.findUserById(actor.userId())
                .orElseThrow(() -> new UnauthorizedException("User account not found"));

        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            return OnboardingStatusResponse.blocked("Account is not active. Status: " + user.status());
        }

        // Canonical Initial Onboarding Check
        Optional<UUID> initialStudioId = studioRepository.findInitialOnboardingStudioId(actor.userId());
        if (initialStudioId.isPresent()) {
            Optional<StudioDetailRecord> sOpt = studioRepository.findStudioById(initialStudioId.get());
            if (sOpt.isPresent()) {
                StudioDetailRecord s = sOpt.get();
                return OnboardingStatusResponse.completed(new OnboardingStatusResponse.StudioSummary(
                        s.id(), s.name(), s.slug(), s.professionalType(), s.status(), s.publicationStatus(), "OWNER"
                ));
            }
        }

        // Check for active draft
        Optional<OnboardingDraftRecord> draftOpt = studioRepository.findDraftByUserId(actor.userId());
        if (draftOpt.isPresent()) {
            OnboardingDraftRecord draft = draftOpt.get();
            if ("COMPLETED".equalsIgnoreCase(draft.status())) {
                // Draft marked completed, studio may be looked up
                return OnboardingStatusResponse.completed(null);
            }
            return OnboardingStatusResponse.inProgress(draft.step(), draft.draftPayload());
        }

        return OnboardingStatusResponse.notStarted();
    }

    /**
     * Saves partial progress of an onboarding draft with security field stripping.
     */
    public void saveDraft(ActorContext actor, OnboardingDraftDto dto) {
        validateAuthenticatedActor(actor);

        if (dto == null || dto.draftPayload() == null || dto.draftPayload().isBlank()) {
            throw new BadRequestException("Draft payload cannot be empty");
        }

        if (dto.draftPayload().getBytes().length > MAX_DRAFT_BYTES) {
            throw new BadRequestException("Draft payload exceeds maximum allowed size of 64KB");
        }

        String sanitizedPayload = sanitizeDraftPayload(dto.draftPayload());
        int step = Math.max(1, Math.min(7, dto.step()));
        studioRepository.saveDraft(actor.userId(), step, sanitizedPayload, "IN_PROGRESS");
    }

    private String sanitizeDraftPayload(String rawJson) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(rawJson);
            if (root instanceof com.fasterxml.jackson.databind.node.ObjectNode obj) {
                obj.remove(java.util.List.of(
                        "userId", "ownerUserId", "studioId", "tenantId",
                        "role", "roles", "permissions", "publicationStatus",
                        "status", "verified", "isAdmin"
                ));
                return objectMapper.writeValueAsString(obj);
            }
            return rawJson;
        } catch (Exception e) {
            throw new BadRequestException("Invalid draft JSON payload");
        }
    }

    /**
     * Atomically completes professional onboarding.
     * Establishes studio entity, slug claim, owner membership, and grants DESIGNER role.
     */
    @Transactional
    public CompletionResult completeOnboarding(
            ActorContext actor,
            OnboardingCompletionRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        validateAuthenticatedActor(actor);

        UserRecord user = securityRepository.findUserById(actor.userId())
                .orElseThrow(() -> new UnauthorizedException("User account not found"));

        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new AccessDeniedException("Only active accounts can complete professional onboarding");
        }

        // Canonical Initial Onboarding Idempotency: check if initial onboarding completion record exists
        Optional<UUID> initialStudioId = studioRepository.findInitialOnboardingStudioId(actor.userId());
        if (initialStudioId.isPresent()) {
            StudioDetailRecord s = studioRepository.findStudioById(initialStudioId.get())
                    .orElseThrow(() -> new IllegalStateException("Initial onboarding studio record missing"));
            return new CompletionResult(
                    new OnboardingStatusResponse.StudioSummary(
                            s.id(), s.name(), s.slug(), s.professionalType(), s.status(), s.publicationStatus(), "OWNER"
                    ),
                    null,
                    "Initial onboarding was already completed. Canonical initial studio retrieved."
            );
        }

        // Validate payload
        validateCompletionPayload(request);

        String rawSlug = request.slug() != null && !request.slug().isBlank()
                ? request.slug()
                : slugValidationService.generateSlug(request.studioName());

        var slugCheck = slugValidationService.checkAvailability(rawSlug);
        if (!slugCheck.available()) {
            throw new ConflictException(slugCheck.reason());
        }

        String normalizedSlug = slugCheck.slug();
        UUID studioId = UuidV7.randomUuid();
        Instant now = Instant.now();

        // Sanitize text inputs
        String studioName = sanitizeText(request.studioName());
        String professionalTitle = sanitizeText(request.professionalTitle());
        String tagline = sanitizeText(request.tagline());
        String cleanAddress = sanitizeText(request.addressLine());
        String cleanCity = sanitizeText(request.city());
        String cleanDistrict = sanitizeText(request.district());
        String cleanState = sanitizeText(request.state());
        String cleanPostal = sanitizeText(request.postalCode());

        ProfessionalType profType = ProfessionalType.fromCode(request.professionalType());

        // 1. Create Studio Entity (Operational: ACTIVE, Publication: UNPUBLISHED)
        StudioDetailRecord studio = new StudioDetailRecord(
                studioId,
                studioName,
                normalizedSlug,
                actor.userId(),
                "ACTIVE",
                profType.name(),
                professionalTitle,
                tagline,
                request.experienceSinceYear(),
                request.teamSize(),
                request.budgetRange(),
                cleanAddress,
                cleanCity,
                cleanDistrict,
                cleanState,
                cleanPostal,
                request.country() != null ? request.country() : "IN",
                request.travelAvailable(),
                request.gstRegistered(),
                request.gstRegistered() ? request.gstNumber() : null,
                "UNPUBLISHED", // Active operationally, but not yet published to public discovery
                now,
                now,
                now,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        studioRepository.createStudio(studio);

        // 2. Claim Slug (State: CURRENT)
        studioRepository.claimSlug(studioId, normalizedSlug, "CURRENT");

        // 3. Add Normalized Contacts
        if (request.businessPhone() != null && !request.businessPhone().isBlank()) {
            studioRepository.addStudioContact(studioId, "PHONE", request.businessPhone().trim(), true, 1);
        }
        if (request.whatsappNumber() != null && !request.whatsappNumber().isBlank()) {
            studioRepository.addStudioContact(studioId, "WHATSAPP", request.whatsappNumber().trim(), true, 2);
        }
        if (request.businessEmail() != null && !request.businessEmail().isBlank()) {
            studioRepository.addStudioContact(studioId, "EMAIL", request.businessEmail().trim().toLowerCase(), true, 3);
        }
        if (request.websiteUrl() != null && !request.websiteUrl().isBlank()) {
            studioRepository.addStudioContact(studioId, "WEBSITE", request.websiteUrl().trim(), true, 4);
        }
        if (request.instagramUrl() != null && !request.instagramUrl().isBlank()) {
            studioRepository.addStudioContact(studioId, "INSTAGRAM", request.instagramUrl().trim(), true, 5);
        }

        // 4. Add Normalized Services
        if (request.services() != null) {
            Set<String> processedServices = new java.util.HashSet<>();
            for (String s : request.services()) {
                if (s != null && !s.isBlank()) {
                    String code = s.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
                    if (processedServices.add(code)) {
                        studioRepository.addStudioService(studioId, code, s.trim());
                    }
                }
            }
        }

        // 5. Add Normalized Canonical Specialties
        if (request.specialties() != null && !request.specialties().isEmpty()) {
            Set<String> processedCodes = new java.util.HashSet<>();
            for (String rawSpecialty : request.specialties()) {
                if (rawSpecialty != null && !rawSpecialty.isBlank()) {
                    var canonical = com.interior.platform.designers.domain.CanonicalSpecialty.from(rawSpecialty);
                    if (processedCodes.add(canonical.code())) {
                        studioRepository.addStudioSpecialty(studioId, canonical.code(), canonical.displayName());
                    }
                }
            }
        }

        // 6. Add Normalized Service Areas
        if (request.serviceAreas() != null) {
            Set<String> processedAreas = new java.util.HashSet<>();
            for (String area : request.serviceAreas()) {
                if (area != null && !area.isBlank()) {
                    String normalizedArea = area.trim();
                    if (processedAreas.add(normalizedArea.toLowerCase())) {
                        studioRepository.addStudioServiceArea(studioId, normalizedArea, null);
                    }
                }
            }
        }

        // 7. Create Studio Membership (role: OWNER)
        securityRepository.addStudioMember(UuidV7.randomUuid(), studioId, actor.userId(), "OWNER");

        // 8. Promote Role: Grant DESIGNER Role in identity_user_roles
        Set<String> existingRoles = securityRepository.getUserRoles(actor.userId());
        if (!existingRoles.contains("DESIGNER")) {
            securityRepository.assignUserRole(UuidV7.randomUuid(), actor.userId(), "DESIGNER", now);
        }

        // 9. Mark Onboarding Draft Completed and Record Initial Onboarding Completion
        studioRepository.markDraftCompleted(actor.userId());
        studioRepository.recordInitialOnboardingCompletion(actor.userId(), studioId);

        // 9. Record Audit Event
        auditService.record(
                actor.userId(),
                studioId,
                "PROFESSIONAL_ONBOARDING_COMPLETED",
                "STUDIO",
                studioId.toString(),
                Map.of("slug", normalizedSlug, "professionalType", request.professionalType()),
                getClientIp(httpRequest),
                getClientUserAgent(httpRequest)
        );

        // 10. Privilege Elevation Session Rotation
        String newCsrfToken = null;
        if (httpRequest != null) {
            SessionRecord session = (SessionRecord) httpRequest.getAttribute(SecurityInterceptor.SESSION_ATTRIBUTE);
            if (session != null && httpResponse != null) {
                var rotated = sessionSecurityService.rotateSession(session, httpResponse);
                newCsrfToken = rotated.rawCsrfToken();
            }
        }

        OnboardingStatusResponse.StudioSummary summary = new OnboardingStatusResponse.StudioSummary(
                studioId, studioName, normalizedSlug, request.professionalType(), "ACTIVE", "UNPUBLISHED", "OWNER"
        );

        return new CompletionResult(summary, newCsrfToken, "Professional onboarding completed successfully.");
    }

    private void validateAuthenticatedActor(ActorContext actor) {
        if (actor == null || !actor.isAuthenticated() || actor.userId() == null) {
            throw new UnauthorizedException("Authentication is required to perform professional onboarding");
        }
    }

    private void validateCompletionPayload(OnboardingCompletionRequest req) {
        if (req == null) {
            throw new BadRequestException("Request body is missing");
        }

        if (req.studioName() == null || req.studioName().trim().length() < 2 || req.studioName().trim().length() > 120) {
            throw new BadRequestException("Studio/Business name must be between 2 and 120 characters");
        }

        if (req.professionalType() == null || req.professionalType().isBlank()) {
            throw new BadRequestException("Professional type is required");
        }
        try {
            ProfessionalType.fromCode(req.professionalType());
        } catch (Exception e) {
            throw new BadRequestException("Invalid professional type: " + req.professionalType());
        }

        if (req.city() == null || req.city().isBlank()) {
            throw new BadRequestException("Primary city is required");
        }
        if (req.state() == null || req.state().isBlank()) {
            throw new BadRequestException("State is required");
        }

        if (req.services() == null || req.services().isEmpty()) {
            throw new BadRequestException("At least one service offering must be selected");
        }

        if (req.businessPhone() == null || req.businessPhone().isBlank()) {
            throw new BadRequestException("Business phone number is required");
        }
        String cleanPhone = req.businessPhone().replaceAll("[\\s-()]", "");
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new BadRequestException("Invalid phone number format: " + req.businessPhone());
        }

        if (req.businessEmail() == null || !EMAIL_PATTERN.matcher(req.businessEmail().trim()).matches()) {
            throw new BadRequestException("Valid business email is required");
        }

        if (req.websiteUrl() != null && !req.websiteUrl().isBlank()) {
            String trimmedUrl = req.websiteUrl().trim();
            if (!SAFE_URL_PATTERN.matcher(trimmedUrl).matches()) {
                throw new BadRequestException("Website URL must use http:// or https:// protocol");
            }
        }

        if (req.instagramUrl() != null && !req.instagramUrl().isBlank()) {
            String trimmedInsta = req.instagramUrl().trim();
            if (trimmedInsta.contains("javascript:") || trimmedInsta.contains("data:")) {
                throw new BadRequestException("Invalid Instagram link");
            }
        }

        if (req.gstRegistered() && req.gstNumber() != null && !req.gstNumber().isBlank()) {
            String normalizedGst = req.gstNumber().trim().toUpperCase();
            if (!GST_PATTERN.matcher(normalizedGst).matches()) {
                throw new BadRequestException("Invalid Indian GSTIN format");
            }
        }

        if (req.experienceSinceYear() != null) {
            int year = req.experienceSinceYear();
            int currentYear = java.time.Year.now().getValue();
            if (year < 1950 || year > currentYear) {
                throw new BadRequestException("Experience since year must be between 1950 and " + currentYear);
            }
        }

        if (!req.confirmedAccuracy()) {
            throw new BadRequestException("You must confirm that the information provided is accurate");
        }

        if (!req.confirmedContentOwnership()) {
            throw new BadRequestException("You must acknowledge that you own or have rights to publish your work");
        }
    }

    private String sanitizeText(String text) {
        if (text == null) return null;
        // Strip script and style blocks entirely
        String stripped = text.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", "");
        // Strip remaining HTML tags
        stripped = stripped.replaceAll("<[^>]*>", "").trim();
        return stripped.isBlank() ? null : stripped;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private String getClientUserAgent(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "unknown";
    }
}
