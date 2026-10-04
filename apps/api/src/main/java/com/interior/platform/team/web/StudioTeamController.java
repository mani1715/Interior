package com.interior.platform.team.web;

import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.team.dto.*;
import com.interior.platform.team.service.StudioTeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/workspace")
@Tag(name = "Studio Team & Memberships", description = "Management of studio members, invitations, roles, and access controls")
public class StudioTeamController {

    private final StudioTeamService teamService;

    public StudioTeamController(StudioTeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/team")
    @Operation(summary = "Get studio team overview and members")
    public ResponseEntity<TeamOverviewResponse> getTeamOverview(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        TeamOverviewResponse response = teamService.getTeamOverview(actor, requestedStudioId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    @PostMapping("/team/invitations")
    @Operation(summary = "Invite a new member to the studio")
    public ResponseEntity<CreateInvitationResponse> createInvitation(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody CreateInvitationRequest body
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        CreateInvitationResponse response = teamService.createInvitation(actor, requestedStudioId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/team/invitations/{invitationId}")
    @Operation(summary = "Revoke a pending studio invitation")
    public ResponseEntity<Void> revokeInvitation(
            HttpServletRequest request,
            @PathVariable("invitationId") UUID invitationId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        teamService.revokeInvitation(actor, requestedStudioId, invitationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/team/members/{memberId}/role")
    @Operation(summary = "Update a studio member's role")
    public ResponseEntity<Void> updateMemberRole(
            HttpServletRequest request,
            @PathVariable("memberId") UUID memberId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam,
            @Valid @RequestBody UpdateMemberRoleRequest body
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        teamService.updateMemberRole(actor, requestedStudioId, memberId, body);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/team/members/{memberId}")
    @Operation(summary = "Remove a member from the studio")
    public ResponseEntity<Void> removeMember(
            HttpServletRequest request,
            @PathVariable("memberId") UUID memberId,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        teamService.removeMember(actor, requestedStudioId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/team/leave")
    @Operation(summary = "Voluntarily leave the studio")
    public ResponseEntity<Void> leaveStudio(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID requestedStudioId = resolveRequestedStudioId(studioIdHeader, studioIdParam);

        teamService.leaveStudio(actor, requestedStudioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invitations/validate")
    @Operation(summary = "Validate an invitation token")
    public ResponseEntity<ValidateInvitationResponse> validateInvitation(
            HttpServletRequest request,
            @RequestParam("token") String token
    ) {
        String clientIp = request.getRemoteAddr();
        ValidateInvitationResponse response = teamService.validateInvitation(token, clientIp);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                .body(response);
    }

    @PostMapping("/invitations/accept")
    @Operation(summary = "Accept an invitation and join the studio")
    public ResponseEntity<Void> acceptInvitation(
            HttpServletRequest request,
            @Valid @RequestBody AcceptInvitationRequest body
    ) {
        ActorContext actor = extractActor(request);
        teamService.acceptInvitation(actor, body);
        return ResponseEntity.ok().build();
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
