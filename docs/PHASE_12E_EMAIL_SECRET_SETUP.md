# Phase 12E: Transactional Email Secret Management & Environment Setup

> **Target Audience:** DevOps, Security Engineers, SREs  
> **Status:** Standardized for Production Deployment (Phase 15 Activation)

---

## 1. Canonical Environment Variables Schema

The following environment variables govern transactional email configuration across all deployment environments:

| Environment Variable | Default Value | Production Requirement | Sensitive? | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `EMAIL_PROVIDER` | `disabled` | Required (`disabled`, `resend`, `ses`) | No | Active email delivery adapter |
| `EMAIL_FROM_ADDRESS` | `noreply@interior.platform` | Required if provider != `disabled` | No | Verified RFC 5322 sender mailbox |
| `EMAIL_FROM_NAME` | `Elégance Platform` | Optional | No | Human-readable sender display name |
| `EMAIL_REPLY_TO` | `support@interior.platform`| Optional | No | Inbound support reply address |
| `EMAIL_MAX_RETRIES` | `3` | Optional (default: 3) | No | Max transient delivery retries |
| `EMAIL_BASE_RETRY_DELAY_SEC` | `60` | Optional (default: 60) | No | Base delay for exponential backoff |
| `EMAIL_TIMEOUT_SECONDS` | `15` | Optional (default: 15) | No | HTTP / SDK connection timeout |
| `EMAIL_WEBHOOK_SECRET` | *(empty)* | Recommended if webhooks enabled | **YES** | Shared secret / signing secret for webhooks |
| `RESEND_API_KEY` | *(empty)* | Required if `EMAIL_PROVIDER=resend` | **YES** | Resend REST API bearer token (`re_...`) |
| `AWS_SES_REGION` | `us-east-1` | Required if `EMAIL_PROVIDER=ses` | No | AWS target region for SES v2 client |
| `AWS_ACCESS_KEY_ID` | *(empty)* | Optional (IAM instance role preferred)| **YES** | AWS IAM access key ID |
| `AWS_SECRET_ACCESS_KEY` | *(empty)* | Optional (IAM instance role preferred)| **YES** | AWS IAM secret access key |

---

## 2. Fail-Fast Production Validation Rules

`ProductionConfigurationValidator` enforces strict invariants during application bootstrap when `spring.profiles.active=production`:

1. **Mock / Fake Providers Strictly Rejected:**
   - If `EMAIL_PROVIDER` is set to `fake`, `mock`, or `test`, the application throws `IllegalStateException` and aborts immediately.
2. **Missing / Placeholder API Keys Rejected:**
   - If `EMAIL_PROVIDER=resend`, `RESEND_API_KEY` must not be blank and must not match regex `^(placeholder|your-.*|fake|mock|none|re_123456789)$`.
3. **Invalid Sender Address Rejected:**
   - If `EMAIL_PROVIDER` is active (`resend` or `ses`), `EMAIL_FROM_ADDRESS` must not be blank, must contain `@`, and must not end with `@example.com` or start with `placeholder`.
4. **Unconfigured SES Region Rejected:**
   - If `EMAIL_PROVIDER=ses`, `AWS_SES_REGION` must not be blank or a placeholder string.
5. **Safe Fallback when Disabled:**
   - If `EMAIL_PROVIDER=disabled`, startup succeeds with an informational log: `Transactional Email: NOT_CONFIGURED (Phase 15 Provider Activation Pending)`.

---

## 3. Secret Masking & Logging Policy

- **Zero Cleartext Credentials in Logs:** API keys, AWS secret access keys, and webhook signing secrets must NEVER appear in application logs or exception traces.
- **Recipient Address Masking:** Any log entry referencing an email recipient must mask the local-part leaving only the first character:
  - Example: `johndoe@example.com` $\to$ `j***@example.com`.
- **Masked Credentials Display in Validation Scripts:** When running `verify-email-provider-live-integration.ps1`, the script masks all except the first 4 and last 4 characters:
  - Example: `re_live_987654321fedcba` $\to$ `re_l...dcba`.

---

## 4. Zero-Downtime Secret Rotation Procedure

### Resend API Key Rotation
1. Log in to [Resend Console](https://resend.com) -> **API Keys**.
2. Click **Create API Key** with name `elegance-prod-key-YYYYMMDD`.
3. Update the production secret store (AWS Secrets Manager / Kubernetes Secret / Vault) with the new key value.
4. Perform a rolling restart of the backend API containers.
5. In Resend Console, monitor the old key's usage graph. Once traffic on the old key ceases, click **Delete** on the old key.

### AWS IAM Access Key Rotation
1. In AWS IAM Console, generate a second access key on the SES sending user.
2. Update the container environment or secret store with the new access key ID and secret access key.
3. Perform a rolling restart of the application containers.
4. Verify email sending succeeds via live logs.
5. Deactivate the old IAM access key in AWS IAM Console, wait 24 hours, and then delete it.
