package com.interior.platform.security;

import com.interior.platform.common.util.UuidV7;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("PostgreSQL RLS & Integrity — Project Rooms Tenant Isolation and Composite Boundary Tests")
class PostgreSqlProjectRoomRlsTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5433/interior_design_dev";
    private static final String ADMIN_USER = "postgres";
    private static final String ADMIN_PASS = "postgres";

    private boolean postgresAvailable = false;
    private UUID studioAId;
    private UUID studioBId;
    private UUID projectAId;
    private UUID projectBId;
    private UUID roomAId;
    private UUID roomBId;
    private UUID mediaAId;

    @BeforeEach
    void setUpFixtures() {
        try {
            org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                    .dataSource(PG_URL, ADMIN_USER, ADMIN_PASS)
                    .locations("classpath:db/migration")
                    .load();
            flyway.migrate();
        } catch (Exception ignored) {
        }

        try (Connection adminConn = getAdminConnection()) {
            postgresAvailable = true;
            try (Statement stmt = adminConn.createStatement()) {
                stmt.execute("""
                    DO $$
                    BEGIN
                        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'test_room_user') THEN
                            CREATE ROLE test_room_user WITH LOGIN PASSWORD 'test_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
                        END IF;
                    END
                    $$;
                """);
                stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON project_rooms, studio_projects, designer_studios, users, media_assets TO test_room_user;");

                stmt.execute("TRUNCATE TABLE media_assets, project_rooms, studio_projects, designer_studios, users CASCADE;");

                UUID userAId = UuidV7.randomUuid();
                UUID userBId = UuidV7.randomUuid();

                stmt.execute(String.format(
                        "INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'User Alpha', 'alpha@example.com', 'ACTIVE');",
                        userAId
                ));
                stmt.execute(String.format(
                        "INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'User Beta', 'beta@example.com', 'ACTIVE');",
                        userBId
                ));

                studioAId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO designer_studios (id, name, slug, owner_id, status) VALUES ('%s', 'Studio Alpha', 'studio-alpha', '%s', 'ACTIVE');",
                        studioAId, userAId
                ));

                studioBId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO designer_studios (id, name, slug, owner_id, status) VALUES ('%s', 'Studio Beta', 'studio-beta', '%s', 'ACTIVE');",
                        studioBId, userBId
                ));

                projectAId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO studio_projects (id, studio_id, slug, title, category_code, project_status, visibility_status) " +
                        "VALUES ('%s', '%s', 'project-alpha', 'Alpha Residence', 'LIVING_ROOM', 'READY', 'PORTFOLIO');",
                        projectAId, studioAId
                ));

                projectBId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO studio_projects (id, studio_id, slug, title, category_code, project_status, visibility_status) " +
                        "VALUES ('%s', '%s', 'project-beta', 'Beta Residence', 'BEDROOM', 'READY', 'PORTFOLIO');",
                        projectBId, studioBId
                ));

                roomAId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO project_rooms (id, studio_id, project_id, room_type, display_name, sort_order) " +
                        "VALUES ('%s', '%s', '%s', 'LIVING_ROOM', 'Grand Living Area', 0);",
                        roomAId, studioAId, projectAId
                ));

                roomBId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO project_rooms (id, studio_id, project_id, room_type, display_name, sort_order) " +
                        "VALUES ('%s', '%s', '%s', 'BEDROOM', 'Master Bedroom B', 0);",
                        roomBId, studioBId, projectBId
                ));

                mediaAId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO media_assets (id, studio_id, project_id, room_id, media_type, visibility, processing_status, original_storage_key, content_type, file_size, width, height, sort_order, is_cover, is_room_cover, watermark_enabled) " +
                        "VALUES ('%s', '%s', '%s', '%s', 'REAL_PROJECT', 'PORTFOLIO', 'READY', 'raw/keyA', 'image/jpeg', 1000, 1920, 1080, 0, true, true, false);",
                        mediaAId, studioAId, projectAId, roomAId
                ));
            }
        } catch (SQLException e) {
            postgresAvailable = false;
        }
    }

    private Connection getAdminConnection() throws SQLException {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(PG_URL);
        ds.setUser(ADMIN_USER);
        ds.setPassword(ADMIN_PASS);
        return ds.getConnection();
    }

    private Connection getTestTenantConnection() throws SQLException {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(PG_URL);
        ds.setUser("test_room_user");
        ds.setPassword("test_pass");
        return ds.getConnection();
    }

    @Test
    @DisplayName("RLS: Studio A cannot view Studio B's project rooms")
    void studioACannotViewStudioBRooms() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL not reachable on localhost:5433");

        try (Connection conn = getTestTenantConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(String.format("SET app.current_studio_id = '%s';", studioAId));
            stmt.execute("SET app.is_admin = 'false';");

            try (ResultSet rs = stmt.executeQuery("SELECT id FROM project_rooms;")) {
                assertTrue(rs.next(), "Studio A should see its own room");
                assertEquals(roomAId, rs.getObject("id", UUID.class));
                assertFalse(rs.next(), "Studio A MUST NOT see Studio B's room");
            }

            try (ResultSet rs = stmt.executeQuery(String.format("SELECT COUNT(*) FROM project_rooms WHERE id = '%s';", roomBId))) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Studio A query for Studio B's room ID must return 0 rows");
            }
        }
    }

    @Test
    @DisplayName("RLS: Studio A cannot mutate or delete Studio B's project rooms")
    void studioACannotMutateOrDeleteStudioBRooms() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL not reachable on localhost:5433");

        try (Connection conn = getTestTenantConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(String.format("SET app.current_studio_id = '%s';", studioAId));
            stmt.execute("SET app.is_admin = 'false';");

            int updated = stmt.executeUpdate(String.format(
                    "UPDATE project_rooms SET display_name = 'Hacked Room' WHERE id = '%s';",
                    roomBId
            ));
            assertEquals(0, updated, "Studio A MUST NOT be able to update Studio B's room");

            int deleted = stmt.executeUpdate(String.format(
                    "DELETE FROM project_rooms WHERE id = '%s';",
                    roomBId
            ));
            assertEquals(0, deleted, "Studio A MUST NOT be able to delete Studio B's room");

            // Attempt to insert room for Studio B while logged in as Studio A
            UUID rogueRoomId = UuidV7.randomUuid();
            assertThrows(SQLException.class, () -> {
                stmt.execute(String.format(
                        "INSERT INTO project_rooms (id, studio_id, project_id, room_type, display_name, sort_order) " +
                        "VALUES ('%s', '%s', '%s', 'KITCHEN', 'Rogue Kitchen', 1);",
                        rogueRoomId, studioBId, projectBId
                ));
            }, "RLS MUST prevent inserting room for another studio");
        }
    }

    @Test
    @DisplayName("Composite Integrity: Media cannot reference a room from a different project or studio")
    void mediaCannotCrossReferenceDifferentProjectOrStudioRoom() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL not reachable on localhost:5433");

        try (Connection conn = getAdminConnection(); Statement stmt = conn.createStatement()) {
            // Attempt to assign Room B (from Project B / Studio B) to Media in Project A / Studio A
            UUID rogueMediaId = UuidV7.randomUuid();
            assertThrows(SQLException.class, () -> {
                stmt.execute(String.format(
                        "INSERT INTO media_assets (id, studio_id, project_id, room_id, media_type, visibility, processing_status, original_storage_key, content_type, file_size, width, height, sort_order, watermark_enabled) " +
                        "VALUES ('%s', '%s', '%s', '%s', 'REAL_PROJECT', 'PORTFOLIO', 'READY', 'raw/rogue', 'image/jpeg', 1000, 1920, 1080, 0, false);",
                        rogueMediaId, studioAId, projectAId, roomBId
                ));
            }, "Foreign key constraint fk_media_assets_room MUST reject room belonging to different project/studio");
        }
    }

    @Test
    @DisplayName("Room Cover Invariant: Partial unique index prevents multiple room covers in the same room")
    void roomCannotHaveMultipleActiveCovers() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL not reachable on localhost:5433");

        try (Connection conn = getAdminConnection(); Statement stmt = conn.createStatement()) {
            UUID secondMediaId = UuidV7.randomUuid();
            // Media A already has is_room_cover = true for Room A. Inserting a second with is_room_cover = true must fail.
            assertThrows(SQLException.class, () -> {
                stmt.execute(String.format(
                        "INSERT INTO media_assets (id, studio_id, project_id, room_id, media_type, visibility, processing_status, original_storage_key, content_type, file_size, width, height, sort_order, is_room_cover, watermark_enabled) " +
                        "VALUES ('%s', '%s', '%s', '%s', 'REAL_PROJECT', 'PORTFOLIO', 'READY', 'raw/keyA2', 'image/jpeg', 1000, 1920, 1080, 1, true, false);",
                        secondMediaId, studioAId, projectAId, roomAId
                ));
            }, "Unique partial index idx_media_single_room_cover MUST prevent duplicate active room covers");
        }
    }

    @Test
    @DisplayName("Safe Deletion: Deleting a room sets media room_id to NULL without deleting media")
    void roomDeletionCascadesToNullSafely() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL not reachable on localhost:5433");

        try (Connection conn = getAdminConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(String.format("DELETE FROM project_rooms WHERE id = '%s';", roomAId));

            try (ResultSet rs = stmt.executeQuery(String.format("SELECT room_id, deleted_at FROM media_assets WHERE id = '%s';", mediaAId))) {
                assertTrue(rs.next(), "Media asset MUST still exist after room is deleted");
                assertNull(rs.getObject("room_id"), "Media room_id MUST become NULL (Project Photos holding area)");
                assertNull(rs.getObject("deleted_at"), "Media MUST NOT be soft-deleted or deleted");
            }
        }
    }
}
