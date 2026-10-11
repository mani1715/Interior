package com.interior.platform.operations.web;

import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.operations.domain.OperationalJobRunRecord;
import com.interior.platform.operations.domain.PlatformOperationalStateRecord;
import com.interior.platform.operations.dto.ProviderMatrixResponse;
import com.interior.platform.operations.dto.UpdateOperationalStateRequest;
import com.interior.platform.operations.service.OperationalJobCoordinator;
import com.interior.platform.operations.service.PlatformOperationsService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/operations")
@Tag(name = "Platform Operations & Incident Controls", description = "Endpoints for platform state, provider health, and operational jobs")
public class AdminOperationsController {

    private final PlatformOperationsService operationsService;
    private final OperationalJobCoordinator jobCoordinator;

    public AdminOperationsController(PlatformOperationsService operationsService, OperationalJobCoordinator jobCoordinator) {
        this.operationsService = operationsService;
        this.jobCoordinator = jobCoordinator;
    }

    private ActorContext extractActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return actor != null ? actor : ActorContext.anonymous();
    }

    private SessionRecord extractSession(HttpServletRequest request) {
        return (SessionRecord) request.getAttribute(SecurityInterceptor.SESSION_ATTRIBUTE);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    @GetMapping("/state")
    @Operation(summary = "Get platform operational state and kill switches")
    public ResponseEntity<PlatformOperationalStateRecord> getState() {
        return ResponseEntity.ok(operationsService.getOperationalState());
    }

    @PutMapping("/state")
    @Operation(summary = "Update maintenance mode and feature kill switches", description = "Requires SUPER_ADMIN role with fresh step-up MFA")
    public ResponseEntity<PlatformOperationalStateRecord> updateState(
            @Valid @RequestBody UpdateOperationalStateRequest req,
            HttpServletRequest request
    ) {
        ActorContext actor = extractActor(request);
        SessionRecord session = extractSession(request);
        PlatformOperationalStateRecord state = operationsService.updateOperationalState(
                actor,
                session,
                req,
                getClientIp(request),
                request.getHeader("User-Agent")
        );
        return ResponseEntity.ok(state);
    }

    @GetMapping("/providers")
    @Operation(summary = "Get infrastructure provider health matrix")
    public ResponseEntity<ProviderMatrixResponse> getProviders(HttpServletRequest request) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(operationsService.getProviderMatrix(actor));
    }

    @GetMapping("/jobs")
    @Operation(summary = "List recent scheduled job runs")
    public ResponseEntity<List<OperationalJobRunRecord>> listJobs(
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "jobKey", required = false) String jobKey,
            HttpServletRequest request
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(operationsService.listJobRuns(actor, limit, offset, jobKey));
    }

    @PostMapping("/jobs/{jobKey}/run")
    @Operation(summary = "Manually trigger an operational job run")
    public ResponseEntity<OperationalJobRunRecord> runJob(
            @PathVariable String jobKey,
            HttpServletRequest request
    ) {
        ActorContext actor = extractActor(request);
        if (!actor.hasRole("ADMIN") && !actor.hasRole("SUPER_ADMIN")) {
            throw new com.interior.platform.common.exception.UnauthorizedException("Admin privilege required to trigger jobs.");
        }

        OperationalJobRunRecord result;
        switch (jobKey.toUpperCase()) {
            case "SESSION_CLEANUP" -> result = jobCoordinator.runSessionCleanup();
            case "UPLOAD_EXPIRY" -> result = jobCoordinator.runUploadExpiry();
            case "AI_STUCK_RECOVERY" -> result = jobCoordinator.runAiStuckRecovery();
            case "PRIVACY_CLEANUP" -> result = jobCoordinator.runPrivacyCleanup();
            case "STORAGE_RECONCILIATION" -> result = jobCoordinator.runStorageReconciliation();
            default -> throw new BadRequestException("Unknown operational job key: " + jobKey);
        }

        return ResponseEntity.ok(result);
    }
}
