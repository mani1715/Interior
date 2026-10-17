# Elégance Platform: Provider Activation Register

## 1. Executive Summary & Timeline Policy

The Elégance Interior Designer Platform is scheduled for commercial release approximately 3 months following completion of core engineering and production readiness hardening.

Per project policy:
- Zero Real External Credentials are provisioned during pre-launch certification phases.
- All implementations, SPI adapters, contract tests, and validation harnesses are implemented and locally verified now.
- Managed PostgreSQL 18 on Neon is ACTIVE and LIVE VERIFIED (Phase 15A).
- All remaining live external provider activations are formally consolidated and deferred to the unified final activation sequence defined in `docs/FINAL_PROVIDER_ACTIVATION_SEQUENCE.md`.
- See `docs/FINAL_PROVIDER_DEFERRED_VALIDATION_REGISTER.md` for individual pillar readiness registers.
- Final commercial branding and custom domains are decided in **Phase 16**.

---

## 2. Master Provider Activation Matrix

| Component / Subsystem | Selected Provider / Candidate | Architecture & Integration Status | Live Validation Status | Target Activation Phase |
| :--- | :--- | :--- | :--- | :--- |
| **Relational Database** | **Managed PostgreSQL (18.6)** *(Neon)* | **PHASE 15A COMPLETE** (Flyway V001..V040, 59 Forced RLS Tables, Native `uuidv7()`, TLS 1.3, Spring Boot Production Boot, Backup/Restore Drills) | **LIVE PROVIDER VALIDATION COMPLETE** | Phase 15A (CLOSED) |
| **Object Storage** | **Cloudflare R2** | **PHASE 15B ARCHITECTURE READY** (6-Op S3 Contract Tested 8/8 PASS, ClamAV Streaming, EXIF Strip, Dual-Bucket Topology, Runbooks Authored) | READY FOR UNIFIED ACTIVATION (Keys in Master Inventory) | Phase 15B (Pre-Flight PASS) |
| **Identity & Auth (OIDC)** | **Google Identity Services** | IMPLEMENTATION READY & LOCALLY CONTRACT TESTED (PKCE S256, RFC 7517 JWKS Caching, RS256 Verification, MFA Step-Up) | READY FOR UNIFIED ACTIVATION (Keys in Master Inventory) | Phase 15C (Pre-Flight PASS) |
| **AI Visualizer** | **Google Vertex AI / Imagen 3** *(Secondary: Stability AI)* | IMPLEMENTATION READY & LOCALLY CONTRACT TESTED (`ConfigurableAiImageProvider`, SSRF firewall, in-memory ClamAV scanning, 20/day quota, cost ledger, fail-fast validator) | READY FOR UNIFIED ACTIVATION (Keys in Master Inventory) | Phase 15D (Pre-Flight PASS) |
| **Transactional Email** | **Resend** *(Secondary: AWS SES)* | IMPLEMENTATION READY & LOCALLY CONTRACT TESTED (`ResendEmailProvider`, `SesEmailProvider`, truthful `NOT_CONFIGURED` disabled status, `ResendWebhookSignatureValidator` Svix HMAC-SHA256, `AwsSnsSignatureValidator` RSA & SSRF cert defense, OWASP escaping, out-of-order webhook defense, fail-fast validator) | READY FOR UNIFIED ACTIVATION (Keys in Master Inventory) | Phase 15E (Pre-Flight PASS) |
| **Commercial Billing** | **Stripe** *(Secondary: Razorpay)* | IMPLEMENTATION READY & LOCALLY CONTRACT TESTED (`StripeBillingProvider` tolerance & v1 HMAC, `RazorpayBillingProvider`, Open Redirect defense, durable idempotency, single active sub, Zero Data Loss on Downgrade, locked entitlement capacities, fail-fast validator) | READY FOR UNIFIED ACTIVATION (Keys in Master Inventory) | Phase 15F (Pre-Flight PASS) |
| **Observability & Logging** | **OpenTelemetry / Actuator / Sentry** | IMPLEMENTATION READY & LOCALLY CONTRACT TESTED (`JsonLoggingLayout` production JSON layout with injection neutralization & PII/secret masking, `RequestCorrelationFilter` `X-Request-Id` MDC propagation, `PlatformMetrics` bounded domain metrics, Actuator `/actuator/prometheus` & hardened exposure, health indicators with truthful provider reporting) | READY FOR UNIFIED ACTIVATION (Actuator UP, DSN Optional) | Phase 15G (Pre-Flight PASS) |
| **Performance, CDN & Autoscaling** | **Cloudflare CDN / Production Hosting Autoscaler** | IMPLEMENTATION READY & LOCALLY VALIDATED (Multi-scenario load suite in `performance/`, fine-grained studio row locks, advisory lock singletons, HikariCP bounded pool decoupled via env, sustained 100 VUs / 100.00% success rerun verified, 5-min multi-studio benchmark verified [175x tail latency reduction], genuine 20-minute soak verified with 0 leaks, tail latency & startup errors fully classified) | PRODUCTION CAPACITY & LIVE CDN VALIDATION DEFERRED | Phase 16 |
| **Brand & Custom Domain** | **Pending User Selection** | Neutral "Elégance" working title active | DEFERRED | Phase 16 |
| **Public Deployment** | **Railway / Docker Swarm** | Container manifests ready (`docker-compose.prod.yml`) | DEPLOYMENT NOT PERFORMED | Phase 16 |

---

## 3. Provider Classification Standard

All external integrations must be classified using strictly one of the following official states:
- `IMPLEMENTED`: Code, SPI adapter, and configuration properties exist in repository.
- `LOCALLY VALIDATED`: Verified against local containers or mocks.
- `CONTRACT TESTED`: Verified via unit, integration, and contract test harnesses.
- `CONFIGURATION READY`: Environment variable schemas and runbooks fully documented.
- `LIVE VALIDATION DEFERRED`: Real provider account, credentials, and live endpoints scheduled for Phase 15.

*Rule*: Never mark a service as `PRODUCTION VALIDATED` unless real provider credentials have executed against real cloud endpoints.
