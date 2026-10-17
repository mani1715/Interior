# Generic Production Deployment Topology

## 1. Architectural Overview

The Elégance platform is structured around a decoupled, provider-neutral micro-service topology optimized for high availability, multi-tenant relational isolation, and secure asset processing.

```
+-----------------------------------------------------------------------------------+
|                                 INTERNET TRAFFIC                                  |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
                         +-------------------------------+
                         |   Edge / CDN / Reverse Proxy  |
                         |  (TLS Termination, Port 443)  |
                         +-------------------------------+
                                  |             |
                     /api/v1/*    |             |  /* (Pages & Assets)
                     (or rewrite) |             |
                                  v             v
        +----------------------------+       +----------------------------+
        |   Spring Boot 3.4 API      | <---- |   Next.js 16 Web Frontend  |
        |   (Java 21, Port 8080)     |       |   (Node 20/22, Port 3000)  |
        +----------------------------+       +----------------------------+
              |              |
              |              +--------------------------+
              v                                         v
+----------------------------+            +----------------------------+
|   PostgreSQL 16/17/18      |            |   ClamAV Daemon            |
|   (RLS Enforced, 5432)     |            |   (TCP Streaming, 3310)    |
+----------------------------+            +----------------------------+
              ^                                         ^
              |                                         |
     (Private VPC Link)                        (Private Internal Net)
                                         
              +-----------------------------------------+
              |   Cloudflare R2 Object Storage          |
              |   (Private Originals & Public WebP CDN) |
              +-----------------------------------------+
```

---

## 2. Component Separation & Contracts

### 2.1. Edge / Ingress Reverse Proxy
- **Role:** Handles TLS termination (HTTPS only), HSTS header injection (`max-age=31536000; includeSubDomains; preload`), HTTP/2 or HTTP/3 multiplexing, and routing.
- **Header Forwarding:** Ingress must pass standard proxy headers (`X-Forwarded-For`, `X-Forwarded-Proto`, `X-Forwarded-Host`) to downstream containers.
- **Unified-Origin Model:** Next.js proxies or rewrites `/api/v1/*` to the Spring Boot container. This eliminates CORS preflight overhead in production and allows strict `__Host-session` cookie isolation.

### 2.2. Web Frontend Service (`web`)
- **Technology:** Next.js 16.3.8 running in standalone output mode.
- **Port:** 3000 (HTTP internal).
- **Execution Model:** Unprivileged non-root user `nextjs:nodejs` (UID 1001).
- **Statelessness:** Completely stateless. Server components communicate with backend API via `INTERNAL_API_URL=http://api:8080/api/v1`.

### 2.3. Backend Core API Service (`api`)
- **Technology:** Spring Boot 3.4.3 / Java 21 LTS (Eclipse Temurin JRE).
- **Port:** 8080 (HTTP internal). Context path: `/api/v1`.
- **Execution Model:** Unprivileged non-root user `spring:spring` (system user).
- **Statelessness & Replicas:** Stateless horizontal scaling supported (>1 replica). Database-backed session store and PostgreSQL advisory locks eliminate sticky-session requirements.

### 2.4. Primary Database (`db`)
- **Technology:** PostgreSQL 18+ (Local: 18.3, Compose: 18.6 `postgres:18-alpine`; native `uuidv7()` required for V001..V040 primary keys).
- **Security:** Strict Row-Level Security (RLS) forced across 59 tenant tables with `tenant_context.current_studio_id`.
- **Connection Model:** HikariCP connection pool managed per backend replica.

### 2.5. Object Storage (`storage`)
- **Technology:** Cloudflare R2 S3-Compatible storage.
- **Buckets:** Dedicated private bucket for uploaded originals, dedicated public bucket/CDN for transformed responsive derivatives.
- **Persistence:** Container disks are strictly ephemeral (`/tmp` only). Zero media is stored on local container disk.

### 2.6. Malware Scanner Daemon (`clamav`)
- **Technology:** ClamAV streaming daemon (TCP port 3310).
- **Network Isolation:** Private internal container network only; never exposed publicly.
- **Fail-Closed Policy:** In production, uploads require a verified clean scan (`REQUIRE_CLEAN_SCAN=true`).
