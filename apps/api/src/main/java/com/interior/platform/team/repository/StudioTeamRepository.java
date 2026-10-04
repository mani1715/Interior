package com.interior.platform.team.repository;

import com.interior.platform.team.domain.StudioMemberDetails;
import com.interior.platform.team.domain.StudioMemberInvitationRecord;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudioTeamRepository {

    List<StudioMemberDetails> getStudioMembersWithUserDetails(UUID studioId);

    Optional<StudioMemberDetails> findMembershipById(UUID membershipId);

    Optional<StudioMemberDetails> findMembershipByStudioAndUser(UUID studioId, UUID userId);

    int countAdmins(UUID studioId);

    void updateMemberRole(UUID membershipId, String newRole);

    void removeMember(UUID membershipId);

    void saveInvitation(StudioMemberInvitationRecord invitation);

    Optional<StudioMemberInvitationRecord> findInvitationById(UUID invitationId);

    Optional<StudioMemberInvitationRecord> findInvitationByTokenHash(byte[] tokenHash);

    List<StudioMemberInvitationRecord> findPendingInvitations(UUID studioId, Instant now);

    Optional<StudioMemberInvitationRecord> findPendingInvitationByEmail(UUID studioId, String email, Instant now);

    void updateInvitationStatus(UUID invitationId, String status, Instant timestamp, UUID acceptedByUserId);

    void revokeInvitation(UUID invitationId, Instant revokedAt);

    String findStudioName(UUID studioId);

    void addStudioMember(UUID id, UUID studioId, UUID userId, String role);

    boolean hasPlatformRole(UUID userId, String roleCode);

    void assignPlatformRole(UUID userId, String roleCode);
}
