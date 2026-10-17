# Production Release Rollout Order

## 1. Overview

Production deployments must follow a deterministic, ordered release procedure to ensure zero database corruption, zero session loss, and clean service transition.

---

## 2. Deterministic 8-Step Release Procedure

```
1. PRE-RELEASE AUDIT & BACKUP
   |
   v
2. ENVIRONMENT & SECRET SYNC
   |
   v
3. DATABASE SCHEMA MIGRATION
   |
   v
4. BACKEND API DEPLOYMENT
   |
   v
5. BACKEND READINESS VERIFICATION
   |
   v
6. FRONTEND WEB DEPLOYMENT
   |
   v
7. POST-DEPLOYMENT SMOKE TESTS
   |
   v
8. RELEASE CONFIRMATION & MONITORING
```

### Step 1: Pre-Release Audit & Backup
- Verify release Git SHA matches signed release manifest.
- Trigger manual/automated PostgreSQL snapshot (`pg_dump -Fc` or cloud snapshot).
- **Hard Gate:** Do not proceed without a verified backup.

### Step 2: Environment & Secret Sync
- Synchronize target runtime environment variables against `deploy/environment.schema.md`.
- Verify no placeholder values exist for active providers.

### Step 3: Database Schema Migration
- Execute Flyway migrations V001..V040 via dedicated release job or single primary instance.
- Flyway automatically locks `flyway_schema_history` via PostgreSQL advisory locks.

### Step 4: Backend API Deployment
- Deploy new backend container image tagged with immutable Git SHA.
- Use rolling update or blue-green replacement.

### Step 5: Backend Readiness Verification
- Poll `/api/v1/actuator/health/readiness` until HTTP 200 `{"status":"UP"}` is received across all backend replicas.
- If readiness fails within timeout, trigger automated rollback.

### Step 6: Frontend Web Deployment
- Deploy new frontend container image tagged with immutable Git SHA.
- Verify Next.js standalone container boots and responds on `/health`.

### Step 7: Post-Deployment Smoke Tests
- Execute automated smoke test suite verifying homepage, directory, project details, dev login (or OIDC), and workspace dashboard.

### Step 8: Release Confirmation & Monitoring
- Monitor error rates (HTTP 5xx), p95 latency, Hikari pool utilization, and container memory for 30 minutes.
- Confirm release success.
