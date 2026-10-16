# Elégance Platform: Phase 12D AI Live Evaluation Runbook
## Execution Window: Phase 15 (Pre-Launch Activation)

---

## 1. Overview & Purpose

This runbook specifies the step-by-step procedures for operators and engineers to activate and validate live AI image generation providers during **Phase 15 (Pre-Launch Activation)**. 

During Phase 12D, live credentials are not used. Follow these instructions only when Phase 15 begins and live provider credentials have been obtained.

---

## 2. Pre-Flight Checklist & Prerequisites

- [ ] Interior project context verified (`scripts/verify-interior-context.ps1`).
- [ ] PostgreSQL database running and migrated to latest schema (V040+).
- [ ] ClamAV daemon running and reachable via TCP port 3310 or local socket.
- [ ] Cloudflare R2 bucket provisioned and verified (`scripts/verify-r2-live-integration.ps1`).
- [ ] Enterprise AI API key provisioned (Google Cloud Vertex AI or Stability AI).
- [ ] Credit limits and billing alerts configured on the provider portal ($50 alert, $200 hard cap).

---

## 3. Step-by-Step Activation Procedure

### Step 1: Configure Environment Secrets
Inject the verified credentials into the production environment or container runtime:
```bash
export AI_VISUALIZER_ENABLED=true
export AI_PROVIDER="google-vertex"
export AI_API_KEY="AIzaSy..." # Or provider bearer token
export AI_ENDPOINT="https://us-central1-aiplatform.googleapis.com/v1/projects/{project}/locations/us-central1/publishers/google/models/imagen-3.0-generate-002:predict"
export AI_MODEL="imagen-3.0-generate-002"
export AI_TIMEOUT_SECONDS=60
export AI_MAX_RETRIES=2
export AI_DAILY_STUDIO_LIMIT=20
export AI_GLOBAL_DAILY_BUDGET_MINOR=500000 # 5,000 INR/USD units
export AI_DEFAULT_STUDIO_MONTHLY_BUDGET_MINOR=100000 # 1,000 INR/USD units
export AI_ESTIMATED_COST_PER_GENERATION_MINOR=350 # ~0.035 units
export AI_PRICING_CURRENCY="INR"
```

### Step 2: Run Opt-In Verification Script
Execute the pre-flight connection test with live credential enforcement:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-ai-provider-live-integration.ps1 -RequireLiveCredentials
```
**Expected Output:**
- Network connectivity: PASS.
- TLS certificate handshake: PASS.
- HTTP status 200 OK.
- Response latency: < 15,000 ms.

### Step 3: Start Spring Boot Service in Production Profile
Verify that `ProductionConfigurationValidator` completes startup without errors:
```bash
java -Dspring.profiles.active=production -jar apps/api/target/interior-api-*.jar
```
**Expected Banner Output:**
```
================================================================================
  ELÉGANCE PRODUCTION CONFIGURATION VALIDATOR -- COMPLETED SUCCESSFULLY
  Profile                   : production
  Storage Provider          : S3 / Cloudflare R2
  Malware Scanner           : ClamAV Daemon (Active)
  Authentication            : Google OpenID Connect (Strict)
  AI Subsystem              : Active (google-vertex / imagen-3.0-generate-002)
  Tenant Isolation (RLS)    : STRICT ENFORCEMENT
================================================================================
```

### Step 4: Execute Live Visualizer Acceptance Drills
Submit controlled interior design test requests via API or UI workspace:

#### Drill A: Wardrobe Recolor (Inpainting + Precision Mask)
- **Source Image**: Master bedroom wardrobe (`fixture_wardrobe_01.jpg`).
- **Mask Image**: Binary PNG mask covering door panels only.
- **Prompt**: `"Matte sage green finish on wardrobe doors, seamless flat panels, brass hardware, preserved lighting and room geometry"`.
- **Verify**:
  - Unmasked walls, floor, and ceiling remain byte-identical or structurally unchanged.
  - Door colors transition smoothly without bleeding beyond mask contours.
  - Job transitions to `COMPLETED` in < 15 seconds.

#### Drill B: Kitchen Cabinet Material Swap (Reference Swatch Conditioning)
- **Source Image**: Kitchen overview (`fixture_kitchen_02.jpg`).
- **Mask Image**: Bounding mask on lower cabinets.
- **Reference Image**: American walnut wood grain swatch (`fixture_swatch_walnut.jpg`).
- **Prompt**: `"Natural American walnut wood grain veneer, vertical grain alignment, satin finish, matching ambient kitchen lighting"`.
- **Verify**:
  - Grain texture reflects reference swatch characteristics.
  - Countertops and tile backsplash remain untouched.

### Step 5: Verify Audit & Ledger Records
Query PostgreSQL to confirm cost accounting:
```sql
SELECT job_id, studio_id, cost_minor, currency, provider, model, created_at 
FROM ai_generation_cost_ledger 
ORDER BY created_at DESC 
LIMIT 5;
```
Ensure each drill recorded the exact estimated minor cost.

### Step 6: Verify Malware Scanner & Storage Assets
1. Check ClamAV logs to confirm scanning occurred before storage upload:
   `clamd.log: OK: /tmp/ai_gen_output_*.jpg`
2. Verify object storage presence in Cloudflare R2 bucket:
   `studio/{studioId}/ai/{jobId}.jpg`

---

## 4. Failure Mode & Edge-Case Drills

| Scenario | Injection Method | Expected Behavior |
| :--- | :--- | :--- |
| **Daily Quota Reached** | Submit 21st generation for a studio on same UTC day | Rejected with `429 Too Many Requests` (`STUDIO_DAILY_QUOTA_EXCEEDED`) |
| **Content Policy Rejection** | Submit prompt containing banned or harmful terms | Rejected with `400 Bad Request` (`AI_SAFETY_REJECTED`); zero cost debited |
| **Emergency Kill Switch** | Set `AI_VISUALIZER_ENABLED=false` | New requests rejected with `503 Service Unavailable` (`AI_SERVICE_DISABLED`) |
| **Invalid/Expired API Key** | Corrupt `AI_API_KEY` header | Provider returns 401; Elégance logs normalized `AI_AUTH_FAILED` alert |
| **Provider Downtime** | Simulate 503 from provider | Retries up to `AI_MAX_RETRIES` (2x) with exponential backoff, then marks job `FAILED` (`PROVIDER_UNAVAILABLE`) |

---

## 5. Troubleshooting Matrix

| Symptom | Probable Cause | Remediation |
| :--- | :--- | :--- |
| `AI_AUTH_FAILED` (401/403) | Expired GCP token or revoked Stability AI key | Generate a new API token in provider dashboard; update secret vault. |
| `MALWARE_DETECTED` | False positive or compromised download URL | Inspect ClamAV signature database; ensure test image source is clean. |
| `SSRF_DETECTED` (400) | Provider output redirected to internal IP | Inspect provider callback URL; ensure provider uses public CDN URLs only. |
| `JOB_TIMED_OUT` (504) | Provider GPU queuing under high global load | Increase `AI_TIMEOUT_SECONDS` to 90s; check provider status dashboard. |
