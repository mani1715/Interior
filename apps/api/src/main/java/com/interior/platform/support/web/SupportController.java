package com.interior.platform.support.web;

import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
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
@RequestMapping("/support/requests")
@Tag(name = "Customer & Designer Support", description = "Endpoints for submitting and viewing support requests")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
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

    @PostMapping
    @Operation(summary = "Submit a new support request", description = "Creates a support request with initial message")
    public ResponseEntity<SupportRequestDetailResponse> createRequest(
            @Valid @RequestBody CreateSupportRequest req,
            HttpServletRequest request
    ) {
        ActorContext actor = extractActor(request);
        SupportRequestDetailResponse response = supportService.createRequest(actor, req, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List user support requests", description = "Returns support requests for the current user or studio")
    public ResponseEntity<List<SupportRequestSummaryResponse>> listRequests(
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "studioId", required = false) UUID studioId,
            HttpServletRequest request
    ) {
        ActorContext actor = extractActor(request);
        if (studioId != null) {
            return ResponseEntity.ok(supportService.listStudioRequests(actor, studioId, limit, offset));
        }
        return ResponseEntity.ok(supportService.listUserRequests(actor, limit, offset));
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get support request details", description = "Retrieves request information and message history")
    public ResponseEntity<SupportRequestDetailResponse> getRequest(
            @PathVariable UUID requestId,
            HttpServletRequest request
    ) {
        ActorContext actor = extractActor(request);
        return ResponseEntity.ok(supportService.getRequestDetail(actor, requestId));
    }

    @PostMapping("/{requestId}/messages")
    @Operation(summary = "Reply to a support request", description = "Adds a message to an existing support request")
    public ResponseEntity<SupportMessageResponse> addMessage(
            @PathVariable UUID requestId,
            @Valid @RequestBody CreateSupportMessageRequest req,
            HttpServletRequest request
    ) {
        ActorContext actor = extractActor(request);
        SupportMessageResponse response = supportService.addMessage(actor, requestId, req, getClientIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{requestId}/close")
    @Operation(summary = "Close a support request", description = "Allows user to mark their support request as closed")
    public ResponseEntity<Map<String, Boolean>> closeRequest(
            @PathVariable UUID requestId,
            HttpServletRequest request
    ) {
        ActorContext actor = extractActor(request);
        supportService.updateRequestStatus(
                actor,
                requestId,
                new UpdateSupportStatusRequest(com.interior.platform.support.domain.SupportStatus.CLOSED, null),
                getClientIp(request),
                request.getHeader("User-Agent")
        );
        return ResponseEntity.ok(Map.of("closed", true));
    }
}
