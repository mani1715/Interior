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

@DisplayName("Phase 27 — Dedicated PostgreSQL 18 RLS Tenant & User Isolation Tests")
class PostgreSqlReviewsVerificationCollectionsTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5433/interior_design_dev";
    private static final String ADMIN_USER = "postgres";
    private static final String ADMIN_PASS = "postgres";

    private boolean postgresAvailable = false;
    private UUID studioAId;
    private UUID studioBId;
    private UUID userAId;
    private UUID userBId;
    private UUID colAId;
    private UUID colBId;

    @BeforeEach
    void setUpFixtures() {
        try (Connection adminConn = getAdminConnection()) {
            postgresAvailable = true;
            try (Statement stmt = adminConn.createStatement()) {
                stmt.execute("""
                    DO $$
                    BEGIN
                        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'test_phase27_user') THEN
                            CREATE ROLE test_phase27_user WITH LOGIN PASSWORD 'test_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
                        END IF;
                    END
                    $$;
                """);
                stmt.execute("REVOKE ALL ON studio_leads, lead_activities, lead_notes, lead_whatsapp_messages FROM test_rls_public_user;");
                stmt.execute("REVOKE ALL ON studio_leads, lead_activities, lead_notes, lead_whatsapp_messages FROM test_phase27_user;");
                stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON user_collections, collection_items, studio_verifications, studio_verification_documents, studio_verification_events TO test_phase27_user;");

                stmt.execute("DELETE FROM collection_items;");
                stmt.execute("DELETE FROM user_collections;");
                stmt.execute("DELETE FROM studio_verification_events;");
                stmt.execute("DELETE FROM studio_verification_documents;");
                stmt.execute("DELETE FROM studio_verifications;");
                stmt.execute("DELETE FROM review_reports;");
                stmt.execute("DELETE FROM studio_reviews;");
                stmt.execute("DELETE FROM review_invitation_sessions;");
                stmt.execute("DELETE FROM review_invitations;");
                stmt.execute("DELETE FROM studio_leads;");
                stmt.execute("DELETE FROM studio_projects;");
                stmt.execute("DELETE FROM designer_studios;");
                stmt.execute("DELETE FROM users;");

                userAId = UuidV7.randomUuid();
                userBId = UuidV7.randomUuid();

                stmt.execute(String.format(
                        "INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'User Alpha', 'alpha@example.com', 'ACTIVE');",
                        userAId
                ));
                stmt.execute(String.format(
                        "INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'User Beta', 'beta@example.com', 'ACTIVE');",
                        userBId
                ));

                // Studio A owned by User A
                studioAId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status) " +
                        "VALUES ('%s', 'Studio Alpha', 'studio-alpha', '%s', 'ACTIVE', 'PUBLISHED');",
                        studioAId, userAId
                ));

                // Studio B owned by User B
                studioBId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status) " +
                        "VALUES ('%s', 'Studio Beta', 'studio-beta', '%s', 'ACTIVE', 'PUBLISHED');",
                        studioBId, userBId
                ));

                // Insert User Collection A
                colAId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO user_collections (id, owner_user_id, title, description, is_default) " +
                        "VALUES ('%s', '%s', 'Alpha Board', 'Private moodboard A', true);",
                        colAId, userAId
                ));

                // Insert User Collection B
                colBId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO user_collections (id, owner_user_id, title, description, is_default) " +
                        "VALUES ('%s', '%s', 'Beta Board', 'Private moodboard B', true);",
                        colBId, userBId
                ));

                // Insert Verification for Studio A
                stmt.execute(String.format(
                        "INSERT INTO studio_verifications (id, studio_id, status, business_name, professional_type) " +
                        "VALUES ('%s', '%s', 'PENDING', 'Alpha Design Studio', 'INTERIOR_DESIGNER');",
                        UuidV7.randomUuid(), studioAId
                ));

                // Insert Verification for Studio B
                stmt.execute(String.format(
                        "INSERT INTO studio_verifications (id, studio_id, status, business_name, professional_type) " +
                        "VALUES ('%s', '%s', 'VERIFIED', 'Beta Design Studio', 'INTERIOR_DESIGNER');",
                        UuidV7.randomUuid(), studioBId
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

    @Test
    @DisplayName("1. User Collections RLS: User A can only see Collection A, not Collection B")
    void testUserCollectionsRls() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 is required for RLS testing");

        try (Connection conn = getAdminConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET ROLE test_phase27_user;");
                // Set session user to User A
                stmt.execute(String.format("SET LOCAL app.current_user_id = '%s';", userAId));
                stmt.execute("SET LOCAL app.is_admin = 'false';");

                try (ResultSet rs = stmt.executeQuery("SELECT id, title FROM user_collections;")) {
                    assertTrue(rs.next(), "User A should see their collection");
                    assertEquals(colAId, rs.getObject("id", UUID.class));
                    assertEquals("Alpha Board", rs.getString("title"));
                    assertFalse(rs.next(), "User A must NOT see Collection B");
                }
            } finally {
                conn.rollback();
            }
        }
    }

    @Test
    @DisplayName("2. User Collections RLS: User B can only see Collection B, not Collection A")
    void testUserCollectionsRlsUserB() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 is required for RLS testing");

        try (Connection conn = getAdminConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET ROLE test_phase27_user;");
                // Set session user to User B
                stmt.execute(String.format("SET LOCAL app.current_user_id = '%s';", userBId));
                stmt.execute("SET LOCAL app.is_admin = 'false';");

                try (ResultSet rs = stmt.executeQuery("SELECT id, title FROM user_collections;")) {
                    assertTrue(rs.next(), "User B should see their collection");
                    assertEquals(colBId, rs.getObject("id", UUID.class));
                    assertEquals("Beta Board", rs.getString("title"));
                    assertFalse(rs.next(), "User B must NOT see Collection A");
                }
            } finally {
                conn.rollback();
            }
        }
    }

    @Test
    @DisplayName("3. Studio Verification RLS: Studio A can only see Verification A, not Verification B")
    void testStudioVerificationRls() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 is required for RLS testing");

        try (Connection conn = getAdminConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET ROLE test_phase27_user;");
                // Set tenant to Studio A
                stmt.execute(String.format("SET LOCAL app.current_studio_id = '%s';", studioAId));
                stmt.execute("SET LOCAL app.is_admin = 'false';");

                try (ResultSet rs = stmt.executeQuery("SELECT studio_id, business_name FROM studio_verifications;")) {
                    assertTrue(rs.next(), "Studio A should see their verification");
                    assertEquals(studioAId, rs.getObject("studio_id", UUID.class));
                    assertEquals("Alpha Design Studio", rs.getString("business_name"));
                    assertFalse(rs.next(), "Studio A must NOT see Studio B's verification");
                }
            } finally {
                conn.rollback();
            }
        }
    }

    @Test
    @DisplayName("4. Admin Bypass RLS: app.is_admin = 'true' can view all collections and verifications")
    void testAdminBypassRls() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 is required for RLS testing");

        try (Connection conn = getAdminConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET ROLE test_phase27_user;");
                stmt.execute("SET LOCAL app.is_admin = 'true';");

                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM user_collections;")) {
                    assertTrue(rs.next());
                    assertEquals(2, rs.getInt(1), "Admin should see both collections");
                }

                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM studio_verifications;")) {
                    assertTrue(rs.next());
                    assertEquals(2, rs.getInt(1), "Admin should see both verifications");
                }
            } finally {
                conn.rollback();
            }
        }
    }
}
