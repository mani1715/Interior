package com.interior.platform.privacy;

import com.interior.platform.common.util.UuidV7;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Phase 9E — PostgreSQL 18 Privacy, Data Export and Legal Hold RLS Security Tests")
class PostgreSqlPrivacySecurityTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5433/interior_design_dev";
    private static final String ADMIN_USER = "postgres";
    private static final String ADMIN_PASS = "postgres";

    private boolean postgresAvailable = false;
    private UUID userAId;
    private UUID userBId;
    private UUID studioAId;

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
        }

        try (Connection adminConn = getAdminConnection()) {
            postgresAvailable = true;
            try (Statement stmt = adminConn.createStatement()) {
                stmt.execute("""
                    DO $$
                    BEGIN
                        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'test_privacy_user') THEN
                            CREATE ROLE test_privacy_user WITH LOGIN PASSWORD 'test_privacy_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
                        END IF;
                    END
                    $$;
                    GRANT USAGE ON SCHEMA public TO test_privacy_user;
                    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO test_privacy_user;
                """);
            }

            userAId = UuidV7.randomUuid();
            userBId = UuidV7.randomUuid();
            studioAId = UuidV7.randomUuid();

            try (Statement stmt = adminConn.createStatement()) {
                stmt.execute(String.format("INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'User A', 'usera_%s@test.com', 'ACTIVE')", userAId, UUID.randomUUID().toString().replace("-", "")));
                stmt.execute(String.format("INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'User B', 'userb_%s@test.com', 'ACTIVE')", userBId, UUID.randomUUID().toString().replace("-", "")));
                stmt.execute(String.format("INSERT INTO designer_studios (id, name, slug, owner_id, status) VALUES ('%s', 'Studio A', 'studio-a-%s', '%s', 'ACTIVE')", studioAId, UUID.randomUUID().toString().replace("-", ""), userAId));
                stmt.execute(String.format("INSERT INTO studio_members (id, studio_id, user_id, role) VALUES ('%s', '%s', '%s', 'DESIGNER_ADMIN')", UuidV7.randomUuid(), studioAId, userAId));
            }
        } catch (Exception e) {
            System.err.println("setUpFixtures failed: " + e.getMessage());
            e.printStackTrace();
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

    private Connection getRestrictedConnection() throws SQLException {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(PG_URL);
        ds.setUser("test_privacy_user");
        ds.setPassword("test_privacy_pass");
        return ds.getConnection();
    }

    @Test
    @DisplayName("RLS: User B cannot see Privacy Requests of User A")
    void testUserCannotSeeOtherUsersPrivacyRequests() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL test container not available");

        UUID reqA = UuidV7.randomUuid();
        try (Connection admin = getAdminConnection(); Statement stmt = admin.createStatement()) {
            stmt.execute("INSERT INTO privacy_requests (id, user_id, request_type, status) VALUES ('" + reqA + "', '" + userAId + "', 'USER_DATA_EXPORT', 'COMPLETED');");
        }

        try (Connection userConn = getRestrictedConnection(); Statement stmt = userConn.createStatement()) {
            // Set session as User B
            stmt.execute("SET app.current_user_id = '" + userBId + "';");
            stmt.execute("SET app.is_admin = 'false';");

            try (ResultSet rs = stmt.executeQuery("SELECT id FROM privacy_requests WHERE id = '" + reqA + "';")) {
                assertFalse(rs.next(), "User B should NOT see User A's privacy request due to RLS");
            }

            // Set session as User A
            stmt.execute("SET app.current_user_id = '" + userAId + "';");
            try (ResultSet rs = stmt.executeQuery("SELECT id FROM privacy_requests WHERE id = '" + reqA + "';")) {
                assertTrue(rs.next(), "User A must see their own privacy request");
                assertEquals(reqA.toString(), rs.getString("id"));
            }
        }
    }

    @Test
    @DisplayName("RLS: User B cannot see Data Export Jobs of User A")
    void testUserCannotSeeOtherUsersExportJobs() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL test container not available");

        UUID reqA = UuidV7.randomUuid();
        UUID jobA = UuidV7.randomUuid();
        try (Connection admin = getAdminConnection(); Statement stmt = admin.createStatement()) {
            stmt.execute("INSERT INTO privacy_requests (id, user_id, request_type, status) VALUES ('" + reqA + "', '" + userAId + "', 'USER_DATA_EXPORT', 'COMPLETED');");
            stmt.execute("INSERT INTO data_export_jobs (id, privacy_request_id, user_id, export_type, status, file_size_bytes) VALUES ('" + jobA + "', '" + reqA + "', '" + userAId + "', 'USER', 'READY', 1024);");
        }

        try (Connection userConn = getRestrictedConnection(); Statement stmt = userConn.createStatement()) {
            // User B
            stmt.execute("SET app.current_user_id = '" + userBId + "';");
            stmt.execute("SET app.is_admin = 'false';");
            try (ResultSet rs = stmt.executeQuery("SELECT id FROM data_export_jobs WHERE id = '" + jobA + "';")) {
                assertFalse(rs.next(), "User B should NOT see User A's data export job");
            }

            // User A
            stmt.execute("SET app.current_user_id = '" + userAId + "';");
            try (ResultSet rs = stmt.executeQuery("SELECT id FROM data_export_jobs WHERE id = '" + jobA + "';")) {
                assertTrue(rs.next(), "User A must see their own export job");
            }
        }
    }

    @Test
    @DisplayName("RLS: Non-admin users cannot query Privacy Legal Holds table")
    void testNonAdminCannotAccessLegalHolds() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL test container not available");

        UUID holdId = UuidV7.randomUuid();
        try (Connection admin = getAdminConnection(); Statement stmt = admin.createStatement()) {
            stmt.execute("INSERT INTO privacy_legal_holds (id, subject_type, subject_id, reason, placed_by, active) " +
                    "VALUES ('" + holdId + "', 'USER', '" + userAId + "', 'Litigation hold case #402', '" + userAId + "', true);");
        }

        try (Connection userConn = getRestrictedConnection(); Statement stmt = userConn.createStatement()) {
            // User A (Non-admin)
            stmt.execute("SET app.current_user_id = '" + userAId + "';");
            stmt.execute("SET app.is_admin = 'false';");

            try (ResultSet rs = stmt.executeQuery("SELECT id FROM privacy_legal_holds WHERE id = '" + holdId + "';")) {
                assertFalse(rs.next(), "Non-admin user should not see legal hold record");
            }

            // Platform Admin
            stmt.execute("SET app.is_admin = 'true';");
            try (ResultSet rs = stmt.executeQuery("SELECT id FROM privacy_legal_holds WHERE id = '" + holdId + "';")) {
                assertTrue(rs.next(), "Platform admin must see legal hold record");
                assertEquals(holdId.toString(), rs.getString("id"));
            }
        }
    }
}
