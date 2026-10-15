# Elégance Platform: Provider Activation Register

## 1. Executive Summary & Timeline Policy

The Elégance Interior Designer Platform is scheduled for commercial release approximately 3 months following completion of core engineering and production readiness hardening.

Per project policy:
- **Zero Real External Credentials** are provisioned during Phases 12A–14.
- All implementations, SPI adapters, contract tests, and validation harnesses are implemented and locally verified now.
- Live external provider validation is formally consolidated and executed during **Phase 15 (Real Provider Activation)**.
- Final commercial branding and custom domains are decided in **Phase 16**.

---

## 2. Master Provider Activation Matrix

| Component / Subsystem | Selected Provider / Candidate | Architecture & Integration Status | Live Validation Status | Target Activation Phase |
| :--- | :--- | :--- | :--- | :--- |
| **Object Storage** | **Cloudflare R2** | IMPLEMENTATION READY & LOCALLY CONTRACT TESTED (`S3StorageService`, `ClamAvMalwareScanner`) | LIVE VALIDATION DEFERRED | Phase 15 |
| **Relational Database** | **Managed PostgreSQL (17.x)** *(Railway / AWS RDS / Supabase / Neon)* | IMPLEMENTATION READY & LOCALLY CONTRACT TESTED (Flyway V001..V040, 59 Forced RLS Tables, Backup/Restore Drills) | LIVE VALIDATION DEFERRED | Phase 15 |
| **Identity & Auth (OIDC)** | **Google Identity Services** | ARCHITECTURE & CONTRACT READY (Dev Persona sandbox active) | LIVE VALIDATION DEFERRED | Phase 15 |
| **AI Visualizer** | **Google Vertex AI / Imagen 3** | ARCHITECTURE READY & BUDGET HARDENED (`DisabledAiImageProvider`) | LIVE VALIDATION DEFERRED | Phase 15 |
| **Transactional Email** | **Resend / Postmark** | ARCHITECTURE & ADAPTER READY (`DisabledEmailProvider`, `FakeEmailProvider`) | LIVE VALIDATION DEFERRED | Phase 15 |
| **Commercial Billing** | **Stripe / Razorpay** | ARCHITECTURE READY (`DisabledBillingProvider`, `FakeBillingProvider`) | LIVE VALIDATION DEFERRED | Phase 15 |
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
