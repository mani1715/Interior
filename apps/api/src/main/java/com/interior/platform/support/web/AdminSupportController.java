package com.interior.platform.support.web;

import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.support.domain.SupportCategory;
import com.interior.platform.support.domain.SupportPriority;
import com.interior.platform.support.domain.SupportStatus;
import com.interior.platform.support.dto.*;
import com.interior.platform.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/support/requests")
@Tag(name = "Admin Support Operations", description = "Endpoints for support agents and administrators to triage tickets")
public class AdminSupportController {

    private final SupportService supportService;

    public AdminSupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    private ActorContext extractActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return actor != null ? actor : ActorContext.anonymous();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    @GetMapping
    @Operation(summary = "List all platform support requests", description = "Allows administrators and moderators to filter support queue")
    public ResponseEntity<List<SupportRequestSummaryResponse>> listRequests(
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "status", required = false) SupportStatus status,
            @RequestParam(name = "category", required = false) SupportCategory category,
            @RequestParam(name = "priority", required = false) SupportPriority priority,
            HttpServletRequest request
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(supportService.listAllRequests(actor, limit, offset, status, category, priority));
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get support ticket detail with internal notes", description = "Retrieves ticket and all internal and public messages")
    public ResponseEntity<SupportRequestDetailResponse> getRequest(
            @PathVariable UUID requestId,
            HttpServletRequest request
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(supportService.getRequestDetail(actor, requestId));
    }

    @PostMapping("/{requestId}/messages")
    @Operation(summary = "Add agent reply or internal note", description = "Adds a public reply or internal team-only note to a support ticket")
    public ResponseEntity<SupportMessageResponse> addMessage(
            @PathVariable UUID requestId,
            @Valid @RequestBody CreateSupportMessageRequest req,
            HttpServletRequest request
    ) {
        ActorContext actor = extractActor(request);
        SupportMessageResponse response = supportService.addMessage(actor, requestId, req, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{requestId}/status")
    @Operation(summary = "Update support ticket status and priority", description = "Updates status or priority of a support request")
    public ResponseEntity<Map<String, Boolean>> updateStatus(
            @PathVariable UUID requestId,
            @Valid @RequestBody UpdateSupportStatusRequest req,
            HttpServletRequest request
    ) {
        ActorContext actor = extractActor(request);
        supportService.updateRequestStatus(actor, requestId, req, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(Map.of("success", true));
    }
}
