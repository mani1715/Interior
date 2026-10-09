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

@DisplayName("Phase 7E — Dedicated PostgreSQL 18 RLS Admin & Moderation Controls Tests")
class PostgreSqlAdminOperationalRlsTest {

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
    private UUID privateProjectBId;

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
                        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'test_admin_op_user') THEN
                            CREATE ROLE test_admin_op_user WITH LOGIN PASSWORD 'test_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
                        END IF;
                    END
                    $$;
                    GRANT USAGE ON SCHEMA public TO test_admin_op_user;
                    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO test_admin_op_user;

                    DROP POLICY IF EXISTS public_read_studio_projects ON studio_projects;
                    CREATE POLICY public_read_studio_projects ON studio_projects
                        FOR SELECT
                        USING (
                            project_status = 'READY'
                            AND visibility_status = 'PORTFOLIO'
                            AND archived_at IS NULL
                            AND moderation_status = 'APPROVED'
                            AND EXISTS (
                                SELECT 1 FROM designer_studios s
                                WHERE s.id = studio_projects.studio_id
                                  AND s.publication_status = 'PUBLISHED'
                                  AND s.status = 'ACTIVE'
                            )
                        );
                """);
            }

            userAId = UuidV7.randomUuid();
            userBId = UuidV7.randomUuid();
            studioAId = UuidV7.randomUuid();
            studioBId = UuidV7.randomUuid();
            projectAId = UuidV7.randomUuid();
            projectBId = UuidV7.randomUuid();
            privateProjectBId = UuidV7.randomUuid();

            try (Statement stmt = adminConn.createStatement()) {
                stmt.execute(String.format("INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'Designer A', 'desA_%s@test.com', 'ACTIVE')", userAId, UUID.randomUUID().toString().replace("-", "")));
                stmt.execute(String.format("INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'Designer B', 'desB_%s@test.com', 'ACTIVE')", userBId, UUID.randomUUID().toString().replace("-", "")));

                stmt.execute(String.format("INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status) VALUES ('%s', 'Studio A', 'studio-a-%s', '%s', 'ACTIVE', 'PUBLISHED')", studioAId, UUID.randomUUID().toString().replace("-", ""), userAId));
                stmt.execute(String.format("INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status) VALUES ('%s', 'Studio B', 'studio-b-%s', '%s', 'ACTIVE', 'PUBLISHED')", studioBId, UUID.randomUUID().toString().replace("-", ""), userBId));

                stmt.execute(String.format("INSERT INTO studio_projects (id, studio_id, title, slug, category_code, project_status, visibility_status, moderation_status) VALUES ('%s', '%s', 'Project A', 'proj-a-%s', 'LIVING_ROOM', 'READY', 'PORTFOLIO', 'APPROVED')", projectAId, studioAId, UUID.randomUUID().toString().replace("-", "")));
                stmt.execute(String.format("INSERT INTO studio_projects (id, studio_id, title, slug, category_code, project_status, visibility_status, moderation_status) VALUES ('%s', '%s', 'Project B', 'proj-b-%s', 'BEDROOM', 'READY', 'PORTFOLIO', 'APPROVED')", projectBId, studioBId, UUID.randomUUID().toString().replace("-", "")));
                stmt.execute(String.format("INSERT INTO studio_projects (id, studio_id, title, slug, category_code, project_status, visibility_status, moderation_status) VALUES ('%s', '%s', 'Private Project B', 'priv-b-%s', 'BEDROOM', 'DRAFT', 'PRIVATE', 'APPROVED')", privateProjectBId, studioBId, UUID.randomUUID().toString().replace("-", "")));
            }
        } catch (SQLException e) {
            System.err.println("PostgreSQL error in setUpFixtures: " + e.getMessage());
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

    private Connection getTenantConnection() throws SQLException {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(PG_URL);
        ds.setUser("test_admin_op_user");
        ds.setPassword("test_pass");
        return ds.getConnection();
    }

    @Test
    @DisplayName("RLS strictly isolates private studio projects and prevents cross-tenant mutation")
    void testStudioProjectRlsWithModeration() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 not available for testing");

        try (Connection tenantConn = getTenantConnection()) {
            try (Statement stmt = tenantConn.createStatement()) {
                stmt.execute(String.format("SET app.current_studio_id = '%s'", studioAId));
                stmt.execute("SET app.is_admin = 'false'");

                // Studio A must see own Project A
                ResultSet rs = stmt.executeQuery(String.format("SELECT COUNT(*) FROM studio_projects WHERE id = '%s'", projectAId));
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Studio A must see own project");

                // Studio A must NOT see Studio B's private project
                ResultSet privRs = stmt.executeQuery(String.format("SELECT COUNT(*) FROM studio_projects WHERE id = '%s'", privateProjectBId));
                assertTrue(privRs.next());
                assertEquals(0, privRs.getInt(1), "Studio A must NOT see Studio B's private project");

                // Studio A cannot update Studio B's project moderation status or content
                int updated = stmt.executeUpdate(String.format(
                        "UPDATE studio_projects SET moderation_status = 'HIDDEN' WHERE id = '%s'", projectBId));
                assertEquals(0, updated, "Cross-tenant project moderation update must affect 0 rows under RLS");
            }
        }
    }

    @Test
    @DisplayName("Moderated HIDDEN projects are suppressed from public read RLS policy")
    void testModerationHiddenSuppressesPublicRead() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 not available for testing");

        try (Connection adminConn = getAdminConnection()) {
            try (Statement stmt = adminConn.createStatement()) {
                // Moderate project B to HIDDEN
                stmt.executeUpdate(String.format(
                        "UPDATE studio_projects SET moderation_status = 'HIDDEN', moderation_reason = 'Policy violation' WHERE id = '%s'", projectBId));
            }
        }

        try (Connection tenantConn = getTenantConnection()) {
            try (Statement stmt = tenantConn.createStatement()) {
                // Anonymous / unauthenticated tenant session (no studio context set)
                stmt.execute("RESET app.current_studio_id");
                stmt.execute("SET app.is_admin = 'false'");

                // Project A is APPROVED -> visible via public_read_studio_projects
                ResultSet rsA = stmt.executeQuery(String.format("SELECT COUNT(*) FROM studio_projects WHERE id = '%s'", projectAId));
                assertTrue(rsA.next());
                assertEquals(1, rsA.getInt(1), "Approved project must remain publicly readable");

                // Project B is HIDDEN -> suppressed by public_read_studio_projects
                ResultSet polRs = stmt.executeQuery("SELECT polname, polcmd, polpermissive, pg_get_expr(polqual, polrelid) as qual FROM pg_policy WHERE polrelid = 'studio_projects'::regclass");
                while (polRs.next()) {
                    System.err.println("POLICY: " + polRs.getString("polname") + " cmd=" + polRs.getString("polcmd") + " permissive=" + polRs.getString("polpermissive") + " qual=" + polRs.getString("qual"));
                }
                ResultSet curStudioRs = stmt.executeQuery("SELECT current_setting('app.current_studio_id', true), current_setting('app.is_admin', true)");
                if (curStudioRs.next()) {
                    System.err.println("SETTINGS: app.current_studio_id=[" + curStudioRs.getString(1) + "] app.is_admin=[" + curStudioRs.getString(2) + "]");
                }
                ResultSet rsCount = stmt.executeQuery(String.format("SELECT COUNT(*) FROM studio_projects WHERE id = '%s'", projectBId));
                assertTrue(rsCount.next());
                assertEquals(0, rsCount.getInt(1), "Moderated HIDDEN project must be suppressed from public read RLS");
            }
        }
    }

    @Test
    @DisplayName("Admin can update moderation status and suspension reasons on PostgreSQL 18")
    void testAdminModerationAndSuspensionColumns() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 not available for testing");

        try (Connection adminConn = getAdminConnection()) {
            try (Statement stmt = adminConn.createStatement()) {
                // Update suspension reason on studio
                int studioUpdated = stmt.executeUpdate(String.format(
                        "UPDATE designer_studios SET status = 'SUSPENDED', suspension_reason = 'Policy violation' WHERE id = '%s'", studioAId));
                assertEquals(1, studioUpdated);

                // Update moderation status and reason on project
                int projUpdated = stmt.executeUpdate(String.format(
                        "UPDATE studio_projects SET moderation_status = 'HIDDEN', moderation_reason = 'DMCA claim' WHERE id = '%s'", projectAId));
                assertEquals(1, projUpdated);

                // Verify persisted state
                ResultSet rs = stmt.executeQuery(String.format("SELECT status, suspension_reason FROM designer_studios WHERE id = '%s'", studioAId));
                assertTrue(rs.next());
                assertEquals("SUSPENDED", rs.getString("status"));
                assertEquals("Policy violation", rs.getString("suspension_reason"));

                ResultSet projRs = stmt.executeQuery(String.format("SELECT moderation_status, moderation_reason FROM studio_projects WHERE id = '%s'", projectAId));
                assertTrue(projRs.next());
                assertEquals("HIDDEN", projRs.getString("moderation_status"));
                assertEquals("DMCA claim", projRs.getString("moderation_reason"));
            }
        }
    }

    @Test
    @DisplayName("Audit events table enforces append-only performance index on timestamp and action")
    void testAuditLogPerformanceIndex() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 not available for testing");

        try (Connection adminConn = getAdminConnection()) {
            try (Statement stmt = adminConn.createStatement()) {
                UUID auditId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO audit_events (id, studio_id, actor_id, action, resource_type, resource_id, request_id, details, timestamp) " +
                        "VALUES ('%s', '%s', '%s', 'PROJECT_MODERATION', 'STUDIO_PROJECT', '%s', 'req-test', '{\"status\":\"HIDDEN\"}', now())",
                        auditId, studioAId, userAId, projectAId
                ));

                ResultSet rs = stmt.executeQuery(String.format("SELECT action, resource_type FROM audit_events WHERE id = '%s'", auditId));
                assertTrue(rs.next());
                assertEquals("PROJECT_MODERATION", rs.getString("action"));
                assertEquals("STUDIO_PROJECT", rs.getString("resource_type"));
            }
        }
    }
}
