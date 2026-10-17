# Phase 14 — Production Release Checklist

> **Status:** APPROVED PRODUCTION RELEASE CHECKLIST  
> **Target Release:** Version 0.1.0-rc1  
> **Platform:** Elégance Interior Designer Platform  
> **Scope:** Phase 14 Readiness & Pre-Launch Operations  

---

## 1. Pre-Deployment Stage (T-24h to T-1h)

### 1.1 Code & Build Verification
- [ ] Working tree clean on `main` branch.
- [ ] Clean build of backend JAR (`mvn clean package -DskipTests=false`).
- [ ] Clean build of frontend Next.js standalone bundle (`npm run build`).
- [ ] All 751 backend unit/integration tests pass.
- [ ] All 47 PostgreSQL & RLS tenant security tests pass.
- [ ] All 373 frontend Vitest component tests pass across 52 test suites.
- [ ] TypeScript compiler check (`tsc --noEmit`) passes with 0 errors.
- [ ] ESLint check passes with 0 warnings/errors.
- [ ] All 16 Playwright browser journeys pass.
- [ ] Master release package verification (`scripts/verify-release-package.ps1`) passes (8/8 Gates PASS).

### 1.2 Container Packaging & Security
- [ ] Backend OCI image `interior-platform-api:0.1.0-rc1` built.
- [ ] Backend container executes as unprivileged user `spring` (UID 10001).
- [ ] Frontend OCI image `interior-platform-web:0.1.0-rc1` built.
- [ ] Frontend container executes as unprivileged user `nextjs` (UID 1001).
- [ ] Static asset secret scan completed: zero sensitive tokens in `apps/web/.next/static`.
- [ ] No `.env` or credential files present inside container images.

### 1.3 Environment & Secrets Audit
- [ ] Production environment schema validated with `scripts/validate-production-environment.ps1`.
- [ ] All required production secrets generated with high entropy and stored in encrypted vault.
- [ ] Optional providers verified:
  - If enabled: verified valid API credentials (no placeholder values).
  - If deferred: verified explicitly disabled / none.
- [ ] Database credentials verified and tested.

---

## 2. Deployment Stage (T-0)

### 2.1 Database Readiness
- [ ] Full pre-deployment database backup created and confirmed restorable.
- [ ] Database connection pool limits configured (HikariCP max-pool-size: 20-30).
- [ ] Apply pending Flyway migrations (e.g. up to V040).
- [ ] Confirm RLS is enabled on all 59 multi-tenant tables.

### 2.2 Container Deployment & Health
- [ ] Deploy ClamAV anti-virus sidecar container and confirm socket ready.
- [ ] Deploy backend container (`interior-platform-api`) with `SPRING_PROFILES_ACTIVE=production`.
- [ ] Verify startup logs: `ProductionConfigurationValidator` reports valid configuration.
- [ ] Verify backend readiness probe: `curl http://backend:8080/actuator/health/readiness` -> `{"status":"UP"}`.
- [ ] Deploy frontend container (`interior-platform-web`).
- [ ] Verify frontend root route responds HTTP 200: `curl -I http://frontend:3000/`.

---

## 3. Post-Deployment Verification (T+0 to T+30m)

### 3.1 Smoke Verification
- [ ] Execute release smoke test harness: `scripts/release-smoke-test.ps1`.
- [ ] Public routes accessible: `/`, `/portfolio`, `/services`, `/about`, `/contact`, `/pricing`.
- [ ] Authentication flows responsive: `/login`, `/register`.
- [ ] Designer portal accessible: `/dashboard`, `/projects`, `/floor-plans`, `/documents`.
- [ ] Admin portal accessible: `/admin/users`, `/admin/studios`, `/admin/audit-logs`.
- [ ] File upload API correctly routes to virus scanner and object storage.

### 3.2 Telemetry & Error Monitoring
- [ ] Application logs streaming to centralized logging platform.
- [ ] Metrics reporting to Prometheus / Datadog.
- [ ] Error rate monitored for 30 minutes; zero critical exceptions recorded.

---

## 4. Rollback Readiness
- [ ] Previous stable container image tags recorded and available in registry.
- [ ] Rollback runbook (`docs/PHASE_14_ROLLBACK_RUNBOOK.md`) reviewed and on standby.
- [ ] Designated incident commander assigned during deployment window.
