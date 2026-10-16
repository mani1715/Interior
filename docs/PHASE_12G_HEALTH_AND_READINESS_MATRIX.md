# Elégance — Phase 12G: Health, Liveness & Readiness Architecture Matrix

**Document:** Health Indicator Architecture, Probes & Dependency Status Specification  
**Platform:** Elégance — Interior Designer Platform  
**Phase:** 12G Observability & Operational Readiness  

---

## 1. Health Probe Architecture

```
                                 ┌────────────────────────────────────────────────────────┐
                                 │                   Ingress / Probes                     │
                                 └───────────┬────────────────────────────┬───────────────┘
                                             │                            │
                                   Public Minimal Probe           K8s / Host Probes
                                      /api/v1/health          /actuator/health/{probe}
                                             │                            │
                                   ┌─────────┴─────────┐        ┌─────────┴─────────┐
                                   │ Status: UP Only   │        │ Liveness: Process │
                                   │ Zero Host Leak    │        │ Readiness: Core   │
                                   └───────────────────┘        └───────────────────┘
```

---

## 2. Health Indicator Catalog

| Indicator Class | Monitored Component | Public Exposure | Liveness Impact | Readiness Impact | Status When Degraded / Unavailable | Status When Disabled By Design |
|:---|:---|:---:|:---:|:---:|:---|:---|
| **`DataSourceHealthIndicator`** | PostgreSQL 18 | Status Only | None | **CRITICAL (DOWN)** | `DOWN` (core DB workload cannot be served) | Not applicable (Database is mandatory) |
| **`StorageHealthIndicator`** | Cloudflare R2 / S3 / Local | Status Only | None | Degraded | `UNAVAILABLE` (File mutations blocked) | `AVAILABLE` (Local fallback in dev) |
| **`MalwareScannerHealthIndicator`** | ClamAV Daemon | Status Only | None | Degraded | `UNAVAILABLE` (`mediaUploadReady=false` when `requireCleanScan=true`) | `UP` (`status: NOT_CONFIGURED`, `mediaUploadReady=true` if not required) |
| **`TransactionalEmailHealthIndicator`** | Resend / AWS SES | Status Only | None | None | `UNAVAILABLE` (Email retries scheduled) | `UP` (`status: NOT_CONFIGURED`, non-blocking) |
| **`AiProviderHealthIndicator`** | Google Vertex / Stability / Replicate | Status Only | None | None | `UNAVAILABLE` (AI returns 503 domain error) | `UP` (`status: NOT_CONFIGURED`, non-blocking) |
| **`BillingHealthIndicator`** | Razorpay / Stripe | Status Only | None | None | `UNAVAILABLE` (Checkout returns 503 domain error) | `UP` (`status: NOT_CONFIGURED`, non-blocking) |
| **`PlatformOperationalStateHealthIndicator`** | Maintenance Mode & Switches | Status Only | None | None | `READ_ONLY` (Mutations blocked with HTTP 503) | `NORMAL` (Standard operational mode) |

---

## 3. Invariants & Security Gating

1. **Minimal Public Exposure Invariant:**
   - Public health probes (`/api/v1/health` and unauthenticated `/actuator/health`) return strictly `{ "status": "UP", "timestamp": "..." }`.
   - Infrastructure hostnames, database software versions, ClamAV ports, bucket names, and provider API credentials are NEVER emitted in public health responses.
2. **Optional Provider Isolation Invariant:**
   - Optional external SaaS dependencies (AI generation, transactional email, commercial checkout) MUST NOT cause the core application process liveness or core readiness to become `DOWN` when disabled or transiently unavailable.
   - When disabled by configuration (e.g. during local development or pre-activation Phase 12), health indicators report `status: NOT_CONFIGURED` while keeping the health status `UP`.
3. **Database Core Dependency:**
   - If PostgreSQL is unreachable, the readiness probe reports `DOWN` or `UNAVAILABLE` because all tenant authentication, session authorization, and CMS operations depend authoritatively on the database.
