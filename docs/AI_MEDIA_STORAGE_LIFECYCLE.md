# AI + MEDIA STORAGE + UPLOAD LIFECYCLE & FAILURE RECOVERY

## Phase 7D Architectural Specification & Verification

### 1. Overview & Core Invariants
Phase 7D hardens the media storage engine, upload lifecycle state machine, AI job visualizer pipeline, and failure recovery protocols for the **Elégance — Interior Designer Platform**.

The media and AI processing boundaries strictly enforce the following five non-negotiable invariants:
1. **NO USER FILE IS LOST SILENTLY:** All uploads require two-phase commit via upload intents (`media_upload_intents`). Quarantine files are moved transactionally to permanent storage. If commit fails, files remain identifiable and cleanable.
2. **NO PRIVATE FILE BECOMES PUBLIC ACCIDENTALLY:** Visibility states (`CLIENT_PRIVATE`, `PORTFOLIO`, `REFERENCE`, `AI_CONCEPT`) are enforced. Private media cannot be enrolled in public portfolio without explicit status updates. `REFERENCE` and `CLIENT_PRIVATE` media are strictly rejected from public portfolio enrollment.
3. **NO FAILED UPLOAD CONSUMES QUOTA FOREVER:** Pending upload intents expire after 24 hours. The storage reconciliation job (`reconcileStorage`) cancels expired intents, releases reserved quota, and unlinks abandoned quarantine files.
4. **NO AI PROVIDER FAILURE CORRUPTS PROJECT/MEDIA STATE:** AI jobs (`ai_generation_jobs`) are fully decoupled from project state. Job failures cleanly transition to `FAILED` with explicit error codes (`AI_PROVIDER_ERROR`, `PROCESSING_TIMEOUT`). Stuck processing jobs are recovered via `reconcileStuckProcessingJobs`.
5. **NO CROSS-STUDIO MEDIA ACCESS IS POSSIBLE:** All queries and updates enforce `studio_id` matching in code and are guarded at the database level by PostgreSQL Row-Level Security (`FORCE ROW LEVEL SECURITY`) with `current_setting('app.current_studio_id', true)::uuid = studio_id`.

---

### 2. Media Upload State Machine & Two-Phase Commit

```
       +--------------------+
       |   createUploadIntent
       +--------------------+
                 |
                 v
       +--------------------+
       |      PENDING       | <---+ (Quota reserved: bytes & photos)
       +--------------------+     |
         /         |        \     |
(Upload) |  (Abort)| (Expiry) \ (User Cancel)
         v         v          v   |
+-----------+ +-----------+ +-----------+
| UPLOADED  | | CANCELLED | |  EXPIRED  |
+-----------+ +-----------+ +-----------+
     |
(Commit)
     v
+-----------+
| COMMITTED | ---> Creates/Updates `media_assets`
+-----------+      Upload intent linked to `media_asset_id`
```

#### Upload Intent Lifecycle States
- `PENDING`: Intent initialized, upload token generated, quota checked and reserved.
- `UPLOADED`: Client binary payload uploaded to quarantined storage (`quarantine/{studioId}/{intentId}.bin`).
- `COMMITTED`: Media asset validated, processed, derivatives generated, metadata extracted, and asset registered in `media_assets`. Idempotent: repeated commit with same intent returns existing `MediaAssetRecord`.
- `CANCELLED`: Client explicitly aborts intent before commit via `POST /media/upload-intent/{id}/cancel`. Quarantined file deleted; quota immediately released.
- `FAILED`: Validation or processing error occurred. Quarantined file cleaned up.
- `EXPIRED`: Pending intent exceeded TTL (24h) without commit. Reclaimed by automated reconciliation.

---

### 3. Storage Quota & Photo Limit Enforcement

```
Studio Quota Checks:
┌────────────────────────────────────────────────────────┐
│ Current Committed Bytes + Pending Intent Bytes         │
│                        + New Upload Size               │
│                                                        │
│                    <= Tier Limit (e.g., 5GB / 50GB)    │
└────────────────────────────────────────────────────────┘

Portfolio Photo Count Checks:
┌────────────────────────────────────────────────────────┐
│ Current Committed Portfolio Photos                     │
│                        + Pending Portfolio Intents     │
│                                                        │
│                    < Tier Max Allowed Photos           │
└────────────────────────────────────────────────────────┘
```

- **Entitlement Service:** Enforces storage bytes (`assertStorageQuotaAllowed`) and portfolio photo counts (`assertPhotoQuotaAllowed`).
- **Slot-Preserving Replacement:** `POST /api/v1/workspace/media/{mediaId}/replace` allows updating photo content without consuming additional photo slots. Quota validation only charges the delta `(newBytes - oldBytes)` if positive. Room association, cover flags, captions, focal points, and motion settings are preserved.

---

### 4. Security Hardening: SSRF & Decompression Bomb Defense

#### Server-Side Request Forgery (SSRF) Defense (`SsrfProtectionService`)
When resolving remote image URLs (e.g., AI reference inputs or remote asset fetching):
- **IP Resolution Check:** Resolves all host IP addresses before connection.
- **Address Range Blocking:** Rejects loopback (`127.0.0.0/8`, `::1`), link-local (`169.254.0.0/16`, `fe80::/10`), private networks (RFC1918: `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, RFC4193 `fc00::/7`), and AWS/cloud metadata services (`169.254.169.254`).
- **Scheme Validation:** Restricts to `http` and `https` only; rejects file, gopher, ftp, etc.
- **Redirect Validation:** Validates destination address at every redirect hop.
- **Bounded Stream:** Enforces max read limit (15MB) to protect memory.

#### Decompression Bomb & Malicious Media Protection (`ImageProcessingService`)
- **Magic Byte Verification:** Inspects image header magic bytes (JPEG `FF D8 FF`, PNG `89 50 4E 47`, WEBP `52 49 46 46...57 45 42 50`).
- **Dimension Caps:** Maximum dimension capped at 10,000px.
- **Pixel Count Caps:** Maximum uncompressed pixel area capped at 50 Megapixels ($50 \times 10^6$ pixels). Rejects oversized decompression bombs before memory exhaustion.

---

### 5. AI Generation Pipeline & Stuck Job Reconciliation

- **AI Concept Provenance:** Generated AI outputs are saved with `visibility_status = 'AI_CONCEPT'`. If enrolled in portfolio, AI badge watermark is mandatory.
- **Decoupled Execution:** Asynchronous job processing (`QUEUED` -> `PROCESSING` -> `COMPLETED` / `FAILED`).
- **Timeout & Failure Recovery:** Jobs remaining in `PROCESSING` status beyond timeout threshold (e.g., 10 minutes) are transitioned to `FAILED` with error code `PROCESSING_TIMEOUT` via `reconcileStuckProcessingJobs`.
- **Reconciliation Endpoint:** `POST /api/v1/workspace/ai/jobs/reconcile` allows manual or scheduled cleanup of orphaned/stuck AI generation tasks.

---

### 6. Storage Reconciliation API & Metrics

- `POST /api/v1/workspace/media/reconcile`
  Returns `StorageReconciliationReport`:
  - `studioId`: UUID of target studio.
  - `expiredIntentsPurged`: Number of abandoned pending upload intents transitioned to `EXPIRED`.
  - `quarantineBytesReclaimed`: Quota released from uncommitted intents.
  - `totalCommittedBytes`: Current active stored bytes.
  - `totalCommittedPhotos`: Total active committed media assets.
  - `storageLimitBytes`: Studio storage quota entitlement.

---

### 7. Verification Evidence

| Test Category | Suite / Class | Count | Result |
| :--- | :--- | :--- | :--- |
| Full Backend Test Suite | Maven Wrapper (`mvnw -f apps/api/pom.xml test`) | **468** | **468 / 468 PASS** |
| PostgreSQL RLS Security | `*RlsTest` (`PostgreSqlMediaLifecycleRlsTest`, `PostgreSqlAnalyticsBillingRlsTest`, `PostgreSqlCommunicationDeliveryRlsTest`, `PostgreSqlProjectRoomRlsTest`, `PostgreSqlStudioTeamRlsTest`) | **17** | **17 / 17 PASS** |
| Media Lifecycle Integration | `MediaUploadLifecycleTest` | **11** | **11 / 11 PASS** |
| AI Pipeline & Recovery | `AiVisualizerServiceTest` | **11** | **11 / 11 PASS** |
| Frontend Vitest Baseline | `npm --prefix apps/web test -- --run` | **368** | **368 / 368 PASS** |
| Frontend Typecheck | `npm --prefix apps/web run typecheck` | — | **0 Errors** |
| Frontend Lint | `npm --prefix apps/web run lint` | — | **0 Errors** |
