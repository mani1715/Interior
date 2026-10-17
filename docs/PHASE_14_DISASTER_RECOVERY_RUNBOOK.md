# Phase 14 — Disaster Recovery Runbook

> **Status:** APPROVED FOR PRODUCTION RELEASE CANDIDATE  
> **Platform:** Elégance Interior Designer Platform  
> **Target Scenarios:** Total Cloud Outage, Database Corruption, Ransomware / Malicious Destruction  
> **RPO Target:** <= 1 hour  
> **RTO Target:** <= 2 hours  

---

## 1. Disaster Classification & Triage

| Tier | Severity Level | Example Scenario | Target RTO | Incident Commander |
|---|---|---|---|---|
| **Tier 1** | Catastrophic | Cloud region outage, total primary database loss | <= 2 Hours | Lead Platform Engineer |
| **Tier 2** | Critical | Storage bucket data loss, corrupted schema migration | <= 1 Hour | Lead Backend Engineer |
| **Tier 3** | Major | Single container crash-loop, partial service degradation | <= 15 Min | On-Call SRE |

---

## 2. Disaster Scenario Runbooks

### 2.1 Scenario A: Primary Database Region Outage / Failover
If the primary PostgreSQL database instance becomes unreachable or corrupted:

1. **Declare Incident:** Notify the platform owner and freeze all inbound deployments.
2. **Promote Standby / Secondary Replica (if configured):**
   - For managed databases (RDS, Supabase, Neon): Trigger automated or 1-click replica promotion in the management console.
3. **If Restoring from Off-site Backup:**
   - Provision a fresh PostgreSQL 16+ instance in an operational zone/provider.
   - Run the restore command using the latest encrypted backup (see `docs/PHASE_14_BACKUP_RESTORE_RUNBOOK.md`):
     ```bash
     pg_restore -h new-db-host -U postgres -d interior_platform_prod --clean --if-exists backup_latest.dump
     ```
   - Re-enable and verify Row-Level Security:
     ```sql
     SELECT count(*) FROM pg_tables WHERE schemaname = 'public' AND rowsecurity = true;
     -- Must match expected 59 tables
     ```
4. **Update Application Connection String:**
   - Update `SPRING_DATASOURCE_URL` in container environment or secret store.
   - Restart backend application containers.
5. **Verify Actuator Health:**
   - Confirm `/actuator/health` returns status `UP`.

---

### 2.2 Scenario B: Total Cloud Provider Outage (Full Re-platforming)
If the primary hosting provider (e.g. Railway, AWS, or VPS) suffers a total, unrecoverable multi-day outage:

1. **Target Secondary Infrastructure:**
   - Target an alternative Docker/Kubernetes host (e.g. alternative cloud provider or fallback VPS).
2. **Pull Immutable OCI Images:**
   - Pull container images from secondary container registry:
     ```bash
     docker pull ghcr.io/mani1715/interior-platform-api:0.1.0-rc1
     docker pull ghcr.io/mani1715/interior-platform-web:0.1.0-rc1
     ```
3. **Provision Database & Restore Backup:**
   - Restore PostgreSQL from off-site Cloudflare R2 / S3 backup bucket.
4. **Deploy Containers with Production Environment:**
   - Populate `.env` from secure encrypted password manager / secrets vault.
   - Launch containers using Docker Compose or Kubernetes manifests.
5. **Reroute DNS:**
   - In Cloudflare DNS or domain registrar, update `A` and `CNAME` records to point to the new provider ingress IP.
   - Set low TTL (300 seconds) during recovery.

---

### 2.3 Scenario C: Storage Bucket Corruption or Data Loss
If object storage (e.g. project blueprints, portfolio imagery, design assets) is lost or compromised:

1. **Storage Bucket Re-Creation:**
   - Create a fresh Cloudflare R2 / S3 bucket in a secure region.
2. **Restore Assets from Mirror/Secondary Sync:**
   - Sync data from the secondary backup bucket:
     ```bash
     aws s3 sync s3://backup-r2-mirror-bucket s3://production-interior-storage
     ```
3. **Trigger File Integrity Audit:**
   - Run storage consistency verification task in the backend to match database document/image records against object storage existence.

---

## 3. Communication Plan

- **Status Page:** Update customer-facing status page (e.g. `status.elegance.interior.design` or hosted third-party status).
- **Designer & Customer Notifications:** Send broadcast email regarding platform maintenance and estimated recovery time.
- **Incident Post-Mortem:** Within 48 hours of recovery, publish an internal Root Cause Analysis (RCA) document with prevention action items.
