# Database Release Specification & Connection Budget

## 1. Engine & Version Compatibility

- **Engine:** PostgreSQL Relational Database Management System.
- **Minimum Supported Version:** PostgreSQL 18+ (native `uuidv7()` in `pg_catalog` required for all primary keys in V001..V040).
- **Validated Release Baseline:** PostgreSQL 18.3 (local Windows baseline) and PostgreSQL 18.6 (`postgres:18-alpine` Docker Compose baseline).
- **Earlier Versions (PG 15–17):** Require manual pre-installation of an explicit `uuidv7` polyfill extension/function before executing Flyway migrations.
- **Connection Protocol:** JDBC with mandatory TLS encryption in production (`sslmode=verify-full` or `sslmode=require`).
- **AWS RDS Global Bundle:** The backend container includes the AWS RDS Global CA Truststore (`aws-rds-global-bundle.crt`) for strict certificate verification.

---

## 2. Flyway Migration Release Contract

- **Canonical Migrations:** V001 through V040 (40 versioned SQL migrations).
- **Migration Strategy:**
  - In production, Flyway migrations are executed automatically during backend container startup or via a single release init-job.
  - **Migration Lock:** Flyway automatically acquires a PostgreSQL advisory lock on `flyway_schema_history` (`pg_advisory_lock`), preventing concurrent multi-replica migration races or schema corruption.
  - **Startup Invariants:** Production startup MUST NEVER execute `flyway.repair()`, `flyway.clean()`, or drop existing tables.
  - `spring.flyway.validate-on-migrate=true`: Confirms all applied migration checksums match repository source migrations before booting.

---

## 3. Connection Budgeting Formula

Production connection pool sizing must strictly respect the managed database capacity:

$$\text{Total Connections} = (N_{\text{replicas}} \times \text{maximum-pool-size}) + \text{Reserve}_{\text{admin/migrations}}$$

### Default Sizing Guidelines:
- **Default `maximum-pool-size` per replica:** 10
- **Default `minimum-idle` per replica:** 2
- **Idle Timeout:** 30,000 ms (30s)
- **Connection Timeout:** 20,000 ms (20s)
- **Max Lifetime:** 1,200,000 ms (20 min)
- **Leak Detection Threshold:** 30,000 ms (30s)

### Sizing Example:
For a managed database tier with `max_connections = 100`:
- Reserve for PostgreSQL superuser, monitoring, and administrative CLI: 15 connections.
- Available for application workload: 85 connections.
- Sizing for 4 backend replicas: $4 \times 20 = 80 \le 85$ connections.
- Configurable at runtime via `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` without code modification.

---

## 4. Multi-Tenant Row-Level Security (RLS)

- **RLS Enforced Tables:** 59 tenant tables have `FORCE ROW LEVEL SECURITY` enabled.
- **Tenant Isolation Policy:** Every query operates under a session-bound variable `tenant_context.current_studio_id`. Queries attempting to cross tenant boundaries are blocked at the PostgreSQL engine level regardless of application bugs.
- **Bypass Role:** Only migrations and administrative maintenance operate under the table owner role.
