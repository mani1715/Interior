# Cloudflare R2 Object Storage Specification

## 1. Selected Provider & Status

- **Selected Provider:** Cloudflare R2 (S3-Compatible Object Storage).
- **Status:** Architecture and SDK adapter verified in Phase 12A; live account/bucket provisioning deferred to Phase 15.
- **Protocol:** AWS SDK for Java v2 (`software.amazon.awssdk:s3:2.29.51`).

---

## 2. Bucket Topology

| Bucket Purpose | Environment Variable | Access Tier | Contents |
|---|---|---|---|
| **Private Originals Bucket** | `S3_BUCKET_PRIVATE` | Strictly Private (Zero public access) | Uploaded master images, high-res project files, quarantine buffers, audit exports |
| **Public Derivatives Bucket** | `S3_BUCKET_PUBLIC` | Public Read via CDN / Custom Domain | Transformed, compressed responsive WebP derivatives (thumbnails, gallery, hero) |

---

## 3. Validated Exact SDK Operations

The platform's S3 adapter (`S3StorageService`) strictly utilizes the following 6 SDK operations:
1. `Presign PUT` (Generates time-bounded presigned upload URLs for browser direct uploads)
2. `PutObject` (Uploads processed derivatives or server-generated exports)
3. `GetObjectAsBytes` (Retrieves private objects for ClamAV streaming or export bundling)
4. `HeadObject` (Audits object existence, MIME type, and metadata)
5. `CopyObject` (Promotes clean scanned objects from quarantine to permanent storage)
6. `DeleteObject` (Purges deleted project assets or expired GDPR exports)

---

## 4. Minimum IAM / API Token Permissions

In Phase 15, the Cloudflare R2 API token MUST be provisioned with least-privilege permissions:
- **Permission Tier:** `Object Read & Write` on the designated private and public buckets only.
- **Forbidden Permissions:** No Account Administrator, Bucket Create, Bucket Delete, or Global Zone permissions.

---

## 5. CORS Configuration Template (For Presigned Uploads)

```json
[
  {
    "AllowedOrigins": [
      "${CANONICAL_PRODUCTION_ORIGIN}"
    ],
    "AllowedMethods": [
      "PUT",
      "GET"
    ],
    "AllowedHeaders": [
      "Content-Type",
      "Content-Length",
      "x-amz-meta-*"
    ],
    "ExposeHeaders": [
      "ETag"
    ],
    "MaxAgeSeconds": 3600
  }
]
```
> Note: `${CANONICAL_PRODUCTION_ORIGIN}` is populated in Phase 16 when the final production domain is selected.
