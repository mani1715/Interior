# Phase 12B / Phase 15 Runbook: Managed PostgreSQL Live Cloud Activation

## 1. Overview

This runbook guides engineers through live validation of a managed cloud PostgreSQL database during **Phase 15 (Real Provider Activation)**, approximately 3 months before public market release.

---

## 2. Managed Provider Provisioning Guidelines

### 2.1 Supported Provider Candidates
- **Railway PostgreSQL**: High developer ergonomics, built-in SSL and automated snapshots.
- **AWS RDS PostgreSQL 17**: Standard enterprise choice with Multi-AZ high availability and automated PITR.
- **Supabase PostgreSQL 17**: Pre-configured RLS support and SSL.
- **Neon Serverless PostgreSQL**: Autoscaling compute and branching support.

### 2.2 Provisioning Requirements
1. **Engine Version**: PostgreSQL 17 (recommended; 15.x–18.x supported).
2. **Encryption in Transit**: SSL mode MUST be enabled (`sslmode=require`).
3. **Database Roles**:
   - `postgres` (or provider admin role): Used solely for Flyway schema migrations and DDL.
   - `app_user` (least privilege): Application runtime role with `NOSUPERUSER`, `NOBYPASSRLS`.
4. **Collation / Encoding**: `UTF-8` character encoding with standard collation.

---

## 3. Running Live Validation (Phase 15)

### 3.1 Environment Setup (Outside Git)
In a secure PowerShell session:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://<HOST>:<PORT>/<DATABASE>?sslmode=require"
$env:DB_USER = "<DB_USER>"
$env:DB_PASSWORD = "<DB_PASSWORD>"
$env:DB_SSL_REQUIRED = "true"

powershell -ExecutionPolicy Bypass -File scripts/verify-managed-postgres-integration.ps1 -RequireLive
```

### 3.2 Automated Validation Checks Performed
When executed with `-RequireLive`, the harness runs:
1. **Context Verification**: Ensures git baseline is clean and verified.
2. **SSL Handshake**: Confirms connection refuses plaintext traffic.
3. **Flyway Migrations**: Applies all 40 migrations (V001..V040) from scratch or baseline.
4. **Catalog Inspection**: Asserts all 59 tenant tables have `rowsecurity=true` and `forcerowsecurity=true`.
5. **Multi-Tenant RLS Integration Suite**: Runs 14 test classes to verify strict boundary isolation between studios, customers, and administrators.
6. **Connection Pool Test**: Validates HikariCP acquires and releases connections under concurrency without leaks.

---

## 4. Troubleshooting Cloud Database Errors

| Error | Root Cause | Remediation |
| :--- | :--- | :--- |
| `FATAL: no pg_hba.conf entry for host ..., SSL off` | Connection string missing SSL | Ensure `?sslmode=require` is appended to JDBC URL. |
| `FlywayException: Schema "public" is not empty` | Pre-existing tables in cloud DB | Run `flyway.baseline()` or provision a fresh empty database. |
| `PSQLException: permission denied for table ...` | Missing grants on schema public | Grant `USAGE` on schema public and table CRUD permissions to application role. |
| `PSQLException: ... bypassrls` | Role has BYPASSRLS attribute | Ensure application role has `NOBYPASSRLS`. |
