package com.interior.platform.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.ValidateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Phase 12B — PostgreSQL Destructive Backup and Restore Drill")
class PostgreSqlDestructiveRestoreDrillTest {

    private static final String DRILL_DB = "interior_restore_drill";
    private static final String PG_URL = "jdbc:postgresql://localhost:5433/" + DRILL_DB;
    private static final String ADMIN_USER = "postgres";
    private static final String ADMIN_PASS = "postgres";

    private PGSimpleDataSource createDataSource(String dbName) {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL("jdbc:postgresql://localhost:5433/" + dbName);
        ds.setUser(ADMIN_USER);
        ds.setPassword(ADMIN_PASS);
        return ds;
    }

    private boolean isPostgresAvailable() {
        try (Connection c = createDataSource("postgres").getConnection()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @DisplayName("Execute full destructive restore drill: State A -> Backup -> Mutate to State B -> Restore -> Verify State A & RLS")
    void testDestructiveBackupAndRestoreDrill() throws Exception {
        assumeTrue(isPostgresAvailable(), "PostgreSQL on port 5433 must be available");

        // 1. Prepare disposable database
        try (Connection adminConn = createDataSource("postgres").getConnection();
             Statement stmt = adminConn.createStatement()) {
            stmt.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + DRILL_DB + "' AND pid <> pg_backend_pid();");
            stmt.execute("DROP DATABASE IF EXISTS " + DRILL_DB + ";");
            stmt.execute("CREATE DATABASE " + DRILL_DB + ";");
        }

        // 2. Migrate from zero to V040
        Flyway flyway = Flyway.configure()
                .dataSource(PG_URL, ADMIN_USER, ADMIN_PASS)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        // 3. Seed identifiable State A fixtures
        UUID userAId = UUID.randomUUID();
        UUID studioAId = UUID.randomUUID();
        UUID projectAId = UUID.randomUUID();
        String stateAProjectTitle = "State A Luxury Penthouse Showcase";

        try (Connection conn = createDataSource(DRILL_DB).getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO users (id, email, display_name, status, created_at, updated_at)
                VALUES (?, ?, ?, 'ACTIVE', NOW(), NOW())
            """)) {
                ps.setObject(1, userAId);
                ps.setString(2, "state-a-user@elegance.test");
                ps.setString(3, "State A Architect");
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO designer_studios (id, owner_id, name, slug, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'ACTIVE', NOW(), NOW())
            """)) {
                ps.setObject(1, studioAId);
                ps.setObject(2, userAId);
                ps.setString(3, "State A Design Studio");
                ps.setString(4, "state-a-studio-" + UUID.randomUUID().toString().substring(0, 8));
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO studio_projects (id, studio_id, title, slug, project_status, category_code, created_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'DRAFT', 'LIVING_ROOM', ?, NOW(), NOW())
            """)) {
                ps.setObject(1, projectAId);
                ps.setObject(2, studioAId);
                ps.setString(3, stateAProjectTitle);
                ps.setString(4, "state-a-project-" + UUID.randomUUID().toString().substring(0, 8));
                ps.setObject(5, userAId);
                ps.executeUpdate();
            }
        }

        // 4. Execute backup utility via PowerShell script
        File rootDir = new File("..\\..").getCanonicalFile();
        File backupScript = new File(rootDir, "scripts\\backup-postgresql.ps1");
        assertThat(backupScript).exists();

        ProcessBuilder backupPb = new ProcessBuilder(
                "powershell", "-ExecutionPolicy", "Bypass", "-File", backupScript.getAbsolutePath(),
                "-Database", DRILL_DB,
                "-Port", "5433"
        );
        backupPb.directory(rootDir);
        backupPb.redirectErrorStream(true);

        long backupStart = System.currentTimeMillis();
        Process backupProc = backupPb.start();
        StringBuilder backupOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(backupProc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                backupOutput.append(line).append("\n");
            }
        }
        int backupExit = backupProc.waitFor();
        long backupDuration = System.currentTimeMillis() - backupStart;

        assertThat(backupExit).as("Backup script output: " + backupOutput).isEqualTo(0);
        assertThat(backupOutput.toString()).contains("BACKUP COMPLETED SUCCESSFULLY");

        // Locate created dump file
        File backupsDir = new File(rootDir, "storage\\backups");
        File[] dumpFiles = backupsDir.listFiles((dir, name) -> name.startsWith("backup_" + DRILL_DB) && name.endsWith(".dump"));
        assertThat(dumpFiles).isNotNull().isNotEmpty();
        File latestDump = dumpFiles[dumpFiles.length - 1];
        long dumpSize = latestDump.length();
        assertThat(dumpSize).isGreaterThan(0);

        try {
            // 5. Mutate database to corrupted State B
            UUID userBId = UUID.randomUUID();
            try (Connection conn = createDataSource(DRILL_DB).getConnection()) {
                // Mutate Project A title
                try (PreparedStatement ps = conn.prepareStatement("UPDATE studio_projects SET title = 'Corrupted State B Title' WHERE id = ?")) {
                    ps.setObject(1, projectAId);
                    ps.executeUpdate();
                }

                // Insert User B (intruder)
                try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO users (id, email, display_name, status, created_at, updated_at)
                    VALUES (?, ?, ?, 'ACTIVE', NOW(), NOW())
                """)) {
                    ps.setObject(1, userBId);
                    ps.setString(2, "state-b-intruder@elegance.test");
                    ps.setString(3, "State B Intruder");
                    ps.executeUpdate();
                }

                // Verify state is mutated
                try (PreparedStatement ps = conn.prepareStatement("SELECT title FROM studio_projects WHERE id = ?")) {
                    ps.setObject(1, projectAId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("title")).isEqualTo("Corrupted State B Title");
                    }
                }
            }

            // 6. Execute restore utility via PowerShell script with -DropAndRecreate
            File restoreScript = new File(rootDir, "scripts\\restore-postgresql.ps1");
            assertThat(restoreScript).exists();

            ProcessBuilder restorePb = new ProcessBuilder(
                    "powershell", "-ExecutionPolicy", "Bypass", "-File", restoreScript.getAbsolutePath(),
                    "-BackupFile", latestDump.getAbsolutePath(),
                    "-Database", DRILL_DB,
                    "-Port", "5433",
                    "-DropAndRecreate"
            );
            restorePb.directory(rootDir);
            restorePb.redirectErrorStream(true);

            long restoreStart = System.currentTimeMillis();
            Process restoreProc = restorePb.start();
            StringBuilder restoreOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(restoreProc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    restoreOutput.append(line).append("\n");
                }
            }
            int restoreExit = restoreProc.waitFor();
            long restoreDuration = System.currentTimeMillis() - restoreStart;

            assertThat(restoreExit).as("Restore script output: " + restoreOutput).isEqualTo(0);
            assertThat(restoreOutput.toString()).contains("RESTORE COMPLETED SUCCESSFULLY");

            // 7. Verify restored State A in database
            try (Connection conn = createDataSource(DRILL_DB).getConnection()) {
                // Verify Project A restored with original State A title
                try (PreparedStatement ps = conn.prepareStatement("SELECT title FROM studio_projects WHERE id = ?")) {
                    ps.setObject(1, projectAId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("title")).isEqualTo(stateAProjectTitle);
                    }
                }

                // Verify User A exists
                try (PreparedStatement ps = conn.prepareStatement("SELECT display_name FROM users WHERE id = ?")) {
                    ps.setObject(1, userAId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getString("display_name")).isEqualTo("State A Architect");
                    }
                }

                // Verify User B (intruder) is completely gone
                try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE id = ?")) {
                    ps.setObject(1, userBId);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isFalse();
                    }
                }

                // 8. Verify RLS table count on restored database
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("""
                        SELECT 
                            count(*) AS total_tables,
                            count(*) FILTER (WHERE relrowsecurity = true) AS rls_enabled_tables,
                            count(*) FILTER (WHERE relforcerowsecurity = true) AS rls_forced_tables
                        FROM pg_class c
                        JOIN pg_namespace n ON n.oid = c.relnamespace
                        WHERE n.nspname = 'public' AND c.relkind = 'r' AND c.relname != 'flyway_schema_history';
                     """)) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt("total_tables")).isEqualTo(77);
                    assertThat(rs.getInt("rls_enabled_tables")).isEqualTo(59);
                    assertThat(rs.getInt("rls_forced_tables")).isEqualTo(59);
                }
            }

            // 9. Verify Flyway validates the restored schema
            Flyway restoredFlyway = Flyway.configure()
                    .dataSource(PG_URL, ADMIN_USER, ADMIN_PASS)
                    .locations("classpath:db/migration")
                    .load();
            ValidateResult validateResult = restoredFlyway.validateWithResult();
            assertThat(validateResult.validationSuccessful).isTrue();

            System.out.println("=== DESTRUCTIVE RESTORE DRILL COMPLETED SUCCESSFULLY ===");
            System.out.println("Backup duration : " + backupDuration + " ms");
            System.out.println("Dump file size  : " + dumpSize + " bytes (" + (dumpSize / 1024) + " KB)");
            System.out.println("Restore duration: " + restoreDuration + " ms");

        } finally {
            // Cleanup dump file
            if (latestDump.exists()) {
                latestDump.delete();
            }
            // Cleanup drill database
            try (Connection adminConn = createDataSource("postgres").getConnection();
                 Statement stmt = adminConn.createStatement()) {
                stmt.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + DRILL_DB + "' AND pid <> pg_backend_pid();");
                stmt.execute("DROP DATABASE IF EXISTS " + DRILL_DB + ";");
            } catch (Exception ignored) {
            }
        }
    }
}
