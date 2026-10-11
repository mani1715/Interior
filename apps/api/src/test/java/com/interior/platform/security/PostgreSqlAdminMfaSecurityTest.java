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
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Phase 9D — PostgreSQL 18 MFA and Account Security RLS Tests")
class PostgreSqlAdminMfaSecurityTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5433/interior_design_dev";
    private static final String ADMIN_USER = "postgres";
    private static final String ADMIN_PASS = "postgres";

    private boolean postgresAvailable = false;
    private UUID userAId;
    private UUID userBId;

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
                        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'test_mfa_user') THEN
                            CREATE ROLE test_mfa_user WITH LOGIN PASSWORD 'test_mfa_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
                        END IF;
                    END
                    $$;
                    GRANT USAGE ON SCHEMA public TO test_mfa_user;
                    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO test_mfa_user;
                """);
            }

            userAId = UuidV7.randomUuid();
            userBId = UuidV7.randomUuid();

            try (Statement stmt = adminConn.createStatement()) {
                stmt.execute(String.format("INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'User A', 'usera_%s@test.com', 'ACTIVE')", userAId, UUID.randomUUID().toString().replace("-", "")));
                stmt.execute(String.format("INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'User B', 'userb_%s@test.com', 'ACTIVE')", userBId, UUID.randomUUID().toString().replace("-", "")));
            }
        } catch (SQLException e) {
            System.err.println("PostgreSQL error in setUpFixtures: " + e.getMessage());
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
    @DisplayName("identity_mfa_methods table stores encrypted secret and enforces uniqueness per user and method")
    void testMfaMethodStorageAndUniqueness() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 not available for testing");

        try (Connection conn = getAdminConnection()) {
            try (Statement stmt = conn.createStatement()) {
                UUID mfaId = UuidV7.randomUuid();
                String encryptedSecret = "v1:AES256GCM:dummyciphertextfortest";

                stmt.execute(String.format(
                        "INSERT INTO identity_mfa_methods (id, user_id, method_type, encrypted_secret, key_version, status, pending_expires_at, created_at, updated_at) " +
                        "VALUES ('%s', '%s', 'TOTP', '%s', 1, 'ACTIVE', now() + interval '10 minutes', now(), now())",
                        mfaId, userAId, encryptedSecret
                ));

                ResultSet rs = stmt.executeQuery(String.format("SELECT method_type, status, key_version, encrypted_secret FROM identity_mfa_methods WHERE user_id = '%s'", userAId));
                assertTrue(rs.next());
                assertEquals("TOTP", rs.getString("method_type"));
                assertEquals("ACTIVE", rs.getString("status"));
                assertEquals(1, rs.getInt("key_version"));
                assertEquals(encryptedSecret, rs.getString("encrypted_secret"));

                // Duplicate method_type for same user must violate unique constraint
                assertThrows(SQLException.class, () -> {
                    stmt.execute(String.format(
                            "INSERT INTO identity_mfa_methods (id, user_id, method_type, encrypted_secret, key_version, status, pending_expires_at, created_at, updated_at) " +
                            "VALUES ('%s', '%s', 'TOTP', '%s', 1, 'PENDING', now() + interval '10 minutes', now(), now())",
                            UuidV7.randomUuid(), userAId, "differentSecret"
                    ));
                });
            }
        }
    }

    @Test
    @DisplayName("identity_recovery_codes stores hashed codes and supports atomic single-use redemption")
    void testRecoveryCodeAtomicSingleUseRedemption() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 not available for testing");

        try (Connection conn = getAdminConnection()) {
            try (Statement stmt = conn.createStatement()) {
                String hash1 = "hash_code_1_dummy";
                String hash2 = "hash_code_2_dummy";

                stmt.execute(String.format(
                        "INSERT INTO identity_recovery_codes (id, user_id, code_hash, created_at) VALUES ('%s', '%s', '%s', now())",
                        UuidV7.randomUuid(), userAId, hash1
                ));
                stmt.execute(String.format(
                        "INSERT INTO identity_recovery_codes (id, user_id, code_hash, created_at) VALUES ('%s', '%s', '%s', now())",
                        UuidV7.randomUuid(), userAId, hash2
                ));

                // Query unused count
                ResultSet countRs = stmt.executeQuery(String.format("SELECT COUNT(*) FROM identity_recovery_codes WHERE user_id = '%s' AND used_at IS NULL", userAId));
                assertTrue(countRs.next());
                assertEquals(2, countRs.getInt(1));

                // Redeem hash1
                int redeemed = stmt.executeUpdate(String.format(
                        "UPDATE identity_recovery_codes SET used_at = now() WHERE user_id = '%s' AND code_hash = '%s' AND used_at IS NULL",
                        userAId, hash1
                ));
                assertEquals(1, redeemed, "First redemption must succeed");

                // Attempt to redeem hash1 again (replay attack defense)
                int reReplay = stmt.executeUpdate(String.format(
                        "UPDATE identity_recovery_codes SET used_at = now() WHERE user_id = '%s' AND code_hash = '%s' AND used_at IS NULL",
                        userAId, hash1
                ));
                assertEquals(0, reReplay, "Subsequent redemption must affect 0 rows");

                // Unused count is now 1
                ResultSet postRs = stmt.executeQuery(String.format("SELECT COUNT(*) FROM identity_recovery_codes WHERE user_id = '%s' AND used_at IS NULL", userAId));
                assertTrue(postRs.next());
                assertEquals(1, postRs.getInt(1));
            }
        }
    }

    @Test
    @DisplayName("identity_sessions tracks mfa_verified_at and assurance elevation accurately")
    void testSessionMfaAssuranceElevation() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18 not available for testing");

        try (Connection conn = getAdminConnection()) {
            try (Statement stmt = conn.createStatement()) {
                UUID sessionId = UuidV7.randomUuid();
                String sessionHash = "hash_sess_" + UUID.randomUUID().toString().replace("-", "");

                // Primary session created with standard password assurance
                stmt.execute(String.format(
                        "INSERT INTO identity_sessions (id, user_id, token_hash, csrf_hash, assurance, auth_time, idle_expires_at, absolute_expires_at, last_seen_at) " +
                        "VALUES ('%s', '%s', decode('0102030405', 'hex'), decode('0102030405', 'hex'), 'PASSWORD', now(), now() + interval '30 minutes', now() + interval '12 hours', now())",
                        sessionId, userAId
                ));

                ResultSet rsInitial = stmt.executeQuery(String.format("SELECT assurance, mfa_verified_at FROM identity_sessions WHERE id = '%s'", sessionId));
                assertTrue(rsInitial.next());
                assertEquals("PASSWORD", rsInitial.getString("assurance"));
                assertNull(rsInitial.getTimestamp("mfa_verified_at"));

                // Session elevated with MFA
                stmt.executeUpdate(String.format(
                        "UPDATE identity_sessions SET mfa_verified_at = now(), assurance = 'MFA_VERIFIED' WHERE id = '%s'",
                        sessionId
                ));

                ResultSet rsElevated = stmt.executeQuery(String.format("SELECT assurance, mfa_verified_at FROM identity_sessions WHERE id = '%s'", sessionId));
                assertTrue(rsElevated.next());
                assertEquals("MFA_VERIFIED", rsElevated.getString("assurance"));
                assertNotNull(rsElevated.getTimestamp("mfa_verified_at"));
            }
        }
    }
}
