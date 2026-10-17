# Phase 14 — Production Rollback Runbook

> **Status:** APPROVED FOR PRODUCTION RELEASE CANDIDATE  
> **Platform:** Elégance Interior Designer Platform  
> **Phase:** 14 (Release Package & Pre-Launch Operations)  
> **Target Release:** Version 0.1.0-rc1  

---

## 1. Overview & Rollback Principles

This runbook establishes standardized procedures for rolling back application containers, configuration changes, or database states in the event of an unrecoverable failure during a release window.

### Rollback Principles
- **Fast Reversion:** Always prefer rolling back to the previously verified immutable container tag over attempting in-place hotfixes during an active production outage.
- **Data Preservation First:** Never drop database tables or execute destructive rollback scripts without confirming a verified backup.
- **Decoupled Components:** Frontend, Backend, and Database can be rolled back independently if backward compatibility contracts are maintained.

---

## 2. Trigger Conditions for Rollback

A rollback is mandatory if any of the following occur within the 30-minute post-release verification window:
1. Actuator liveness/readiness probes report `DOWN` or unresolvable health degradation.
2. Error rate (HTTP 5xx) exceeds 1% of total incoming requests for > 3 consecutive minutes.
3. Database connection pool exhaustion or unresolvable lock contention occurs.
4. Core designer or customer journeys (login, portfolio viewing, design booking, lead submission) fail basic smoke checks.
5. Critical security defect, authentication bypass, or RLS tenant leak is detected.

---

## 3. Component Rollback Procedures

### 3.1 Frontend Web Container Rollback
Because Next.js runs as a stateless container, rollback is instant and risk-free:
1. Identify the previous stable image tag (e.g. `interior-platform-web:0.1.0-rc0` or previous commit SHA).
2. Update the container image reference in Docker Compose, Kubernetes Deployment, or platform orchestrator:
   ```bash
   docker stop elegance-web
   docker rm elegance-web
   docker run -d --name elegance-web \
     --network elegance-net \
     -p 3000:3000 \
     --env-file /etc/elegance/web.env \
     interior-platform-web:<PREVIOUS_TAG>
   ```
3. Purge edge CDN cache (e.g. Cloudflare / CloudFront) to invalidate cached HTML and JavaScript bundles:
   ```bash
   # Example Cloudflare cache purge
   curl -X POST "https://api.cloudflare.com/client/v4/zones/<ZONE_ID>/purge_cache" \
     -H "Authorization: Bearer <CF_PURGE_TOKEN>" \
     -H "Content-Type: application/json" \
     --data '{"purge_everything":true}'
   ```
4. Verify HTTP 200 on frontend health: `curl -I https://elegance.interior.design/`.

### 3.2 Backend API Container Rollback
1. Identify the previous stable backend image tag (e.g. `interior-platform-api:<PREVIOUS_TAG>`).
2. Deploy the previous image:
   ```bash
   docker stop elegance-api
   docker rm elegance-api
   docker run -d --name elegance-api \
     --network elegance-net \
     -p 8080:8080 \
     --env-file /etc/elegance/api.env \
     interior-platform-api:<PREVIOUS_TAG>
   ```
3. Wait for readiness probe:
   ```bash
   curl -f http://localhost:8080/actuator/health/readiness
   ```
4. Confirm response: `{"status":"UP"}`.

### 3.3 Database Schema Rollback
Schema rollbacks must be handled with extreme care:
- **Additive Migrations (Standard):** If the new migration only added nullable columns or new tables, **no database rollback is necessary**. The previous backend version simply ignores the new columns/tables.
- **Destructive or Incompatible Migrations:** If a migration altered existing columns in a way that breaks the previous backend code:
  1. Halt backend application containers to prevent inconsistent writes.
  2. Restore the database from the snapshot taken immediately prior to deployment:
     ```bash
     # Drop and recreate database
     dropdb -U postgres -h db interior_platform_prod
     createdb -U postgres -h db interior_platform_prod
     # Restore from pre-release dump
     pg_restore -U postgres -h db -d interior_platform_prod /backups/pre_release_backup.dump
     ```
  3. Start the previous stable backend container.
  4. Verify data integrity and RLS policies.

---

## 4. Post-Rollback Validation Checklist

After completing any rollback procedure:
1. [ ] Check actuator endpoints: `/actuator/health/liveness` and `/actuator/health/readiness`.
2. [ ] Run `scripts/release-smoke-test.ps1` against the rolled-back deployment.
3. [ ] Verify logs for absence of recurring `500 Internal Server Error` or DB connection exceptions.
4. [ ] Notify engineering team and platform owner.
5. [ ] Open an incident post-mortem issue with logs and failure telemetry.
