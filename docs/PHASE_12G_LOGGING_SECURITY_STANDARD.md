# Elégance — Phase 12G: Logging Security & PII Redaction Standard

**Document:** Application & Security Logging Architecture Standard  
**Platform:** Elégance — Interior Designer Platform  
**Phase:** 12G Observability & Operational Readiness  

---

## 1. Principles

1. **Observability Must Never Become a Data-Leak Channel:**
   Application logs are operational diagnostic tools and must never be used to store or inadvertently capture user credentials, security secrets, authentication tokens, payment details, or raw PII.
2. **Ephemeral Application Logs vs. Immutable Audit Ledgers:**
   Application stdout logs are ephemeral and subject to rotation/truncation. Critical security and governance mutations (e.g. administrative actions, MFA resets, legal holds, billing events) are stored authoritatively in the PostgreSQL database (`audit_events` and respective domain ledgers), protected by PostgreSQL Row-Level Security.
3. **Log Injection Neutralization:**
   All user-controlled inputs must have carriage return (`\r`) and newline (`\n`) characters stripped or sanitized before emission, preventing forged JSON log entries or log-splitting attacks.

---

## 2. Redaction & Masking Rules

| Data Category | Handling Policy | Implementation Mechanism | Example Input | Logged Output |
|:---|:---|:---|:---|:---|
| **Passwords / PINs** | Complete Redaction | `JsonLoggingLayout` regex | `password=SecretPass123` | `password=[REDACTED]` |
| **Bearer Tokens** | Complete Redaction | `JsonLoggingLayout` regex | `Authorization: Bearer eyJ...` | `Bearer [REDACTED]` |
| **Session Cookies** | Complete Redaction | `JsonLoggingLayout` regex | `Cookie: __Host-session=xyz` | `session=[REDACTED]` |
| **OAuth / OIDC Codes** | Complete Redaction | `JsonLoggingLayout` regex | `code=4/0AY0...` | `code=[REDACTED]` |
| **API Keys / Secrets** | Complete Redaction | `JsonLoggingLayout` regex | `api-key=sk_live_123` | `api-key=[REDACTED]` |
| **Webhook Signatures** | Complete Redaction | `JsonLoggingLayout` regex | `signature=t=123,v1=abc` | `signature=[REDACTED]` |
| **Customer Email PII** | First-Character Masking | `JsonLoggingLayout` regex | `clara.oswald@domain.com` | `c***@domain.com` |
| **Request Query Secrets**| Query Redaction | `RequestCorrelationFilter` | `?token=abc&page=2` | `?token=[REDACTED]&page=2` |
| **Unbounded Payloads** | Truncation at 4096 chars | `JsonLoggingLayout` | 100 KB AI prompt / crash | Bounded to 4096 chars + `... [TRUNCATED]` |

---

## 3. Log Layouts by Environment

### 3.1 Non-Production Profile (`dev`, `test`, `local`)
- **Appender:** `ch.qos.logback.core.ConsoleAppender`
- **Pattern:** `%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) [%thread] [%X{requestId:-none}] %cyan(%logger{36}) : %msg%n`
- **Focus:** Developer readability, color-coded severity, clear correlation ID tracking.

### 3.2 Production Profile (`production`)
- **Appender:** `ch.qos.logback.core.ConsoleAppender` with `JsonLoggingLayout`
- **Format:** Single-line, machine-parseable JSON per log event:
  ```json
  {
    "timestamp": "2026-10-16T19:30:00.123Z",
    "level": "INFO",
    "service": "interior-platform-api",
    "environment": "production",
    "thread": "main",
    "logger": "com.interior.platform.billing.service.BillingService",
    "requestId": "01923000-0000-7000-8000-000000000001",
    "message": "Subscription plan transitioned successfully for studio",
    "exception": "Optional sanitized stack trace frames"
  }
  ```
- **Properties:**
  - `logging.level.root=INFO`
  - `logging.level.org.hibernate.SQL=WARN` (Zero SQL statement leakage in production)
  - `logging.level.org.hibernate.orm.jdbc.bind=WARN` (Zero SQL bind parameter leakage)
  - `logging.level.com.interior.platform=INFO` (No DEBUG/TRACE verbosity in production)

---

## 4. Retention & Access Control Policy

- **Commercial / Regulatory Log Retention:** `RETENTION POLICY DECISION REQUIRED` (Deferred to Phase 15 hosting provisioning).
- **Access Control:** Production stdout and centralized logs contain internal diagnostic metadata and must be strictly restricted to authenticated platform operators via VPC/IAM. No public log viewer or unauthenticated access is permitted.
