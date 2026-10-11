# Production AI Provider, Usage, Cost Controls & Privacy Architecture

## Overview
Elégance integrates an AI Visual Concept Studio enabling interior designers to generate room transformations, style transfer concepts, and precision-inpainted visual concepts from client room photos. 

Phase 9C productionizes this subsystem, implementing:
1. Pluggable provider abstraction with multi-model capability matrices.
2. Production usage reservations and strict concurrency-safe daily studio quotas (20 generations/day).
3. Multi-currency cost ledger accounting (`ai_cost_ledger`) and real-time studio/global spend ceilings.
4. Autonomous stuck-job reconciliation and timeout safety (`TIMED_OUT`).
5. Strict tenant isolation with PostgreSQL Row-Level Security (RLS).
6. Enterprise data protection, SSRF safeguards, and privacy boundaries.

---

## 1. Provider Abstraction & Capability Matrix

The platform interfaces with AI generation backends via `AiImageProvider` and `ConfigurableAiImageProvider`:

| Provider | Supported Models | Inpainting / Precision | Cost Tracking Source | Default / Status |
| :--- | :--- | :--- | :--- | :--- |
| **Google Imagen** | `imagen-3.0-generate-002`, `imagen-3.0-fast-generate-001` | Yes (Mask-based) | Calculated / Provider-Reported | Production candidate |
| **Stability AI** | `stable-diffusion-xl-1024-v1-0`, `sd3-medium`, `sd3-large-turbo` | Yes (Search-and-replace & inpaint) | Calculated (Minor units USD) | Production candidate |
| **OpenAI** | `dall-e-3`, `dall-e-2` | DALL-E 2 only for mask edits | Calculated | Supported abstraction |
| **Mock Provider** | `mock-interior-v1` | Simulated | Zero / Simulated | Dev & CI Default |

### Allowed Models Enforcement
`AiVisualizerProperties` maintains `allowedModels` whitelist:
- Generates only through explicitly allowlisted model names.
- Blocks execution immediately with `400 Bad Request` / `ILLEGAL_ARGUMENT` if an unsupported model is requested.

---

## 2. Quota Management & Atomic Reservations

### Daily Studio Quota
- **Limit:** 20 visual generations per designer studio per 24-hour cycle.
- **Cycle Reset:** Daily at 00:00:00 UTC.
- **Active Reservation Counting:** Quota checks count all jobs currently `QUEUED`, `PROCESSING`, and `SUCCEEDED` created within the current UTC day.

### Atomic Reservation Protocol
1. **Pre-check:** Evaluates `countTodayActiveReservations(studioId) >= dailyStudioLimit`.
2. **Reservation Event:** Emits `GENERATION_RESERVED` to `ai_usage_events` prior to dispatching provider work.
3. **Execution:** Dispatches background generation job.
4. **Resolution:**
   - On Success: Status transitions to `SUCCEEDED`, and `GENERATION_SUCCESS` is logged.
   - On Failure: Status transitions to `FAILED`, and `RESERVATION_REFUNDED` is logged.
   - On Cancellation: Status transitions to `CANCELLED`, and `RESERVATION_REFUNDED` is logged.
   - On Timeout: Status transitions to `TIMED_OUT`, and `RESERVATION_REFUNDED` is logged.
   This guarantees that transient failures and timeouts do not permanently penalize a studio's daily generation quota.

---

## 3. Cost Ledger & Monetary Accounting

### Flyway V036 Database Ledger (`ai_cost_ledger`)
Every successful visual generation writes an immutable ledger entry:
- `id`: UUIDv7 unique identifier.
- `studio_id`: Owning designer studio.
- `job_id`: Reference to `ai_visualization_jobs`.
- `provider_key`: Canonical provider (`google`, `stability`, `mock`).
- `model_name`: Provider model utilized.
- `operation`: Operation type (`IMAGE_GENERATION`, `INPAINTING`, `VARIATION`).
- `input_units` & `output_units`: Token or image counts.
- `amount_minor`: Monetary cost in minor currency units (e.g., cents in USD).
- `currency`: ISO-4217 3-letter currency code (default: `USD`).
- `cost_source`: `PROVIDER_REPORTED`, `CALCULATED`, or `UNKNOWN`.
- `pricing_version`: Pricing catalog snapshot version (default: `v1`).
- `created_at`: UTC timestamp.

### PostgreSQL Row-Level Security
RLS policy `tenant_isolation_ai_cost_ledger` is enforced at the database kernel level:
- Studio members can exclusively select their studio's own cost records (`studio_id = current_setting('app.current_studio_id')::uuid`).
- Cross-tenant queries return empty result sets.
- Platform administrators with `app.is_admin = 'true'` can inspect costs across all studios.

---

## 4. Operational Cost Controls & Budgets

The system implements a four-tier cost control guardrail:
1. **Global Master Kill-Switch:** `app.ai.enabled=false` halts all generation requests platform-wide.
2. **Global Daily Monetary Budget:** `app.ai.global-daily-budget-minor` caps total API spend across all studios within a 24-hour window.
3. **Studio Kill-Switch:** `designer_studios.ai_enabled=false` enables granular admin disablement of compromised or abusive accounts.
4. **Studio Monthly Spend Ceiling:** `designer_studios.ai_monthly_budget_minor` allows capping per-studio spend to prevent runaway bills.

---

## 5. Fault Tolerance & Timeout Recovery

### 5-Minute Execution Timeout
- Background generation jobs are governed by a 5-minute deadline.
- `reconcileStuckProcessingJobs()` runs periodically to identify jobs lingering in `PROCESSING` longer than 5 minutes.
- Stuck jobs are transitioned to `TIMED_OUT`.
- The reserved quota is released via `RESERVATION_REFUNDED` event.

---

## 6. Privacy, Security & Data Protection

1. **No External Training Data Leakage:**
   - Production provider contracts must mandate zero-retention / opt-out of foundational model fine-tuning on user uploads.
2. **SSRF Guardrails:**
   - Input images are served directly from validated, internal object storage (`media_assets`) using canonical keys.
   - User-supplied external URLs are never fetched blindly by the backend or forwarded to provider APIs.
3. **Clean Metadata Separation:**
   - Prompts sent to providers omit studio names, client identities, PII, and financial values.

---

## 7. Open Commercial Decisions

The following policy and vendor decisions remain open and must be determined prior to Phase 11 production launch:

1. **`PRODUCTION AI PROVIDER DECISION REQUIRED`:**
   - Formal selection between Google Vertex AI (Imagen 3) vs. Stability AI vs. OpenAI DALL-E.
2. **`PRODUCTION AI MODEL DECISION REQUIRED`:**
   - Determination of default production model based on output quality, latency, and per-image cost.
3. **`AI BUDGET AND PRICING POLICY DECISION REQUIRED`:**
   - Business determination of whether AI generations will be bundled into standard subscriptions (e.g. 20 included/day) or billed as metered overage.
4. **`AI RETENTION & PRIVACY DPA DECISION REQUIRED`:**
   - Execution of Data Processing Addendum (DPA) guaranteeing zero customer photo retention for model training.
5. **`AI USER DISCLOSURE COPY DECISION REQUIRED`:**
   - Legal copy sign-off for client-facing and studio-facing AI provenance disclaimers.
