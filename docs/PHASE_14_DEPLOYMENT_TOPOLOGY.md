# Phase 14: Deployment Topology & Architecture Specification

## 1. Provider-Neutral Architecture

The Elégance Interior Designer Platform is architected for provider neutrality. No proprietary vendor features (e.g. AWS-specific IAM roles, Railway-specific configurations, or Vercel-locked Edge runtimes) are hardcoded into application source code.

```
                                 [PUBLIC INTERNET]
                                         |
                                         v
                         +-------------------------------+
                         |   Edge / CDN / Reverse Proxy  |
                         |   (TLS, HTTPS, HSTS, Port 443)|
                         +-------------------------------+
                                  |             |
                     /api/v1/*    |             |  /* (Public Routes & Assets)
                                  v             v
        +----------------------------+       +----------------------------+
        |   Backend Core API         | <---- |   Web Frontend Service     |
        |   (Spring Boot 3.4 / JRE21)|       |   (Next.js 16 Standalone)  |
        |   Port: 8080 (Internal)    |       |   Port: 3000 (Internal)    |
        +----------------------------+       +----------------------------+
              |              |
              |              +--------------------------+
              v                                         v
+----------------------------+            +----------------------------+
|   PostgreSQL Database      |            |   ClamAV Malware Daemon    |
|   (RLS Enforced, Port 5432)|            |   (TCP Stream, Port 3310)  |
+----------------------------+            +----------------------------+
              ^                                         ^
              |                                         |
     (Private VPC Link)                        (Private Container Net)
                                         
              +-----------------------------------------+
              |   Cloudflare R2 Object Storage          |
              |   (Private Masters & Public WebP CDN)   |
              +-----------------------------------------+
```

---

## 2. Service Definitions

### 2.1. Web Frontend Service (`web`)
- **Runtime:** Node.js 20 Alpine (`node:20-alpine`), unprivileged user `nextjs:nodejs` (UID 1001).
- **Output:** Next.js standalone server bundle (`apps/web/server.js`).
- **Internal Port:** `3000`.
- **Scaling:** Stateless; horizontally scalable across any number of replicas.
- **Backend Communication:** Server components query the backend via `INTERNAL_API_URL=http://api:8080/api/v1`.

### 2.2. Backend API Service (`api`)
- **Runtime:** Eclipse Temurin OpenJDK 21 JRE (`eclipse-temurin:21-jre-jammy`), unprivileged user `spring:spring`.
- **Internal Port:** `8080`. Servlet context path: `/api/v1`.
- **Scaling:** Stateless; horizontally scalable (>1 replica).
- **Session Model:** Spring Session JDBC backed by PostgreSQL `SPRING_SESSION` tables. Advisory locks prevent singleton job race conditions across multiple nodes.

### 2.3. Relational Database (`db`)
- **Engine:** PostgreSQL 18+ (Local baseline: 18.3, Container baseline: 18.6 `postgres:18-alpine`; native `uuidv7()` required for V001..V040 primary keys).
- **Connection Model:** HikariCP connection pool per replica.
- **Security:** 59 tenant tables protected with `FORCE ROW LEVEL SECURITY`.
- **Network Boundary:** Private subnet / VPC only; public access strictly disabled.

### 2.4. Object Storage Service (`storage`)
- **Provider:** Cloudflare R2 S3-Compatible Storage.
- **Topology:** Dual bucket architecture:
  - `S3_BUCKET_PRIVATE`: Master high-res images, quarantine buffers, and GDPR data exports.
  - `S3_BUCKET_PUBLIC`: Optimized, compressed WebP derivatives served via Cloudflare CDN.

### 2.5. Malware Scanning Daemon (`clamav`)
- **Engine:** `clamav/clamav:1.4.1` streaming daemon on port `3310`.
- **Network Boundary:** Isolated internal network reachable only by backend API service.

---

## 3. Network & Ingress Security

1. **HTTPS-Only Invariant:** All public traffic must terminate at edge TLS. HSTS header `max-age=31536000; includeSubDomains; preload` is injected.
2. **Reverse Proxy Headers:** The edge proxy forwards `X-Forwarded-For`, `X-Forwarded-Proto`, and `X-Forwarded-Host`. Spring Boot consumes these via `server.forward-headers-strategy=framework`.
3. **Cookie Security:** The authentication cookie `__Host-session` is strictly `Secure`, `HttpOnly`, `SameSite=Lax`, `Path=/`, and omits the `Domain` attribute, locking it to the canonical origin.
4. **CORS:** Disabled for relative unified-origin setups. In multi-origin deployments, `app.security.allowed-origins` enforces an explicit whitelist; wildcards (`*`) with credentials are strictly forbidden.
