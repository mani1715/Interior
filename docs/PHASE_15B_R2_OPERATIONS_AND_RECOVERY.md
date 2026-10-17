# Phase 15B — Cloudflare R2 Operations, Lifecycle & Disaster Recovery Runbook

## 1. Overview & Operational Principles

Cloudflare R2 provides durable, distributed object storage with zero egress fees, making it an ideal choice for high-resolution interior photography and architectural renderings. To maintain operational health, storage hygiene, and business continuity, this document outlines operational runbooks for:
- Quarantine lifecycle management
- Orphaned object pruning
- Cost management & zero-egress economics
- Disaster recovery & point-in-time sync

---

## 2. Automated Object Lifecycle Rules

To prevent storage bloat from aborted or abandoned client uploads, automated lifecycle expiration rules must be enabled on the private bucket.

### R2 Bucket Lifecycle Rule: Quarantine Pruning
- **Bucket**: `interior-platform-media-private`
- **Prefix / Rule Filter**: `pending/`
- **Action**: Delete expired objects
- **Retention Period**: `1 day` (24 hours)

**Rationale**:
Presigned upload URLs are valid for 30 minutes. Any object remaining under `pending/` for more than 24 hours represents an abandoned upload that was never confirmed or scanned. Deleting these automatically via R2 lifecycle rules ensures zero manual intervention and prevents unmonitored disk waste.

### Enabling Lifecycle via Cloudflare Dashboard:
1. Navigate to **R2** > `interior-platform-media-private` > **Settings**.
2. Under **Object Lifecycle Rules**, click **Add Rule**.
3. Name: `Prune Abandoned Quarantine Uploads`.
4. Prefix: `pending/`.
5. Lifecycle Action: `Delete object` after `1` day.
6. Click **Save Rule**.

---

## 3. Storage Accounting & Database Reconciliation

Storage quotas are tracked in the PostgreSQL `studios` table (`storage_used_bytes`). To maintain strict alignment between database records and physical R2 objects:

### Automated Audit Script:
A scheduled Spring Batch / background maintenance job periodically verifies:
1. Every `READY` media record in PostgreSQL has a corresponding object in R2 (`s3Client.headObject()`).
2. If an object is missing, the record is flagged for administrative review (`ORPHANED_RECORD`).
3. Total physical bucket bytes match the aggregate of `studio.storage_used_bytes`.

---

## 4. Disaster Recovery & Backup Strategy

Cloudflare R2 guarantees 99.999999999% (11 9's) annual durability across multiple availability zones within Cloudflare's global infrastructure. However, protection against catastrophic accidental deletion or credential compromise requires an off-site recovery plan:

### 1. Bucket Versioning
- Enable **Object Versioning** on `interior-platform-media-private` in Cloudflare R2.
- Protects against accidental overwrites or malicious batch deletions.
- Deleted objects receive a delete marker and can be restored immediately.

### 2. Secondary Offsite Backup (Cold Storage)
- For enterprise tiers or disaster recovery compliance, an automated quarterly sync copies canonical private originals (`studio/` prefix) to cold archive storage (e.g., AWS S3 Glacier Flexible Retrieval or Backblaze B2):
  ```bash
  # Example rclone sync command for cold backup
  rclone sync r2:interior-platform-media-private/studio/ b2:interior-cold-archive/studio/
  ```

### 3. Public Derivative Regeneration
- In the unlikely event that the public bucket `interior-platform-media-public` is lost or corrupted, **no original data is lost**.
- Public derivatives can be 100% regenerated on-demand by re-running the WebP transcoding pipeline against the high-resolution originals stored in the private bucket.

---

## 5. Incident Response & Playbooks

### Playbook A: Malicious Bulk Upload Spike Detected
1. **Diagnosis**: Spike in incoming `POST /media/upload-intent` calls.
2. **Action**:
   - Check Rate Limiting in Cloudflare WAF.
   - Restrict upload intent endpoint to authenticated designers with active sessions.
   - If necessary, reduce max concurrent uploads per studio in `application.yml` (`app.media.max-concurrent-uploads=5`).

### Playbook B: R2 API Credentials Compromised
1. **Action**:
   - Immediately log in to Cloudflare Dashboard > **R2** > **Manage R2 API Tokens**.
   - **Revoke** the compromised token immediately.
   - Generate a new API token with `Object Read & Write` scoped strictly to `interior-platform-media-private` and `interior-platform-media-public`.
   - Update `S3_ACCESS_KEY_ID` and `S3_SECRET_ACCESS_KEY` in local/container environment.
   - Restart the backend container (`docker compose restart api`).
   - Run `verify-r2-live-integration.ps1` to confirm connectivity.
