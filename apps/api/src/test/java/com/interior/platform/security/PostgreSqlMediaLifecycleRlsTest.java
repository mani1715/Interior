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

@DisplayName("Phase 7D — Dedicated PostgreSQL 18 RLS Media Lifecycle Isolation Tests")
class PostgreSqlMediaLifecycleRlsTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5433/interior_design_dev";
    private static final String ADMIN_USER = "postgres";
    private static final String ADMIN_PASS = "postgres";

    private boolean postgresAvailable = false;
    private UUID studioAId;
    private UUID studioBId;
    private UUID userAId;
    private UUID userBId;
    private UUID projectAId;
    private UUID projectBId;
    private UUID mediaAId;
    private UUID mediaBId;
    private UUID uploadIntentAId;
    private UUID uploadIntentBId;

    @BeforeEach
    void setUpFixtures() {
        try {
            org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                    .dataSource(PG_URL, ADMIN_USER, ADMIN_PASS)
                    .locations("classpath:db/migration")
                    .load();
            flyway.repair();
            flyway.migrate();
        } catch (Exception e) {
            System.err.println("Flyway migrate failed: " + e.getMessage());
            e.printStackTrace();
        }

        try (Connection adminConn = getAdminConnection()) {
            postgresAvailable = true;
            try (Statement stmt = adminConn.createStatement()) {
                stmt.execute("""
                    DO $$
                    BEGIN
                        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'test_media_user') THEN
                            CREATE ROLE test_media_user WITH LOGIN PASSWORD 'test_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
                        END IF;
                    END
                    $$;
                """);
                stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON upload_intents, media_assets, studio_projects, designer_studios, users TO test_media_user;");

                stmt.execute("TRUNCATE TABLE upload_intents, media_assets, studio_projects, designer_studios, users CASCADE;");

                userAId = UuidV7.randomUuid();
                userBId = UuidV7.randomUuid();
                studioAId = UuidV7.randomUuid();
                studioBId = UuidV7.randomUuid();
                projectAId = UuidV7.randomUuid();
                projectBId = UuidV7.randomUuid();
                mediaAId = UuidV7.randomUuid();
                mediaBId = UuidV7.randomUuid();
                uploadIntentAId = UuidV7.randomUuid();
                uploadIntentBId = UuidV7.randomUuid();

                stmt.execute(String.format("INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'User A', 'usera@studioa.com', 'ACTIVE');", userAId));
                stmt.execute(String.format("INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'User B', 'userb@studiob.com', 'ACTIVE');", userBId));

                stmt.execute(String.format("INSERT INTO designer_studios (id, name, slug, owner_id, status) VALUES ('%s', 'Studio Alpha', 'studio-alpha-%s', '%s', 'ACTIVE');", studioAId, studioAId.toString().substring(0, 8), userAId));
                stmt.execute(String.format("INSERT INTO designer_studios (id, name, slug, owner_id, status) VALUES ('%s', 'Studio Beta', 'studio-beta-%s', '%s', 'ACTIVE');", studioBId, studioBId.toString().substring(0, 8), userBId));

                stmt.execute(String.format("INSERT INTO studio_projects (id, studio_id, slug, title, category_code, project_status, visibility_status) VALUES ('%s', '%s', 'proj-a', 'Project A', 'LIVING_ROOM', 'READY', 'PORTFOLIO');", projectAId, studioAId));
                stmt.execute(String.format("INSERT INTO studio_projects (id, studio_id, slug, title, category_code, project_status, visibility_status) VALUES ('%s', '%s', 'proj-b', 'Project B', 'LIVING_ROOM', 'READY', 'PORTFOLIO');", projectBId, studioBId));

                // Insert upload intents
                stmt.execute(String.format("""
                    INSERT INTO upload_intents (id, studio_id, project_id, media_type, expected_content_type, expected_size_bytes, quarantine_key, status, expires_at, created_by)
                    VALUES ('%s', '%s', '%s', 'REAL_PROJECT', 'image/jpeg', 1024, 'quarantine/a.jpg', 'PENDING', now() + interval '1 hour', '%s');
                """, uploadIntentAId, studioAId, projectAId, userAId));

                stmt.execute(String.format("""
                    INSERT INTO upload_intents (id, studio_id, project_id, media_type, expected_content_type, expected_size_bytes, quarantine_key, status, expires_at, created_by)
                    VALUES ('%s', '%s', '%s', 'REAL_PROJECT', 'image/jpeg', 1024, 'quarantine/b.jpg', 'PENDING', now() + interval '1 hour', '%s');
                """, uploadIntentBId, studioBId, projectBId, userBId));

                // Insert media assets
                stmt.execute(String.format("""
                    INSERT INTO media_assets (id, studio_id, project_id, media_type, visibility, processing_status, original_storage_key, content_type, file_size, width, height, sort_order, created_by)
                    VALUES ('%s', '%s', '%s', 'REAL_PROJECT', 'PORTFOLIO', 'READY', 'canonical/a.jpg', 'image/jpeg', 5000, 800, 600, 0, '%s');
                """, mediaAId, studioAId, projectAId, userAId));

                stmt.execute(String.format("""
                    INSERT INTO media_assets (id, studio_id, project_id, media_type, visibility, processing_status, original_storage_key, content_type, file_size, width, height, sort_order, created_by)
                    VALUES ('%s', '%s', '%s', 'REAL_PROJECT', 'PORTFOLIO', 'READY', 'canonical/b.jpg', 'image/jpeg', 5000, 800, 600, 0, '%s');
                """, mediaBId, studioBId, projectBId, userBId));
            }
        } catch (SQLException e) {
            System.err.println("PostgreSQL not available or connection failed: " + e.getMessage());
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

    private Connection getTenantConnection(UUID studioId) throws SQLException {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(PG_URL);
        ds.setUser("test_media_user");
        ds.setPassword("test_pass");
        Connection conn = ds.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(String.format("SET app.current_studio_id = '%s';", studioId));
        }
        return conn;
    }

    @Test
    @DisplayName("RLS: Studio A sees only its own upload intents and media assets")
    void studioACannotSeeStudioBMedia() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 instance not available on port 5433");

        try (Connection conn = getTenantConnection(studioAId)) {
            // Upload intents isolation
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id FROM upload_intents;")) {
                assertTrue(rs.next(), "Studio A must see its own upload intent");
                assertEquals(uploadIntentAId, UUID.fromString(rs.getString("id")));
                assertFalse(rs.next(), "Studio A MUST NOT see Studio B's upload intent");
            }

            // Media assets isolation
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id FROM media_assets;")) {
                assertTrue(rs.next(), "Studio A must see its own media asset");
                assertEquals(mediaAId, UUID.fromString(rs.getString("id")));
                assertFalse(rs.next(), "Studio A MUST NOT see Studio B's media asset");
            }
        }
    }

    @Test
    @DisplayName("RLS: Studio A cannot mutate or delete Studio B media assets")
    void studioACannotMutateStudioBMedia() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 instance not available on port 5433");

        try (Connection conn = getTenantConnection(studioAId)) {
            try (Statement stmt = conn.createStatement()) {
                int updated = stmt.executeUpdate(String.format(
                        "UPDATE media_assets SET caption = 'Hacked' WHERE id = '%s';", mediaBId
                ));
                assertEquals(0, updated, "Studio A must not be able to update Studio B's media asset");

                int deleted = stmt.executeUpdate(String.format(
                        "DELETE FROM media_assets WHERE id = '%s';", mediaBId
                ));
                assertEquals(0, deleted, "Studio A must not be able to delete Studio B's media asset");
            }
        }
    }

    @Test
    @DisplayName("RLS: Studio A cannot insert media assets claiming Studio B ownership")
    void studioACannotInsertMediaForStudioB() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 instance not available on port 5433");

        try (Connection conn = getTenantConnection(studioAId)) {
            try (Statement stmt = conn.createStatement()) {
                UUID forgedMediaId = UuidV7.randomUuid();
                assertThrows(SQLException.class, () -> {
                    stmt.execute(String.format("""
                        INSERT INTO media_assets (id, studio_id, project_id, media_type, visibility, processing_status, original_storage_key, content_type, file_size, width, height, sort_order, created_by)
                        VALUES ('%s', '%s', '%s', 'REAL_PROJECT', 'PORTFOLIO', 'READY', 'canonical/forged.jpg', 'image/jpeg', 1000, 100, 100, 0, '%s');
                    """, forgedMediaId, studioBId, projectBId, userAId));
                }, "Inserting media belonging to Studio B must violate RLS policy");
            }
        }
    }
}
