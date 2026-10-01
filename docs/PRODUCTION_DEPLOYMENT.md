# Production Deployment & Infrastructure Architecture
## Canonical Launch Architecture & Operations Runbook

**Current Baseline:** Phase 29 Complete & Verified  
**Architecture Type:** Modular Monolith (Stateless Next.js 16 + Stateless Spring Boot 3.4 + Managed PostgreSQL 16 + Cloudflare R2 / CDN)

---

## 1. Executive Summary & Topology

The Interior Design Platform is designed as an operationally lean, secure, and cost-effective **Modular Monolith**. It avoids the operational tax of microservices, Kubernetes, Redis, or Kafka for launch, while ensuring strict defense-in-depth, horizontal compute scalability, and complete isolation of compute from persistent state and media.

```
                    ┌───────────────────────────────────────────────┐
                    │            Cloudflare Global Edge             │
                    │  - Global Anycast DNS                         │
                    │  - Automated TLS / HTTPS Termination          │
                    │  - DDoS Protection & Web Application Firewall │
                    └───────┬───────────────────────────────┬───────┘
                            │                               │
             https://interior.com                           │ https://media.interior.com
             https://interior.com/api/v1/*                  │
                            │                               │
                ┌───────────▼───────────┐         ┌─────────▼─────────┐
                │  Render Edge Ingress  │         │   Cloudflare R2   │
                │  (Reverse Proxy / SSL)│         │   Public Bucket   │
                └─────┬───────────┬─────┘         │ (WebP Derivatives)│
                      │           │               └───────────────────┘
         Static/HTML  │           │  /api/v1/*
                      │           │
         ┌────────────▼───┐   ┌───▼────────────┐
         │ Next.js 16 Web │   │Spring Boot API │
         │ (Node 20 / 3000│   │(Temurin 21/8080│
         │ Standalone JIT)│   │Stateless Java) │
         └────────────────┘   └───┬────────────┘
                                  │
                                  │ JDBC / SSL (HikariCP)
                                  │ Private VPC
                                  │
                      ┌───────────▼───────────┐
                      │  Managed PostgreSQL   │
                      │  - Version 16+        │
                      │  - UUIDv7 Primary Keys│
                      │  - Row-Level Security │
                      │  - Automated Backups  │
                      └───────────────────────┘
                                  ▲
                                  │ IAM S3 API
                      ┌───────────┴───────────┐
                      │     Cloudflare R2     │
                      │    Private Bucket     │
                      │  (Originals, AI Refs, │
                      │   Verification PDFs)  │
                      └───────────────────────┘
```

---

## 2. URL & Domain Architecture

### Topology Comparison

#### Option A: Unified Domain (`https://interior.com` + `https://interior.com/api/v1/*`) — **RECOMMENDED**
- **Routing:** Edge router (Cloudflare or Render Ingress) routes `/api/v1/*` to the Spring Boot backend service, and all other paths to the Next.js frontend service.
- **Security Advantages:**
  - **`__Host-` Cookie Standard:** RFC 6265bis compliant. Auth session cookies (`__Host-session`) can enforce `Path=/`, `Secure`, and NO domain attribute, preventing subdomain hijacking or cookie leaking.
  - **Zero CORS Preflight:** Eliminates browser `OPTIONS` preflight requests for every API mutation. Drastically reduces latency for mobile users in India.
  - **Double-Submit CSRF:** Same-origin CSRF tokens (`XSRF-TOKEN`) work natively without cross-subdomain cookie relaxation.
  - **Unified SSL Certificate:** A single apex/www certificate covers the entire application surface.

#### Option B: Separate Subdomains (`https://interior.com` + `https://api.interior.com`) — **FALLBACK**
- Used if hosting backend and frontend on separate platforms without a unified edge reverse proxy.
- **Trade-offs:** Requires CORS headers (`Access-Control-Allow-Credentials: true`), preflight latency, and relaxes `__Host-` cookie prefixes to standard domain cookies (`domain=.interior.com`).

**Decision:** **Option A** is the primary production architecture. The Next.js `next.config.js` includes built-in fallback proxy rewrites via `INTERNAL_API_URL` if an edge proxy is not configured.

---

## 3. Recommended Hosting Architecture

### Primary Recommendation: Render + Cloudflare
- **Frontend Compute:** Render Web Service running `apps/web/Dockerfile` (Node 20 Alpine, standalone Next.js 16).
- **Backend Compute:** Render Web Service running `apps/api/Dockerfile` (Eclipse Temurin 21 JRE, Spring Boot 3.4).
- **Database:** Render Managed PostgreSQL 16 (automated daily backups, enforced SSL, private VPC networking).
- **Object Storage:** Cloudflare R2 (S3-compatible, zero egress fees, worldwide distribution).
- **CDN:** Cloudflare CDN (global edge caching, edge SSL termination, DDoS protection).
- **DNS & TLS:** Cloudflare Managed DNS with automated Let's Encrypt / Cloudflare Edge TLS.

### Why This Combination?
1. **Low Operational Overhead:** No VPC peering headaches, no Kubernetes cluster management, zero YAML configuration drift.
2. **Predictable Cost:** Starts at ~$25–$45/month with zero surprise egress bandwidth fees (Cloudflare R2 has $0 egress).
3. **Region Latency:** Render Singapore or Frankfurt regions provide low latency to Indian and European traffic.
4. **Git-Driven Continuous Delivery:** Automatic container rebuilds upon pushing to `main`.

### Secondary Alternative: AWS (ECS Fargate + RDS PostgreSQL + S3 + CloudFront)
- Ideal when enterprise SOC2 compliance, dedicated AWS PrivateLink, or enterprise client procurement mandates AWS.
- Baseline cost: ~$70–$120/month.

---

## 4. Compute vs. Media Storage Separation

**Strict Invariant:** Compute containers are 100% stateless and ephemeral. No media, uploaded images, or client documents are ever stored on container disks in production.

### Media Layout:
1. **Private Bucket (`interior-platform-media-private`):**
   - Strictly private. No public read access. Accessible only via backend API credentials.
   - Keys:
     - `pending/{studioId}/{uploadIntentId}/{mediaAssetId}.ext` (quarantine uploads)
     - `studio/{studioId}/projects/{projectId}/original/{mediaAssetId}.ext` (high-res originals)
     - `ai/references/{studioId}/{generationId}.ext` (client style references)
     - `ai/generations/{studioId}/{generationId}.ext` (AI renders before publication)
     - `verification/{studioId}/{documentId}.pdf` (business registration proof documents)
2. **Public Bucket / CDN Prefix (`interior-platform-media-public`):**
   - Read-only public bucket mapped to `https://media.interior.com`.
   - Keys:
     - `public/studio/{studioId}/projects/{projectId}/derivatives/{variant}_{mediaAssetId}.webp`
   - Cache headers emitted by CDN: `Cache-Control: public, max-age=31536000, immutable`.
   - Only optimized derivatives (WebP/JPEG, watermarked or resized) are placed here.

---

## 5. Database Architecture

### Requirements & Configuration
- **Engine:** PostgreSQL 16 (Render Managed PostgreSQL).
- **Region:** Singapore (`singapore`) or Frankfurt (`frankfurt`) — must match the backend compute service region.
- **Recommended Launch Tier:** Starter Tier (1 vCPU, 1 GB RAM, 10–25 GB SSD storage) — adequate for initial production traffic without over-provisioning costs.
- **Primary Keys:** RFC 9562 UUIDv7 generated application-side (`java.util.UUID` with time-ordered monotonic bits).
- **Connection Security:** `sslmode=require` mandatory in production JDBC URL.
- **Connection Strings:**
  - **Internal Connection String (Render Private Network):** `jdbc:postgresql://dpg-<id>-a:5432/interiordb?sslmode=require` — used by the Spring Boot backend service for zero-latency, private intra-datacenter communication without routing over the public internet.
  - **External Connection String:** `jdbc:postgresql://dpg-<id>-a.<region>-postgres.render.com:5432/interiordb?sslmode=require` — used only for administrator ad-hoc migrations or schema audits.
- **Connection Pooling:** Spring Boot HikariCP configured in `application-production.properties`:
  - `maximum-pool-size`: 10 (keeps total pool well within managed DB connection limits).
  - `minimum-idle`: 2.
  - `idle-timeout`: 30,000ms.
  - `connection-timeout`: 20,000ms.
  - `max-lifetime`: 1,200,000ms (20 minutes).
- **Row-Level Security (RLS):** Enabled and enforced across all tenant tables (`designer_studios`, `projects`, `leads`, `client_invites`, `studio_reviews`, `billing_subscriptions`). Non-admin queries require `SET LOCAL app.current_studio_id = '<studio_uuid>'`.
- **Database Credential Model:**
  - The runtime application user possesses standard DML (`SELECT`, `INSERT`, `UPDATE`, `DELETE`) privileges.
  - In hardened enterprise environments with role separation, a DDL migration user runs Flyway, and the runtime `interior_app_user` has zero DDL privileges.
  - **Critical Invariant:** The runtime application role must NEVER be granted `SUPERUSER` or `BYPASSRLS`.

### Cloudflare R2 Storage Provisioning Specifications
- **S3 API Endpoint:** `https://<account_id>.r2.cloudflarestorage.com`
- **Region:** `auto`
- **Private Bucket (`interior-platform-media-private`):**
  - Public Access: **Disabled** (Strictly private).
  - Object Versioning: **Enabled** (protects originals against accidental deletion or corruption).
  - Lifecycle Policy: Auto-delete objects under `pending/**` older than 7 days (cleans up aborted quarantine uploads).
  - CORS Configuration (for direct browser presigned uploads):
    ```json
    [
      {
        "AllowedOrigins": ["https://interior.com", "http://localhost:3000"],
        "AllowedMethods": ["PUT"],
        "AllowedHeaders": ["Content-Type", "Content-Length"],
        "MaxAgeSeconds": 3600
      }
    ]
    ```
- **Public Bucket (`interior-platform-media-public`):**
  - Public Access: **Enabled** (or mapped to custom domain `https://media.interior.com`).
  - Directory Listing: **Disabled** (only known hash paths can be fetched).
  - Content: Strictly public derivatives generated by the backend image processing pipeline.
- **API Token Permissions:** Scoped exclusively to Read & Write on `interior-platform-media-private` and `interior-platform-media-public`.

### Migration Strategy (Flyway)
- Migrations are sequential and forward-only (`V001` through `V023`).
- **Execution:** Flyway runs automatically on backend application startup.
- **Concurrency Safety:** Flyway uses PostgreSQL table-level locking (`pg_advisory_lock` / `flyway_schema_history` table lock), guaranteeing that multiple backend instances starting concurrently cannot race or corrupt schema migrations.
- **Failure Policy:** If a migration fails, the backend JVM aborts launch immediately. Traffic is never routed to an unmigrated or corrupted database state.
- **Rollback Policy:** In production, schema rollbacks are strictly handled via new forward migrations (`V024__...`).

---

## 6. Backup & Disaster Recovery

### PostgreSQL Database:
- **Automated Snapshots:** Daily snapshots at 02:00 UTC, retained for 30 days.
- **Point-in-Time Recovery (PITR):** WAL archiving enabled on managed database (RPO < 15 minutes, RTO < 60 minutes).
- **Pre-Deployment Backup Command:**
  ```bash
  pg_dump -h <db_host> -U <db_user> -Fc -d interiordb -f "backup_pre_deploy_$(date +%Y%m%d_%H%M%S).dump"
  ```

### Media Assets:
- Cloudflare R2 / AWS S3 Object Versioning enabled on `interior-platform-media-private`.
- Accidental delete protection enabled via bucket lifecycle policies.
- 30-day auto-expiry lifecycle rule on `pending/**` quarantine directory.

---

## 7. Secret Management & Environment Variables

All production secrets must be injected via the hosting platform's secure environment manager (Render Secret Environment Variables or AWS Secrets Manager). **Never commit secrets to git or Docker images.**

### Environment Matrix (See `.env.example`)

| Variable | Category | Visibility | Description / Value |
|---|---|---|---|
| `NEXT_PUBLIC_APP_URL` | Frontend | Public (Browser) | `https://interior.com` |
| `NEXT_PUBLIC_API_BASE_URL` | Frontend | Public (Browser) | `https://interior.com/api/v1` |
| `NODE_ENV` | Frontend | Server-Only | `production` |
| `PORT` | Frontend | Server-Only | `3000` |
| `INTERNAL_API_URL` | Frontend | Server-Only | `http://platform-api:8080/api/v1` |
| `SPRING_PROFILES_ACTIVE` | Backend | Server-Only | `production` |
| `SERVER_PORT` | Backend | Server-Only | `8080` |
| `SERVER_FORWARD_HEADERS_STRATEGY` | Backend | Server-Only | `framework` |
| `SPRING_DATASOURCE_URL` | Backend | Server-Only | `jdbc:postgresql://<host>:5432/<db>?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Backend | Secret | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Backend | Secret | High-entropy database password |
| `APP_SECURITY_DEV_AUTH_ENABLED` | Backend | Server-Only | `false` (**Mandatory**) |
| `APP_SECURITY_SESSION_COOKIE_SECURE`| Backend | Server-Only | `true` (**Mandatory**) |
| `APP_SECURITY_ALLOWED_ORIGINS` | Backend | Server-Only | `https://interior.com` |

---

## 8. Reverse Proxy, Forwarded Headers & TLS

1. **Proxy Headers:** `server.forward-headers-strategy=framework` is enabled. Spring Boot automatically processes `X-Forwarded-Proto`, `X-Forwarded-Host`, and `X-Forwarded-For`.
2. **HTTPS Detection:** `request.isSecure()` detects HTTPS through edge SSL termination, ensuring `Strict-Transport-Security` headers and `Secure` cookie attributes are correctly applied.
3. **Security Headers Filter:** Emits:
   - `Content-Security-Policy`: Disallows unsafe object loading; permits Next.js scripts and remote HTTPS images.
   - `X-Frame-Options: DENY`
   - `X-Content-Type-Options: nosniff`
   - `Referrer-Policy: strict-origin-when-cross-origin`
   - `Strict-Transport-Security: max-age=31536000; includeSubDomains` (emitted on secure requests only).

---

## 9. Container Resource Planning

| Service | Minimum Spec (Launch) | Recommended Spec (Traffic) | Sizing Rationale |
|---|---|---|---|
| **Next.js Frontend** | 0.5 vCPU, 512 MB RAM | 1.0 vCPU, 1 GB RAM | Standalone JIT output with React Server Components. Low memory footprint. |
| **Spring Boot API** | 0.5 vCPU, 1 GB RAM | 1.0 vCPU, 2 GB RAM | JVM container sizing flags: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`. |
| **PostgreSQL 16** | 1.0 vCPU, 2 GB RAM, 20 GB SSD | 2.0 vCPU, 4 GB RAM, 50 GB SSD | Accommodates GIN trigram indexes and concurrent Hikari connection pool. |

---

## 10. Horizontal Scaling Strategy

- **Stateless Compute:** Both frontend and backend are completely stateless.
- **Session Scalability:** Sessions are persisted in the PostgreSQL `identity_sessions` table with SHA-256 token hashing. Multiple backend instances can validate sessions without Redis session-affinity or sticky sessions.
- **In-Memory Rate Limiting:** Sliding-window buckets run per-instance; provides adequate protection at single/dual instance scale. For high multi-region scale, rate limits can be migrated to edge WAF rules (Cloudflare Rate Limiting).

---

## 11. Step-by-Step Production Deployment Sequence

Follow this exact order during initial production provisioning:

```
[1. Provision Database] ──> [2. Set Secrets] ──> [3. Deploy Backend] ──> [4. Verify Backend UP]
                                                                                  │
[8. Smoke Test] <── [7. Configure Storage/CDN] <── [6. Configure DNS/SSL] <── [5. Deploy Frontend]
```

1. **Provision Managed PostgreSQL 16:**
   - Create database `interiordb`.
   - Enable SSL enforcement (`sslmode=require`).
   - Create restricted application user `interior_app_user`.
2. **Configure Environment Secrets:**
   - Add backend environment variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_PASSWORD`, `APP_SECURITY_ALLOWED_ORIGINS`, etc.).
   - Ensure `APP_SECURITY_DEV_AUTH_ENABLED=false` and `APP_SECURITY_SESSION_COOKIE_SECURE=true`.
3. **Deploy Backend Service:**
   - Build container from `apps/api/Dockerfile`.
   - On container startup, Flyway validates and applies migrations `V001` through `V023`.
   - `ProductionConfigurationValidator` asserts security parameters.
4. **Verify Backend Health:**
   - Probe liveness: `GET /api/v1/actuator/health/liveness` -> `{"status":"UP"}`.
   - Probe readiness: `GET /api/v1/actuator/health/readiness` -> `{"status":"UP"}`.
5. **Deploy Frontend Service:**
   - Set frontend environment variables (`NEXT_PUBLIC_APP_URL`, `NEXT_PUBLIC_API_BASE_URL`).
   - Build container from `apps/web/Dockerfile`.
   - Probe frontend health: `GET /health` -> `{"status":"healthy"}`.
6. **Configure Domain & DNS:**
   - Point apex `interior.com` and `www.interior.com` CNAME/A records to Cloudflare / Render ingress.
   - Enable automated SSL certificate provisioning.
7. **Configure Cloudflare R2 / CDN:**
   - Create `interior-platform-media-private` (private) and `interior-platform-media-public` (public).
   - Route `media.interior.com` to public bucket with 1-year caching rules.
8. **Run Launch Smoke Test:**
   - Execute verification checklist below.

---

## 12. Production Launch Smoke-Test Checklist

Verify each of the following endpoints and workflows in the live environment:

- [ ] **Public Homepage (`GET /`):** Page renders with semantic H1, brand hero, 2.5D scroll animation, and 0 console errors.
- [ ] **Interior Showcase (`GET /interior-journey`):** 5 approved interior scenes render in order with accessible text.
- [ ] **Public Discovery (`GET /projects`):** Catalog renders with project cards, filter chips, and GIN trigram search.
- [ ] **Public Professional Directory (`GET /professionals`):** Studio listings render with location and specialty tags.
- [ ] **Health Probes:**
  - `GET /health` -> HTTP 200 `{"status":"healthy"}`.
  - `GET /api/v1/actuator/health/liveness` -> HTTP 200 `{"status":"UP"}`.
  - `GET /api/v1/actuator/health/readiness` -> HTTP 200 `{"status":"UP"}`.
- [ ] **Authentication Boundary:**
  - Visiting `/sign-in` displays sign-in screen.
  - Attempting to access `/workspace` without authentication redirects to `/sign-in`.
  - Attempting to access `/admin` without admin role displays Access Denied boundary.
- [ ] **Database & RLS Integrity:**
  - Verify tenant queries enforce `app.current_studio_id`.
  - Verify public reviews query only returns `status = 'PUBLISHED'`.
- [ ] **Disabled Provider Truthfulness:**
  - Billing workspace `/workspace/billing` shows commercial checkout not configured (`DisabledBillingProvider`).
  - WhatsApp lead handoff shows direct WhatsApp link (`wa.me`) without claiming managed cloud API.
  - Verification badge shows only for verified studios.
- [ ] **Mobile Responsiveness:**
  - Test on viewport 390px (iPhone) and 768px (iPad).
  - Verify zero horizontal scrolling (`overflow-x: hidden`).
  - Touch targets $\ge 44\text{px}$.
- [ ] **Security Headers:**
  - Inspect response headers on `https://interior.com`:
    - `Strict-Transport-Security: max-age=31536000; includeSubDomains`
    - `X-Frame-Options: DENY`
    - `X-Content-Type-Options: nosniff`
    - `Content-Security-Policy` active.

---

## 13. External Actions Still Required Before Launch

The following items must be provisioned or configured in third-party provider dashboards (cannot be completed via code):

1. **Domain Registration & DNS:** Purchase domain (e.g. `interior.com`) and point nameservers to Cloudflare.
2. **Cloudflare Account:** Set up Cloudflare zone, enable Full (Strict) SSL, and configure edge caching rules.
3. **Cloudflare R2 Buckets:** Create private bucket (`interior-platform-media-private`) and public bucket (`interior-platform-media-public`).
4. **Render Account & Project:** Create Render team/account and link GitHub repository `https://github.com/mani1715/Interior.git`.
5. **Render Managed PostgreSQL:** Provision PostgreSQL 16 database instance in Singapore or Frankfurt region.
6. **Set Environment Secrets:** Inject all secrets listed in Section 7 into the Render dashboard.
