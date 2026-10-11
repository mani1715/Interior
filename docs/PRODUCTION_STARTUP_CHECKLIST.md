# PRODUCTION STARTUP CHECKLIST — ELÉGANCE

This checklist must be executed prior to enabling production traffic during Phase 11 Go-Live.

---

## 1. Environment & Infrastructure Pre-flight
- [ ] PostgreSQL connection configured with SSL (`sslmode=require`).
- [ ] Database migrations validated (`Flyway validate` confirms V000 to V040 match all checksums).
- [ ] Row-Level Security (RLS) enabled and verified on all tenant tables.
- [ ] Object storage buckets provisioned (Quarantine bucket, Public derivative bucket) with private access policies.
- [ ] External provider API keys populated securely via secret manager (Stripe, Resend, OpenAI/Stability).

---

## 2. Platform Operational State
- [ ] Verify `platform_operational_state` has mode set to `NORMAL`.
- [ ] Verify kill switches are in expected operational state:
  - `uploads_enabled = true`
  - `ai_enabled = true`
  - `registrations_enabled = true`
- [ ] Super admin MFA step-up authentication verified and functional.

---

## 3. Background Job Coordinator
- [ ] Background job coordinator running with active advisory lock acquisition.
- [ ] Background jobs verified operational:
  - `SESSION_CLEANUP`
  - `UPLOAD_EXPIRY`
  - `STORAGE_RECONCILIATION`
  - `COMMUNICATION_RETRY`
  - `AI_STUCK_RECOVERY`
  - `PRIVACY_CLEANUP`

---

## 4. Health & Observability Smoke Checks
- [ ] Public health endpoint `GET /api/v1/health` responds HTTP 200 `{"status":"UP"}` without leaking database connection details.
- [ ] Request correlation filter correctly tags incoming requests with `X-Request-Id` and `X-Correlation-Id` in response headers and MDC.
- [ ] Admin operations provider matrix `GET /api/v1/admin/operations/providers` reports truthful provider connectivity.
