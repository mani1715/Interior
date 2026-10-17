# Environment Variable Inventory & Classification Schema

## 1. Classification Definitions

Every environment variable in Elégance is classified according to security tier and operational sensitivity:

- **`REQUIRED`:** Mandatory for application startup; missing value triggers fail-fast termination.
- **`OPTIONAL`:** Safe defaults exist; failure to supply defaults to disabled or safe fallback behavior.
- **`SECRET`:** Sensitive credential. MUST NEVER be committed to Git, baked into container images, or exposed in public client bundles.
- **`PUBLIC_BROWSER_SAFE`:** Variables exposed to the browser bundle via `NEXT_PUBLIC_*` prefix. Must never contain private keys, passwords, or internal tokens.
- **`SERVER_ONLY`:** Variables accessible strictly within server-side Node.js or JVM processes.
- **`PROVIDER_SPECIFIC`:** Variables required only when an external third-party integration is activated.
- **`OPERATIONAL`:** Operational tuning parameters (timeouts, pool sizes, log levels).

---

## 2. Complete Environment Variable Inventory

| Variable Name | Component | Required When | Secret? | Default Value | Production Required? | Phase 15 Provider | Validation Rule |
|---|---|---|---|---|---|---|---|
| `NODE_ENV` | Web | Always | No | `production` | Yes | N/A | Must be `production` |
| `PORT` | Web | Always | No | `3000` | Yes | N/A | Numeric port (1-65535) |
| `NEXT_PUBLIC_APP_URL` | Web | Production | No | Unset (Phase 16) | Yes (Phase 16) | Edge/Domain | Valid RFC 3986 HTTPS URL; no placeholders |
| `NEXT_PUBLIC_API_BASE_URL` | Web | Always | No | `/api/v1` | Yes | N/A | Relative `/api/v1` or absolute HTTPS URL |
| `INTERNAL_API_URL` | Web | Server runtime | No | `http://api:8080/api/v1` | Yes | N/A | HTTP/HTTPS internal network URL |
| `SPRING_PROFILES_ACTIVE` | API | Always | No | `production` | Yes | N/A | Must contain `production` |
| `SERVER_PORT` | API | Always | No | `8080` | Yes | N/A | Numeric port (1-65535) |
| `SERVER_FORWARD_HEADERS_STRATEGY` | API | Ingress Proxy | No | `framework` | Yes | N/A | `framework` or `native` |
| `SPRING_DATASOURCE_URL` | API | Always | No | None | Yes | Managed DB | Valid PostgreSQL JDBC URL; TLS required |
| `SPRING_DATASOURCE_USERNAME` | API | Always | No | None | Yes | Managed DB | Non-empty alphanumeric string |
| `SPRING_DATASOURCE_PASSWORD` | API | Always | **YES** | None | Yes | Managed DB | Non-empty secure secret string |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | API | Optional | No | `10` | No | Managed DB | Numeric >= 2 |
| `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE` | API | Optional | No | `2` | No | Managed DB | Numeric >= 1 |
| `APP_SECURITY_DEV_AUTH_ENABLED` | API | Production | No | `false` | Yes | N/A | Must be `false` in production |
| `APP_SECURITY_SESSION_COOKIE_SECURE` | API | Production | No | `true` | Yes | N/A | Must be `true` in production |
| `APP_SECURITY_SESSION_COOKIE_NAME` | API | Always | No | `__Host-session` | Yes | N/A | Strict cookie prefix `__Host-` |
| `APP_SECURITY_ALLOWED_ORIGINS` | API | Always | No | None | Yes | Edge/Domain | Comma-separated valid origins; no wildcards |
| `APP_SECURITY_MFA_ENCRYPTION_KEY` | API | Optional | **YES** | None | Yes (MFA) | N/A | 32-byte hex/base64 AES-256 key |
| `MEDIA_STORAGE_PROVIDER` | API | Always | No | `S3_COMPATIBLE` | Yes | Cloudflare R2 | `S3_COMPATIBLE` or `LOCAL` |
| `S3_ENDPOINT` | API | S3_COMPATIBLE | No | None | Yes (R2) | Cloudflare R2 | Valid HTTPS endpoint URL |
| `S3_REGION` | API | S3_COMPATIBLE | No | `auto` | Yes (R2) | Cloudflare R2 | Valid S3 region string |
| `S3_BUCKET_PRIVATE` | API | S3_COMPATIBLE | No | None | Yes (R2) | Cloudflare R2 | Valid RFC S3 bucket name |
| `S3_BUCKET_PUBLIC` | API | S3_COMPATIBLE | No | None | Yes (R2) | Cloudflare R2 | Valid RFC S3 bucket name |
| `S3_ACCESS_KEY_ID` | API | S3_COMPATIBLE | **YES** | None | Yes (R2) | Cloudflare R2 | Cloudflare R2 access key ID |
| `S3_SECRET_ACCESS_KEY` | API | S3_COMPATIBLE | **YES** | None | Yes (R2) | Cloudflare R2 | Cloudflare R2 secret access key |
| `PUBLIC_MEDIA_BASE_URL` | API | S3_COMPATIBLE | No | `/api/v1/media/public` | Yes (R2) | Cloudflare R2 | Relative or absolute CDN URL |
| `MALWARE_SCANNER_PROVIDER` | API | Always | No | `clamav` | Yes | ClamAV | `clamav` or `disabled` (`fake` forbidden) |
| `REQUIRE_CLEAN_SCAN` | API | Always | No | `true` | Yes | ClamAV | Boolean (`true` fail-closed) |
| `CLAMAV_HOST` | API | clamav scanner | No | `clamav` | Yes (clamav) | ClamAV | Valid resolvable hostname |
| `CLAMAV_PORT` | API | clamav scanner | No | `3310` | Yes (clamav) | ClamAV | Numeric port (default 3310) |
| `GOOGLE_OIDC_ISSUER` | API | OIDC enabled | No | `https://accounts.google.com` | If active | Google OIDC | Valid HTTPS issuer URL |
| `GOOGLE_CLIENT_ID` | API | OIDC enabled | No | None | If active | Google OIDC | Valid Google client ID string |
| `GOOGLE_CLIENT_SECRET` | API | OIDC enabled | **YES** | None | If active | Google OIDC | Valid Google client secret string |
| `GOOGLE_REDIRECT_URI` | API | OIDC enabled | No | None | If active | Google OIDC | Exact HTTPS OAuth redirect URI |
| `AI_VISUALIZER_ENABLED` | API | Optional | No | `true` | No | AI Candidate | Boolean |
| `AI_PROVIDER` | API | Always | No | `none` | Yes | AI Candidate | `none`, `replicate`, `stability`, `vertex` |
| `AI_API_KEY` | API | AI enabled | **YES** | None | If active | AI Candidate | Non-placeholder provider API key |
| `AI_MODEL` | API | AI enabled | No | `interior-diffusion-v1` | If active | AI Candidate | Must match allowed models list |
| `EMAIL_PROVIDER` | API | Always | No | `disabled` | Yes | Resend / SES | `disabled`, `resend`, `ses` |
| `EMAIL_FROM_ADDRESS` | API | Email enabled | No | None | If active | Resend / SES | Valid RFC 5322 email address |
| `RESEND_API_KEY` | API | Resend email | **YES** | None | If Resend | Resend | Valid non-placeholder API key |
| `AWS_SES_REGION` | API | SES email | No | `us-east-1` | If SES | AWS SES | Valid AWS region string |
| `AWS_SES_ACCESS_KEY_ID` | API | SES email | **YES** | None | If SES | AWS SES | Valid AWS IAM access key ID |
| `AWS_SES_SECRET_ACCESS_KEY` | API | SES email | **YES** | None | If SES | AWS SES | Valid AWS IAM secret key |
| `BILLING_PROVIDER` | API | Always | No | `disabled` | Yes | Razorpay/Stripe | `disabled`, `razorpay`, `stripe` |
| `RAZORPAY_KEY_ID` | API | Razorpay billing| No | None | If Razorpay | Razorpay | Valid non-placeholder key ID |
| `RAZORPAY_KEY_SECRET` | API | Razorpay billing| **YES** | None | If Razorpay | Razorpay | Valid secret key string |
| `RAZORPAY_WEBHOOK_SECRET` | API | Razorpay billing| **YES** | None | If Razorpay | Razorpay | Valid webhook secret string |
| `STRIPE_API_KEY` | API | Stripe billing | **YES** | None | If Stripe | Stripe | Valid non-placeholder API key |
| `STRIPE_WEBHOOK_SECRET` | API | Stripe billing | **YES** | None | If Stripe | Stripe | Valid webhook secret string |
| `OTEL_METRICS_EXPORT_ENABLED` | API | Optional | No | `false` | No | Observability | Boolean |
| `OTEL_EXPORTER_OTLP_ENDPOINT`| API | OTLP enabled | No | None | If active | Observability | Valid OTLP gRPC/HTTP endpoint |

---

## 3. Fail-Closed Validation Matrix

| Component | Disabled Allowed? | When Enabled Required Config | Placeholder Rejection | Failure Mode |
|---|---|---|---|---|
| **Database** | NO | JDBC URL, Username, Password | Rejects `jdbc:h2:*` and blank | `IllegalStateException` (abort startup) |
| **Session Security** | NO | `dev-auth-enabled=false`, `cookie-secure=true` | Fails if dev-auth=true or secure=false | `IllegalStateException` (abort startup) |
| **Object Storage** | YES (`LOCAL` allowed with warning) | `S3_ENDPOINT`, `S3_ACCESS_KEY_ID`, `S3_SECRET_ACCESS_KEY` | Fails if blank or unconfigured | `IllegalStateException` (abort startup) |
| **Malware Scanner** | YES (`disabled` allowed) | `CLAMAV_HOST`, `CLAMAV_PORT` | Rejects `fake` in production | `IllegalStateException` (abort startup) |
| **Google OIDC** | YES (`disabled` / unconfigured allowed) | `CLIENT_ID`, `CLIENT_SECRET`, `ISSUER`, `REDIRECT_URI` | Rejects `your-*`, `*placeholder*`, `change-me` | `IllegalStateException` (abort startup) |
| **AI Visualizer** | YES (`none` allowed) | `API_KEY`, `MODEL` | Rejects `fake`, `mock`, `your-*`, and unauthorized models | `IllegalStateException` (abort startup) |
| **Transactional Email** | YES (`disabled` allowed) | `FROM_ADDRESS`, provider API keys | Rejects `fake`, `mock`, `your-*`, and malformed email | `IllegalStateException` (abort startup) |
| **Commercial Billing** | YES (`disabled` allowed) | Gateway keys + trusted redirect origin | Rejects `fake`, `mock`, `your-*`, and untrusted origins | `IllegalStateException` (abort startup) |
| **Actuator Monitoring** | NO | Safe endpoints (`health,info,metrics,prometheus`) | Rejects wildcard `*` or dangerous endpoints (`env,beans...`) | `IllegalStateException` (abort startup) |
