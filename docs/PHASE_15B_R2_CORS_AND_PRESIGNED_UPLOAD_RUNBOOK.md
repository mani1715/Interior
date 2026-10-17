# Phase 15B — Cloudflare R2 CORS & Direct Browser Upload Runbook

## 1. Overview

Because the Elégance platform uses direct browser uploads via presigned PUT URLs, client web browsers initiate cross-origin HTTP PUT requests directly to the Cloudflare R2 endpoint:
`https://<ACCOUNT_ID>.r2.cloudflarestorage.com/<BUCKET_NAME>/pending/...`

For modern browsers to permit these requests without triggering Cross-Origin Resource Sharing (CORS) security blocks, the private quarantine bucket must have an appropriate CORS policy configured.

---

## 2. Bucket CORS Policy Specification

### Recommended CORS JSON Policy:
```json
[
  {
    "AllowedOrigins": [
      "https://elegance.interior",
      "https://*.elegance.interior",
      "http://localhost:3000"
    ],
    "AllowedMethods": [
      "PUT",
      "GET",
      "HEAD"
    ],
    "AllowedHeaders": [
      "Content-Type",
      "Content-Length",
      "x-amz-*"
    ],
    "ExposeHeaders": [
      "ETag"
    ],
    "MaxAgeSeconds": 3600
  }
]
```

### Policy Breakdown:
- **`AllowedOrigins`**:
  - `https://elegance.interior` & subdomains: Production application origin.
  - `http://localhost:3000`: Local Next.js frontend development.
- **`AllowedMethods`**:
  - `PUT`: Required for uploading raw binary assets via presigned URLs.
  - `GET`, `HEAD`: Permitted for pre-flight verification and thumbnail checks.
- **`AllowedHeaders`**:
  - `Content-Type`: Required to send the image MIME type (`image/jpeg`, etc.).
  - `Content-Length`: Standard payload size header.
  - `x-amz-*`: Required for AWS SDK signature headers sent by some HTTP clients.
- **`ExposeHeaders`**:
  - `ETag`: Allows the frontend to receive the MD5/hash checksum calculated by R2 upon upload completion.
- **`MaxAgeSeconds`**:
  - `3600` (1 hour): Browsers cache CORS preflight `OPTIONS` responses, minimizing redundant network roundtrips.

---

## 3. Configuration Step-by-Step

### Option A: Via Cloudflare Dashboard
1. Log in to the [Cloudflare Dashboard](https://dash.cloudflare.com).
2. In the left navigation, select **R2 Object Storage** > **Overview**.
3. Click on the private bucket: `interior-platform-media-private`.
4. Navigate to the **Settings** tab.
5. Scroll down to the **CORS Policy** section and click **Add CORS policy** (or **Edit**).
6. Paste the JSON configuration block from Section 2 above.
7. Click **Save**.

### Option B: Via Cloudflare Wrangler CLI
If using Wrangler locally:
```bash
# Save policy to cors.json
wrangler r2 bucket cors set interior-platform-media-private --file cors.json
```

---

## 4. Frontend Browser Upload Implementation

Frontend components perform direct upload using standard browser `fetch`:
```typescript
async function uploadMediaFile(presignedUrl: string, file: File): Promise<string | null> {
  const response = await fetch(presignedUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': file.type,
    },
    body: file,
  });

  if (!response.ok) {
    throw new Error(`Upload to R2 failed with status: ${response.status}`);
  }

  // Extract ETag if returned
  return response.headers.get('ETag');
}
```

---

## 5. Troubleshooting CORS Issues

| Symptom | Root Cause | Solution |
|---|---|---|
| `Cross-Origin Request Blocked: The Same Origin Policy disallows reading the remote resource` | `AllowedOrigins` does not contain the current browser domain. | Add `http://localhost:3000` or the staging domain to the bucket CORS policy. |
| `Method PUT is not allowed by Access-Control-Allow-Methods` | `PUT` missing from `AllowedMethods`. | Ensure `"PUT"` is explicitly present in `AllowedMethods`. |
| `Request header field content-type is not allowed` | `Content-Type` header missing in `AllowedHeaders`. | Add `"Content-Type"` to `AllowedHeaders`. |
| SignatureDoesNotMatch error from R2 | Browser sent additional headers not included in the backend presign signature. | Ensure frontend only sends the exact headers signed by `S3Presigner` (specifically `Content-Type`). |
