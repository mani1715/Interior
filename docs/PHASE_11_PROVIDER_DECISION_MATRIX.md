# Elégance — Phase 11 Provider Decision Matrix
**Document Status:** Complete / Ready for User Review  
**Phase:** Phase 11A — Production Decision Lock  
**Date:** 2026-10-13  
**Classification:** Technical & Operational Provider Comparative Matrix  

---

## 1. Overview & Evaluation Principles

This matrix provides a rigorous comparative evaluation of candidate external providers for each subsystem of the Elégance platform.

Evaluation Criteria:
1. **Architectural Compatibility:** How cleanly the provider interfaces with existing Spring Boot Java 21 / Next.js 16.3.8 adapters.
2. **Operational Overhead:** Complexity of ongoing maintenance, certificate management, and deployment pipelines.
3. **Security & Compliance:** Row-level isolation, encryption at rest, GDPR/DPDP alignment, and webhook signature verification.
4. **Target Market Fit:** Alignment with Indian domestic interior design studios (UPI, GST) and international expansion.

> **CRITICAL RULE:** No provider is automatically selected. Technical recommendations are provided for guidance only; the final choice is designated as **`USER DECISION REQUIRED`**.

---

## 2. Comparative Subsystem Matrices

### 2.1 Frontend Hosting

| Candidate | Fit with Next.js 16.3.8 | Advantages | Limitations | Operational Complexity | Repo Changes Required | Secrets Required | Recommendation |
|---|---|---|---|---|---|---|---|
| **Vercel** | Native First-Party | Zero configuration; instant edge deployment; automatic preview builds; global CDN. | Higher commercial pricing; vendor lock-in for advanced edge middleware. | Minimal (Git push to deploy) | None (`next.config.js` is already compatible) | `NEXT_PUBLIC_API_URL`, `APP_CDN_HOSTNAME` | **RECOMMENDED (Option A)** |
| **Railway** | Excellent (Docker Standalone) | Unified dashboard and billing with backend API; predictable flat pricing; private networking. | Requires manual CDN configuration if edge caching is desired. | Low (Dockerfile build) | None (Dockerfile in `apps/web`) | `PORT`, `NEXT_PUBLIC_API_URL`, `APP_CDN_HOSTNAME` | **RECOMMENDED (Option B)** |
| **Render** | Good (Docker Standalone) | Managed web service; automated TLS; simple environment variables. | Slower build times; cold starts on free/low tiers. | Low | None | `NEXT_PUBLIC_API_URL`, `APP_CDN_HOSTNAME` | Viable alternative |
| **AWS ECS Fargate / CloudFront** | Enterprise Container | Full VPC control; compliance certifications; deep IAM governance. | High setup overhead; requires Route 53, ALB, and CloudFront configuration. | High | Terraform / CDK manifests required | AWS IAM credentials, VPC config | Enterprise only |

---

### 2.2 Backend API Hosting

| Candidate | Fit with Spring Boot Java 21 | Advantages | Limitations | Operational Complexity | Repo Changes Required | Secrets Required | Recommendation |
|---|---|---|---|---|---|---|---|
| **Railway** | Excellent | Multi-stage Docker build; native private networking to PostgreSQL; built-in health checks (`/actuator/health`). | Fixed cloud regions (US-West, EU-Central). | Very Low | None (`apps/api/Dockerfile` ready) | Standard production env vars | **RECOMMENDED (Option A)** |
| **Render** | Good | Simple Docker runtime; transparent resource scaling; automated TLS. | Limited regional egress routing. | Low | None | Standard production env vars | Viable alternative |
| **AWS ECS Fargate** | Enterprise Standard | High availability across Availability Zones; private subnets; direct RDS peering; infinite scale. | Steep operational curve; manual secret rotation setup. | High | ECS task definitions, ALB rules | AWS Secrets Manager integration | **RECOMMENDED (Option B - Enterprise)** |
| **GCP Cloud Run** | Good | Scale-to-zero capability; fast autoscaling. | Cold starts on Java 21 (unless min instances > 0); JVM memory sizing tuning. | Medium | Cloud Run YAML | Secret Manager env vars | Viable alternative |

---

### 2.3 PostgreSQL Database

| Candidate | Version Support | HA / Failover | PITR / Backups | Connection Limits | RLS Support | Operational Complexity | Recommendation |
|---|---|---|---|---|---|---|---|
| **AWS RDS PostgreSQL** | 16.x / 17.x | Multi-AZ Automated Failover | Automated daily snapshots + continuous WAL archiving (PITR up to 35 days) | Configurable (Hikari pool fits easily) | Native PostgreSQL RLS verified | Medium (Managed by AWS) | **RECOMMENDED (Enterprise)** |
| **Supabase** | 16.x | Managed Multi-AZ on Pro | Daily backups + WAL PITR | Built-in Supavisor connection pooler | Native PostgreSQL RLS verified | Low | **RECOMMENDED (Beta)** |
| **Neon** | 16.x / 17.x | Serverless Multi-AZ | Instant branching, continuous PITR | PgBouncer included | Native PostgreSQL RLS verified | Very Low | Viable alternative |
| **Railway Managed PG** | 16.x | Single-node default | Automated daily backups | Direct connection limits apply | Native PostgreSQL RLS verified | Very Low | Suitable for Closed Beta only |

---

### 2.4 Object Storage & Media Delivery

| Candidate | S3 Compatibility | Egress Bandwidth Pricing | Private / Quarantine Buckets | CDN Integration | Operational Complexity | Recommendation |
|---|---|---|---|---|---|---|
| **Cloudflare R2** | 100% S3 Compatible | **Zero ($0.00) Egress Fees** | Supported via separate bucket namespaces | Native Cloudflare global edge network | Low | **RECOMMENDED** |
| **AWS S3 + CloudFront** | Native Standard | Standard AWS data transfer fees apply | Supported with fine-grained IAM & KMS | Global AWS CloudFront distribution | Medium | Enterprise alternative |
| **Backblaze B2** | S3 Compatible | Low egress fees ($0.01/GB) | Supported | Requires third-party Fastly/Cloudflare CDN integration | Medium | Cost alternative |

---

### 2.5 OIDC Identity Providers

| Provider | Protocol Support | Studio Designer Workflow | Email Verification Behavior | Code Status | Recommendation |
|---|---|---|---|---|---|
| **Google Identity** | OpenID Connect / OAuth2 | One-click login for creative studios using Google Workspace / Gmail | Guaranteed email verification | Fully supported by `OidcService` | **RECOMMENDED (Primary)** |
| **Apple Sign-In** | OpenID Connect | Essential for iOS / macOS designer users; supports private relay | Email relay handled cleanly | Supported by `OidcService` | **RECOMMENDED (Secondary)** |
| **GitHub** | OAuth2 | Common for technical users | May have unverified emails | Supported | Optional |
| **Auth0 / Clerk** | Universal Identity Broker | Aggregates all social providers into single endpoint | Managed user pool | Requires custom adapter | Defer to post-launch |

---

### 2.6 Transactional Email Providers

| Candidate | Protocol & Webhooks | Deliverability / Reputation | Template Engine Support | Bounce & Complaint Ingestion | Recommendation |
|---|---|---|---|---|---|
| **Resend** | REST API + Webhooks | High reputation on modern IP pools; developer-first | Server-side HTML rendered by `EmailTemplateService` | Webhook verification supported (`/webhooks/email/resend`) | **RECOMMENDED (Option A)** |
| **Postmark** | REST API + Webhooks | Industry gold-standard transactional deliverability | Native HTML templates supported | Webhooks supported | **RECOMMENDED (Option B)** |
| **AWS SES** | SMTP / AWS SDK | Cost-effective; requires domain warm-up | Raw MIME dispatch | Ingested via SNS / SQS webhook bridge | High complexity |
| **SendGrid** | REST API + Webhooks | Broad commercial standard | Supported | Webhooks supported | Viable alternative |

---

### 2.7 AI Image Generation Providers & Models

| Provider | Input Modalities | Interior Mask Editing | Reference Conditioning | Data Retention & Privacy | Recommendation |
|---|---|---|---|---|---|
| **Stability AI** | Image-to-Image, Inpainting | Supported via SDXL Inpaint | Supported via multi-concept LoRAs | Enterprise DPA available (zero training on customer inputs) | **RECOMMENDED (Option A)** |
| **Replicate (FLUX / SDXL)** | Image-to-Image, Inpainting | Supported via FLUX.1 Fill | Supported via IP-Adapter | Standard API terms; enterprise tier required for zero retention | **RECOMMENDED (Option B)** |
| **Google Vertex AI (Imagen 3)** | Text-to-Image, Inpainting | Supported | Supported | Enterprise Google Cloud DPA guaranteed zero training | High cost, enterprise fit |

---

### 2.8 Malware Scanning Engines

| Candidate | Architecture | Inspection Latency | Fail-Closed Compatibility | Hosting Model | Recommendation |
|---|---|---|---|---|---|
| **ClamAV Daemon** | Containerized Daemon (TCP port 3310) | < 500ms for images/PDFs | 100% fail-closed via `require-clean-scan=true` | Co-located Docker container alongside API worker | **RECOMMENDED (Option A)** |
| **AWS GuardDuty for S3** | Cloud-native S3 event scanning | Asynchronous (1-5 seconds) | Requires upload intent polling or webhook completion | Fully managed by AWS | **RECOMMENDED (Option B - if on AWS)** |
| **VirusTotal API** | Public Cloud API | 2-10 seconds | Commercial rate limits apply | External SaaS API | Higher operational cost |

---

### 2.9 Commercial Payment Gateways

| Candidate | Target Geography | Payment Methods (India & Global) | Recurring Billing / Subscriptions | Webhook Verification | Recommendation |
|---|---|---|---|---|---|
| **Razorpay** | India Primary | UPI, Credit/Debit Cards, NetBanking, e-mandates | Native recurring subscriptions with RBI mandate compliance | HMAC-SHA256 signature verification supported | **RECOMMENDED (India Market)** |
| **Stripe** | Global Primary (India Invite) | Global Cards, Apple Pay, Google Pay, SEPA | Industry standard Stripe Billing & Customer Portal | Native webhook signature verification | **RECOMMENDED (International Market)** |
| **Cashfree** | India Secondary | UPI, Cards, NetBanking, PayLater | Recurring subscriptions supported | Supported | Secondary alternative |
| **LemonSqueezy / Paddle** | Global Merchant of Record (MoR) | International cards, PayPal | Handles global VAT/sales tax automatically | Supported | Ideal for international MoR |

---

### 2.10 Observability & Centralized Logging

| Subsystem | Candidate | Advantages | Operational Complexity | Recommendation |
|---|---|---|---|---|
| **Metrics & APM** | **Prometheus + Grafana** | Open source, native Spring Boot Actuator scraping (`/actuator/prometheus`) | Low to Medium | **RECOMMENDED (Self-Hosted / Managed)** |
| **Metrics & APM** | **Datadog** | Full-stack APM, distributed tracing, automated anomaly alerts | High cost | Enterprise alternative |
| **Centralized Logging** | **Better Stack (Logtail)** | Fast vector search, built-in log alerting, live tailing | Low | **RECOMMENDED** |
| **Centralized Logging** | **AWS CloudWatch Logs** | Native integration with ECS Fargate and RDS | Medium | Native AWS choice |
| **Uptime Monitoring** | **Better Stack Uptime** | Automated synthetic HTTP probes, SSL expiry alarms, public status page | Very Low | **RECOMMENDED** |

---

## 3. Summary of Recommendations Awaiting User Approval

1. **Frontend:** Vercel (or Railway standalone Docker).
2. **Backend:** Railway (or AWS ECS Fargate).
3. **Database:** Supabase / Neon (Beta) or AWS RDS PostgreSQL 16 (Enterprise).
4. **Storage:** Cloudflare R2 (zero egress fees for high-resolution portfolio imagery).
5. **CDN:** Cloudflare Edge CDN (bound to `APP_CDN_HOSTNAME`).
6. **OIDC:** Google Identity (with Apple Sign-In as secondary).
7. **Email:** Resend (or Postmark) for clean deliverability and modern webhooks.
8. **AI:** Stability AI or Replicate with strict DPA data-use exclusions.
9. **Malware:** Containerized ClamAV daemon co-located with API.
10. **Billing:** Razorpay for India domestic, or Stripe for international commercial operations.
