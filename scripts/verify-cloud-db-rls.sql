-- ============================================================================
-- Elégance Interior Platform — Cloud PostgreSQL Diagnostics & RLS Verification
-- Purpose: Non-destructive verification of cloud database configuration,
--          extensions, Flyway schema history, RLS enforcement, and role attributes.
-- Safe: Queries catalog metadata only. Dumps ZERO user data.
-- ============================================================================

\echo '========================================================================'
\echo '  ELÉGANCE CLOUD POSTGRESQL READINESS & SECURITY VERIFICATION'
\echo '========================================================================'

-- 1. Database Version
\echo '--- 1. POSTGRESQL ENGINE VERSION ---'
SELECT version() AS postgresql_version;

-- 2. Current Connected Role Attributes
\echo '--- 2. CURRENT CONNECTED ROLE & PRIVILEGES ---'
SELECT 
    rolname AS connected_user,
    rolsuper AS is_superuser,
    rolcanlogin AS can_login,
    rolbypassrls AS can_bypass_rls,
    rolcreaterole AS can_create_roles,
    rolcreatedb AS can_create_db
FROM pg_roles 
WHERE rolname = current_user;

-- 3. Installed PostgreSQL Extensions
\echo '--- 3. INSTALLED EXTENSIONS ---'
SELECT 
    extname AS extension_name, 
    extversion AS installed_version 
FROM pg_extension 
ORDER BY extname;

-- 4. Flyway Schema History Baseline
\echo '--- 4. FLYWAY MIGRATION STATUS ---'
SELECT 
    installed_rank,
    version,
    description,
    type,
    installed_by,
    installed_on,
    execution_time AS execution_ms,
    success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;

-- 5. Row-Level Security (RLS) Status on Tenant Tables
\echo '--- 5. ROW-LEVEL SECURITY (RLS) STATUS (ALL TENANT TABLES) ---'
SELECT 
    tablename,
    rowsecurity AS rls_enabled,
    forcerowsecurity AS rls_forced
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename NOT IN ('flyway_schema_history')
ORDER BY tablename;

-- 6. Verification Summary Count
\echo '--- 6. SECURITY CONTROL AUDIT TOTALS ---'
SELECT 
    count(*) AS total_tables,
    count(*) FILTER (WHERE rowsecurity = true) AS rls_enabled_tables,
    count(*) FILTER (WHERE forcerowsecurity = true) AS rls_forced_tables,
    count(*) FILTER (WHERE rowsecurity = false OR forcerowsecurity = false) AS rls_missing_tables
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename NOT IN ('flyway_schema_history');

-- 7. Active Security Policies on Tenant Tables
\echo '--- 7. ACTIVE SECURITY POLICIES ---'
SELECT 
    tablename,
    policyname,
    permissive,
    roles,
    cmd AS command
FROM pg_policies
WHERE schemaname = 'public'
ORDER BY tablename, policyname;
