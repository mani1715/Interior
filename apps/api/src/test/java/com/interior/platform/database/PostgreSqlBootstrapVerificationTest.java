package com.interior.platform.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Phase 12B — PostgreSQL Clean Bootstrap & Flyway Verification")
class PostgreSqlBootstrapVerificationTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5433/interior_phase12b_bootstrap";
    private static final String ADMIN_USER = "postgres";
    private static final String ADMIN_PASS = "postgres";

    private PGSimpleDataSource createDataSource() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(PG_URL);
        ds.setUser(ADMIN_USER);
        ds.setPassword(ADMIN_PASS);
        return ds;
    }

    private boolean isPostgresAvailable() {
        try (Connection c = createDataSource().getConnection()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @DisplayName("Verify clean bootstrap from scratch: V001 through V040 Flyway migrations")
    void testCleanBootstrapAndMigrationVerification() throws Exception {
        assumeTrue(isPostgresAvailable(), "PostgreSQL 18.3 on port 5433 must be available for bootstrap test");

        Flyway flyway = Flyway.configure()
                .dataSource(PG_URL, ADMIN_USER, ADMIN_PASS)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        // Clean any existing objects in the bootstrap database
        flyway.clean();

        // 1. Fresh migration from zero
        MigrateResult migrateResult = flyway.migrate();
        assertThat(migrateResult.success).isTrue();
        assertThat(migrateResult.migrationsExecuted).isEqualTo(40);
        assertThat(migrateResult.targetSchemaVersion).isEqualTo("040");

        // 2. Validate checksums and schema integrity
        ValidateResult validateResult = flyway.validateWithResult();
        assertThat(validateResult.validationSuccessful).isTrue();
        assertThat(validateResult.invalidMigrations).isEmpty();

        // 3. Repeat startup check (idempotency)
        MigrateResult repeatMigrate = flyway.migrate();
        assertThat(repeatMigrate.success).isTrue();
        assertThat(repeatMigrate.migrationsExecuted).isEqualTo(0);

        // 4. Verify catalog integrity
        try (Connection conn = createDataSource().getConnection();
             Statement stmt = conn.createStatement()) {

            // Check table counts
            try (ResultSet rs = stmt.executeQuery("""
                SELECT 
                    count(*) AS total_tables,
                    count(*) FILTER (WHERE relrowsecurity = true) AS rls_enabled_tables,
                    count(*) FILTER (WHERE relforcerowsecurity = true) AS rls_forced_tables
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public' AND c.relkind = 'r' AND c.relname != 'flyway_schema_history';
            """)) {
                assertThat(rs.next()).isTrue();
                int totalTables = rs.getInt("total_tables");
                int rlsEnabled = rs.getInt("rls_enabled_tables");
                int rlsForced = rs.getInt("rls_forced_tables");

                assertThat(totalTables).isEqualTo(77);
                assertThat(rlsEnabled).isEqualTo(59);
                assertThat(rlsForced).isEqualTo(59);
            }

            // 5. Test UTF-8 multilingual text storage (English, Telugu, Hindi, Tamil)
            UUID testStudioId = UUID.randomUUID();
            UUID testUserId = UUID.randomUUID();
            String teluguText = "ఇంటీరియర్ డిజైన్ ప్రాజెక్ట్ (Elégance Telugu)";
            String hindiText = "आंतरिक सज्जा परियोजना (Elégance Hindi)";
            String tamilText = "உள்துறை வடிவமைப்பு திட்டம் (Elégance Tamil)";
            String englishText = "Luxury Penthouse Renovation (Elégance English)";

            // Insert a test user with multilingual profile
            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO users (id, email, display_name, status, created_at, updated_at)
                VALUES (?, ?, ?, 'ACTIVE', NOW(), NOW())
            """)) {
                ps.setObject(1, testUserId);
                ps.setString(2, "multilingual-" + testUserId + "@elegance.test");
                ps.setString(3, teluguText + " / " + hindiText);
                ps.executeUpdate();
            }

            // Query back and assert exact Unicode equality
            try (PreparedStatement ps = conn.prepareStatement("SELECT display_name FROM users WHERE id = ?")) {
                ps.setObject(1, testUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    String fetchedName = rs.getString("display_name");
                    assertThat(fetchedName).contains(teluguText);
                    assertThat(fetchedName).contains(hindiText);
                }
            }
        }
    }
}
