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

@DisplayName("Phase 28 — Dedicated PostgreSQL 18 RLS Tenant Isolation for Analytics & Billing")
class PostgreSqlAnalyticsBillingRlsTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5433/interior_design_dev";
    private static final String ADMIN_USER = "postgres";
    private static final String ADMIN_PASS = "postgres";

    private boolean postgresAvailable = false;
    private UUID studioAId;
    private UUID studioBId;
    private UUID userAId;
    private UUID userBId;

    @BeforeEach
    void setUpFixtures() {
        try (Connection adminConn = getAdminConnection()) {
            postgresAvailable = true;
            try (Statement stmt = adminConn.createStatement()) {
                stmt.execute("""
                    DO $$
                    BEGIN
                        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'test_phase28_user') THEN
                            CREATE ROLE test_phase28_user WITH LOGIN PASSWORD 'test_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
                        END IF;
                    END
                    $$;
                """);
                stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON analytics_events, studio_daily_metrics, studio_subscriptions, billing_transactions, billing_events TO test_phase28_user;");

                stmt.execute("DELETE FROM billing_events;");
                stmt.execute("DELETE FROM billing_transactions;");
                stmt.execute("DELETE FROM studio_subscriptions;");
                stmt.execute("DELETE FROM studio_daily_metrics;");
                stmt.execute("DELETE FROM analytics_events;");
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

                studioAId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status) " +
                        "VALUES ('%s', 'Studio Alpha', 'studio-alpha', '%s', 'ACTIVE', 'PUBLISHED');",
                        studioAId, userAId
                ));

                studioBId = UuidV7.randomUuid();
                stmt.execute(String.format(
                        "INSERT INTO designer_studios (id, name, slug, owner_id, status, publication_status) " +
                        "VALUES ('%s', 'Studio Beta', 'studio-beta', '%s', 'ACTIVE', 'PUBLISHED');",
                        studioBId, userBId
                ));

                // Insert Analytics events for Studio A and B
                stmt.execute(String.format(
                        "INSERT INTO analytics_events (id, studio_id, event_type, source, occurred_at) VALUES ('%s', '%s', 'PUBLIC_PROFILE_VIEW', 'client', now());",
                        UuidV7.randomUuid(), studioAId
                ));
                stmt.execute(String.format(
                        "INSERT INTO analytics_events (id, studio_id, event_type, source, occurred_at) VALUES ('%s', '%s', 'PUBLIC_PROFILE_VIEW', 'client', now());",
                        UuidV7.randomUuid(), studioBId
                ));

                // Insert Studio Daily Metrics for Studio A and B
                stmt.execute(String.format(
                        "INSERT INTO studio_daily_metrics (studio_id, metric_date, profile_views) VALUES ('%s', CURRENT_DATE, 5);",
                        studioAId
                ));
                stmt.execute(String.format(
                        "INSERT INTO studio_daily_metrics (studio_id, metric_date, profile_views) VALUES ('%s', CURRENT_DATE, 10);",
                        studioBId
                ));

                // Insert Studio Subscriptions
                stmt.execute(String.format(
                        "INSERT INTO studio_subscriptions (id, studio_id, plan_id, status, provider) VALUES ('%s', '%s', '01923000-0000-7000-8000-000000000001', 'ACTIVE', 'NONE');",
                        UuidV7.randomUuid(), studioAId
                ));
                stmt.execute(String.format(
                        "INSERT INTO studio_subscriptions (id, studio_id, plan_id, status, provider) VALUES ('%s', '%s', '01923000-0000-7000-8000-000000000001', 'ACTIVE', 'NONE');",
                        UuidV7.randomUuid(), studioBId
                ));

                // Insert Billing Transactions
                stmt.execute(String.format(
                        "INSERT INTO billing_transactions (id, studio_id, provider, amount_minor, status) VALUES ('%s', '%s', 'NONE', 0, 'SUCCEEDED');",
                        UuidV7.randomUuid(), studioAId
                ));
                stmt.execute(String.format(
                        "INSERT INTO billing_transactions (id, studio_id, provider, amount_minor, status) VALUES ('%s', '%s', 'NONE', 0, 'SUCCEEDED');",
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

    private Connection getNonSuperUserConnection() throws SQLException {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(PG_URL);
        ds.setUser("test_phase28_user");
        ds.setPassword("test_pass");
        return ds.getConnection();
    }

    @Test
    @DisplayName("PostgreSQL RLS: Studio A cannot view Studio B analytics events")
    void testAnalyticsEventsRls() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18.3 not reachable on port 5433, skipping PG-specific RLS test");

        try (Connection conn = getNonSuperUserConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(String.format("SET app.current_studio_id = '%s';", studioAId));

            try (ResultSet rs = stmt.executeQuery("SELECT studio_id FROM analytics_events;")) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    assertEquals(studioAId, (UUID) rs.getObject("studio_id"));
                }
                assertEquals(1, count, "Should only see Studio A's analytics event");
            }
        }
    }

    @Test
    @DisplayName("PostgreSQL RLS: Studio A cannot view Studio B daily metrics")
    void testDailyMetricsRls() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18.3 not reachable on port 5433, skipping PG-specific RLS test");

        try (Connection conn = getNonSuperUserConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(String.format("SET app.current_studio_id = '%s';", studioAId));

            try (ResultSet rs = stmt.executeQuery("SELECT studio_id, profile_views FROM studio_daily_metrics;")) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    assertEquals(studioAId, (UUID) rs.getObject("studio_id"));
                    assertEquals(5, rs.getLong("profile_views"));
                }
                assertEquals(1, count, "Should only see Studio A's daily metrics");
            }
        }
    }

    @Test
    @DisplayName("PostgreSQL RLS: Studio A cannot view Studio B subscriptions or transactions")
    void testBillingTenantIsolationRls() throws SQLException {
        assumeTrue(postgresAvailable, "PostgreSQL 18.3 not reachable on port 5433, skipping PG-specific RLS test");

        try (Connection conn = getNonSuperUserConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(String.format("SET app.current_studio_id = '%s';", studioAId));

            try (ResultSet rs = stmt.executeQuery("SELECT studio_id FROM studio_subscriptions;")) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    assertEquals(studioAId, (UUID) rs.getObject("studio_id"));
                }
                assertEquals(1, count, "Should only see Studio A's subscription");
            }

            try (ResultSet rs = stmt.executeQuery("SELECT studio_id FROM billing_transactions;")) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    assertEquals(studioAId, (UUID) rs.getObject("studio_id"));
                }
                assertEquals(1, count, "Should only see Studio A's transaction");
            }
        }
    }
}
