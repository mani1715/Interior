# Phase 14: Environment Variable Reference & Configuration Contracts

## 1. Variable Reference by Service

### 1.1. Web Frontend Service (`apps/web`)

| Variable Name | Required? | Secret? | Default Value | Description & Constraints |
|---|---|---|---|---|
| `NODE_ENV` | Yes | No | `production` | Node environment flag (`production` enables optimizations). |
| `PORT` | Yes | No | `3000` | Port on which the Next.js standalone server listens. |
| `NEXT_PUBLIC_APP_URL` | Yes (Ph 16) | No | Unset | Public canonical URL (e.g. `https://elegance.design`). Required for SEO canonical tags. |
| `NEXT_PUBLIC_API_BASE_URL` | Yes | No | `/api/v1` | Base URL used by client components. Relative `/api/v1` avoids CORS. |
| `INTERNAL_API_URL` | Yes | No | `http://api:8080/api/v1` | Internal container URL used by Next.js Server Components. |

---

### 1.2. Backend API Service (`apps/api`)

#### Core & Container Server
| Variable Name | Required? | Secret? | Default Value | Description & Constraints |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | No | `production` | Spring profile; must include `production` for security validators. |
| `SERVER_PORT` | Yes | No | `8080` | Port on which Spring Boot Tomcat listens. |
| `SERVER_FORWARD_HEADERS_STRATEGY` | Yes | No | `framework` | Reads `X-Forwarded-*` headers from edge proxy. |

#### Database & Connection Pool
| Variable Name | Required? | Secret? | Default Value | Description & Constraints |
|---|---|---|---|---|
| `SPRING_DATASOURCE_URL` | Yes | No | None | PostgreSQL JDBC connection URL. Must enforce TLS in production. |
| `SPRING_DATASOURCE_USERNAME` | Yes | No | None | PostgreSQL database user. |
| `SPRING_DATASOURCE_PASSWORD` | Yes | **YES** | None | PostgreSQL database password. Never log or commit. |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | No | No | `10` | Maximum connections per backend replica. |
| `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE` | No | No | `2` | Minimum idle connections maintained in pool. |

#### Security & Sessions
| Variable Name | Required? | Secret? | Default Value | Description & Constraints |
|---|---|---|---|---|
| `APP_SECURITY_DEV_AUTH_ENABLED` | Yes | No | `false` | Must remain `false` in production. |
| `APP_SECURITY_SESSION_COOKIE_SECURE` | Yes | No | `true` | Must remain `true` in production (`Secure` cookie flag). |
| `APP_SECURITY_SESSION_COOKIE_NAME` | Yes | No | `__Host-session` | Strict cookie name; enforces host-only scoping. |
| `APP_SECURITY_ALLOWED_ORIGINS` | Yes | No | None | Comma-separated whitelist of allowed CORS origins. |
| `APP_SECURITY_MFA_ENCRYPTION_KEY` | Optional | **YES** | None | 32-byte AES-256 key for TOTP MFA secret encryption at rest. |

#### Object Storage (Cloudflare R2 / S3)
| Variable Name | Required? | Secret? | Default Value | Description & Constraints |
|---|---|---|---|---|
| `MEDIA_STORAGE_PROVIDER` | Yes | No | `S3_COMPATIBLE` | `S3_COMPATIBLE` for R2 or `LOCAL` for test simulation. |
| `S3_ENDPOINT` | If R2 | No | None | Cloudflare R2 endpoint URL. |
| `S3_REGION` | If R2 | No | `auto` | R2 region identifier. |
| `S3_BUCKET_PRIVATE` | If R2 | No | `interior-platform-media-private` | Private bucket for masters and quarantine. |
| `S3_BUCKET_PUBLIC` | If R2 | No | `interior-platform-media-public` | Public bucket for WebP derivatives. |
| `S3_ACCESS_KEY_ID` | If R2 | **YES** | None | Cloudflare R2 API token access key. |
| `S3_SECRET_ACCESS_KEY` | If R2 | **YES** | None | Cloudflare R2 API token secret key. |
| `PUBLIC_MEDIA_BASE_URL` | If R2 | No | `/api/v1/media/public` | Public derivative delivery URL. |

#### Malware Scanning (ClamAV)
| Variable Name | Required? | Secret? | Default Value | Description & Constraints |
|---|---|---|---|---|
| `MALWARE_SCANNER_PROVIDER` | Yes | No | `clamav` | `clamav` or `disabled`. `fake` is strictly forbidden in prod. |
| `REQUIRE_CLEAN_SCAN` | Yes | No | `true` | When `true`, uploads fail-closed if scanner is unreachable. |
| `CLAMAV_HOST` | If clamav | No | `clamav` | Hostname of ClamAV daemon container. |
| `CLAMAV_PORT` | If clamav | No | `3310` | Port of ClamAV TCP streaming daemon. |

---

## 2. Deferred Provider Placeholders (Phase 15 Activation)

The following provider variables are established in code and configuration contracts, but default to `disabled` or `none` until Phase 15 pre-launch activation:

- **Google OIDC:** `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`
- **AI Visualizer:** `AI_PROVIDER=none`, `AI_API_KEY`, `AI_MODEL=interior-diffusion-v1`
- **Transactional Email:** `EMAIL_PROVIDER=disabled`, `RESEND_API_KEY`, `AWS_SES_ACCESS_KEY_ID`, `AWS_SES_SECRET_ACCESS_KEY`
- **Commercial Billing:** `BILLING_PROVIDER=disabled`, `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET`
- **Observability:** `OTEL_METRICS_EXPORT_ENABLED=false`, `OTEL_EXPORTER_OTLP_ENDPOINT`
