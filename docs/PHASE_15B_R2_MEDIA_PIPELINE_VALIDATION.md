# Phase 15B — Media Pipeline & Asset Lifecycle Live Validation

## 1. Overview & Pipeline Philosophy

The **Elégance — Interior Designer Platform** enforces a zero-trust, asynchronous media ingestion pipeline. No client binary is ever trusted or exposed directly to end users upon upload. All interior design photography, 3D renderings, and floorplans must pass through a multi-stage security, sanitization, and optimization funnel before reaching `READY` status.

```
Client Browser               API Backend                 Cloudflare R2              ClamAV Daemon
     │                           │                             │                         │
     │ 1. POST /media/intents    │                             │                         │
     │──────────────────────────>│                             │                         │
     │   (filename, size, MIME)  │ [Check studio storage quota]│                         │
     │                           │ [Generate UUIDv7 & intent]  │                         │
     │<──────────────────────────│                             │                         │
     │ 2. Presigned PUT URL + ID │ (points to pending/... key) │                         │
     │                           │                             │                         │
     │ 3. Direct HTTP PUT Binary ─────────────────────────────>│                         │
     │                           │                             │                         │
     │ 4. POST /media/confirm    │                             │                         │
     │──────────────────────────>│                             │                         │
     │                           │ 5. GetObject from pending/ ─>│                         │
     │                           │<────────────────────────────│                         │
     │                           │ 6. Stream bytes to ClamAV ───────────────────────────>│
     │                           │<─────────────────────────────────── CLEAN / INFECTED ─│
     │                           │                             │                         │
     │                           │ 7. Sanitize EXIF / Privacy  │                         │
     │                           │ 8. Generate WebP Derivatives│                         │
     │                           │                             │                         │
     │                           │ 9. CopyObject to canonical ─>│                         │
     │                           │ 10. DeleteObject from pend.─>│                         │
     │                           │ 11. PutObject derivatives ──>│                         │
     │                           │                             │                         │
     │                           │ 12. DB State -> READY       │                         │
     │<──────────────────────────│                             │                         │
     │ 200 OK (READY)            │                             │                         │
```

---

## 2. Stage-by-Stage Media Lifecycle

### Stage 1: Upload Intent & Quota Reservation
1. The designer's browser sends an upload intent request (`POST /api/v1/studios/{studioId}/media/upload-intent`):
   - Parameters: `filename`, `contentType`, `byteSize`, `projectId`.
2. The platform validates:
   - File size does not exceed platform limits (Max 50 MB for high-res images, 100 MB for project packages).
   - Content-Type is on the approved MIME allowlist (`image/jpeg`, `image/png`, `image/webp`, `image/avif`).
   - The studio's remaining storage quota is verified against the database. If quota is exceeded, HTTP 422 `QUOTA_EXCEEDED` is returned.
3. The platform generates:
   - A unique `uploadIntentId` (UUIDv7).
   - An isolated quarantine object key: `pending/{studioId}/{uploadIntentId}/source.{ext}`.
   - A time-bounded 30-minute presigned PUT URL via `S3Presigner`.
4. An `upload_intents` record is committed to PostgreSQL with status `INITIALIZED`.

### Stage 2: Direct Client Upload to Quarantine
1. The client browser uploads the file directly to Cloudflare R2 using standard `fetch` / `XMLHttpRequest` via the presigned PUT URL.
2. The backend server does not proxy the raw upload, protecting server network bandwidth and memory.
3. If the browser upload fails or times out, the `upload_intents` record naturally expires after 30 minutes.

### Stage 3: Confirmation & Object Verification
1. Upon successful upload, the client calls `POST /api/v1/studios/{studioId}/media/upload-intent/{intentId}/confirm`.
2. The platform issues an `s3Client.headObject()` request to Cloudflare R2 to verify:
   - The object physically exists in the quarantine prefix.
   - The actual uploaded byte size matches the reserved intent size (± tolerances).
   - Non-empty byte count (`contentLength > 0`).

### Stage 4: ClamAV TCP Streaming Malware Inspection
1. The platform streams the object bytes from R2 via `s3Client.getObjectAsBytes()` to the ClamAV daemon over TCP socket using the `zINSTREAM` command.
2. If ClamAV reports `FOUND [malware-name]`:
   - The quarantine object in R2 is immediately scheduled for deletion.
   - The database record is marked as `INFECTED`.
   - An administrative security alert is dispatched.
   - HTTP 422 `MALWARE_DETECTED` is returned to the client.
3. If ClamAV is offline or unreachable:
   - If `app.media.security.require-clean-scan=true` (production default), the upload **fails closed** with HTTP 503 `SCANNER_UNAVAILABLE`. No unscanned binary is ever promoted to canonical storage.

### Stage 5: EXIF & Privacy Sanitization
1. Living spaces and residential client projects frequently contain sensitive location metadata (GPS coordinates, camera serial numbers, date/time stamps).
2. The platform decodes the image using Java `ImageIO` / metadata extractors and strips:
   - `GPSLatitude`, `GPSLongitude`, `GPSAltitude`
   - Camera make, model, serial number
   - Original creation timestamps and author tags
3. Color space ICC profiles (sRGB / Display P3) are preserved to maintain interior color fidelity.

### Stage 6: Promotion & Derivative Generation
1. The sanitized original is promoted to canonical private storage:
   - Source: `pending/{studioId}/{intentId}/source.{ext}`
   - Target: `studio/{studioId}/projects/{projectId}/original/{mediaId}.{ext}`
   - Execution: `s3Client.copyObject()` followed by `s3Client.deleteObject()` on the quarantine key.
2. Public web derivatives are generated:
   - Thumbnail (`400x300` WebP)
   - Medium (`800x600` WebP)
   - Large / Hero (`1920x1080` WebP)
3. Derivatives are stored in the public bucket:
   - Target: `public/studio/{studioId}/projects/{projectId}/derivatives/{mediaId}_{size}.webp`
   - Cache-Control header applied: `public, max-age=31536000, immutable`.

### Stage 7: Database Commitment & Quota Accounting
1. A permanent record in the `media` table is inserted with status `READY`.
2. The studio's actual consumed storage bytes in `studios` table is updated atomically.
3. The intent status is updated to `COMPLETED`.

---

## 3. Failure & Deletion Semantics

| Scenario | System Behavior |
|---|---|
| **Client Aborts Upload** | Quarantine key expires; nightly cleanup worker deletes unconfirmed `pending/` objects older than 24 hours. |
| **Malware Detected** | Quarantine object deleted immediately; intent marked `MALWARE_DETECTED`; studio quota reservation released. |
| **Invalid Image Bytes** | If image decoding fails (corrupted JPEG/PNG or fake extension), quarantine object deleted; intent marked `INVALID_PAYLOAD`. |
| **Media Deletion Request** | When a designer deletes an image: original deleted from private bucket; all derivatives deleted from public bucket; database row soft-deleted; quota decremented. |
