package com.interior.platform.operations;

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

@DisplayName("Phase 9G — PostgreSQL 18 Support & Platform Operations RLS Security Tests")
class PostgreSqlSupportAndOperationsRlsTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5433/interior_design_dev";
    private static final String ADMIN_USER = "postgres";
    private static final String ADMIN_PASS = "postgres";

    private boolean postgresAvailable = false;
    private UUID userAId;
    private UUID userBId;
    private UUID studioAId;
    private UUID requestAId;
    private UUID requestBId;
    private UUID msgPublicAId;
    private UUID msgInternalAId;

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
                        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'test_support_op_user') THEN
                            CREATE ROLE test_support_op_user WITH LOGIN PASSWORD 'test_support_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
                        END IF;
                    END
                    $$;
                    GRANT USAGE ON SCHEMA public TO test_support_op_user;
                    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO test_support_op_user;
                """);
            }

            userAId = UuidV7.randomUuid();
            userBId = UuidV7.randomUuid();
            studioAId = UuidV7.randomUuid();
            requestAId = UuidV7.randomUuid();
            requestBId = UuidV7.randomUuid();
            msgPublicAId = UuidV7.randomUuid();
            msgInternalAId = UuidV7.randomUuid();

            try (Statement stmt = adminConn.createStatement()) {
                stmt.execute("INSERT INTO users (id, display_name, email, status) VALUES ('" + userAId + "', 'User A', 'usera_" + userAId + "@elegance.local', 'ACTIVE')");
                stmt.execute("INSERT INTO users (id, display_name, email, status) VALUES ('" + userBId + "', 'User B', 'userb_" + userBId + "@elegance.local', 'ACTIVE')");
                stmt.execute("INSERT INTO designer_studios (id, name, slug, owner_id) VALUES ('" + studioAId + "', 'Studio A', 'studio-a-" + studioAId + "', '" + userAId + "')");

                // Seed Support Request A (owned by User A)
                stmt.execute("INSERT INTO support_requests (id, user_id, studio_id, category, subject, status, priority) " +
                        "VALUES ('" + requestAId + "', '" + userAId + "', '" + studioAId + "', 'BILLING', 'Billing Issue A', 'OPEN', 'NORMAL')");

                // Seed Support Request B (owned by User B)
                stmt.execute("INSERT INTO support_requests (id, user_id, studio_id, category, subject, status, priority) " +
                        "VALUES ('" + requestBId + "', '" + userBId + "', NULL, 'ACCOUNT', 'Account Question B', 'OPEN', 'NORMAL')");

                // Seed Public Message on Request A
                stmt.execute("INSERT INTO support_messages (id, request_id, author_user_id, author_type, body, is_internal) " +
                        "VALUES ('" + msgPublicAId + "', '" + requestAId + "', '" + userAId + "', 'USER', 'Public question', false)");

                // Seed Internal Note on Request A
                stmt.execute("INSERT INTO support_messages (id, request_id, author_user_id, author_type, body, is_internal) " +
                        "VALUES ('" + msgInternalAId + "', '" + requestAId + "', '" + userAId + "', 'SUPPORT_ADMIN', 'Confidential admin note', true)");

                // Seed Job run
                stmt.execute("INSERT INTO operational_job_runs (id, job_key, instance_id, status, processed_count) " +
                        "VALUES ('" + UuidV7.randomUuid() + "', 'SESSION_CLEANUP', 'test-node-1', 'SUCCEEDED', 42)");

                // Seed Platform Operational State
                stmt.execute("INSERT INTO platform_operational_state (id, maintenance_mode, uploads_enabled, ai_enabled, registrations_enabled, updated_at) " +
                        "VALUES ('GLOBAL', 'NORMAL', true, true, true, now()) ON CONFLICT (id) DO UPDATE SET maintenance_mode = 'NORMAL'");
            }
        } catch (SQLException e) {
            postgresAvailable = false;
        }
    }

    private Connection getAdminConnection() throws SQLException {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(PG_URL);
        ds.setUser(ADMIN_USER);
        ds.setPassword(ADMIN_PASS);
        return ds.getConnection();
    }

    private Connection getTenantConnection() throws SQLException {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(PG_URL);
        ds.setUser("test_support_op_user");
        ds.setPassword("test_support_pass");
        return ds.getConnection();
    }

    @Test
    @DisplayName("RLS: User A sees only their own support requests, never User B's")
    void testSupportRequestUserIsolation() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 is not available on port 5433");

        try (Connection conn = getTenantConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET app.current_user_id = '" + userAId + "';");
            stmt.execute("SET app.is_admin = 'false';");

            try (ResultSet rs = stmt.executeQuery("SELECT id FROM support_requests")) {
                assertTrue(rs.next(), "User A should see their request");
                assertEquals(requestAId, rs.getObject("id", UUID.class));
                assertFalse(rs.next(), "User A should not see any other requests (User B's request must be filtered by RLS)");
            }
        }
    }

    @Test
    @DisplayName("RLS: Non-admin User A cannot see is_internal = true support messages")
    void testSupportMessageInternalMasking() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 is not available on port 5433");

        try (Connection conn = getTenantConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET app.current_user_id = '" + userAId + "';");
            stmt.execute("SET app.is_admin = 'false';");

            try (ResultSet rs = stmt.executeQuery("SELECT id, is_internal FROM support_messages WHERE request_id = '" + requestAId + "'")) {
                assertTrue(rs.next(), "User A should see public message");
                assertEquals(msgPublicAId, rs.getObject("id", UUID.class));
                assertFalse(rs.getBoolean("is_internal"));
                assertFalse(rs.next(), "Internal message must NOT be visible to non-admin user");
            }
        }
    }

    @Test
    @DisplayName("RLS: Admin sees all tickets and internal messages")
    void testAdminFullVisibility() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 is not available on port 5433");

        try (Connection conn = getTenantConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET app.is_admin = 'true';");

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM support_requests")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) >= 2, "Admin should see all requests across all users");
            }

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM support_messages WHERE request_id = '" + requestAId + "'")) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1), "Admin should see both public and internal messages");
            }
        }
    }

    @Test
    @DisplayName("RLS: Non-admin cannot read or modify platform_operational_state or operational_job_runs")
    void testOperationsTablesAdminIsolation() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 is not available on port 5433");

        // Non-admin attempt
        try (Connection conn = getTenantConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET app.current_user_id = '" + userAId + "';");
            stmt.execute("SET app.is_admin = 'false';");

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM platform_operational_state")) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Non-admin must not see platform_operational_state");
            }

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM operational_job_runs")) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Non-admin must not see operational_job_runs");
            }
        }

        // Admin attempt
        try (Connection conn = getTenantConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET app.is_admin = 'true';");

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM platform_operational_state")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Admin should see platform_operational_state");
            }

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM operational_job_runs")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) >= 1, "Admin should see operational_job_runs");
            }
        }
    }
}
