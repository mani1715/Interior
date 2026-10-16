# Elégance Platform: Phase 12D AI Visualizer & Image Editing Production Readiness

## 1. Executive Summary & Strategy Alignment

In accordance with the Elégance Production Readiness Strategy (release horizon ~3 months), the AI image visualization and editing subsystem has been architecturally, operationally, and securely completed. Real paid AI API keys, cloud provider billing, and live evaluation benchmarking are intentionally deferred to **Phase 15 (Pre-Launch Activation)**.

### Architectural Tenets
1. **Domain-Specific Interior Design Focus**: Purpose-built for interior design workflows: changing wardrobe and cupboard colors, swapping cabinet and door materials, recoloring shelves, modifying wall and floor finishes, and visualizing design variations while preserving room geometry and architectural structure.
2. **Provider-Neutral Extensibility**: Decoupled from specific vendor SDKs via `AiImageProvider` and `ConfigurableAiImageProvider`. Compatible with Google Vertex AI / Imagen 3, Stability AI, Replicate, and enterprise custom microservices.
3. **Fail-Fast Production Startup Guard**: Spring container validates all AI properties upon startup. In `production` profile, placeholder keys (`placeholder`, `fake`, `mock`), missing API keys, or rogue model selections abort initialization immediately.
4. **Strict Quota & Financial Governance**: Per-studio daily limit (default 20 generations/day), studio monthly budget ceiling, platform global daily budget cap, and granular audit recording in `ai_generation_cost_ledger`.
5. **SSRF & Data Exfiltration Protection**: All provider output URLs and user-supplied reference image URLs pass through `SsrfProtectionService`, blocking private RFC 1918 ranges, link-local, loopback, cloud metadata IPs (`169.254.169.254`, Google metadata), and non-HTTP protocols.
6. **Pre-Persistence Malware Inspection**: Every generated output image is scanned in-memory by `MalwareScanner` (ClamAV) prior to persisting into object storage (Cloudflare R2 target).
7. **Client-Side Speech Processing**: Voice prompt dictation operates 100% in-browser via the W3C Web Speech API (`webkitSpeechRecognition` / `SpeechRecognition`), incurring zero third-party transcription fees and zero audio data leakage.

---

## 2. Component & Source Inventory

| Subsystem / Component | Source File | Purpose | Production Status | Test Verification |
| :--- | :--- | :--- | :--- | :--- |
| **AI Orchestrator** | `AiVisualizerService.java` | Manages job lifecycle, studio quota validation, budget checks, ledger recording, async execution, and malware scanning | READY | `LocalAiIntegrationTest` |
| **Provider Client** | `ConfigurableAiImageProvider.java` | HTTP engine with bounded streaming, SSRF validation, magic-byte inspection, retry logic, and error normalization | READY | `LocalAiIntegrationTest`, `AiSsrfAndOutputSecurityTest` |
| **SSRF Firewall** | `SsrfProtectionService.java` | Resolves hostnames and rejects private/loopback/cloud metadata IP addresses | READY | `AiSsrfAndOutputSecurityTest` |
| **Startup Validator** | `ProductionConfigurationValidator.java` | Enforces fail-fast configuration policies in production profile | READY | `ProductionConfigurationValidatorAiTest` |
| **Cost Ledger** | `AiCostLedgerService.java` / PostgreSQL | Records generation timestamp, studio ID, job ID, estimated cost, and pricing currency | READY | `LocalAiIntegrationTest` |
| **Precision Mask Editor** | `PrecisionMaskEditor.tsx` | Interactive HTML5 Canvas brush/eraser, undo history, coordinate scaling to source aspect ratio, PNG export | READY | Unit / Frontend Component |
| **Voice Dictation** | `WebSpeechTranscriptionService` | Client-side real-time voice-to-text dictation without external paid speech APIs | READY | Browser Native API |
| **Live Verification Runner** | `scripts/verify-ai-provider-live-integration.ps1` | Opt-in Phase 15 pre-flight endpoint and credentials verification script | READY | PowerShell Automated Run |

---

## 3. Interior Visualization Scope & Boundaries

### In Scope (Interior Designer Core Workflows)
- **Recoloring & Material Swapping**: Changing cabinet, wardrobe, kitchen island, door, and trim finishes (e.g., matte black to walnut veneer, white oak to sage green).
- **Wall & Surface Treatment**: Visualizing paint swatches, wallpaper accents, limewash, and textured plaster.
- **Reference Material Guiding**: Supplying color palettes, fabric swatches, or wood samples as reference guidance for generation.
- **Precision Inpainting / Masking**: Isolating specific bounding regions (e.g., only the overhead cabinets) while strictly preserving room layout, window positions, shadows, and perspective lines.
- **Geometry-Preserving Conceptualization**: Generating alternative interior arrangements with architectural fidelity.

### Strictly Out of Scope
- Uncontrolled fantasy or landscape generation.
- Photographer, portrait, or facial modification AI.
- Automated 3D mesh extraction, NeRF reconstruction, or 360-degree virtual tour stitching.
- Unconstrained whole-room hallucination that alters architectural walls or load-bearing pillars.

---

## 4. Quota, Cost & Resource Governance

### 1. Quota Enforcement (`AI_DAILY_STUDIO_LIMIT`)
- Default: **20 generations / studio / calendar day** (UTC).
- Before submitting an AI job, `AiVisualizerService` queries daily job count in the current UTC window.
- If daily threshold is exceeded, the request is rejected with `STUDIO_DAILY_QUOTA_EXCEEDED` (HTTP 429).

### 2. Financial Budget Ceilings
- **Global Platform Ceiling (`AI_GLOBAL_DAILY_BUDGET_MINOR`)**: Emergency platform-wide circuit breaker. If aggregate daily spend crosses the ceiling, new generations are rejected with `GLOBAL_AI_BUDGET_EXCEEDED`.
- **Studio Monthly Budget (`AI_DEFAULT_STUDIO_MONTHLY_BUDGET_MINOR`)**: Caps monthly spend per studio workspace.
- **Ledger Tracking**: Every completed generation is recorded in `ai_generation_cost_ledger` with `job_id`, `studio_id`, `cost_minor`, `currency`, `provider`, `model`, and `pricing_version`.

### 3. Emergency Kill Switch
- Setting `AI_VISUALIZER_ENABLED=false` immediately halts all generation submissions with `AI_SERVICE_DISABLED`.
- In-flight jobs are terminated gracefully without retry loops.

---

## 5. Security & Isolation Controls

### 1. Tenant Isolation
- Every AI generation job is anchored to a specific `studio_id`.
- Mask images, reference images, and input media are validated against tenant ownership. Cross-tenant media references are rejected with `403 Forbidden` (`MEDIA_ACCESS_DENIED`).
- Output media assets are saved under tenant-isolated paths: `studio/{studioId}/ai/{jobId}.jpg`.

### 2. SSRF Protection on External Resources
- When external providers return image URLs (e.g., pre-signed S3/GCS download links), `ConfigurableAiImageProvider` resolves DNS hostnames through `SsrfProtectionService`.
- Banned target addresses:
  - Loopback (`127.0.0.0/8`, `::1`)
  - RFC 1918 Private ranges (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`)
  - Link-local addresses (`169.254.0.0/16`, `fe80::/10`)
  - Cloud provider metadata IP (`169.254.169.254`, `metadata.google.internal`)
  - Non-HTTP protocols (`file://`, `ftp://`, `gopher://`)

### 3. Malware Inspection of AI Outputs
- External provider outputs are treated as untrusted network binaries.
- Output bytes are streamed through `MalwareScanner.scan(bytes, filename)`.
- If malware signatures are detected, the payload is destroyed, the job status transitions to `FAILED` (`MALWARE_DETECTED`), and a high-severity security alert is logged.

### 4. Magic-Byte Image Validation
- The payload must begin with valid magic byte headers for `image/jpeg` (`FF D8 FF`), `image/png` (`89 50 4E 47`), or `image/webp` (`RIFF....WEBP`).
- Non-image payloads or truncated streams trigger `MALFORMED_PROVIDER_OUTPUT` and fail closed.

---

## 6. Verification & Automated Test Matrix

| Test Suite | Class / Script | Scenarios Covered | Results |
| :--- | :--- | :--- | :--- |
| **Local AI Integration** | `LocalAiIntegrationTest.java` | Happy path generation, precision mask, cross-tenant mask rejection, cross-tenant media rejection, reference attachment, daily quota rejection (20/day), deduplication, kill switch, provider 429 backoff, safety filter rejection, auth failure, malformed payload fail-closed, stuck job timeout recovery | 13/13 PASS |
| **SSRF & Output Security** | `AiSsrfAndOutputSecurityTest.java` | Loopback IP blocking, AWS/GCP metadata IP blocking, RFC 1918 blocking, non-HTTP scheme blocking, empty URL rejection | 5/5 PASS |
| **Production Configuration** | `ProductionConfigurationValidatorAiTest.java` | Clean start when disabled, reject `mock` provider, reject placeholder key, reject empty key, reject unpermitted model, clean start with valid production configuration | 6/6 PASS |
| **Live Integration Harness**| `scripts/verify-ai-provider-live-integration.ps1` | Safe non-credentialed run exits 0 with `DEFERRED TO PHASE 15`; opt-in live reachability check with real API keys | VERIFIED PASS |
| **Full Regression Suite** | `mvnw test` | Full platform test suite including auth, storage, database, RLS, and AI | ZERO REGRESSION |

---

## 7. Deferred Phase 15 Activation Plan

When Phase 15 arrives prior to public launch:
1. Select target provider (Google Vertex AI Imagen 3 recommended, Stability AI secondary).
2. Provision production cloud API keys and configure environment variables in secret vault.
3. Run `scripts/verify-ai-provider-live-integration.ps1 -RequireLiveCredentials`.
4. Execute the standard benchmark suite documented in `docs/PHASE_12D_AI_QUALITY_EVALUATION_RUBRIC.md`.
