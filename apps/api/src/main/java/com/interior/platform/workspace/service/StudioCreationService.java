package com.interior.platform.workspace.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.ProfessionalType;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.designers.service.SlugValidationService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import com.interior.platform.workspace.dto.CreateStudioRequest;
import com.interior.platform.workspace.dto.CreateStudioResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StudioCreationService {

    private final SecurityRepository securityRepository;
    private final StudioRepository studioRepository;
    private final SlugValidationService slugValidationService;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public StudioCreationService(
            SecurityRepository securityRepository,
            StudioRepository studioRepository,
            SlugValidationService slugValidationService,
            AuthorizationService authorizationService,
            AuditService auditService
    ) {
        this.securityRepository = securityRepository;
        this.studioRepository = studioRepository;
        this.slugValidationService = slugValidationService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional
    public CreateStudioResponse createStudio(
            ActorContext actor,
            CreateStudioRequest request,
            HttpServletRequest httpRequest
    ) {
        authorizationService.requireAuthenticated(actor);

        UserRecord user = securityRepository.findUserById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));

        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new AccessDeniedException("Account is suspended or inactive");
        }

        validateRequest(request);

        String cleanName = sanitizeText(request.name());
        String cleanCity = sanitizeText(request.city());
        String cleanState = sanitizeText(request.state());

        ProfessionalType profType = ProfessionalType.fromCode(request.professionalType());

        // Resolve and claim slug safely
        String resolvedSlug;
        if (request.slug() != null && !request.slug().isBlank()) {
            var slugCheck = slugValidationService.checkAvailability(request.slug());
            if (!slugCheck.available()) {
                throw new ConflictException(slugCheck.reason());
            }
            resolvedSlug = slugCheck.slug();
        } else {
            String baseSlug = slugValidationService.generateSlug(cleanName);
            var slugCheck = slugValidationService.checkAvailability(baseSlug);
            if (slugCheck.available()) {
                resolvedSlug = slugCheck.slug();
            } else if (slugCheck.suggestedSlug() != null && !slugCheck.suggestedSlug().isBlank()) {
                resolvedSlug = slugCheck.suggestedSlug();
            } else {
                resolvedSlug = baseSlug + "-" + UUID.randomUUID().toString().substring(0, 4);
            }
        }

        UUID studioId = UuidV7.randomUuid();
        Instant now = Instant.now();

        StudioDetailRecord studio = new StudioDetailRecord(
                studioId,
                cleanName,
                resolvedSlug,
                actor.userId(),
                "ACTIVE",
                profType.name(),
                cleanName,
                "Bespoke Interior & Architectural Design",
                null,
                null,
                null,
                null,
                cleanCity,
                cleanCity,
                cleanState,
                null,
                "IN",
                true,
                false,
                null,
                "UNPUBLISHED",
                now,
                now,
                now,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        // 1. Create studio entity (status: ACTIVE, publication_status: UNPUBLISHED)
        studioRepository.createStudio(studio);

        // 2. Claim slug in registry
        studioRepository.claimSlug(studioId, resolvedSlug, "CURRENT");

        // 3. Grant creator DESIGNER_ADMIN studio membership
        securityRepository.addStudioMember(UuidV7.randomUuid(), studioId, actor.userId(), "DESIGNER_ADMIN");

        // 4. Record audit event
        auditService.record(
                actor.userId(),
                studioId,
                "STUDIO_CREATED",
                "STUDIO",
                studioId.toString(),
                Map.of("slug", resolvedSlug, "professionalType", profType.name()),
                getClientIp(httpRequest),
                getClientUserAgent(httpRequest)
        );

        return new CreateStudioResponse(
                studioId,
                cleanName,
                resolvedSlug,
                profType.name(),
                "DESIGNER_ADMIN",
                "Studio created successfully."
        );
    }

    private void validateRequest(CreateStudioRequest req) {
        if (req == null) {
            throw new BadRequestException("Request payload is required");
        }
        if (req.name() == null || req.name().trim().length() < 2 || req.name().trim().length() > 100) {
            throw new BadRequestException("Studio name must be between 2 and 100 characters");
        }
        if (req.professionalType() == null || req.professionalType().isBlank()) {
            throw new BadRequestException("Professional type is required");
        }
        if (req.city() == null || req.city().trim().length() < 2 || req.city().trim().length() > 50) {
            throw new BadRequestException("City must be between 2 and 50 characters");
        }
        if (req.state() == null || req.state().trim().length() < 2 || req.state().trim().length() > 50) {
            throw new BadRequestException("State must be between 2 and 50 characters");
        }
    }

    private String sanitizeText(String input) {
        if (input == null) return null;
        return input.trim().replaceAll("[\\r\\n\\t]+", " ");
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getClientUserAgent(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "unknown";
    }
}
