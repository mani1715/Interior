package com.interior.platform.security.mfa;

import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.email.service.CommunicationDeliveryService;
import com.interior.platform.email.service.EmailTemplateService;
import com.interior.platform.security.config.AuthSecurityProperties;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.mfa.dto.*;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class MfaService {

    private final SecurityRepository securityRepository;
    private final TotpService totpService;
    private final RecoveryCodeService recoveryCodeService;
    private final AuthSecurityProperties properties;
    private final AuditService auditService;
    private final AuthorizationService authorizationService;
    private final CommunicationDeliveryService communicationDeliveryService;
    private final EmailTemplateService emailTemplateService;

    @Autowired
    public MfaService(
            SecurityRepository securityRepository,
            TotpService totpService,
            RecoveryCodeService recoveryCodeService,
            AuthSecurityProperties properties,
            AuditService auditService,
            AuthorizationService authorizationService,
            @Autowired(required = false) CommunicationDeliveryService communicationDeliveryService,
            @Autowired(required = false) EmailTemplateService emailTemplateService
    ) {
        this.securityRepository = securityRepository;
        this.totpService = totpService;
        this.recoveryCodeService = recoveryCodeService;
        this.properties = properties;
        this.auditService = auditService;
        this.authorizationService = authorizationService;
        this.communicationDeliveryService = communicationDeliveryService;
        this.emailTemplateService = emailTemplateService;
    }

    public boolean isMfaRequiredForRole(ActorContext actor) {
        if (actor == null || !actor.isAuthenticated()) {
            return false;
        }
        if (actor.hasRole("SUPER_ADMIN") || actor.hasRole("ADMIN")) {
            return true;
        }
        if (actor.hasRole("MODERATOR") && properties.getMfa().isModeratorMfaRequired()) {
            return true;
        }
        return false;
    }

    public MfaStatusResponse getMfaStatus(ActorContext actor, SessionRecord session) {
        authorizationService.requireAuthenticated(actor);
        authorizationService.requireActiveUser(actor);

        Optional<MfaMethodRecord> methodOpt = securityRepository.findMfaMethodByUserId(actor.userId(), "TOTP");
        boolean mfaEnabled = methodOpt.isPresent() && methodOpt.get().isActive();
        int recoveryCodesCount = mfaEnabled ? securityRepository.countUnusedRecoveryCodes(actor.userId()) : 0;

        boolean sessionVerified = session != null && session.isMfaVerified();
        Instant stepUpValidUntil = null;
        if (session != null && session.mfaVerifiedAt() != null) {
            stepUpValidUntil = session.mfaVerifiedAt().plusSeconds(properties.getMfa().getStepUpFreshnessSeconds());
        }

        return new MfaStatusResponse(
                mfaEnabled,
                mfaEnabled ? "TOTP" : null,
                mfaEnabled ? methodOpt.get().confirmedAt() : null,
                recoveryCodesCount,
                sessionVerified,
                stepUpValidUntil,
                isMfaRequiredForRole(actor)
        );
    }

    public MfaEnrollmentResponse startTotpEnrollment(ActorContext actor) {
        authorizationService.requireAuthenticated(actor);
        authorizationService.requireActiveUser(actor);

        byte[] rawSecret = totpService.generateSecret();
        String base32Secret = Base32.encode(rawSecret);
        int keyVersion = properties.getMfa().getCurrentKeyVersion();
        String encryptedSecret = totpService.encryptSecret(rawSecret, keyVersion);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getMfa().getPendingEnrollmentTtlSeconds());

        MfaMethodRecord record = new MfaMethodRecord(
                com.interior.platform.common.util.UuidV7.randomUuid(),
                actor.userId(),
                "TOTP",
                encryptedSecret,
                keyVersion,
                "PENDING",
                expiresAt,
                null,
                null,
                0,
                null,
                now,
                now
        );

        securityRepository.saveMfaMethod(record);

        auditService.record(
                actor.userId(),
                actor.activeStudioId(),
                "MFA_ENROLLMENT_STARTED",
                "USER",
                actor.userId().toString(),
                Map.of("methodType", "TOTP"),
                null,
                null
        );

        String otpauthUri = totpService.buildOtpAuthUri(actor.email(), base32Secret);
        return new MfaEnrollmentResponse(base32Secret, otpauthUri, expiresAt);
    }

    @Transactional
    public MfaConfirmResponse confirmTotpEnrollment(ActorContext actor, SessionRecord session, String code) {
        authorizationService.requireAuthenticated(actor);
        authorizationService.requireActiveUser(actor);

        MfaMethodRecord method = securityRepository.findMfaMethodByUserId(actor.userId(), "TOTP")
                .orElseThrow(() -> new BadRequestException("No pending MFA enrollment found. Please start setup again."));

        Instant now = Instant.now();
        if (method.isPendingExpired(now)) {
            throw new BadRequestException("MFA setup has expired. Please initiate setup again.");
        }

        byte[] secret = totpService.decryptSecret(method.encryptedSecret());
        TotpService.TotpVerificationResult result = totpService.verifyCode(secret, code, now, method.lastUsedCounter());

        if (!result.isValid()) {
            throw new BadRequestException("Invalid verification code. Please check your authenticator application.");
        }

        securityRepository.confirmMfaMethod(actor.userId(), "TOTP", now);
        securityRepository.resetFailedMfaAttempts(actor.userId(), "TOTP", result.matchedCounter());

        // Generate one-time recovery codes
        RecoveryCodeService.GeneratedRecoveryCodes batch = recoveryCodeService.generateBatch();
        securityRepository.saveRecoveryCodes(actor.userId(), batch.hashedCodes());

        // Elevate current session
        if (session != null) {
            securityRepository.updateSessionMfaVerified(session.id(), now, "MFA_VERIFIED");
            // Terminate other non-MFA sessions
            securityRepository.revokeAllOtherUserSessions(actor.userId(), session.id(), now);
        }

        auditService.record(
                actor.userId(),
                actor.activeStudioId(),
                "MFA_ENABLED",
                "USER",
                actor.userId().toString(),
                Map.of("methodType", "TOTP"),
                null,
                null
        );

        sendSecurityEmail(actor.userId(), "Two-Factor Authentication Enabled",
                "Authenticator app (TOTP) multi-factor authentication was successfully enabled on your account.", "/account");

        return new MfaConfirmResponse(true, batch.rawCodes(), true);
    }

    public MfaStatusResponse verifyTotpChallenge(ActorContext actor, SessionRecord session, String code) {
        authorizationService.requireAuthenticated(actor);
        authorizationService.requireActiveUser(actor);

        MfaMethodRecord method = securityRepository.findMfaMethodByUserId(actor.userId(), "TOTP")
                .orElseThrow(() -> new BadRequestException("MFA is not enabled on this account."));

        if (!method.isActive()) {
            throw new BadRequestException("MFA is not active on this account.");
        }

        Instant now = Instant.now();
        if (method.isLocked(now)) {
            long remainingMinutes = Math.max(1, Duration.between(now, method.lockedUntil()).toMinutes());
            throw new MfaLockedException("MFA verification is temporarily locked. Please try again in " + remainingMinutes + " minutes.");
        }

        byte[] secret = totpService.decryptSecret(method.encryptedSecret());
        TotpService.TotpVerificationResult result = totpService.verifyCode(secret, code, now, method.lastUsedCounter());

        if (!result.isValid()) {
            int newFailed = method.failedAttempts() + 1;
            Instant lockedUntil = null;
            if (newFailed >= properties.getMfa().getMaxFailedAttempts()) {
                lockedUntil = now.plusSeconds(properties.getMfa().getLockoutDurationSeconds());
            }
            securityRepository.recordFailedMfaAttempt(actor.userId(), "TOTP", newFailed, lockedUntil);

            auditService.record(
                    actor.userId(),
                    actor.activeStudioId(),
                    "MFA_CHALLENGE_FAILED",
                    "USER",
                    actor.userId().toString(),
                    Map.of("failedAttempts", newFailed, "locked", lockedUntil != null),
                    null,
                    null
            );

            if (lockedUntil != null) {
                throw new MfaLockedException("Too many failed verification attempts. Verification is locked for 15 minutes.");
            }
            throw new BadRequestException("Invalid verification code. Please check your authenticator application.");
        }

        securityRepository.resetFailedMfaAttempts(actor.userId(), "TOTP", result.matchedCounter());

        if (session != null) {
            securityRepository.updateSessionMfaVerified(session.id(), now, "MFA_VERIFIED");
        }

        auditService.record(
                actor.userId(),
                actor.activeStudioId(),
                "MFA_CHALLENGE_VERIFIED",
                "USER",
                actor.userId().toString(),
                Map.of("counter", result.matchedCounter()),
                null,
                null
        );

        return getMfaStatus(actor, session);
    }

    @Transactional
    public MfaStatusResponse verifyRecoveryCode(ActorContext actor, SessionRecord session, String rawCode) {
        authorizationService.requireAuthenticated(actor);
        authorizationService.requireActiveUser(actor);

        MfaMethodRecord method = securityRepository.findMfaMethodByUserId(actor.userId(), "TOTP")
                .orElseThrow(() -> new BadRequestException("MFA is not enabled on this account."));

        if (!method.isActive()) {
            throw new BadRequestException("MFA is not active on this account.");
        }

        Instant now = Instant.now();
        if (method.isLocked(now)) {
            throw new MfaLockedException("Verification is temporarily locked due to too many failed attempts.");
        }

        String hash = recoveryCodeService.hashCode(rawCode);
        boolean redeemed = securityRepository.redeemRecoveryCode(actor.userId(), hash, now);

        if (!redeemed) {
            int newFailed = method.failedAttempts() + 1;
            Instant lockedUntil = null;
            if (newFailed >= properties.getMfa().getMaxFailedAttempts()) {
                lockedUntil = now.plusSeconds(properties.getMfa().getLockoutDurationSeconds());
            }
            securityRepository.recordFailedMfaAttempt(actor.userId(), "TOTP", newFailed, lockedUntil);

            auditService.record(
                    actor.userId(),
                    actor.activeStudioId(),
                    "RECOVERY_CODE_FAILED",
                    "USER",
                    actor.userId().toString(),
                    Map.of("failedAttempts", newFailed),
                    null,
                    null
            );

            if (lockedUntil != null) {
                throw new MfaLockedException("Too many failed attempts. Verification is locked for 15 minutes.");
            }
            throw new BadRequestException("Invalid or already used recovery code.");
        }

        // Recovery code valid
        securityRepository.resetFailedMfaAttempts(actor.userId(), "TOTP", method.lastUsedCounter() != null ? method.lastUsedCounter() : 0);

        if (session != null) {
            securityRepository.updateSessionMfaVerified(session.id(), now, "MFA_VERIFIED");
        }

        auditService.record(
                actor.userId(),
                actor.activeStudioId(),
                "RECOVERY_CODE_USED",
                "USER",
                actor.userId().toString(),
                Map.of(),
                null,
                null
        );

        sendSecurityEmail(actor.userId(), "Backup Recovery Code Used",
                "A one-time backup recovery code was used to access your account. If you did not perform this, review your sessions immediately.", "/account");

        return getMfaStatus(actor, session);
    }

    @Transactional
    public List<String> regenerateRecoveryCodes(ActorContext actor, SessionRecord session) {
        requireFreshStepUp(actor, session);

        MfaMethodRecord method = securityRepository.findMfaMethodByUserId(actor.userId(), "TOTP")
                .orElseThrow(() -> new BadRequestException("MFA is not active on this account."));

        if (!method.isActive()) {
            throw new BadRequestException("MFA is not active on this account.");
        }

        RecoveryCodeService.GeneratedRecoveryCodes batch = recoveryCodeService.generateBatch();
        securityRepository.saveRecoveryCodes(actor.userId(), batch.hashedCodes());

        auditService.record(
                actor.userId(),
                actor.activeStudioId(),
                "RECOVERY_CODES_REGENERATED",
                "USER",
                actor.userId().toString(),
                Map.of("count", batch.rawCodes().size()),
                null,
                null
        );

        sendSecurityEmail(actor.userId(), "Recovery Codes Regenerated",
                "New backup recovery codes have been generated for your account. Old unused recovery codes have been invalidated.", "/account");

        return batch.rawCodes();
    }

    @Transactional
    public void disableMfa(ActorContext actor, SessionRecord session) {
        requireFreshStepUp(actor, session);

        securityRepository.disableMfaMethod(actor.userId(), "TOTP");
        securityRepository.deleteRecoveryCodes(actor.userId());

        // Revoke all other active sessions so older sessions cannot remain
        if (session != null) {
            securityRepository.revokeAllOtherUserSessions(actor.userId(), session.id(), Instant.now());
            // Demote current session assurance from MFA_VERIFIED to PRIMARY_AUTHENTICATED
            securityRepository.updateSessionMfaVerified(session.id(), null, "PASSWORD");
        }

        auditService.record(
                actor.userId(),
                actor.activeStudioId(),
                "MFA_DISABLED",
                "USER",
                actor.userId().toString(),
                Map.of(),
                null,
                null
        );

        sendSecurityEmail(actor.userId(), "Multi-Factor Authentication Disabled",
                "Multi-factor authentication was disabled on your account. Privileged administrator tools will require re-enrollment.", "/account");
    }

    @Transactional
    public void adminResetMfa(ActorContext actor, UUID targetUserId, String reason) {
        authorizationService.requireSuperAdmin(actor);

        if (reason == null || reason.trim().length() < 10) {
            throw new BadRequestException("A valid administrative reason (minimum 10 characters) is required.");
        }

        // Self-bypass prevention: super admin cannot reset their own MFA via this administrative endpoint
        if (actor.userId().equals(targetUserId)) {
            throw new BadRequestException("Super Administrators cannot use the administrative reset endpoint on their own account. Use your authenticator app or one-time backup recovery codes.");
        }

        UserRecord targetUser = securityRepository.findUserById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        securityRepository.disableMfaMethod(targetUserId, "TOTP");
        securityRepository.deleteRecoveryCodes(targetUserId);

        // Revoke ALL active sessions for the target user immediately
        securityRepository.revokeAllUserSessions(targetUserId, Instant.now());

        auditService.record(
                actor.userId(),
                actor.activeStudioId(),
                "MFA_RESET_BY_ADMIN",
                "USER",
                targetUserId.toString(),
                Map.of("reason", reason.trim(), "targetEmail", targetUser.email()),
                null,
                null
        );

        sendSecurityEmail(targetUserId, "Administrator Reset Your Multi-Factor Authentication",
                "A platform Super Administrator has reset the multi-factor authentication configuration on your account. All active sessions have been terminated. Reason: " + reason.trim(), "/help");
    }

    public void requireMfaVerifiedForAdmin(ActorContext actor, SessionRecord session) {
        if (!isMfaRequiredForRole(actor)) {
            return;
        }

        Optional<MfaMethodRecord> methodOpt = securityRepository.findMfaMethodByUserId(actor.userId(), "TOTP");
        if (methodOpt.isEmpty() || !methodOpt.get().isActive()) {
            throw new MfaEnrollmentRequiredException("Administrator accounts must enroll in multi-factor authentication before accessing control-plane tools.");
        }

        boolean isSessionVerified = session != null && session.isMfaVerified();
        boolean isActorVerified = actor.hasMfaAssurance();

        if (!isSessionVerified && !isActorVerified) {
            throw new MfaRequiredException("Multi-factor authentication verification required to access platform administration.");
        }
    }

    public void requireFreshStepUp(ActorContext actor, SessionRecord session) {
        authorizationService.requireAuthenticated(actor);
        authorizationService.requireActiveUser(actor);

        if (session == null || session.mfaVerifiedAt() == null) {
            throw new MfaStepUpRequiredException("Fresh multi-factor authentication verification is required for this action.");
        }

        Instant threshold = Instant.now().minusSeconds(properties.getMfa().getStepUpFreshnessSeconds());
        if (session.mfaVerifiedAt().isBefore(threshold)) {
            throw new MfaStepUpRequiredException("Your multi-factor authentication verification has expired. Fresh verification is required.");
        }
    }

    private void sendSecurityEmail(UUID userId, String eventTitle, String eventDetails, String relativeUrl) {
        if (communicationDeliveryService == null || emailTemplateService == null || userId == null) {
            return;
        }
        try {
            securityRepository.findUserById(userId).ifPresent(user -> {
                if (user.email() != null && !user.email().isBlank()) {
                    EmailTemplateService.RenderedEmail rendered = emailTemplateService.renderSecurityAlert(
                            user.displayName(), eventTitle, eventDetails, relativeUrl
                    );
                    communicationDeliveryService.attemptEmailDelivery(
                            null,
                            userId,
                            "SECURITY_NOTICE",
                            user.email(),
                            rendered.subject(),
                            rendered.textBody(),
                            rendered.htmlBody(),
                            "sec_notice:" + userId + ":" + Instant.now().toEpochMilli()
                    );
                }
            });
        } catch (Exception ignored) {}
    }
}
