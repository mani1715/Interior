package com.interior.platform.designers.web;

import com.interior.platform.designers.dto.OnboardingCompletionRequest;
import com.interior.platform.designers.dto.OnboardingDraftDto;
import com.interior.platform.designers.dto.OnboardingStatusResponse;
import com.interior.platform.designers.dto.SlugCheckResponse;
import com.interior.platform.designers.service.ProfessionalOnboardingService;
import com.interior.platform.designers.service.SlugValidationService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/professional-onboarding")
@Tag(name = "Designer Onboarding", description = "Endpoints for professional studio creation, multi-step draft persistence, slug validation, and role promotion")
public class ProfessionalOnboardingController {

    private final ProfessionalOnboardingService onboardingService;
    private final SlugValidationService slugValidationService;

    public ProfessionalOnboardingController(
            ProfessionalOnboardingService onboardingService,
            SlugValidationService slugValidationService
    ) {
        this.onboardingService = onboardingService;
        this.slugValidationService = slugValidationService;
    }

    @GetMapping
    @Operation(summary = "Get professional onboarding status", description = "Retrieves authoritative onboarding status, active draft, or completed studio summary.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Onboarding status returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<OnboardingStatusResponse> getStatus(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return ResponseEntity.ok(onboardingService.getStatus(actor));
    }

    @PutMapping
    @Operation(summary = "Save onboarding draft", description = "Persists current step and draft payload. Requires X-CSRF-Token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Draft saved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or exceeds size limit"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "CSRF token missing or invalid")
    })
    public ResponseEntity<Map<String, Object>> saveDraft(
            @RequestBody OnboardingDraftDto dto,
            HttpServletRequest request
    ) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        onboardingService.saveDraft(actor, dto);
        return ResponseEntity.ok(Map.of("success", true, "message", "Draft saved successfully"));
    }

    @PostMapping("/complete")
    @Operation(summary = "Atomically complete onboarding", description = "Validates data, establishes studio, owner membership, promotes role to DESIGNER, and rotates session.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Onboarding completed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Account inactive or CSRF invalid"),
            @ApiResponse(responseCode = "409", description = "Slug collision or reserved keyword")
    })
    public ResponseEntity<Map<String, Object>> completeOnboarding(
            @RequestBody OnboardingCompletionRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        var result = onboardingService.completeOnboarding(actor, requestBody, request, response);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", result.message(),
                "studio", result.studio(),
                "newCsrfToken", result.newCsrfToken() != null ? result.newCsrfToken() : ""
        ));
    }

    @GetMapping("/check-slug")
    @Operation(summary = "Check slug availability", description = "Validates syntax, reserved keywords, and database uniqueness.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Slug availability result")
    })
    public ResponseEntity<SlugCheckResponse> checkSlug(@RequestParam("slug") String slug) {
        return ResponseEntity.ok(slugValidationService.checkAvailability(slug));
    }
}
