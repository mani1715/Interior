# PRODUCTION SECRET AND KEY INVENTORY — ELÉGANCE

> **NOTICE**: This document specifies the canonical list of environment variables, secrets, and provider keys required for production deployment (Phase 11). **NO live credentials or production secrets are stored in this codebase or repository.**

---

## 1. Core Platform & Security Secrets

| Environment Variable | Description | Sensitivity | Rotation Period |
|---|---|---|---|
| `JWT_SECRET` | 256-bit symmetric signing secret for platform session tokens | CRITICAL | 90 days |
| `SPRING_DATASOURCE_URL` | Production PostgreSQL connection string (with SSL mode) | CRITICAL | On infrastructure change |
| `SPRING_DATASOURCE_USERNAME`| PostgreSQL database username | HIGH | 180 days |
| `SPRING_DATASOURCE_PASSWORD`| PostgreSQL database user password | CRITICAL | 90 days |
| `APP_ADMIN_MFA_ENCRYPTION_KEY` | Symmetric key for encrypting TOTP secrets at rest | CRITICAL | 180 days (with re-encryption) |

---

## 2. Third-Party Provider Credentials (Phase 11 Deferred)

| Provider | Environment Variable | Usage | Status in Phase 9G |
|---|---|---|---|
| **Stripe** | `STRIPE_SECRET_KEY` | Commercial billing API operations | `NOT_CONFIGURED` (Deferred to Phase 11) |
| **Stripe** | `STRIPE_WEBHOOK_SECRET` | Webhook signature verification | `NOT_CONFIGURED` (Deferred to Phase 11) |
| **Resend / SMTP** | `APP_MAIL_SMTP_HOST` | Host for transactional email | Mocked / Local Test |
| **Resend / SMTP** | `APP_MAIL_SMTP_PASSWORD` | Password for transactional email | Not configured |
| **OpenAI / Stability** | `APP_AI_API_KEY` | AI generation provider key | Mocked / Local Provider |
| **Cloudflare R2 / S3** | `APP_STORAGE_S3_ACCESS_KEY`| Object storage access key | `LOCAL_DISK` (Deferred to Phase 11) |
| **Cloudflare R2 / S3** | `APP_STORAGE_S3_SECRET_KEY`| Object storage secret key | `LOCAL_DISK` (Deferred to Phase 11) |
| **Malware Scanner** | `APP_MEDIA_SECURITY_API_KEY`| External ICAP or scanner API key | Generic local engine |

---

## 3. Secret Rotation & Handling Rules

1. **Zero Secret Check-ins**: No live private keys, API secrets, or certificates may ever be committed to git.
2. **Environment Variable Injection Only**: Secrets must only be provided via container orchestration (e.g., Railway / Kubernetes secret vaults).
3. **Audit Trail**: Any rotation of administrative or system encryption keys must be accompanied by an auditable log entry and regression testing.
