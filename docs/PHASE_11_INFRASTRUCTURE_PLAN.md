# Elégance — Phase 11B.1 Infrastructure & Deployment Plan
**Document Status:** Complete / Source-Truth Corrected / Awaiting User Sign-Off  
**Phase:** Phase 11B.1 — Production Decision Source-Truth Correction  
**Date:** 2026-10-13  
**Classification:** Cloud Infrastructure Architecture & Operational Runbook  
**Release Candidate Baseline:** `5c3f41772304b9a5ad33dd43607d1d7e2bbd820f` (Flyway `V040`)  

---

## 1. Overview & Architectural Principles

This document defines the physical infrastructure architecture, containerization blueprints, database provisioning, storage lifecycle policies, network routing, and deployment execution sequences for the Elégance platform.

> **CRITICAL DIRECTIVE:** This plan is an operational specification. **NO LIVE PROVISIONING IS PERFORMED** during Phase 11B.1. All deployment steps await explicit user approval of the decisions in [docs/PHASE_11_FINAL_USER_APPROVAL_SHEET.md](file:///c:/my%20projects/interior%20design/docs/PHASE_11_FINAL_USER_APPROVAL_SHEET.md).

---

## 2. Containerization & Runtime Specifications

### 2.1 Backend API Container (`apps/api/Dockerfile`)
- **Base Image:** Eclipse Temurin 21 JRE Alpine / Debian Slim.
- **Build Method:** Multi-stage Maven build packaging Spring Boot fat JAR (`platform-api-0.1.0-SNAPSHOT.jar`).
- **Resource Allocation:**
  - Minimum: 1 vCPU, 1.5 GB RAM.
  - Recommended: 2 vCPU, 2.0 GB RAM.
  - JVM Flags: `-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError`.
- **Health & Readiness Endpoints:**
  - Liveness Probe: `GET http://localhost:8080/api/v1/actuator/health/liveness` (HTTP 200).
  - Readiness Probe: `GET http://localhost:8080/api/v1/actuator/health/readiness` (HTTP 200).
- **Graceful Shutdown:** Configured via `server.shutdown=graceful` (30-second drain window for active transactions).

### 2.2 Frontend Web Container (`apps/web/Dockerfile`)
- **Base Image:** Node.js 20-alpine.
- **Build Method:** Standalone output (`output: 'standalone'` in `next.config.js`).
- **Execution:** `node server.js` (minimal container footprint, omitting external `node_modules`).
- **Resource Allocation:**
  - Minimum: 0.5 vCPU, 512 MB RAM.
  - Recommended: 1 vCPU, 1 GB RAM.
- **Static Assets:** Hosted and cached via CDN with immutable cache headers (`_next/static`).

---

## 3. Network Architecture & Ingress Routing

```
 [ Public Internet Traffic ]
            │
            ▼
 ┌────────────────────────────────────────────────────────┐
 │            CLOUDFLARE EDGE PROXY / WAF / CDN           │
 │  - Automated TLS 1.3 Termination                       │
 │  - DDoS & Bot Mitigation                               │
 │  - Security Headers & Strict Origin Validation         │
 └──────┬───────────────────────────────────┬─────────────┘
        │                                   │
        │ https://<ROOT_DOMAIN>             │ https://api.<ROOT_DOMAIN>
        ▼                                   ▼
 ┌──────────────────────┐          ┌───────────────────────────────┐
 │   FRONTEND RUNNER    │          │      BACKEND API RUNNER       │
 │   (Next.js Standalone│          │     (Spring Boot Container)   │
 │    Node on Port 3000)│          │      Tomcat on Port 8080      │
 └──────────────────────┘          └───────┬───────────────┬───────┘
                                           │               │
                 ┌─────────────────────────┘               └──────────────────┐
                 ▼ (Private Network / TLS)                                    ▼ (HTTPS / S3 API)
   ┌──────────────────────────────┐                             ┌───────────────────────────────┐
   │  RAILWAY POSTGRESQL SERVICE  │                             │   S3-COMPATIBLE STORAGE (R2)  │
   │    - 40 Flyway Migrations    │                             │   - <BUCKET_PRIVATE>          │
   │    - 59 RLS Entity Tables    │                             │   - <BUCKET_PUBLIC>           │
   │    - Hikari Connection Pool  │                             │     (<MEDIA_DOMAIN>)          │
   └──────────────────────────────┘                             └───────────────────────────────┘
```

### Ingress & Domain Rules
1. **Frontend Origin:** `https://<ROOT_DOMAIN>` (and `https://www.<ROOT_DOMAIN>` redirecting to apex).
2. **Backend API Origin:** `https://api.<ROOT_DOMAIN>` (all endpoints prefixed with `/api/v1`).
3. **Media CDN:** `https://<MEDIA_DOMAIN>` (proxies clean derivative assets from the public S3 bucket).
4. **CORS Policy:** Strict origin restriction (`APP_SECURITY_ALLOWED_ORIGINS=https://<ROOT_DOMAIN>`). Wildcards and localhost are rejected at startup in production.
5. **Session Cookies:** `__Host-session` cookie configured with `Secure=true`, `HttpOnly=true`, `SameSite=Lax`, and `Domain` omitted (origin-locked).

---

## 4. Production Database Architecture & Runbook

### 4.1 PostgreSQL Engine Specifications
- **Target Engine:** PostgreSQL 17 on Railway PostgreSQL service (or Supabase PG 17).
- **SSL / TLS:** Mandatory encrypted connections (`sslmode=require` or `sslmode=verify-full`).
- **Connection Pooling:**
  - Application HikariCP: `maximum-pool-size=10`, `minimum-idle=2`, `idle-timeout=30000ms`, `max-lifetime=1200000ms`.
  - Infrastructure Pooler: PgBouncer / Supavisor in transaction pooling mode if backend scales horizontally.

### 4.2 Database Role Segregation
- **Admin / Migration Role (`interior_admin`):**
  - Owner of the database and schema `public`.
  - Used strictly for executing Flyway schema migrations `V001` through `V040`.
  - Permissions: `CREATE TABLE`, `CREATE INDEX`, `ALTER TABLE`.
- **Application Runtime Role (`interior_app`):**
  - Used by Spring Boot application in `SPRING_DATASOURCE_USERNAME`.
  - Permissions: `SELECT`, `INSERT`, `UPDATE`, `DELETE` on tables; `USAGE`, `SELECT` on sequences.
  - **Zero DDL privileges:** Cannot alter tables, drop tables, or modify schema constraints.
  - Subject to Row Level Security (`FORCE ROW LEVEL SECURITY`).

### 4.3 Database Deployment Sequence (Future Phase)
1. **Target Instance Provisioning:** Provision Railway PostgreSQL service (PostgreSQL 17).
2. **Backup & PITR Activation:** Explicitly enable scheduled backups and PITR continuous WAL archiving; verify both in provider dashboard before storing customer data.
3. **Execute Flyway Migrations:**
   ```bash
   mvnw.cmd flyway:migrate -Dflyway.url=jdbc:postgresql://<host>:5432/<db>?sslmode=require \
                           -Dflyway.user=interior_admin \
                           -Dflyway.password=<admin-password>
   ```
4. **Verify RLS Security:** Run SQL verification script:
   ```bash
   psql -h <host> -U interior_admin -d <db> -f scripts/verify-cloud-db-rls.sql
   ```
   *Requirement:* Exactly 59 tables must verify with `FORCE ROW LEVEL SECURITY = true`.
5. **Seed System Catalogs:** Confirm system subscription plan records (`STANDARD`, `PREMIUM`, `PRO`) exist in `system_subscription_plans`.

---

## 5. Object Storage Architecture & Lifecycle Policies

### 5.1 Dual Bucket Topology
1. **Private Bucket (`S3_BUCKET_PRIVATE`):**
   - **Access:** Private only (zero direct public HTTP access).
   - **Prefixes:**
     - `/originals/{studioId}/{projectId}/`: Master unedited customer uploads.
     - `/quarantine/{intentId}/`: Temporary holding area during malware scan.
     - `/privacy-exports/{exportId}/`: Encrypted user GDPR ZIP bundles.
     - `/ai-inputs/{jobId}/`: Customer room images submitted for AI visualizer processing.
2. **Public Derivative Bucket (`S3_BUCKET_PUBLIC`):**
   - **Access:** Public read via CDN (`https://<MEDIA_DOMAIN>`).
   - **Prefixes:**
     - `/derivatives/{studioId}/{projectId}/`: Scaled and optimized WebP/JPEG images.
     - `/cinematic/{studioId}/{projectId}/`: Optimized cinematic presentation keyframes.

### 5.2 Direct Upload Flow via Presigned URLs
```
 1. Browser ────────▶ POST /api/v1/media/upload-intent ────────▶ API
 2. Browser ◀──────── Presigned S3 PUT URL + Upload Token ◀───── API
 3. Browser ────────▶ PUT raw bytes directly to S3 Quarantine ──▶ S3
 4. Browser ────────▶ POST /api/v1/media/upload-commit ────────▶ API
 5. API checks scan ──▶ Scan Clean? ──▶ Move to Originals ─────▶ S3
```

### 5.3 S3 Operations Compatibility (Audit Finding)
The codebase uses only standard S3 operations in `S3StorageService.java`:
- `PutObject`
- `GetObject`
- `HeadObject`
- `CopyObject`
- `DeleteObject`
- Presigned `PUT` (`PutObjectPresignRequest`)

**R2 Compatibility Status:** **VERIFIED FOR ELÉGANCE CALL SET / REQUIRES DEPLOYMENT VALIDATION**. Cloudflare R2 supports this exact call set. Zero egress fees represent Cloudflare's pricing model, not an architectural guarantee.

### 5.4 Automated Storage Lifecycle Policies
- **Quarantine Expiration:** Automatically purge abandoned upload intents in `/quarantine/` older than **14 calendar days**.
- **Privacy Export Expiration:** Automatically delete exported data ZIP bundles in `/privacy-exports/` after **7 calendar days** (matching `app.privacy.export-ttl-days: 7`).
- **Infected Payloads:** Quarantine files flagged as `INFECTED` are immediately isolated and permanently destroyed after **14 days** of forensic logging.

---

## 6. Observability, Logging & Alerting

### 6.1 Spring Boot Actuator Configuration
- Exposed endpoints: `/actuator/health`, `/actuator/info`, `/actuator/metrics`.
- Liveness and Readiness probes active.
- Sensitive environment variables and configuration properties strictly hidden from `/actuator/env`.

### 6.2 Structured Logging & Correlation
- Standardized Logback JSON format with SLF4J.
- MDC context propagation: Every HTTP request is tagged with an RFC-compliant `X-Request-Id` UUID, propagated through service layers and outbound HTTP calls.
- Privacy Masking: PII, email addresses, passwords, and API tokens are masked or SHA-256 hashed in log outputs.

### 6.3 Critical Production Alert Thresholds
- **HTTP 5xx Error Spike:** Alert if 5xx rate > 1% over 5-minute rolling window.
- **Database Connection Pool Saturation:** Alert if HikariCP active connections > 80% capacity for > 2 minutes.
- **Unhealthy Probes:** Instant alert if `/actuator/health/readiness` fails 2 consecutive probes.
- **Malware Detections:** Alert platform security team on any `INFECTED` scan result.

---

## 7. Disaster Recovery & Emergency Operations

### 7.1 Emergency Maintenance Mode (`READ_ONLY`)
If a database or infrastructure emergency occurs:
1. Super Admin issues command:
   ```bash
   POST /api/v1/admin/operations/maintenance-mode
   Body: {"mode": "READ_ONLY"}
   ```
2. `MaintenanceModeInterceptor` immediately rejects all non-control-plane mutating requests (POST, PUT, PATCH, DELETE) with HTTP 503 `MAINTENANCE_MODE_ACTIVE`.
3. Read operations remain accessible, preventing platform data corruption.

### 7.2 Backup & Restoration Strategy
- **Prerequisite:** Scheduled database backups and continuous WAL archiving (PITR) must be **explicitly enabled and verified** prior to storing customer data.
- **SLA Reality:** Closed beta operates with **NO CONTRACTUAL SLA**. Commercial RPO and RTO remain **DECISION REQUIRED**.

---

## 8. Rollback Runbook (Future Operational Guidance)

### 8.1 Application Layer Rollback
- **Frontend:** Instant rollback to previous deployment tag via Vercel dashboard / CLI (`vercel rollback`).
- **Backend:** Rollback to previous container image tag in Railway dashboard / CLI (`railway rollback`).

### 8.2 Database Layer Rollback
- **Strict Rule:** **NO BLIND DOWN-MIGRATIONS.** Flyway migrations `V001` through `V040` are additive and backwards-compatible.
- **Corrupted Schema / Data Recovery:** In catastrophic failure, restore database from the pre-deployment snapshot or PITR point.
