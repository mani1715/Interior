package com.interior.platform.verification.web;

import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.verification.dto.AdminVerificationDecisionRequest;
import com.interior.platform.verification.dto.StudioVerificationDto;
import com.interior.platform.verification.service.StudioVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/verification")
@Tag(name = "Admin Verification", description = "Platform administrator endpoints for reviewing and deciding on studio business verification requests")
public class AdminVerificationController {

    private final StudioVerificationService verificationService;

    public AdminVerificationController(StudioVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/{studioId}/decision")
    @Operation(summary = "Record verification decision", description = "Approve, request more info, reject, or require reverification for a studio. Restricted strictly to platform admins.")
    public ResponseEntity<StudioVerificationDto> recordDecision(
            HttpServletRequest request,
            @PathVariable("studioId") UUID studioId,
            @Valid @RequestBody AdminVerificationDecisionRequest req
    ) {
        ActorContext actor = extractActor(request);
        StudioVerificationDto result = verificationService.adminDecision(actor, studioId, req);
        return ResponseEntity.ok(result);
    }

    private ActorContext extractActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return actor != null ? actor : ActorContext.anonymous();
    }
}
