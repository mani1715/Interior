package com.interior.platform.leads;

import com.interior.platform.common.util.UuidV7;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Phase 26.2 Dedicated PostgreSQL 18 RLS and Security Definer Tests")
class PostgreSqlLeadSecurityClosureTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5433/interior_design_dev";
    private static final String ADMIN_USER = "postgres";
    private static final String ADMIN_PASS = "postgres";
    private static final String RESTRICTED_USER = "test_rls_public_user";
    private static final String RESTRICTED_PASS = "test_pass";

    private boolean postgresAvailable = false;
    private UUID studioAId;
    private UUID studioBId;
    private UUID studioCId;

    @BeforeEach
    void setUpFixtures() {
        try (Connection adminConn = getAdminConnection()) {
            postgresAvailable = true;
            try (Statement stmt = adminConn.createStatement()) {
                stmt.execute("DELETE FROM lead_activities;");
                stmt.execute("DELETE FROM lead_notes;");
                stmt.execute("DELETE FROM lead_whatsapp_messages;");
                stmt.execute("DELETE FROM studio_leads;");
                stmt.execute("DELETE FROM studio_projects;");
                stmt.execute("DELETE FROM designer_studios;");
                stmt.execute("DELETE FROM users;");

                UUID ownerId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'Owner User', 'owner@example.com', 'ACTIVE');",
                        ownerId
                ));

                // Studio A: PUBLISHED, ACTIVE
                studioAId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status) " +
                        "VALUES ('%s', 'Studio Alpha', 'studio-alpha', '%s', 'ACTIVE', 'PUBLISHED');",
                        studioAId, ownerId
                ));

                // Studio B: UNPUBLISHED, ACTIVE
                studioBId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status) " +
                        "VALUES ('%s', 'Studio Beta', 'studio-beta', '%s', 'ACTIVE', 'UNPUBLISHED');",
                        studioBId, ownerId
                ));

                // Studio C: PUBLISHED, SUSPENDED
                studioCId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status) " +
                        "VALUES ('%s', 'Studio Gamma', 'studio-gamma', '%s', 'SUSPENDED', 'PUBLISHED');",
                        studioCId, ownerId
                ));

                // Studio A Projects
                stmt.execute(String.format(
                        "INSERT INTO studio_projects (id, studio_id, title, slug, category_code, project_status, visibility_status, created_at, updated_at, version) " +
                        "VALUES ('%s', '%s', 'Ready Project', 'proj-ready', 'LIVING_ROOM', 'READY', 'PORTFOLIO', now(), now(), 1);",
                        UuidV7.randomUuid(), studioAId
                ));

                stmt.execute(String.format(
                        "INSERT INTO studio_projects (id, studio_id, title, slug, category_code, project_status, visibility_status, created_at, updated_at, version) " +
                        "VALUES ('%s', '%s', 'Draft Project', 'proj-draft', 'LIVING_ROOM', 'DRAFT', 'PORTFOLIO', now(), now(), 1);",
                        UuidV7.randomUuid(), studioAId
                ));

                stmt.execute(String.format(
                        "INSERT INTO studio_projects (id, studio_id, title, slug, category_code, project_status, visibility_status, created_at, updated_at, version) " +
                        "VALUES ('%s', '%s', 'Private Project', 'proj-private', 'LIVING_ROOM', 'READY', 'PRIVATE', now(), now(), 1);",
                        UuidV7.randomUuid(), studioAId
                ));

                stmt.execute(String.format(
                        "INSERT INTO studio_projects (id, studio_id, title, slug, category_code, project_status, visibility_status, created_at, updated_at, version, archived_at) " +
                        "VALUES ('%s', '%s', 'Archived Project', 'proj-archived', 'LIVING_ROOM', 'READY', 'PORTFOLIO', now(), now(), 1, now());",
                        UuidV7.randomUuid(), studioAId
                ));
            }
        } catch (Exception ex) {
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
        ds.setUser(RESTRICTED_USER);
        ds.setPassword(RESTRICTED_PASS);
        return ds.getConnection();
    }

    @Test
    @DisplayName("PostgreSQL: Restricted public role can execute submit_public_lead successfully for valid published studio")
    void testDirectFunctionInvocationSuccess() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 is not available locally on port 5433");

        try (Connection conn = getRestrictedConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM public.submit_public_lead(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, "studio-alpha");
            ps.setString(2, "proj-ready");
            ps.setString(3, "Direct DB Customer");
            ps.setString(4, "+919876543210");
            ps.setString(5, "direct@example.com");
            ps.setString(6, "Hyderabad");
            ps.setString(7, "Living Room");
            ps.setString(8, "15L-25L");
            ps.setString(9, "Direct DB submission message");
            ps.setString(10, "WHATSAPP");
            ps.setBoolean(11, true);
            ps.setString(12, "db-test-idemp-001");

            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Function should return a row");
                assertNotNull(rs.getObject("lead_id"));
                assertEquals(studioAId, rs.getObject("studio_id"));
                assertEquals("Studio Alpha", rs.getString("studio_name"));
                assertTrue(rs.getString("reference_number").startsWith("INQ-"));
            }
        }
    }

    @Test
    @DisplayName("PostgreSQL: Direct table privileges are strictly denied to restricted public role")
    void testDirectTablePrivilegesDenied() {
        assumeTrue(postgresAvailable, "PostgreSQL 18 is not available locally on port 5433");

        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT * FROM studio_leads;");
            }
        }, "Direct SELECT on studio_leads must be denied");

        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT * FROM lead_activities;");
            }
        }, "Direct SELECT on lead_activities must be denied");

        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT * FROM lead_notes;");
            }
        }, "Direct SELECT on lead_notes must be denied");

        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT * FROM lead_whatsapp_messages;");
            }
        }, "Direct SELECT on lead_whatsapp_messages must be denied");

        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO studio_leads (id, studio_id, source, status, name, phone_normalized, message) " +
                        "VALUES (uuidv7(), uuidv7(), 'DIRECT_INQUIRY', 'WON', 'Hacker', '+919876543210', 'Hack');");
            }
        }, "Direct INSERT on studio_leads must be denied");

        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO lead_activities (id, lead_id, studio_id, actor_id, activity_type, details) " +
                        "VALUES (uuidv7(), uuidv7(), uuidv7(), null, 'WON', '{}');");
            }
        }, "Direct INSERT on lead_activities must be denied");
    }

    @Test
    @DisplayName("PostgreSQL: Negative attribution checks reject unpublished, suspended, private, or draft targets")
    void testNegativeTargetChecks() {
        assumeTrue(postgresAvailable, "PostgreSQL 18 is not available locally on port 5433");

        // 1. Unpublished studio
        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM public.submit_public_lead(?, null, 'Cust', '+919876543210', null, null, null, null, 'Msg', null, false, null)")) {
                ps.setString(1, "studio-beta");
                ps.executeQuery();
            }
        });

        // 2. Suspended studio
        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM public.submit_public_lead(?, null, 'Cust', '+919876543210', null, null, null, null, 'Msg', null, false, null)")) {
                ps.setString(1, "studio-gamma");
                ps.executeQuery();
            }
        });

        // 3. Nonexistent studio
        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM public.submit_public_lead(?, null, 'Cust', '+919876543210', null, null, null, null, 'Msg', null, false, null)")) {
                ps.setString(1, "nonexistent");
                ps.executeQuery();
            }
        });

        // 4. Draft project
        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM public.submit_public_lead('studio-alpha', ?, 'Cust', '+919876543210', null, null, null, null, 'Msg', null, false, null)")) {
                ps.setString(1, "proj-draft");
                ps.executeQuery();
            }
        });

        // 5. Private project
        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM public.submit_public_lead('studio-alpha', ?, 'Cust', '+919876543210', null, null, null, null, 'Msg', null, false, null)")) {
                ps.setString(1, "proj-private");
                ps.executeQuery();
            }
        });

        // 6. Archived project
        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM public.submit_public_lead('studio-alpha', ?, 'Cust', '+919876543210', null, null, null, null, 'Msg', null, false, null)")) {
                ps.setString(1, "proj-archived");
                ps.executeQuery();
            }
        });

        // 7. Nonexistent project
        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM public.submit_public_lead('studio-alpha', ?, 'Cust', '+919876543210', null, null, null, null, 'Msg', null, false, null)")) {
                ps.setString(1, "nonexistent-project");
                ps.executeQuery();
            }
        });
    }

    @Test
    @DisplayName("PostgreSQL: Idempotency under duplicate submission returns same lead and inserts no duplicate activity")
    void testIdempotencyUnderDuplicateSubmission() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 is not available locally on port 5433");

        String idempKey = "idemp-dup-test-" + UUID.randomUUID();
        UUID firstLeadId;

        // First call
        try (Connection conn = getRestrictedConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM public.submit_public_lead('studio-alpha', 'proj-ready', 'Idemp Client', '+919876543210', null, null, null, null, 'Msg', null, false, ?)")) {
            ps.setString(1, idempKey);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                firstLeadId = (UUID) rs.getObject("lead_id");
            }
        }

        // Second call with same idempotency key
        try (Connection conn = getRestrictedConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM public.submit_public_lead('studio-alpha', 'proj-ready', 'Idemp Client', '+919876543210', null, null, null, null, 'Msg', null, false, ?)")) {
            ps.setString(1, idempKey);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                UUID secondLeadId = (UUID) rs.getObject("lead_id");
                assertEquals(firstLeadId, secondLeadId, "Second call must return identical lead_id");
            }
        }

        // Verify with admin connection: exactly 1 lead and 1 activity
        try (Connection adminConn = getAdminConnection();
             Statement stmt = adminConn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM studio_leads WHERE idempotency_key = '" + idempKey + "';")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Exactly 1 lead should exist for this idempotency key");
            }
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM lead_activities WHERE lead_id = '" + firstLeadId + "';")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Exactly 1 activity should exist for this lead");
            }
        }
    }

    @Test
    @DisplayName("PostgreSQL: Privilege escalation and schema tampering are strictly rejected")
    void testPrivilegeEscalationPrevented() {
        assumeTrue(postgresAvailable, "PostgreSQL 18 is not available locally on port 5433");

        // 1. Cannot SET ROLE postgres
        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SET ROLE postgres;");
            }
        }, "Restricted role must not be able to SET ROLE postgres");

        // 2. Cannot CREATE table in public schema
        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE public.shadow_table(id int);");
            }
        }, "Restricted role must not have CREATE on schema public");

        // 3. Cannot alter function
        assertThrows(SQLException.class, () -> {
            try (Connection conn = getRestrictedConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER FUNCTION public.submit_public_lead(text,text,text,text,text,text,text,text,text,text,boolean,text,uuid,uuid) OWNER TO test_rls_public_user;");
            }
        }, "Restricted role must not be able to alter function owner");
    }
}
