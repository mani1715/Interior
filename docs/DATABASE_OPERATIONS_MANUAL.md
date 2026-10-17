# ELÉGANCE — DATABASE OPERATIONS MANUAL
## Managed PostgreSQL 18 on Neon
**Target Platform:** Elégance Interior Designer Platform  
**Engine:** PostgreSQL 18.x  
**Provider:** Neon Serverless PostgreSQL  
**Document Revision:** 1.0 (Phase 15A Release)  

---

## 1. System Overview & Architecture

The Elégance platform database layer uses Neon Serverless PostgreSQL running PostgreSQL 18.x with native `uuidv7()` support. Neon decouples compute from storage, enabling autoscaling and point-in-time recovery (PITR).

```
                      +-----------------------------------+
                      |   Spring Boot Backend Service     |
                      |   (HikariCP Pool: size 10-20)     |
                      +-----------------+-----------------+
                                        |
                 Direct TLS 1.3         |  Port 5432
                 (Migration/Advisory)   v
                      +-----------------------------------+
                      |  Neon Compute Endpoint (Direct)   |
                      |  ep-crimson-fog-b3t603ml...       |
                      +-----------------+-----------------+
                                        |
                                        v
                      +-----------------------------------+
                      |   Neon Cloud Storage Engine       |
                      |   (LSS / Page Servers / S3)       |
                      +-----------------------------------+
```

---

## 2. Connection Endpoints & Routing Policy

| Endpoint Type | Host Pattern | Primary Use Case | Sizing & Pooling |
| :--- | :--- | :--- | :--- |
| **Direct Host** | `ep-<project-id>.c-4.<region>.aws.neon.tech` | Spring Boot API, Flyway Migrations, Advisory Locks | Managed by HikariCP (Max 20 connections) |
| **Pooled Host** | `ep-<project-id>-pooler.c-4.<region>.aws.neon.tech` | Edge Lambdas, Webhooks, Serverless Functions | Managed by Neon PgBouncer (Transaction mode) |

### Invariant:
**Never run Flyway migrations or acquire session-level advisory locks through the pooled endpoint.** PgBouncer in transaction mode assigns subsequent statements to different server backends, breaking migration locks.

---

## 3. Connection Pool Configuration (HikariCP)

For the primary Spring Boot backend runtime, production settings in `application-production.properties`:

```properties
# Primary Datasource Configuration
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# HikariCP Production Pool Settings
spring.datasource.hikari.pool-name=InteriorPlatformHikariPool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.max-lifetime=1200000
spring.datasource.hikari.leak-detection-threshold=60000
spring.datasource.hikari.connection-test-query=SELECT 1
```

---

## 4. Database Schema Migration Procedure (Flyway)

1. **Local Development:**
   - Write immutable forward-only SQL migration scripts named `V<NNN>__<description>.sql`.
   - Test against local PostgreSQL 18 container.
2. **Pre-Deployment Check:**
   - Validate checksums and syntax.
   - Verify non-superuser role compatibility (avoid ungranted role alterations).
3. **Deployment Execution:**
   - Spring Boot executes Flyway migrations automatically upon container startup.
   - Migrations run in an isolated transaction per script.
4. **Post-Deployment Verification:**
   - Check `/actuator/health` endpoint for `"status": "UP"`.
   - Query `flyway_schema_history` to verify successful installation.

---

## 5. Security & Access Control Policies

1. **TLS Enforcement:** All client connections must require TLS 1.3 (`sslmode=require&channel_binding=require`).
2. **Row-Level Security (RLS):** All 59 tenant-facing tables have `FORCE ROW LEVEL SECURITY` enabled. Queries must set `app.current_studio_id` in their transaction context.
3. **Master User Separation:**
   - Master user (`neondb_owner`) is reserved for administrative tasks and schema migrations.
   - Application runtime roles should have `NOBYPASSRLS` to enforce RLS boundaries.
4. **Credential Rotation:**
   - Neon passwords can be rotated from the Neon CLI or web console.
   - Zero credentials in code: all connection strings are injected via runtime environment variables.

---

## 6. Routine Maintenance & Monitoring

- **Connection Counts:** Monitor active connections against the 901 connection limit.
- **Compute Autoscaling:** Neon compute suspends automatically when idle (if scale-to-zero is enabled) and resumes in < 1 second.
- **Storage Metrics:** Neon automatically expands storage as data volume grows.
- **Log Inspection:** Access Postgres engine logs through the Neon Console or via `neon logs`.
