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

@DisplayName("Phase 7C — Dedicated PostgreSQL 18 RLS Communication Delivery Isolation Tests")
class PostgreSqlCommunicationDeliveryRlsTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5433/interior_design_dev";
    private static final String ADMIN_USER = "postgres";
    private static final String ADMIN_PASS = "postgres";

    private boolean postgresAvailable = false;
    private UUID studioAId;
    private UUID studioBId;
    private UUID userAId;
    private UUID userBId;
    private UUID deliveryAId;
    private UUID deliveryBId;

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
                        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'test_comm_user') THEN
                            CREATE ROLE test_comm_user WITH LOGIN PASSWORD 'test_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
                        END IF;
                    END
                    $$;
                """);
                stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON communication_deliveries, notifications, designer_studios, users TO test_comm_user;");

                stmt.execute("TRUNCATE TABLE communication_deliveries, notifications, designer_studios, users CASCADE;");

                userAId = UuidV7.randomUuid();
                userBId = UuidV7.randomUuid();
                studioAId = UuidV7.randomUuid();
                studioBId = UuidV7.randomUuid();
                deliveryAId = UuidV7.randomUuid();
                deliveryBId = UuidV7.randomUuid();

                stmt.execute(String.format("INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'User A', 'usera@studioa.com', 'ACTIVE');", userAId));
                stmt.execute(String.format("INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'User B', 'userb@studiob.com', 'ACTIVE');", userBId));

                stmt.execute(String.format("INSERT INTO designer_studios (id, name, slug, owner_id, status) VALUES ('%s', 'Studio Alpha', 'studio-alpha', '%s', 'ACTIVE');", studioAId, userAId));
                stmt.execute(String.format("INSERT INTO designer_studios (id, name, slug, owner_id, status) VALUES ('%s', 'Studio Beta', 'studio-beta', '%s', 'ACTIVE');", studioBId, userBId));

                // Insert communication delivery records for Studio A and Studio B
                stmt.execute(String.format("""
                    INSERT INTO communication_deliveries (
                        id, studio_id, recipient_user_id, channel, event_type, recipient,
                        subject_or_summary, status, provider, attempt_count, max_attempts
                    ) VALUES (
                        '%s', '%s', '%s', 'EMAIL', 'TEAM_INVITATION', 'colleague@studioa.com',
                        'Join Studio Alpha', 'NOT_CONFIGURED', 'DISABLED', 0, 3
                    );
                """, deliveryAId, studioAId, userAId));

                stmt.execute(String.format("""
                    INSERT INTO communication_deliveries (
                        id, studio_id, recipient_user_id, channel, event_type, recipient,
                        subject_or_summary, status, provider, attempt_count, max_attempts
                    ) VALUES (
                        '%s', '%s', '%s', 'EMAIL', 'REVIEW_INVITATION', 'client@studiob.com',
                        'Review Studio Beta', 'SENT', 'SES', 1, 3
                    );
                """, deliveryBId, studioBId, userBId));
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

    private Connection getTenantConnection() throws SQLException {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(PG_URL);
        ds.setUser("test_comm_user");
        ds.setPassword("test_pass");
        return ds.getConnection();
    }

    @Test
    @DisplayName("RLS: Studio A tenant context cannot select Studio B delivery records")
    void testStudioIsolationOnDeliveries() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL not reachable on port 5433");

        try (Connection conn = getTenantConnection();
             Statement stmt = conn.createStatement()) {

            // Set tenant session to Studio A
            stmt.execute(String.format("SET app.current_studio_id = '%s';", studioAId));

            try (ResultSet rs = stmt.executeQuery("SELECT id, studio_id, recipient FROM communication_deliveries;")) {
                assertTrue(rs.next(), "Studio A should see its own delivery record");
                assertEquals(deliveryAId, rs.getObject("id", UUID.class));
                assertEquals(studioAId, rs.getObject("studio_id", UUID.class));
                assertEquals("colleague@studioa.com", rs.getString("recipient"));
                assertFalse(rs.next(), "Studio A must NOT see Studio B's delivery record");
            }
        }
    }

    @Test
    @DisplayName("RLS: Studio B tenant context cannot select Studio A delivery records")
    void testStudioBIsolationOnDeliveries() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL not reachable on port 5433");

        try (Connection conn = getTenantConnection();
             Statement stmt = conn.createStatement()) {

            // Set tenant session to Studio B
            stmt.execute(String.format("SET app.current_studio_id = '%s';", studioBId));

            try (ResultSet rs = stmt.executeQuery("SELECT id, studio_id, recipient FROM communication_deliveries;")) {
                assertTrue(rs.next(), "Studio B should see its own delivery record");
                assertEquals(deliveryBId, rs.getObject("id", UUID.class));
                assertEquals(studioBId, rs.getObject("studio_id", UUID.class));
                assertEquals("client@studiob.com", rs.getString("recipient"));
                assertFalse(rs.next(), "Studio B must NOT see Studio A's delivery record");
            }
        }
    }

    @Test
    @DisplayName("RLS: Unset studio context sees zero tenant-scoped delivery records")
    void testEmptyContextSeesNoDeliveries() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL not reachable on port 5433");

        try (Connection conn = getTenantConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("RESET app.current_studio_id;");

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM communication_deliveries WHERE studio_id IS NOT NULL;")) {
                assertTrue(rs.next());
                assertEquals(0L, rs.getLong(1), "Unset studio context must not see studio deliveries");
            }
        }
    }
}
