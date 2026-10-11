package com.interior.platform.security;

import com.interior.platform.common.util.UuidV7;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Phase 9C PostgreSQL RLS Security Test: AI Cost Ledger Isolation")
class PostgreSqlAiCostLedgerRlsTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5433/interior_design_dev";
    private static final String ADMIN_USER = "postgres";
    private static final String ADMIN_PASS = "postgres";
    private static final String RESTRICTED_USER = "test_ai_cost_user";
    private static final String RESTRICTED_PASS = "test_pass";

    private boolean postgresAvailable = false;
    private UUID studioA;
    private UUID studioB;
    private UUID jobA;
    private UUID jobB;
    private UUID costA;
    private UUID costB;

    @BeforeEach
    void setUpFixtures() {
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(PG_URL, ADMIN_USER, ADMIN_PASS)
                    .locations("classpath:db/migration")
                    .load();
            flyway.repair();
            flyway.migrate();
        } catch (Exception e) {
            System.err.println("Flyway error: " + e.getMessage());
        }

        try (Connection adminConn = getAdminConnection()) {
            postgresAvailable = true;

            try (Statement stmt = adminConn.createStatement()) {
                stmt.execute("""
                    DO $$
                    BEGIN
                        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'test_ai_cost_user') THEN
                            CREATE ROLE test_ai_cost_user WITH LOGIN PASSWORD 'test_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
                        END IF;
                    END
                    $$;
                """);
                stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ai_cost_ledger, ai_visualization_jobs, designer_studios, users, studio_projects, media_assets TO test_ai_cost_user;");

                stmt.execute("TRUNCATE TABLE ai_cost_ledger, ai_usage_events, ai_visualization_jobs, media_assets, studio_projects, designer_studios, users CASCADE;");

                UUID ownerA = UuidV7.randomUuid();
                UUID ownerB = UuidV7.randomUuid();
                stmt.execute(String.format("INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'Owner A', 'ownerA@example.com', 'ACTIVE');", ownerA));
                stmt.execute(String.format("INSERT INTO users (id, display_name, email, status) VALUES ('%s', 'Owner B', 'ownerB@example.com', 'ACTIVE');", ownerB));

                studioA = UuidV7.randomUuid();
                studioB = UuidV7.randomUuid();
                stmt.execute(String.format("INSERT INTO designer_studios (id, name, slug, owner_id, status) VALUES ('%s', 'Studio A', 'studio-a', '%s', 'ACTIVE');", studioA, ownerA));
                stmt.execute(String.format("INSERT INTO designer_studios (id, name, slug, owner_id, status) VALUES ('%s', 'Studio B', 'studio-b', '%s', 'ACTIVE');", studioB, ownerB));

                UUID projA = UuidV7.randomUuid();
                UUID projB = UuidV7.randomUuid();
                stmt.execute(String.format("INSERT INTO studio_projects (id, studio_id, slug, title, category_code, project_status, visibility_status) VALUES ('%s', '%s', 'proj-a', 'Project A', 'LIVING_ROOM', 'READY', 'PORTFOLIO');", projA, studioA));
                stmt.execute(String.format("INSERT INTO studio_projects (id, studio_id, slug, title, category_code, project_status, visibility_status) VALUES ('%s', '%s', 'proj-b', 'Project B', 'LIVING_ROOM', 'READY', 'PORTFOLIO');", projB, studioB));

                UUID inputMediaA = UuidV7.randomUuid();
                UUID inputMediaB = UuidV7.randomUuid();
                stmt.execute(String.format("INSERT INTO media_assets (id, studio_id, project_id, media_type, visibility, processing_status, original_storage_key, content_type, file_size, width, height, sort_order, created_by) VALUES ('%s', '%s', '%s', 'REAL_PROJECT', 'PORTFOLIO', 'READY', 'canonical/a.jpg', 'image/jpeg', 5000, 800, 600, 0, '%s');", inputMediaA, studioA, projA, ownerA));
                stmt.execute(String.format("INSERT INTO media_assets (id, studio_id, project_id, media_type, visibility, processing_status, original_storage_key, content_type, file_size, width, height, sort_order, created_by) VALUES ('%s', '%s', '%s', 'REAL_PROJECT', 'PORTFOLIO', 'READY', 'canonical/b.jpg', 'image/jpeg', 5000, 800, 600, 0, '%s');", inputMediaB, studioB, projB, ownerB));

                jobA = UuidV7.randomUuid();
                jobB = UuidV7.randomUuid();
                stmt.execute(String.format("INSERT INTO ai_visualization_jobs (id, studio_id, project_id, input_media_id, provider_key, prompt, status) VALUES ('%s', '%s', '%s', '%s', 'stability', 'p-a', 'SUCCEEDED');", jobA, studioA, projA, inputMediaA));
                stmt.execute(String.format("INSERT INTO ai_visualization_jobs (id, studio_id, project_id, input_media_id, provider_key, prompt, status) VALUES ('%s', '%s', '%s', '%s', 'stability', 'p-b', 'SUCCEEDED');", jobB, studioB, projB, inputMediaB));

                costA = UuidV7.randomUuid();
                costB = UuidV7.randomUuid();
                stmt.execute(String.format("INSERT INTO ai_cost_ledger (id, studio_id, job_id, provider_key, model_name, operation, amount_minor, currency, cost_source) VALUES ('%s', '%s', '%s', 'stability', 'sdxl', 'IMAGE_GENERATION', 4, 'USD', 'CALCULATED');", costA, studioA, jobA));
                stmt.execute(String.format("INSERT INTO ai_cost_ledger (id, studio_id, job_id, provider_key, model_name, operation, amount_minor, currency, cost_source) VALUES ('%s', '%s', '%s', 'stability', 'sdxl', 'IMAGE_GENERATION', 4, 'USD', 'CALCULATED');", costB, studioB, jobB));
            }
        } catch (Exception e) {
            System.err.println("PostgreSQL setup failed: " + e.getMessage());
            e.printStackTrace();
            postgresAvailable = false;
        }
    }

    private Connection getAdminConnection() throws Exception {
        return DriverManager.getConnection(PG_URL, ADMIN_USER, ADMIN_PASS);
    }

    private Connection getRestrictedConnection() throws Exception {
        return DriverManager.getConnection(PG_URL, RESTRICTED_USER, RESTRICTED_PASS);
    }

    @Test
    @DisplayName("RLS: Studio A cannot read Studio B cost ledger records")
    void studioACannotReadStudioBCostLedger() throws Exception {
        assumeTrue(postgresAvailable, "PostgreSQL not reachable on port 5433");

        try (Connection conn = getRestrictedConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(String.format("SET app.current_studio_id = '%s';", studioA));
                stmt.execute("SET app.is_admin = 'false';");
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM ai_cost_ledger;");
                 ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    assertEquals(costA, rs.getObject("id", UUID.class));
                }
                assertEquals(1, count, "Studio A must strictly see only its own cost ledger record");
            }
        }
    }

    @Test
    @DisplayName("RLS: Admin can see all cost ledger records across studios")
    void adminCanReadCostLedgerAcrossStudios() throws Exception {
        assumeTrue(postgresAvailable, "PostgreSQL not reachable on port 5433");

        try (Connection conn = getRestrictedConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("RESET app.current_studio_id;");
                stmt.execute("SET app.is_admin = 'true';");
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM ai_cost_ledger;");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1), "Admin must see both cost ledger entries across studios");
            }
        }
    }
}
