# Phase 12A.2 Runbook: Cloudflare R2 Live Cloud Validation

## 1. Overview

This runbook guides engineers through running the live Cloudflare R2 validation suite once actual cloud storage credentials have been provisioned in Phase 12A.2.

> **CRITICAL SECURITY RULE**: Never commit real Cloudflare API tokens, access keys, or secret access keys into git. Use environment variables or pass arguments into the PowerShell runner.

---

## 2. Cloudflare R2 Provisioning Steps

### 2.1. Bucket Creation
1. Log in to the Cloudflare Dashboard and select **R2** from the left navigation.
2. Click **Create Bucket**:
   - Private bucket name: `elegance-media-private` (location hint: default / closest to compute region).
   - Public bucket name: `elegance-media-public` (location hint: default).
3. On the `elegance-media-public` bucket:
   - Enable **Custom Domains** or enable **R2.dev subdomain** for public CDN distribution.

### 2.2. API Token Generation
1. In Cloudflare R2 dashboard, click **Manage R2 API Tokens**.
2. Click **Create API Token**.
3. Configure token:
   - **Token Name**: `elegance-api-production`
   - **Permissions**: `Object Read & Write`
   - **Apply to**: Specific buckets (`elegance-media-private`, `elegance-media-public`) or `All buckets`.
   - **TTL**: Appropriate expiration per organizational policy.
4. Note the generated:
   - **Access Key ID**
   - **Secret Access Key**
   - **Jurisdiction-specific / Account Endpoint**: `https://<ACCOUNT_ID>.r2.cloudflarestorage.com`

---

## 3. Executing Live Verification

### 3.1. Using Environment Variables
In a secure PowerShell terminal:

```powershell
$env:S3_ENDPOINT = "https://<ACCOUNT_ID>.r2.cloudflarestorage.com"
$env:S3_ACCESS_KEY_ID = "<YOUR_ACCESS_KEY_ID>"
$env:S3_SECRET_ACCESS_KEY = "<YOUR_SECRET_ACCESS_KEY>"
$env:S3_BUCKET_PRIVATE = "elegance-media-private"
$env:S3_BUCKET_PUBLIC = "elegance-media-public"
$env:S3_REGION = "auto"

powershell -ExecutionPolicy Bypass -File scripts/verify-r2-live-integration.ps1
```

### 3.2. Passing Parameters Directly
```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-r2-live-integration.ps1 `
    -Endpoint "https://<ACCOUNT_ID>.r2.cloudflarestorage.com" `
    -AccessKeyId "<YOUR_ACCESS_KEY_ID>" `
    -SecretAccessKey "<YOUR_SECRET_ACCESS_KEY>" `
    -PrivateBucket "elegance-media-private" `
    -PublicBucket "elegance-media-public" `
    -RequireLive
```

---

## 4. Verification Lifecycle & Safety Guarantees

When executed, the live runner:
1. Verifies that the interior repository context is intact.
2. Connects to the Cloudflare R2 endpoint using AWS SigV4 path-style access.
3. Allocates a unique namespace: `integration-tests/<UUID>/`.
4. Executes the 6 storage operations:
   - **PutObject**: Writes `live-contract-test.txt`.
   - **HeadObject**: Verifies payload size and content-type metadata.
   - **GetObject**: Downloads byte array and validates identical hash.
   - **CopyObject**: Copies test object to `live-contract-copy.txt`.
   - **DeleteObject**: Purges both test objects from R2.
   - **HeadObject (Post-deletion)**: Confirms `NoSuchKeyException`.
5. Guarantees safety:
   - Never deletes any object outside `integration-tests/<UUID>/`.
   - Never outputs secret access keys into console logs.

---

## 5. Troubleshooting

| Symptom | Cause | Solution |
| :--- | :--- | :--- |
| `SignatureDoesNotMatch` | Invalid Secret Access Key | Verify secret key copied without whitespace or quotes. |
| `AccessDenied` / `403 Forbidden` | API Token lacks write permissions | Ensure token permissions include `Object Read & Write`. |
| `NoSuchBucket` | Bucket name mismatch | Confirm bucket was created in the matching Cloudflare account. |
| `EndpointConnectionError` | Invalid R2 endpoint URL | Check format: `https://<ACCOUNT_ID>.r2.cloudflarestorage.com` without trailing bucket path. |
