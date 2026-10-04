package com.interior.platform.team.dto;

import java.util.List;
import java.util.UUID;

public record TeamOverviewResponse(
        UUID studioId,
        String studioName,
        int totalMembers,
        String currentUserRole,
        boolean isCurrentUserAdmin,
        List<TeamMemberDto> members,
        List<PendingInvitationDto> pendingInvitations
) {}
