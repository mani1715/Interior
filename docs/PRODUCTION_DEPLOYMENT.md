# Production Deployment & Infrastructure Architecture
## Canonical Launch Architecture & Operations Runbook

**Current Baseline:** Phase 29 Complete & Verified  
**Target Architecture:** Hostinger VPS (Docker Compose + Caddy) + AWS RDS PostgreSQL 16 + Cloudflare R2

---

## 1. Executive Summary & Topology

The Interior Design Platform is deployed as a secure, containerized **Modular Monolith** running on a **Hostinger VPS** managed via **Docker Compose**, with state persisted in a dedicated **AWS RDS for PostgreSQL 16** managed instance and media stored in **Cloudflare R2**.

This architecture provides dedicated compute resources, predictable VPS hosting costs, enterprise-grade database management with automated backups/PITR, zero egress bandwidth costs for media, and unified-origin routing via a Caddy reverse proxy.

```
                    ┌───────────────────────────────────────────────┐
                    │            Cloudflare Global Edge             │
                    │  - Global Anycast DNS                         │
                    │  - Full (Strict) Edge TLS                     │
                    │  - DDoS Protection & Edge WAF                 │
                    └───────┬───────────────────────────────┬───────┘
                            │                               │
              https://yourdomain.com                        │ https://media.yourdomain.com
              https://yourdomain.com/api/v1/*               │
                            │                               │
                ┌───────────▼───────────┐         ┌─────────▼─────────┐
                │     Hostinger VPS     │         │   Cloudflare R2   │
                │    (Ubuntu 24.04)     │         │   Public Bucket   │
                │  Ports: 80 / 443 / 22 │         │ (WebP Derivatives)│
                └───────────┬───────────┘         └───────────────────┘
                            │
              ┌─────────────▼─────────────┐
              │    Caddy Reverse Proxy    │
              │  (Docker Container:proxy) │
              │  - Automatic HTTPS (ACME) │
              │  - Unified Origin Routing │
              └───────┬─────────────┬─────┘
                      │             │
         Static/HTML  │             │  /api/v1/*
                      │             │
        ┌─────────────▼───┐     ┌───▼─────────────┐
        │ Next.js 16 Web  │     │ Spring Boot API │
        │ (Docker: web)   │     │ (Docker: api)   │
        │ Internal :3000  │     │ Internal :8080  │
        └─────────────────┘     └───┬─────────────┘
                                    │
                                    │ TLS / SSL (HikariCP)
                                    │ AWS Security Group (Port 5432)
                                    │ Restricted to Hostinger VPS IP
                                    │
                        ┌───────────▼───────────┐
                        │   AWS RDS PostgreSQL  │
                        │  - Version 16         │
                        │  - Region: ap-south-1 │
                        │  - Enforced SSL       │
                        │  - Row-Level Security │
                        │  - Automated Backups  │
                        └───────────────────────┘
                                    ▲
                                    │ IAM S3 API / Presigned URLs
                        ┌───────────┴───────────┐
                        │     Cloudflare R2     │
                        │    Private Bucket     │
                        │  (Originals, AI Refs, │
                        │   Verification PDFs)  │
                        └───────────────────────┘
```

---

## 2. Unified Origin Domain Architecture

The platform uses a **Unified Origin Model** (`Option A`):

- **Frontend Surface:** `https://yourdomain.com/*` → routed by Caddy to Next.js container (`web:3000`).
- **Backend API Surface:** `https://yourdomain.com/api/v1/*` → routed by Caddy to Spring Boot container (`api:8080`).
- **Media Delivery Surface (Optional):** `https://media.yourdomain.com/*` → Cloudflare R2 public bucket for processed derivatives.

### Operational Advantages of Unified Origin
1. **Strict `__Host-` Cookies:** Session cookies (`__Host-session`) adhere to RFC 6265bis with `Path=/`, `Secure`, and no domain scope, preventing subdomain cookie leakage.
2. **Zero CORS Preflight Overhead:** Browser requests from `https://yourdomain.com` to `/api/v1/*` are same-origin. This completely eliminates unnecessary `OPTIONS` preflight roundtrips, reducing API latency for mobile users.
3. **Double-Submit CSRF Protection:** Same-origin CSRF tokens (`XSRF-TOKEN`) work natively without cross-subdomain relaxation.
4. **Single SSL Certificate:** Caddy automatically issues and manages a single Let's Encrypt / ZeroSSL certificate for the primary domain.

---

## 3. Hostinger VPS Specifications & Hardening

### Platform Requirements
- **Hosting Type:** **Hostinger VPS** (KVM virtualization) with Docker. *(Shared web hosting is NOT supported; root access, Docker Engine, and systemd are required).*
- **Operating System:** Ubuntu 24.04 LTS (or Debian 12 / Hostinger Docker VPS Application Template).
- **Runtime Tools:** Docker Engine 26+, Docker Compose v2.

### Resource Sizing Guidelines
| Tier | Specifications | Target Workload |
|---|---|---|
| **Minimum Baseline** | 2 vCPU, 4 GB RAM, 50 GB NVMe SSD (Hostinger KVM 2) | Launch baseline; Next.js + Spring Boot + Caddy. |
| **Recommended / Preferred** | 4 vCPU, 8 GB RAM, 100 GB NVMe SSD (Hostinger KVM 4) | Production headroom; smooth JVM JIT compilation, React Server Component concurrency, and buffer cache. |

### Swap Configuration (Burst Memory Protection)
Configure a 2 GB swap file to prevent sudden Linux OOM-killer termination during peak traffic bursts:
```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### Hostinger VPS Firewall & Network Security
Configure the UFW firewall on the VPS to expose **only** necessary public ports:
```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp   # SSH (restrict to admin IP if static: sudo ufw allow from <ADMIN_IP> to any port 22 proto tcp)
sudo ufw allow 80/tcp   # HTTP (ACME challenge & HTTP->HTTPS redirect)
sudo ufw allow 443/tcp  # HTTPS
sudo ufw allow 443/udp  # HTTP/3 (QUIC)
sudo ufw enable
```

**Critical Invariant:** Port `8080` (Spring Boot) and Port `3000` (Next.js) are **NEVER** exposed on the host interface. In `docker-compose.prod.yml`, they use Docker Compose `expose` directives, accessible **only** within the internal Docker bridge network (`interior_network`) by the Caddy reverse proxy.

---

## 4. AWS RDS for PostgreSQL 16

The persistent database is hosted on **AWS RDS for PostgreSQL 16**, fully isolated from the Hostinger VPS compute instances.

### Configuration Parameters
- **Database Engine:** PostgreSQL 16.x.
- **Recommended AWS Region:** `ap-south-1` (Mumbai) for low latency to Indian and regional traffic, closely matching the Hostinger VPS location.
- **DB Instance Class:** `db.t4g.micro` (development/testing) or `db.t4g.small` / `db.t4g.medium` (production).
- **Storage:** 20 GB to 100 GB gp3 with storage autoscaling enabled.
- **Master Database Name:** `interiordb`.

### RDS Network Security & Security Group Rules
Because the Hostinger VPS is external to AWS VPC:
1. **Public Accessibility:** Set `Publicly accessible = Yes` so external traffic can reach the RDS endpoint.
2. **Security Group Restriction:** Configure an AWS VPC Security Group attached to the RDS instance with a single inbound rule:
   - **Type:** PostgreSQL
   - **Protocol:** TCP
   - **Port Range:** `5432`
   - **Source:** `<HOSTINGER_VPS_PUBLIC_IP>/32` (Single Hostinger VPS Public IP only).
   - **Strict Warning:** **NEVER** use `0.0.0.0/0` or open access.
3. If the Hostinger VPS IP changes, immediately update this Security Group inbound rule.

### RDS SSL Enforcement & Verification
- **Force SSL:** In the RDS custom DB Parameter Group, set `rds.force_ssl = 1`.
- **Backend JDBC Configuration:**
  ```properties
  SPRING_DATASOURCE_URL=jdbc:postgresql://<rds-endpoint>:5432/interiordb?sslmode=verify-full
  ```
- **Truststore & Certificates:** The backend Docker container (`apps/api/Dockerfile`) installs the AWS RDS Global CA bundle (`https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem`) into the system trust store during image build. This ensures that `sslmode=verify-full` verifies both the certificate chain and the exact RDS hostname, guarding against DNS spoofing and MITM attacks.

### Database Roles & Privilege Model
- **Runtime Application Role (`app_runtime`):**
  - Granted `CONNECT`, `SELECT`, `INSERT`, `UPDATE`, `DELETE` on all application tables and sequences.
  - **Invariants:** Must **NEVER** possess `SUPERUSER`, `BYPASSRLS`, `CREATEDB`, or `CREATEROLE`.
- **Row-Level Security (RLS):**
  - Enabled and forced (`ALTER TABLE ... FORCE ROW LEVEL SECURITY`) on all 26 tenant tables.
  - Tenant context set per transaction: `SET LOCAL app.current_studio_id = '<studio_id>'`.

### Automated Backups & Disaster Recovery
- **Automated Snapshots:** Retained for 7 to 35 days with daily automated snapshot windows during low-traffic periods (e.g. 02:00 UTC).
- **Point-in-Time Recovery (PITR):** Continuous transaction log archiving enabled.
- **Deletion Protection:** Enabled on the RDS instance in the AWS Management Console to prevent accidental deletion.
- **Final Snapshot:** "Create final snapshot before deletion" enabled.

---

## 5. Cloudflare R2 Media Storage Architecture

Media assets are separated completely from VPS disk storage using **Cloudflare R2** (S3-compatible, zero egress bandwidth fees).

### Storage Buckets & Policies
1. **Private Bucket (`interior-platform-media-private`):**
   - Public access: **Disabled**.
   - Accessible only via backend API credentials with server-controlled paths:
     - `pending/{studioId}/{uploadIntentId}/{mediaAssetId}.ext` (quarantine uploads)
     - `studio/{studioId}/projects/{projectId}/original/{mediaAssetId}.ext` (high-res originals)
     - `ai/references/{studioId}/{generationId}.ext` (client style references)
     - `ai/generations/{studioId}/{generationId}.ext` (AI concepts before publication)
     - `verification/{studioId}/{documentId}.pdf` (private studio verification documents)
   - Lifecycle rules: 7-day auto-expiry on `pending/**` quarantine objects.
   - CORS policy configured for direct browser presigned uploads:
     ```json
     [
       {
         "AllowedOrigins": ["https://yourdomain.com"],
         "AllowedMethods": ["PUT"],
         "AllowedHeaders": ["Content-Type", "Content-Length"],
         "MaxAgeSeconds": 3600
       }
     ]
     ```
2. **Public Bucket (`interior-platform-media-public`):**
   - Custom domain / public access: `https://media.yourdomain.com`.
   - Contains **only** processed, EXIF-stripped WebP/JPEG derivatives generated by `ImageProcessingService`.
   - Cache headers emitted: `Cache-Control: public, max-age=31536000, immutable`.

---

## 6. Docker & Compose Production Architecture

The production environment on the Hostinger VPS runs using `docker-compose.prod.yml`:

```
Hostinger VPS
├── Container: interior-platform-proxy (Caddy 2.9 Alpine)
│   └── Exposes ports 80, 443 (TCP & UDP)
├── Container: interior-platform-web (Next.js 16 Standalone)
│   └── Internal port 3000
└── Container: interior-platform-api (Spring Boot 3.4 / Temurin 21)
    └── Internal port 8080
```

### Logging & Disk Management
Docker log rotation is enforced across all containers to prevent log files from exhausting the VPS SSD:
```yaml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

### Caddy Reverse Proxy Configuration (`infrastructure/caddy/Caddyfile`)
- Automatically provisions and renews TLS certificates via Let's Encrypt / ZeroSSL.
- Injects standard security headers (`HSTS`, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`).
- Routes `/api/v1/*` to `api:8080` with standard forwarded headers (`X-Forwarded-For`, `X-Forwarded-Proto`, `X-Forwarded-Host`).
- Routes all other traffic to `web:3000`.

---

## 7. CI/CD & Deployment Strategy

The project utilizes **GitHub Actions** and **GitHub Container Registry (GHCR)**:

1. **Continuous Integration (CI):** Every push and pull request runs:
   - Frontend linting, typecheck, Vitest component/integration tests, and standalone build.
   - Backend Maven compilation, unit tests, RLS security tests, and Flyway schema verification.
2. **Container Build & Publish:** On push to `main`:
   - Builds multi-stage Docker images for Web and API.
   - Pushes versioned and `latest` tags to GitHub Container Registry (`ghcr.io/<owner>/interior-web`, `ghcr.io/<owner>/interior-api`).
3. **Automated SSH Deployment:**
   - Connects to the Hostinger VPS over SSH using a dedicated deployment key stored in GitHub Secrets.
   - Pulls updated images (`docker compose -f docker-compose.prod.yml pull`).
   - Restarts containers with zero drift (`docker compose -f docker-compose.prod.yml up -d --remove-orphans`).
   - Automatically executes Flyway migrations (`V001` through `V023`) inside the Spring Boot container upon startup.
   - Verifies health probes (`curl -fsSL http://localhost/health`).
   - Automatically prunes dangling images older than 72 hours.

### Required GitHub Secrets for Automated Deployment
- `HOSTINGER_HOST`: Public IP of the Hostinger VPS.
- `HOSTINGER_USER`: Deployment user (e.g. `deploy` or `root`).
- `HOSTINGER_SSH_KEY`: Private SSH key matching `~/.ssh/authorized_keys` on the VPS.
- `HOSTINGER_PORT`: SSH port (default `22`).

---

## 8. Step-by-Step Operator Runbook

Follow these exact steps to commission the production platform:

### Step A: AWS RDS PostgreSQL Provisioning
1. Log in to the AWS Console in region `ap-south-1` (Mumbai).
2. Create an AWS RDS PostgreSQL 16 instance:
   - Database identifier: `interior-platform-db`.
   - Master username: `postgres` (or custom admin username).
   - Master password: generate a strong 32-character password.
   - Database name: `interiordb`.
   - Publicly Accessible: **Yes**.
3. Create a custom DB Parameter Group and verify `rds.force_ssl = 1`.
4. Create an AWS VPC Security Group:
   - Inbound Rule: PostgreSQL (5432) from `<HOSTINGER_VPS_IP>/32`.
   - Attach this Security Group to the RDS instance.
5. Record the RDS Endpoint hostname (`<id>.ap-south-1.rds.amazonaws.com`).

### Step B: Cloudflare R2 Provisioning
1. In the Cloudflare Dashboard, navigate to **R2**.
2. Create bucket `interior-platform-media-private` (Private, no public R2.dev domain).
3. Create bucket `interior-platform-media-public` (Connect custom domain `media.yourdomain.com`).
4. Generate an R2 API Token with Object Read & Write permissions on both buckets.

### Step C: Hostinger VPS Setup
1. Provision a Hostinger KVM VPS with Ubuntu 24.04 LTS.
2. Connect to the VPS via SSH:
   ```bash
   ssh root@<HOSTINGER_VPS_IP>
   ```
3. Install Docker Engine and Docker Compose v2:
   ```bash
   sudo apt-get update
   sudo apt-get install -y ca-certificates curl gnupg
   sudo install -m 0755 -d /etc/apt/keyrings
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
   sudo chmod a+r /etc/apt/keyrings/docker.gpg
   echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
   sudo apt-get update
   sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
   ```
4. Configure firewall (UFW) as specified in Section 3.
5. Create deployment directory:
   ```bash
   sudo mkdir -p /opt/interior-platform/infrastructure/caddy
   sudo chown -R $USER:$USER /opt/interior-platform
   ```
6. Copy `docker-compose.prod.yml` and `infrastructure/caddy/Caddyfile` to `/opt/interior-platform/`.
7. Create `/opt/interior-platform/.env.production` (or `.env`) with restricted permissions:
   ```bash
   touch /opt/interior-platform/.env
   chmod 600 /opt/interior-platform/.env
   ```
8. Populate `/opt/interior-platform/.env` using the template from `.env.example`.

### Step D: Domain & DNS Configuration
1. In Cloudflare DNS (or domain registrar DNS):
   - `A` record for `@` pointing to `<HOSTINGER_VPS_IP>` (Proxy status: Proxied or DNS-only).
   - `CNAME` record for `www` pointing to `yourdomain.com`.
   - `CNAME` record for `media` pointing to Cloudflare R2 public bucket.

### Step E: Initial Deployment & Health Verification
1. On the Hostinger VPS:
   ```bash
   cd /opt/interior-platform
   docker compose -f docker-compose.prod.yml up -d
   ```
2. Inspect logs to confirm Flyway migration execution:
   ```bash
   docker compose -f docker-compose.prod.yml logs -f api
   ```
3. Verify all containers report healthy:
   ```bash
   docker compose -f docker-compose.prod.yml ps
   ```
4. Verify public endpoints:
   - `curl -I https://yourdomain.com/health` (HTTP 200)
   - `curl -I https://yourdomain.com/api/v1/actuator/health/readiness` (HTTP 200)

---

## 9. Launch Smoke-Test Checklist

Verify each of the following endpoints and workflows in the live environment:

- [ ] **Public Homepage (`GET /`):** Renders semantic H1, brand hero, 2.5D scroll animation, 0 console errors.
- [ ] **Interior Showcase (`GET /interior-journey`):** 5 approved interior scenes render in order with accessible text.
- [ ] **Public Discovery (`GET /projects`):** Project catalog renders with filter chips and search.
- [ ] **Public Directory (`GET /professionals`):** Studio listings render with location and specialty tags.
- [ ] **Health Probes:**
  - `GET /health` -> HTTP 200 `{"status":"healthy"}`.
  - `GET /api/v1/actuator/health/liveness` -> HTTP 200 `{"status":"UP"}`.
  - `GET /api/v1/actuator/health/readiness` -> HTTP 200 `{"status":"UP"}`.
- [ ] **Authentication Boundary:**
  - Visiting `/sign-in` displays sign-in screen.
  - Accessing `/workspace` without auth redirects to `/sign-in`.
  - Accessing `/admin` without admin role displays Access Denied boundary.
- [ ] **Database & RLS Integrity:**
  - Tenant queries enforce `app.current_studio_id`.
  - Public reviews query returns only `status = 'PUBLISHED'`.
- [ ] **Storage Privacy:**
  - Presigned upload PUT functions to private bucket.
  - Raw original cannot be accessed anonymously.
  - Derivatives delivered via public bucket / CDN.
- [ ] **Security Headers:**
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload`
  - `X-Frame-Options: DENY`
  - `X-Content-Type-Options: nosniff`
  - `Referrer-Policy: strict-origin-when-cross-origin`

---

## 10. Disaster Recovery Runbook

In the event of a catastrophic VPS hardware failure or datacenter outage:

1. **Database Recovery:** AWS RDS maintains automated snapshots and point-in-time recovery independent of the VPS. If needed, restore a snapshot to a new RDS instance in minutes.
2. **VPS Recovery:**
   - Provision a fresh Hostinger VPS.
   - Install Docker Engine and Docker Compose v2.
   - Clone repository or copy `docker-compose.prod.yml` and `infrastructure/caddy/Caddyfile`.
   - Restore `/opt/interior-platform/.env` from secure offline password manager.
   - Update AWS RDS Security Group inbound rule to allow the new Hostinger VPS IP.
   - Update Cloudflare DNS `A` record to point to the new Hostinger VPS IP.
   - Run `docker compose -f docker-compose.prod.yml up -d`.
3. **Media Recovery:** Cloudflare R2 bucket data remains completely intact and unaffected by VPS recreation.
