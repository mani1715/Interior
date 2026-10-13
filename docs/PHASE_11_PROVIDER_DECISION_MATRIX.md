# Elégance — Phase 11B.1 Provider Decision Matrix
**Document Status:** Complete / Source-Truth Corrected / Awaiting User Sign-Off  
**Phase:** Phase 11B.1 — Production Decision Source-Truth Correction  
**Date:** 2026-10-13  
**Classification:** Comparative Evaluation & Operational Recommendations  
**Release Candidate Baseline:** `5c3f41772304b9a5ad33dd43607d1d7e2bbd820f` (Flyway `V040`)  

---

## 1. Overview & Evaluation Principles

This matrix provides a rigorous comparative evaluation of candidate external providers for each subsystem of the Elégance platform.

> **CRITICAL RULE:** No provider is automatically selected. Technical recommendations are provided for guidance only; every choice requires explicit user approval. Recommendations are marked **`RECOMMENDED — WAITING USER APPROVAL`**, while public/commercial items are marked **`DEFERRED`**.

---

## 2. Subsystem Comparative Evaluations

### 2.1 Frontend Hosting
| Candidate | Fit with Next.js 16.3.8 | Advantages | Limitations | Operational Overhead | Secrets / Env Required | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Vercel** | Native First-Party | Zero configuration, automatic edge caching, instant preview URLs, edge image optimization. | Higher commercial bandwidth tier costs. | Minimal | `NEXT_PUBLIC_API_URL`, `APP_CDN_HOSTNAME` | **RECOMMENDED — WAITING USER APPROVAL** |
| **Railway** | Excellent (Docker Standalone) | Unified dashboard and billing with backend API, predictable flat pricing. | Requires manual CDN configuration for edge caching. | Low | `PORT`, `NEXT_PUBLIC_API_URL`, `APP_CDN_HOSTNAME` | Viable Alternative |
| **Render** | Good (Docker Standalone) | Managed web service, automated TLS. | Slower builds, cold starts on lower tiers. | Low | Same as Railway | Alternative |
| **AWS ECS Fargate / CloudFront** | Enterprise Container | Full VPC control, enterprise compliance. | High setup overhead (Route 53, ALB, CloudFront). | High | AWS IAM, VPC, ALB config | Enterprise Alternative |

---

### 2.2 Backend API Hosting
| Candidate | Fit with Spring Boot Java 21 | Advantages | Limitations | Operational Overhead | Health Checks & Probes | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Railway** | Excellent | Multi-stage Docker build, native private networking to PostgreSQL, zero-downtime deploys. | Fixed cloud regions. | Very Low | Built-in `/api/v1/actuator/health/liveness` and `/readiness` integration | **RECOMMENDED — WAITING USER APPROVAL** |
| **AWS ECS Fargate** | Enterprise Standard | High availability across AZs, private VPC subnets, direct RDS peering, infinite scale. | Steep operational curve, manual secret rotation setup. | High | Target Group health checks | Enterprise Alternative |
| **Render** | Good | Simple Docker runtime, transparent scaling. | Limited regional egress routing. | Low | Native health path | Alternative |
| **GCP Cloud Run** | Good | Scale-to-zero capability. | Cold starts on Java 21 without min-instances. | Medium | Cloud Run probes | Alternative |

---

### 2.3 PostgreSQL Database
| Candidate | PostgreSQL Engine Version | High Availability / Failover | Backup & PITR State | Connection Limits & HikariCP | RLS Compatibility | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Railway PostgreSQL service** | PostgreSQL 17 | Single-node default | Scheduled backups & PITR must be explicitly enabled and verified | Direct connection limits fit beta (Hikari pool = 10) | Native RLS verified on all 59 tables | **RECOMMENDED (Closed Beta) — WAITING USER APPROVAL** |
| **Supabase PostgreSQL** | PostgreSQL 17 | Managed Multi-AZ on Pro | Scheduled backups & WAL PITR must be enabled and verified | Built-in Supavisor connection pooler | Native RLS verified on all 59 tables | Alternative (Beta / Prod) |
| **Neon** | PostgreSQL 17 | Serverless Multi-AZ | Instant branching, continuous PITR | PgBouncer included | Native RLS verified on all 59 tables | Alternative |
| **AWS RDS PostgreSQL** | PostgreSQL 17 | Multi-AZ Automated Failover | Automated daily snapshots + continuous WAL (up to 35 days PITR) | Highly configurable Hikari pool | Native RLS verified on all 59 tables | Enterprise Alternative |

---

### 2.4 Object Storage & Media Delivery
| Candidate | S3 API Compatibility | Egress Bandwidth Pricing Behavior | Private / Public Buckets | CDN Delivery Integration | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Cloudflare R2** | Compatible with Elégance S3 operations (`PutObject`, `GetObject`, `HeadObject`, `CopyObject`, `DeleteObject`, presigned `PUT`) | **Zero ($0.00) Egress Fees** under Cloudflare pricing model | Separate bucket namespaces (`S3_BUCKET_PRIVATE`, `S3_BUCKET_PUBLIC`) | Cloudflare custom media domain / CDN | **RECOMMENDED — WAITING USER APPROVAL** |
| **AWS S3** | Native Standard | Standard AWS data transfer fees ($0.09/GB) | Full IAM & bucket policies | AWS CloudFront distribution | Enterprise Alternative |
| **Backblaze B2** | S3 Compatible | Low egress ($0.01/GB) | Supported | Requires third-party CDN | Alternative |

---

### 2.5 Identity & Authentication (OIDC)
| Provider | Protocol Support | Studio Designer Workflow | Code Implementation Status | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Google OIDC** | OpenID Connect / OAuth2 | One-click login for creative studios using Google Workspace / Gmail | Supported by `OidcService` | **RECOMMENDED (Closed Beta) — WAITING USER APPROVAL** |
| **Apple Sign-In** | OpenID Connect | Useful for iOS / macOS designer demographic | Supported by `OidcService` | DEFERRED TO PUBLIC BETA |
| **GitHub** | OAuth2 | Useful for developer / technical admin logins | Supported by `OidcService` | Optional / Deferred |
| **Clerk / Auth0** | Universal Broker | Aggregates multiple social logins | Requires custom bridge | Not Recommended / Deferred |

---

### 2.6 Transactional Email
| Candidate | Protocol & Webhooks | Deliverability / Reputation | Code Implementation Status | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Disabled Initially** | None | N/A | `DisabledEmailProvider` active (invitations copyable in UI/admin) | **RECOMMENDED (Closed Beta) — WAITING USER APPROVAL** |
| **Resend** | REST API + Webhooks | High reputation on modern IP pools | Adapter required for Public Beta | DEFERRED TO PUBLIC BETA |
| **Postmark** | REST API + Webhooks | Industry gold-standard deliverability | Adapter required for Public Beta | DEFERRED TO PUBLIC BETA |
| **AWS SES** | SMTP / AWS SDK | Cost-effective; requires domain warm-up | Adapter required for Public Beta | DEFERRED TO PUBLIC BETA |

---

### 2.7 Generative AI Image Provider
| Provider | Modalities & Masking | Privacy & Training Policy | Code Implementation Status | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Disabled Initially** | None | Zero risk | `DisabledAiImageProvider` active | **RECOMMENDED (Closed Beta) — WAITING USER APPROVAL** |
| **Stability AI** | Inpainting, multi-reference | Enterprise DPA (zero customer data training) | Supported via `ConfigurableAiImageProvider` | DEFERRED TO AI EVALUATION |
| **Replicate (FLUX / SDXL)** | Inpainting, reference conditioning | Enterprise tier required for zero retention | Supported via `ConfigurableAiImageProvider` | DEFERRED TO AI EVALUATION |
| **Google Vertex AI** | Text-to-Image, Inpainting | Enterprise Google Cloud DPA guaranteed zero training | Supported via `ConfigurableAiImageProvider` | DEFERRED TO AI EVALUATION |

---

### 2.8 Upload Malware Scanning
| Provider / Implementation | Source Implementation Status | Fail-Closed Compatibility | Operational Profile | Status |
| :--- | :--- | :--- | :--- | :--- |
| **DisabledMalwareScanner** | **IMPLEMENTED** (in main source) | Returns `UNAVAILABLE`; fails closed under `require-clean-scan=true` | Does not permit uploads | Active baseline |
| **FakeMalwareScanner** | **TEST ONLY** (mock/dev profile) | Simulates clean/infected/error/timeout | In-memory simulation only | Test only |
| **ClamAV Daemon Adapter** | **NOT IMPLEMENTED** | Containerized daemon (TCP 3310) | Requires adapter implementation & container deploy | Mandatory before public uploads |
| **AWS GuardDuty for S3** | **NOT IMPLEMENTED** | Asynchronous bucket scanning | AWS-native | Alternative for AWS |
| **VirusTotal API** | **NOT IMPLEMENTED** | External commercial API | Rate-limited external calls | Alternative |

> **SECURITY INVARIANT:** The production invariant remains `require-clean-scan=true` and **NO CLEAN SCAN $\rightarrow$ NO CANONICAL COMMIT**. Invited users cannot be assumed trusted. Uploads in production will fail closed with `MALWARE_SCANNER_NOT_CONFIGURED` until a real scanner adapter is wired.

---

### 2.9 Commercial Payment Gateway
| Candidate | Target Geography | Payment Methods | Recurring Subscriptions | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Disabled Initially** | N/A | N/A | Complimentary beta access | **RECOMMENDED (Closed Beta) — WAITING USER APPROVAL** |
| **Razorpay** | India Primary | UPI, domestic cards, NetBanking, e-mandates | Native recurring subscriptions with RBI mandate compliance & GST invoicing | **RECOMMENDED (Commercial Launch) — WAITING USER APPROVAL** |
| **Stripe** | Global Primary (India Invite) | Global cards, Apple Pay, Google Pay | Stripe Billing & Customer Portal | Alternative (Commercial Launch) |
| **LemonSqueezy / Paddle** | Global Merchant of Record | International cards, PayPal | Handles global sales tax automatically | Alternative (International MoR) |

---

## 3. Summary of Authoritative Recommendations

1. **Frontend:** **Vercel** (Alternative: Railway).
2. **Backend:** **Railway** (Alternative: AWS ECS Fargate).
3. **Database:** **Railway PostgreSQL service** (PostgreSQL 17) or **Supabase PostgreSQL 17**.
4. **Storage:** **Cloudflare R2** (S3-compatible API for Elégance call set; zero egress fees under Cloudflare pricing model).
5. **CDN:** **Cloudflare custom media domain / CDN** (`<MEDIA_DOMAIN>`).
6. **Domain:** User selection required (placeholder `<ROOT_DOMAIN>`).
7. **OIDC:** **Google OIDC**.
8. **Email:** **Disabled for Closed Beta** $\rightarrow$ Defer provider selection to Public Beta.
9. **AI:** **Disabled for Closed Beta** $\rightarrow$ Defer to AI quality evaluation.
10. **Malware:** **Production scanner adapter required before normal user media uploads**.
11. **Billing:** **Disabled for Closed Beta** $\rightarrow$ Razorpay for India-First Commercial Launch.
12. **Backups & PITR:** Must be **explicitly enabled and verified** before storing real customer data.
13. **Contractual SLAs:** Closed beta operates with **NO CONTRACTUAL SLA**. Commercial SLAs remain `USER / EXECUTIVE DECISION REQUIRED`.
