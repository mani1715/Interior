package com.interior.platform.security.mfa.web;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.mfa.MfaService;
import com.interior.platform.security.mfa.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/auth/mfa", "/api/v1/auth/mfa"})
@Tag(name = "Multi-Factor Authentication", description = "TOTP enrollment, challenges, recovery codes, and assurance elevation")
public class MfaController {

    private final MfaService mfaService;

    public MfaController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    private ActorContext getActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        if (actor == null || !actor.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        return actor;
    }

    private SessionRecord getSession(HttpServletRequest request) {
        return (SessionRecord) request.getAttribute(SecurityInterceptor.SESSION_ATTRIBUTE);
    }

    @GetMapping("/status")
    @Operation(summary = "Get MFA status", description = "Returns active MFA methods, recovery codes remaining, session elevation status, and role requirement.")
    public ResponseEntity<MfaStatusResponse> getStatus(HttpServletRequest request) {
        ActorContext actor = getActor(request);
        SessionRecord session = getSession(request);
        return ResponseEntity.ok(mfaService.getMfaStatus(actor, session));
    }

    @PostMapping("/totp/enroll")
    @Operation(summary = "Initiate TOTP enrollment", description = "Generates a pending TOTP secret and standard otpauth URI. Secret is returned once.")
    public ResponseEntity<MfaEnrollmentResponse> startEnrollment(HttpServletRequest request) {
        ActorContext actor = getActor(request);
        return ResponseEntity.ok(mfaService.startTotpEnrollment(actor));
    }

    @PostMapping("/totp/confirm")
    @Operation(summary = "Confirm TOTP enrollment", description = "Verifies 6-digit TOTP code, activates MFA, elevates session, and generates one-time backup recovery codes.")
    public ResponseEntity<MfaConfirmResponse> confirmEnrollment(
            @Valid @RequestBody MfaConfirmRequest req,
            HttpServletRequest request
    ) {
        ActorContext actor = getActor(request);
        SessionRecord session = getSession(request);
        return ResponseEntity.ok(mfaService.confirmTotpEnrollment(actor, session, req.code()));
    }

    @PostMapping("/challenge")
    @Operation(summary = "Verify TOTP challenge", description = "Verifies 6-digit code for login challenge or step-up authentication and elevates session.")
    public ResponseEntity<MfaStatusResponse> verifyChallenge(
            @Valid @RequestBody MfaChallengeRequest req,
            HttpServletRequest request
    ) {
        ActorContext actor = getActor(request);
        SessionRecord session = getSession(request);
        return ResponseEntity.ok(mfaService.verifyTotpChallenge(actor, session, req.code()));
    }

    @PostMapping("/recovery-code")
    @Operation(summary = "Redeem backup recovery code", description = "Consumes a single one-time recovery code to elevate session assurance.")
    public ResponseEntity<MfaStatusResponse> redeemRecoveryCode(
            @Valid @RequestBody MfaRecoveryCodeRequest req,
            HttpServletRequest request
    ) {
        ActorContext actor = getActor(request);
        SessionRecord session = getSession(request);
        return ResponseEntity.ok(mfaService.verifyRecoveryCode(actor, session, req.recoveryCode()));
    }

    @PostMapping("/recovery-codes/regenerate")
    @Operation(summary = "Regenerate backup recovery codes", description = "Requires fresh step-up authentication. Invalidates old codes and returns 10 new codes.")
    public ResponseEntity<List<String>> regenerateRecoveryCodes(HttpServletRequest request) {
        ActorContext actor = getActor(request);
        SessionRecord session = getSession(request);
        return ResponseEntity.ok(mfaService.regenerateRecoveryCodes(actor, session));
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable MFA", description = "Requires fresh step-up authentication. Disables MFA and revokes all other sessions.")
    public ResponseEntity<Map<String, Boolean>> disableMfa(HttpServletRequest request) {
        ActorContext actor = getActor(request);
        SessionRecord session = getSession(request);
        mfaService.disableMfa(actor, session);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
