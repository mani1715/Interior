# Elégance Platform: Phase 12D AI Secret Setup & Configuration Reference

## 1. Overview & Security Policy

This document defines the canonical environment variable schema and secret management procedures for the Elégance AI subsystem.

### Critical Security Directives
- **Zero Real Secrets in Phase 12D**: Do NOT create, enter, or commit real AI API credentials during Phase 12D.
- **Git Commit Immunity**: Real secrets must never appear in source code, configuration files, commit messages, or pull requests.
- **Fail-Fast Defense**: In `production` Spring profile, placeholder secrets (`placeholder`, `your-api-key`, `fake`, `mock`) trigger a fatal JVM abort during boot.

---

## 2. Environment Variable Schema

| Variable Name | Required in Prod? | Type | Default Value | Description / Permitted Values |
| :--- | :---: | :---: | :---: | :--- |
| `AI_VISUALIZER_ENABLED` | Yes | Boolean | `false` | Master toggle switch for AI image generation and editing subsystem |
| `AI_PROVIDER` | Yes (if enabled) | String | `none` | Provider identifier: `google-vertex`, `stability-ai`, `replicate`, or `custom` |
| `AI_API_KEY` | Yes (if enabled) | String | None | Confidential provider authorization token or API key |
| `AI_ENDPOINT` | Yes (if enabled) | String | None | Canonical provider HTTPS endpoint URL |
| `AI_MODEL` | Yes (if enabled) | String | None | Permitted model identifier (e.g., `imagen-3.0-generate-002`) |
| `AI_TIMEOUT_SECONDS` | No | Integer | `45` | Maximum socket read timeout per generation request |
| `AI_MAX_RETRIES` | No | Integer | `2` | Number of retry attempts on transient network or 503 provider errors |
| `AI_DAILY_STUDIO_LIMIT` | No | Integer | `20` | Max generations allowed per studio per calendar day (UTC) |
| `AI_GLOBAL_DAILY_BUDGET_MINOR` | No | Long | `500000` | Platform-wide daily expenditure ceiling in minor currency units |
| `AI_DEFAULT_STUDIO_MONTHLY_BUDGET_MINOR` | No | Long | `100000` | Per-studio monthly expenditure ceiling in minor currency units |
| `AI_ESTIMATED_COST_PER_GENERATION_MINOR` | No | Integer | `350` | Estimated cost recorded in ledger per successful generation |
| `AI_PRICING_CURRENCY` | No | String | `INR` | ISO 4217 currency code for budget and ledger accounting (`INR`, `USD`, `EUR`) |
| `AI_PRICING_VERSION` | No | String | `2026-Q4-v1` | Audit version tag for pricing model tracking |

---

## 3. Permitted Production Models

The `ProductionConfigurationValidator` enforces an explicit allowlist of model identifiers to prevent arbitrary or unsupported provider calls:

- **Google Vertex AI**:
  - `imagen-3.0-generate-002`
  - `imagen-3.0-fast-generate-001`
  - `imagen-3.0-capability-001`
  - `imagegeneration@006`
- **Stability AI**:
  - `stable-diffusion-xl-1024-v1-0`
  - `sd3-large`
  - `sd3-large-turbo`
  - `sd3-medium`
- **Replicate**:
  - `black-forest-labs/flux-fill-pro`
  - `stability-ai/sdxl-inpaint`
- **Custom**:
  - Any model identifier matching `^[a-zA-Z0-9_\-\.\/]{3,64}$` when `AI_PROVIDER=custom`.

---

## 4. Secret Injection Across Deployment Environments

### 4.1 Local Development (`local` Profile)
- Defaults: `AI_VISUALIZER_ENABLED=false` (or `mock_configured` with `MockAiImageProvider` in unit tests).
- Developer `.env` file (strictly Git-ignored):
  ```bash
  AI_VISUALIZER_ENABLED=false
  ```

### 4.2 CI/CD Pipeline (GitHub Actions)
- Automated CI builds run tests with in-process mock servers (`LocalAiIntegrationTest`).
- No live external AI secrets are required or configured in GitHub Actions secrets.

### 4.3 Staging / Production Environments (Phase 15 Activation)
- Secrets are provisioned through a centralized secret store:
  - **AWS Secrets Manager / SSM Parameter Store**:
    `/elegance/production/ai/api_key`
  - **Kubernetes Secret Object**:
    ```yaml
    apiVersion: v1
    kind: Secret
    metadata:
      name: elegance-ai-secrets
    type: Opaque
    stringData:
      AI_API_KEY: "AIzaSy..."
    ```

---

## 5. Secret Rotation Procedure

1. **Dual-Key Rotation (Where Supported)**:
   - Generate secondary API key on Google Cloud or Stability portal.
   - Update Kubernetes secret or container environment variable with new key.
   - Trigger rolling deployment of `interior-api`.
   - Validate health via `scripts/verify-ai-provider-live-integration.ps1`.
   - Deprecate and revoke primary key on provider portal.
2. **Emergency Key Revocation**:
   - If an API key is suspected to be compromised, set `AI_VISUALIZER_ENABLED=false` immediately to kill incoming calls.
   - Revoke the compromised key in the provider console.
   - Generate a replacement key, inject it, and re-enable `AI_VISUALIZER_ENABLED=true`.
