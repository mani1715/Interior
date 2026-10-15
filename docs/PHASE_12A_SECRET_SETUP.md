# Phase 12A: Secret and Environment Variable Configuration

## 1. Overview

This document specifies the exact environment variable schema required for Cloudflare R2 object storage and ClamAV malware scanning across environments.

> **CRITICAL SECURITY REQUIREMENT**:
> Zero secrets may be committed to this repository. All production keys, tokens, and credentials must be injected dynamically via environment variables or secret management vaults.

---

## 2. Environment Variable Schema

| Variable Name | Required in Prod? | Default (Dev) | Example Value (Production) | Description |
| :--- | :--- | :--- | :--- | :--- |
| `STORAGE_PROVIDER` | Yes | `local` | `s3` | Storage backend SPI implementation (`local` or `s3`) |
| `S3_ENDPOINT` | Yes (if `s3`) | `""` | `https://<ACCOUNT_ID>.r2.cloudflarestorage.com` | Cloudflare R2 S3-compatible API endpoint |
| `S3_ACCESS_KEY_ID` | Yes (if `s3`) | `""` | `<R2_API_ACCESS_KEY_ID>` | Cloudflare R2 API token access key ID |
| `S3_SECRET_ACCESS_KEY` | Yes (if `s3`) | `""` | `<R2_API_SECRET_ACCESS_KEY>` | Cloudflare R2 API token secret access key |
| `S3_BUCKET_PRIVATE` | Yes (if `s3`) | `""` | `elegance-media-private` | Private bucket for documents and staged uploads |
| `S3_BUCKET_PUBLIC` | Yes (if `s3`) | `""` | `elegance-media-public` | Public bucket for CDN-distributed imagery |
| `S3_REGION` | No | `auto` | `auto` | S3 region (Cloudflare R2 defaults to `auto`) |
| `MEDIA_PUBLIC_BASE_URL`| Yes (if `s3`) | `""` | `https://media.elegance.app` | Public CDN base URL resolving public media files |
| `MALWARE_SCANNER_PROVIDER`| Yes | `fake` | `clamav` | Malware scanner implementation (`fake` or `clamav`) |
| `CLAMAV_HOST` | Yes (if `clamav`)| `localhost` | `clamav` | Hostname or IP of ClamAV daemon |
| `CLAMAV_PORT` | No | `3310` | `3310` | Port for ClamAV daemon TCP socket |
| `CLAMAV_CONNECT_TIMEOUT_MS`| No | `3000` | `3000` | Socket connection timeout in milliseconds |
| `CLAMAV_READ_TIMEOUT_MS` | No | `10000` | `10000` | Socket read timeout in milliseconds |

---

## 3. Local Development (`.env`)

For local development without live cloud resources:
```dotenv
STORAGE_PROVIDER=local
MALWARE_SCANNER_PROVIDER=fake
```

To run with local Docker ClamAV:
```dotenv
MALWARE_SCANNER_PROVIDER=clamav
CLAMAV_HOST=localhost
CLAMAV_PORT=3310
```

---

## 4. Production Deployment Setup

When deploying to production infrastructure (e.g. Railway or Docker Swarm/Compose):
1. Configure environment variables in the container platform dashboard or secrets manager.
2. In `docker-compose.prod.yml`, the `api` service automatically consumes these variables:
   - `CLAMAV_HOST=clamav`
   - `MALWARE_SCANNER_PROVIDER=clamav`
   - S3/R2 variables passed through from environment.
3. Validate startup logs: `ProductionConfigurationValidator` will audit these variables on boot and refuse startup if `fake` scanner is used in production or if S3 credentials are missing when `STORAGE_PROVIDER=s3`.
