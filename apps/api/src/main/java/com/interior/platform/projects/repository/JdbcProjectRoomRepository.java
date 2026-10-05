package com.interior.platform.projects.repository;

import com.interior.platform.projects.domain.ProjectRoomRecord;
import com.interior.platform.projects.domain.RoomType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcProjectRoomRepository implements ProjectRoomRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProjectRoomRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ProjectRoomRecord> roomMapper = (rs, rowNum) -> new ProjectRoomRecord(
            getUuid(rs, "id"),
            getUuid(rs, "studio_id"),
            getUuid(rs, "project_id"),
            RoomType.valueOf(rs.getString("room_type")),
            rs.getString("display_name"),
            rs.getInt("sort_order"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
    );

    @Override
    public ProjectRoomRecord createRoom(ProjectRoomRecord room) {
        String sql = """
            INSERT INTO project_rooms (id, studio_id, project_id, room_type, display_name, sort_order, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                room.id(),
                room.studioId(),
                room.projectId(),
                room.roomType().name(),
                room.displayName(),
                room.sortOrder(),
                Timestamp.from(room.createdAt()),
                Timestamp.from(room.updatedAt())
        );
        return room;
    }

    @Override
    public Optional<ProjectRoomRecord> findRoomById(UUID roomId, UUID studioId) {
        String sql = "SELECT * FROM project_rooms WHERE id = ? AND studio_id = ?";
        List<ProjectRoomRecord> list = jdbcTemplate.query(sql, roomMapper, roomId, studioId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public Optional<ProjectRoomRecord> findRoomByIdAndProject(UUID roomId, UUID projectId, UUID studioId) {
        String sql = "SELECT * FROM project_rooms WHERE id = ? AND project_id = ? AND studio_id = ?";
        List<ProjectRoomRecord> list = jdbcTemplate.query(sql, roomMapper, roomId, projectId, studioId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public List<ProjectRoomRecord> findRoomsByProject(UUID projectId, UUID studioId) {
        String sql = "SELECT * FROM project_rooms WHERE project_id = ? AND studio_id = ? ORDER BY sort_order ASC, created_at ASC";
        return jdbcTemplate.query(sql, roomMapper, projectId, studioId);
    }

    @Override
    public void updateRoom(ProjectRoomRecord room) {
        String sql = """
            UPDATE project_rooms
            SET room_type = ?, display_name = ?, updated_at = ?
            WHERE id = ? AND project_id = ? AND studio_id = ?
        """;
        jdbcTemplate.update(
                sql,
                room.roomType().name(),
                room.displayName(),
                Timestamp.from(room.updatedAt()),
                room.id(),
                room.projectId(),
                room.studioId()
        );
    }

    @Override
    public void deleteRoom(UUID roomId, UUID studioId) {
        String sql = "DELETE FROM project_rooms WHERE id = ? AND studio_id = ?";
        jdbcTemplate.update(sql, roomId, studioId);
    }

    @Override
    public void updateSortOrder(UUID roomId, UUID studioId, int sortOrder) {
        String sql = "UPDATE project_rooms SET sort_order = ?, updated_at = now() WHERE id = ? AND studio_id = ?";
        jdbcTemplate.update(sql, sortOrder, roomId, studioId);
    }

    @Override
    public int getNextSortOrder(UUID projectId, UUID studioId) {
        String sql = "SELECT COALESCE(MAX(sort_order), -1) + 1 FROM project_rooms WHERE project_id = ? AND studio_id = ?";
        Integer next = jdbcTemplate.queryForObject(sql, Integer.class, projectId, studioId);
        return next != null ? next : 0;
    }

    @Override
    public int countRooms(UUID projectId, UUID studioId) {
        String sql = "SELECT COUNT(*) FROM project_rooms WHERE project_id = ? AND studio_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, projectId, studioId);
        return count != null ? count : 0;
    }

    private UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val == null) return null;
        if (val instanceof UUID u) return u;
        return UUID.fromString(val.toString());
    }
}
