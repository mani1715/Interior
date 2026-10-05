package com.interior.platform.projects.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.media.domain.DerivativeVariant;
import com.interior.platform.media.domain.MediaAssetRecord;
import com.interior.platform.media.domain.MediaDerivativeRecord;
import com.interior.platform.media.repository.MediaRepository;
import com.interior.platform.projects.domain.ProjectRoomRecord;
import com.interior.platform.projects.domain.RoomType;
import com.interior.platform.projects.domain.StudioProjectRecord;
import com.interior.platform.projects.dto.CreateRoomRequest;
import com.interior.platform.projects.dto.ProjectRoomDto;
import com.interior.platform.projects.dto.ReorderRoomsRequest;
import com.interior.platform.projects.dto.UpdateRoomRequest;
import com.interior.platform.projects.repository.ProjectRepository;
import com.interior.platform.projects.repository.ProjectRoomRepository;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.StudioMemberRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ProjectRoomService {

    private final ProjectRoomRepository projectRoomRepository;
    private final ProjectRepository projectRepository;
    private final MediaRepository mediaRepository;
    private final StudioRepository studioRepository;
    private final SecurityRepository securityRepository;
    private final AuthorizationService authorizationService;

    public ProjectRoomService(
            ProjectRoomRepository projectRoomRepository,
            ProjectRepository projectRepository,
            MediaRepository mediaRepository,
            StudioRepository studioRepository,
            SecurityRepository securityRepository,
            AuthorizationService authorizationService
    ) {
        this.projectRoomRepository = projectRoomRepository;
        this.projectRepository = projectRepository;
        this.mediaRepository = mediaRepository;
        this.studioRepository = studioRepository;
        this.securityRepository = securityRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public ProjectRoomDto createRoom(ActorContext actor, UUID requestedStudioId, UUID projectId, CreateRoomRequest request) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        projectRepository.findProjectById(context.studioId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found in studio"));

        String displayName = request.displayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = request.roomType().getDefaultDisplayName();
        } else {
            displayName = displayName.trim();
        }

        int sortOrder = projectRoomRepository.getNextSortOrder(projectId, context.studioId());
        Instant now = Instant.now();
        ProjectRoomRecord room = new ProjectRoomRecord(
                UuidV7.randomUuid(),
                context.studioId(),
                projectId,
                request.roomType(),
                displayName,
                sortOrder,
                now,
                now
        );

        ProjectRoomRecord created = projectRoomRepository.createRoom(room);
        return toDto(created, context.studioId());
    }

    @Transactional
    public ProjectRoomDto updateRoom(ActorContext actor, UUID requestedStudioId, UUID projectId, UUID roomId, UpdateRoomRequest request) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        ProjectRoomRecord existing = projectRoomRepository.findRoomByIdAndProject(roomId, projectId, context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found in project"));

        RoomType newType = request.roomType() != null ? request.roomType() : existing.roomType();
        String newName = existing.displayName();
        if (request.displayName() != null) {
            if (request.displayName().isBlank()) {
                newName = newType.getDefaultDisplayName();
            } else {
                newName = request.displayName().trim();
            }
        }

        ProjectRoomRecord updated = new ProjectRoomRecord(
                existing.id(),
                existing.studioId(),
                existing.projectId(),
                newType,
                newName,
                existing.sortOrder(),
                existing.createdAt(),
                Instant.now()
        );

        projectRoomRepository.updateRoom(updated);
        return toDto(updated, context.studioId());
    }

    @Transactional
    public void deleteRoom(ActorContext actor, UUID requestedStudioId, UUID projectId, UUID roomId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        projectRoomRepository.findRoomByIdAndProject(roomId, projectId, context.studioId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found in project"));

        // Safe room deletion: photos in this room move to Project Photos (unassigned, room_id = NULL)
        mediaRepository.clearRoomForMediaByRoomId(roomId, context.studioId());
        projectRoomRepository.deleteRoom(roomId, context.studioId());
    }

    @Transactional(readOnly = true)
    public List<ProjectRoomDto> getProjectRooms(ActorContext actor, UUID requestedStudioId, UUID projectId) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        projectRepository.findProjectById(context.studioId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found in studio"));

        List<ProjectRoomRecord> rooms = projectRoomRepository.findRoomsByProject(projectId, context.studioId());
        return rooms.stream()
                .map(r -> toDto(r, context.studioId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectRoomDto> getPublicProjectRooms(UUID studioId, UUID projectId) {
        List<ProjectRoomRecord> rooms = projectRoomRepository.findRoomsByProject(projectId, studioId);
        return rooms.stream()
                .map(r -> toDto(r, studioId))
                .toList();
    }

    @Transactional
    public void reorderRooms(ActorContext actor, UUID requestedStudioId, UUID projectId, ReorderRoomsRequest request) {
        authorizationService.requireAuthenticated(actor);
        validateActiveUser(actor.userId());
        validateProfessionalRole(actor);

        ResolvedStudioContext context = resolveStudioContext(actor, requestedStudioId);
        requireStudioManagePermission(context, actor);

        projectRepository.findProjectById(context.studioId(), projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found in studio"));

        List<ProjectRoomRecord> existing = projectRoomRepository.findRoomsByProject(projectId, context.studioId());
        List<UUID> roomIds = request.roomIds();

        if (roomIds.size() != existing.size()) {
            throw new BadRequestException("Room count mismatch for reordering");
        }

        Set<UUID> existingIds = new HashSet<>(existing.stream().map(ProjectRoomRecord::id).toList());
        Set<UUID> providedIds = new HashSet<>(roomIds);

        if (!existingIds.equals(providedIds)) {
            throw new BadRequestException("Invalid room IDs provided for reordering");
        }

        for (int i = 0; i < roomIds.size(); i++) {
            projectRoomRepository.updateSortOrder(roomIds.get(i), context.studioId(), i);
        }
    }

    private ProjectRoomDto toDto(ProjectRoomRecord room, UUID studioId) {
        int count = mediaRepository.countMediaByRoom(room.id(), studioId);
        Optional<MediaAssetRecord> coverOpt = mediaRepository.findRoomCoverMedia(room.id(), studioId);

        UUID coverMediaId = null;
        String coverMediaUrl = null;

        if (coverOpt.isPresent()) {
            MediaAssetRecord coverAsset = coverOpt.get();
            coverMediaId = coverAsset.id();
            coverMediaUrl = resolveCoverUrl(coverAsset, studioId);
        } else {
            // Fallback to first photo in room ordered by sort_order ASC
            List<MediaAssetRecord> roomMedia = mediaRepository.findMediaAssetsByRoom(room.id(), studioId);
            if (!roomMedia.isEmpty()) {
                MediaAssetRecord first = roomMedia.getFirst();
                coverMediaId = first.id();
                coverMediaUrl = resolveCoverUrl(first, studioId);
            }
        }

        return new ProjectRoomDto(
                room.id(),
                room.studioId(),
                room.projectId(),
                room.roomType(),
                room.roomType().getDefaultDisplayName(),
                room.displayName(),
                room.sortOrder(),
                count,
                coverMediaId,
                coverMediaUrl,
                room.createdAt(),
                room.updatedAt()
        );
    }

    private String resolveCoverUrl(MediaAssetRecord asset, UUID studioId) {
        List<MediaDerivativeRecord> derivatives = mediaRepository.findDerivativesByMediaId(asset.id(), studioId);
        for (MediaDerivativeRecord d : derivatives) {
            if (d.variantName() == DerivativeVariant.MEDIUM) {
                return d.publicUrl();
            }
        }
        for (MediaDerivativeRecord d : derivatives) {
            if (d.variantName() == DerivativeVariant.LARGE || d.variantName() == DerivativeVariant.THUMBNAIL) {
                return d.publicUrl();
            }
        }
        return null;
    }

    private UserRecord validateActiveUser(UUID userId) {
        UserRecord user = securityRepository.findUserById(userId)
                .orElseThrow(() -> new AccessDeniedException("User account not found"));
        if (!"ACTIVE".equalsIgnoreCase(user.status())) {
            throw new AccessDeniedException("Account is suspended or inactive");
        }
        return user;
    }

    private void validateProfessionalRole(ActorContext actor) {
        if (actor.activeStudioId() != null && actor.activeStudioRole() != null) {
            return;
        }
        if (actor.hasRole("DESIGNER") || actor.hasRole("DESIGNER_TEAM") ||
            actor.hasRole("SUPER_ADMIN") || actor.hasRole("ADMIN")) {
            return;
        }
        throw new AccessDeniedException("Professional onboarding or studio membership required");
    }

    private ResolvedStudioContext resolveStudioContext(ActorContext actor, UUID requestedStudioId) {
        List<StudioMemberRecord> memberships = securityRepository.getStudioMemberships(actor.userId());
        if (memberships == null || memberships.isEmpty()) {
            throw new AccessDeniedException("No studio membership found for account");
        }

        if (requestedStudioId != null) {
            Optional<StudioMemberRecord> match = memberships.stream()
                    .filter(m -> m.studioId().equals(requestedStudioId))
                    .findFirst();
            if (match.isEmpty()) {
                throw new AccessDeniedException("Access denied: you are not a member of the requested studio");
            }
            return new ResolvedStudioContext(match.get().studioId(), match.get().role());
        }

        if (actor.activeStudioId() != null) {
            Optional<StudioMemberRecord> match = memberships.stream()
                    .filter(m -> m.studioId().equals(actor.activeStudioId()))
                    .findFirst();
            if (match.isPresent()) {
                return new ResolvedStudioContext(match.get().studioId(), match.get().role());
            }
        }

        StudioMemberRecord primary = memberships.get(0);
        return new ResolvedStudioContext(primary.studioId(), primary.role());
    }

    private void requireStudioManagePermission(ResolvedStudioContext context, ActorContext actor) {
        if (actor.hasRole("SUPER_ADMIN") || actor.hasRole("ADMIN")) {
            return;
        }
        if (!"DESIGNER_ADMIN".equalsIgnoreCase(context.role()) && !"OWNER".equalsIgnoreCase(context.role()) && !"ADMIN".equalsIgnoreCase(context.role())) {
            throw new AccessDeniedException("Access denied: elevated studio role (DESIGNER_ADMIN) required");
        }
    }

    private record ResolvedStudioContext(UUID studioId, String role) {}
}
