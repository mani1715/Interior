# Security Hardening & Threat Model

## 1. Overview & Threat Model

The Interior Design Platform employs defense-in-depth across the web frontend, REST API, session layer, and database.
Key threats mitigated:
- **Tenant Data Leakage / Cross-Tenant Access**: Enforced via PostgreSQL Row-Level Security (RLS) and server-side tenant isolation checks (`app.current_studio_id`).
- **Privilege Escalation**: Explicit platform-level role separation (`ADMIN`, `SUPER_ADMIN`) independent of tenant memberships.
- **Cross-Site Request Forgery (CSRF)**: Double-submit cookie with cryptographic verification on state-changing requests (`POST`, `PUT`, `DELETE`).
- **Session Hijacking**: HttpOnly, SameSite=Lax (or Strict), Secure cookies, cryptographically signed session tokens with constant-time equality checks.
- **Abuse & Scraping**: Tiered IP-based rate limiting on sensitive endpoints (auth, token exchange, reviews, analytics ingestion, admin).
- **Honeypot Bot Protection**: Hidden honeypot fields on public lead and review forms.

---

## 2. Row-Level Security (RLS) Boundaries

PostgreSQL enforces RLS on tenant-owned entities.
- Non-admin tenant queries require `SET LOCAL app.current_studio_id = '<studio_uuid>'`.
- In `studio_reviews`, public users can only read `status = 'PUBLISHED'` reviews.
- Audit events are strictly append-only; updates and deletions are rejected by DB triggers.

---

## 3. Rate Limiting Configuration

Sensitive routes are guarded by in-memory sliding-window bucket rate limiters:

| Route / Domain | Rate Limit | Behavior on Limit |
|---|---|---|
| `/api/v1/auth/exchange` | 10 req / min per IP | HTTP 429 Too Many Requests |
| `/api/v1/public/reviews/token/*` | 30 req / min per IP | HTTP 429 Too Many Requests |
| `/api/v1/public/reviews/submit` | 5 req / min per IP | HTTP 429 Too Many Requests |
| `/api/v1/public/reviews/*/report` | 10 req / min per IP | HTTP 429 Too Many Requests |
| `/api/v1/public/analytics/events` | 120 req / min per IP | HTTP 429 Too Many Requests |
| `/api/v1/admin/**` | 60 req / min per IP | HTTP 429 Too Many Requests |

---

## 4. Security Headers & TLS

Emitted automatically by `SecurityHeadersFilter` and Next.js configuration:
- `Content-Security-Policy`: Disallows unsafe object loading, frame embedding, and restricts script origins.
- `X-Frame-Options: DENY`: Prevents clickjacking.
- `X-Content-Type-Options: nosniff`: Prevents MIME-type sniffing.
- `Referrer-Policy: strict-origin-when-cross-origin`: Restricts referrer data leakage.
- `Strict-Transport-Security`: `max-age=31536000; includeSubDomains` (emitted only on HTTPS/secure connections).

---

## 5. Production Configuration Validator

The backend runs `ProductionConfigurationValidator` on Spring application startup:
- If `spring.profiles.active == 'production'`, it asserts that `app.security.dev-auth-enabled` is explicitly `false`.
- If dev-auth is detected in production, the application intentionally throws `IllegalStateException` and aborts JVM launch immediately.
- Prints a startup security configuration matrix logging the status of all operational providers.
