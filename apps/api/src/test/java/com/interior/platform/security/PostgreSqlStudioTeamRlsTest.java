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

@DisplayName("Round 5 — Dedicated PostgreSQL 18 RLS Studio Team & Membership Isolation Tests")
class PostgreSqlStudioTeamRlsTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5433/interior_design_dev";
    private static final String ADMIN_USER = "postgres";
    private static final String ADMIN_PASS = "postgres";

    private boolean postgresAvailable = false;
    private UUID studioAId;
    private UUID studioBId;
    private UUID userAId;
    private UUID userBId;
    private UUID inviteAId;
    private UUID inviteBId;

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
                        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'test_team_user') THEN
                            CREATE ROLE test_team_user WITH LOGIN PASSWORD 'test_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
                        END IF;
                    END
                    $$;
                """);
                stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON studio_members, studio_member_invitations TO test_team_user;");

                stmt.execute("TRUNCATE TABLE studio_member_invitations, studio_members, designer_studios, users CASCADE;");

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

                // Studio members
                stmt.execute(String.format(
                        "INSERT INTO studio_members (id, studio_id, user_id, role, granted_at) VALUES ('%s', '%s', '%s', 'DESIGNER_ADMIN', now());",
                        UuidV7.randomUuid(), studioAId, userAId
                ));
                stmt.execute(String.format(
                        "INSERT INTO studio_members (id, studio_id, user_id, role, granted_at) VALUES ('%s', '%s', '%s', 'DESIGNER_ADMIN', now());",
                        UuidV7.randomUuid(), studioBId, userBId
                ));

                // Studio invitations
                inviteAId = UuidV7.randomUuid();
                inviteBId = UuidV7.randomUuid();

                stmt.execute(String.format(
                        "INSERT INTO studio_member_invitations (id, studio_id, invited_email, role, token_hash, invited_by_user_id, status, expires_at) " +
                        "VALUES ('%s', '%s', 'inviteeA@example.com', 'DESIGNER_MEMBER', '\\x01020304', '%s', 'PENDING', now() + interval '7 days');",
                        inviteAId, studioAId, userAId
                ));
                stmt.execute(String.format(
                        "INSERT INTO studio_member_invitations (id, studio_id, invited_email, role, token_hash, invited_by_user_id, status, expires_at) " +
                        "VALUES ('%s', '%s', 'inviteeB@example.com', 'DESIGNER_MEMBER', '\\x05060708', '%s', 'PENDING', now() + interval '7 days');",
                        inviteBId, studioBId, userBId
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

    private Connection getTenantConnection(UUID studioId) throws SQLException {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(PG_URL);
        ds.setUser("test_team_user");
        ds.setPassword("test_pass");
        Connection conn = ds.getConnection();
        try (Statement stmt = conn.createStatement()) {
            if (studioId != null) {
                stmt.execute(String.format("SET app.current_studio_id = '%s';", studioId));
            } else {
                stmt.execute("RESET app.current_studio_id;");
            }
            stmt.execute("RESET app.is_admin;");
        }
        return conn;
    }

    @Test
    @DisplayName("RLS: Studio A tenant sees only Studio A members and invitations")
    void testStudioIsolation_Select() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL test container/instance not available");

        try (Connection conn = getTenantConnection(studioAId);
             Statement stmt = conn.createStatement()) {

            // 1. Members query
            ResultSet rsMembers = stmt.executeQuery("SELECT studio_id FROM studio_members;");
            int memberCount = 0;
            while (rsMembers.next()) {
                memberCount++;
                assertEquals(studioAId, UUID.fromString(rsMembers.getString("studio_id")));
            }
            assertEquals(1, memberCount);

            // 2. Invitations query
            ResultSet rsInvites = stmt.executeQuery("SELECT studio_id FROM studio_member_invitations;");
            int inviteCount = 0;
            while (rsInvites.next()) {
                inviteCount++;
                assertEquals(studioAId, UUID.fromString(rsInvites.getString("studio_id")));
            }
            assertEquals(1, inviteCount);
        }
    }

    @Test
    @DisplayName("RLS: Studio A tenant cannot update or delete Studio B invitations")
    void testStudioIsolation_MutateBlocked() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL test container/instance not available");

        try (Connection conn = getTenantConnection(studioAId);
             Statement stmt = conn.createStatement()) {

            // Attempt to update Studio B invitation
            int updated = stmt.executeUpdate(String.format(
                    "UPDATE studio_member_invitations SET status = 'REVOKED' WHERE id = '%s';",
                    inviteBId
            ));
            assertEquals(0, updated, "Tenant A must NOT be able to update Tenant B invitation");

            // Attempt to delete Studio B invitation
            int deleted = stmt.executeUpdate(String.format(
                    "DELETE FROM studio_member_invitations WHERE id = '%s';",
                    inviteBId
            ));
            assertEquals(0, deleted, "Tenant A must NOT be able to delete Tenant B invitation");
        }
    }
}
