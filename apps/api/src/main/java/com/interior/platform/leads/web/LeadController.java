package com.interior.platform.leads.web;

import com.interior.platform.leads.domain.LeadStatus;
import com.interior.platform.leads.dto.*;
import com.interior.platform.leads.service.LeadService;
import com.interior.platform.leads.service.WhatsAppService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/leads")
@Tag(name = "Workspace Leads CRM", description = "Endpoints for professional studio lead management, pipeline updates, internal notes, and WhatsApp communication")
public class LeadController {

    private final LeadService leadService;
    private final WhatsAppService whatsAppService;

    public LeadController(LeadService leadService, WhatsAppService whatsAppService) {
        this.leadService = leadService;
        this.whatsAppService = whatsAppService;
    }

    @GetMapping
    @Operation(summary = "List studio leads", description = "Paginated list of leads for the authenticated professional studio with status filtering, search, and sorting.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leads retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<LeadListResponse> listLeads(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @RequestParam(value = "status", required = false) String statusStr,
            @RequestParam(value = "assignedUserId", required = false) UUID assignedUserId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false, defaultValue = "createdAt:desc") String sort,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        LeadStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !statusStr.equalsIgnoreCase("ALL")) {
            try {
                status = LeadStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // If unknown status, return empty list or let service handle
            }
        }

        int clampedLimit = Math.max(1, Math.min(limit, 100));
        int clampedOffset = Math.max(0, offset);

        List<LeadSummaryDto> items = leadService.listLeads(actor, requestedStudioId, status, assignedUserId, search, sort, clampedLimit, clampedOffset);
        long total = leadService.countLeads(actor, requestedStudioId, status, assignedUserId, search);
        boolean hasMore = (clampedOffset + items.size()) < total;

        LeadListResponse response = new LeadListResponse(items, total, clampedLimit, clampedOffset, hasMore);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    @GetMapping("/counts")
    @Operation(summary = "Get lead status counts", description = "Aggregated count badges across pipeline statuses for the studio.")
    public ResponseEntity<LeadCountsDto> getCounts(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        LeadCountsDto counts = leadService.getCounts(actor, requestedStudioId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(counts);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lead detail", description = "Full details for a lead including immutable audit timeline, internal notes, and WhatsApp history.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lead found"),
            @ApiResponse(responseCode = "404", description = "Lead not found")
    })
    public ResponseEntity<LeadDetailDto> getLeadDetail(
            HttpServletRequest request,
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        LeadDetailDto detail = leadService.getLeadDetail(actor, requestedStudioId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(detail);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update lead pipeline state", description = "Updates status, next follow-up date, or lost reason with optimistic locking.")
    public ResponseEntity<LeadDetailDto> updateLead(
            HttpServletRequest request,
            @PathVariable("id") UUID id,
            @Valid @RequestBody LeadUpdateRequest req,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        LeadDetailDto updated = leadService.updateLead(actor, requestedStudioId, id, req);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(updated);
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign lead to team member", description = "Assigns lead to an active member of the studio.")
    public ResponseEntity<LeadDetailDto> assignLead(
            HttpServletRequest request,
            @PathVariable("id") UUID id,
            @Valid @RequestBody LeadAssignmentRequest req,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        LeadDetailDto updated = leadService.assignLead(actor, requestedStudioId, id, req);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(updated);
    }

    @PostMapping("/{id}/notes")
    @Operation(summary = "Add internal note", description = "Appends an internal note to the lead record.")
    public ResponseEntity<LeadNoteDto> addNote(
            HttpServletRequest request,
            @PathVariable("id") UUID id,
            @Valid @RequestBody LeadNoteCreateRequest req,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        LeadNoteDto note = leadService.addNote(actor, requestedStudioId, id, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(note);
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive lead", description = "Archives a lead record, removing it from active pipeline views.")
    public ResponseEntity<Map<String, Object>> archiveLead(
            HttpServletRequest request,
            @PathVariable("id") UUID id,
            @RequestParam(value = "expectedVersion", required = false) Long expectedVersion,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        leadService.archiveLead(actor, requestedStudioId, id, expectedVersion);
        return ResponseEntity.ok(Map.of("success", true, "id", id.toString(), "archived", true));
    }

    @GetMapping("/{id}/whatsapp/status")
    @Operation(summary = "Get WhatsApp configuration and consent status", description = "Checks whether managed WhatsApp provider is configured and public handoff is enabled.")
    public ResponseEntity<WhatsAppProviderStatusDto> getWhatsAppStatus(
            HttpServletRequest request,
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        // Validates actor access to lead
        LeadDetailDto lead = leadService.getLeadDetail(actor, requestedStudioId, id);
        WhatsAppProviderStatusDto status = whatsAppService.getProviderStatus(lead.studioId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(status);
    }

    @PostMapping("/{id}/whatsapp/messages")
    @Operation(summary = "Send managed WhatsApp message", description = "Dispatches an outbound WhatsApp message via configured provider if lead has given explicit consent.")
    public ResponseEntity<WhatsAppMessageDto> sendWhatsAppMessage(
            HttpServletRequest request,
            @PathVariable("id") UUID id,
            @Valid @RequestBody SendWhatsAppMessageRequest req,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        // Validates actor access to lead
        LeadDetailDto lead = leadService.getLeadDetail(actor, requestedStudioId, id);
        WhatsAppMessageDto msg = whatsAppService.sendMessage(lead.studioId(), id, req, actor.userId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(msg);
    }

    private ActorContext extractActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return actor != null ? actor : ActorContext.anonymous();
    }

    private UUID resolveRequestedStudioId(String header, UUID param) {
        if (param != null) {
            return param;
        }
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
