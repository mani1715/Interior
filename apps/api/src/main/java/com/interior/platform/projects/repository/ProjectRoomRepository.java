package com.interior.platform.projects.repository;

import com.interior.platform.projects.domain.ProjectRoomRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRoomRepository {
    ProjectRoomRecord createRoom(ProjectRoomRecord room);

    Optional<ProjectRoomRecord> findRoomById(UUID roomId, UUID studioId);

    Optional<ProjectRoomRecord> findRoomByIdAndProject(UUID roomId, UUID projectId, UUID studioId);

    List<ProjectRoomRecord> findRoomsByProject(UUID projectId, UUID studioId);

    void updateRoom(ProjectRoomRecord room);

    void deleteRoom(UUID roomId, UUID studioId);

    void updateSortOrder(UUID roomId, UUID studioId, int sortOrder);

    int getNextSortOrder(UUID projectId, UUID studioId);

    int countRooms(UUID projectId, UUID studioId);
}
