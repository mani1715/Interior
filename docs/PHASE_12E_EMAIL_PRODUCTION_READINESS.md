# Phase 12E: Transactional Email & Communication Delivery Production Readiness

> **Document Type:** Production Readiness Architecture & Audit  
> **Status:** IMPLEMENTED | LOCALLY VALIDATED | CONTRACT TESTED | CONFIGURATION READY | LIVE PROVIDER VALIDATION DEFERRED TO PHASE 15  
> **Target Subsystem:** Transactional Email & Communication Delivery  
> **Context:** Elégance — Interior Designer Platform (`apps/api`)  
> **Current Flyway Migration Baseline:** `V040` (Unchanged — existing schema supports complete delivery lifecycle)

---

## 1. Executive Summary

Phase 12E delivers production readiness for the transactional email and communication subsystem of the Elégance Interior Designer Platform without creating, consuming, or requiring real paid email provider credentials.

The platform's transactional email system is strictly scoped to business-critical operational communications:
- **Studio Team Member Invitations**
- **Client Project Review Invitations**
- **New Designer Lead Alerts**
- **Client Feedback / Approval Alerts**
- **Subscription Lifecycle Notifications** (Activated, Renewed, Payment Failed, Cancel At Period End)
- **Account & Security Notifications** (Security Alert, Privacy Data Export Ready, Account Closure Confirmation)

**Zero Marketing / Promotional Mail:** The platform explicitly avoids promotional newsletters, campaign tracking pixels, or unsolicited marketing emails. Authentication is handled exclusively via OpenID Connect (Google OIDC), eliminating the attack surface of password-reset emails.

**Phase 15 Provider Activation Policy:** Production email credentials, domain DNS verification (SPF, DKIM, DMARC), MX records, and public webhook webhooks are intentionally deferred to Phase 15. The default configuration is `EMAIL_PROVIDER=disabled`. The system safely queues communications or logs structured events without errors when disabled, and the production startup validator (`ProductionConfigurationValidator`) strictly forbids mock/fake providers or placeholder credentials if an external provider is activated in `production`.

---

## 2. Source Audit & Component Inventory

| Component | Source File | Function & Purpose | Provider Specific? | Production Status | Test Status | Known Gap / Phase 15 Dependency |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `EmailProvider` | `com.interior.platform.email.provider.EmailProvider` | Interface defining `send(EmailMessage): EmailSendResult` | No | IMPLEMENTED | CONTRACT TESTED | None |
| `DisabledEmailProvider` | `...email.provider.DisabledEmailProvider` | Safe default implementation returning `SENT` with `disabled_provider` ID | No | IMPLEMENTED | CONTRACT TESTED | Intentional Phase 12E default |
| `FakeEmailProvider` | `...email.provider.FakeEmailProvider` | Test-only in-memory provider with sent-message queue | No | TEST ONLY | CONTRACT TESTED | Forbidden in `production` profile |
| `ResendEmailProvider` | `...email.provider.ResendEmailProvider` | HTTP/2 API adapter targeting Resend (`https://api.resend.com/emails`) | Resend | IMPLEMENTED | CONTRACT TESTED | Live API key & verified domain deferred to Phase 15 |
| `SesEmailProvider` | `...email.provider.SesEmailProvider` | AWS SES v2 SDK client adapter targeting regional SES | AWS SES | IMPLEMENTED | CONTRACT TESTED | AWS IAM credentials & domain verification deferred to Phase 15 |
| `EmailProperties` | `...email.config.EmailProperties` | Type-safe configuration properties for provider, sender, retry, and timeouts | No | IMPLEMENTED | LOCALLY VALIDATED | None |
| `EmailProviderConfig` | `...email.config.EmailProviderConfig` | Spring `@Configuration` conditionally instantiating selected provider bean | No | IMPLEMENTED | LOCALLY VALIDATED | None |
| `EmailMessage` | `...email.domain.EmailMessage` | Value object containing recipient, subject, HTML/text body, headers, metadata | No | IMPLEMENTED | CONTRACT TESTED | None |
| `EmailTemplateType` | `...email.domain.EmailTemplateType` | Typed enum of all approved transactional email template identifiers | No | IMPLEMENTED | CONTRACT TESTED | None |
| `EmailTemplateService` | `...email.service.EmailTemplateService` | High-fidelity HTML & plaintext template rendering with HTML entity escaping | No | IMPLEMENTED | CONTRACT TESTED | None |
| `CommunicationDeliveryService`| `...email.service.CommunicationDeliveryService` | Idempotent dispatch, delivery logging, retry loop, and out-of-order defense | No | IMPLEMENTED | CONTRACT TESTED | None |
| `EmailWebhookController` | `...email.web.EmailWebhookController` | Signature-verified webhook receiver for Resend and AWS SES/SNS bounce/delivery events | Both | IMPLEMENTED | CONTRACT TESTED | Live webhook signing secret deferred to Phase 15 |
| `ProductionConfigurationValidator`| `...config.ProductionConfigurationValidator` | Fail-fast startup validator verifying email provider credentials in production | No | IMPLEMENTED | CONTRACT TESTED | None |

---

## 3. Candidate Production Email Providers

Two production-grade candidate providers have been fully implemented behind the `EmailProvider` abstraction:

### Primary Candidate: Resend (`ResendEmailProvider`)
- **Transport:** Native Java 21 `HttpClient` using HTTP/2, bounded connection/request timeouts (15s default).
- **Endpoint:** Canonical Resend REST API `https://api.resend.com/emails`.
- **Payload:** Structured JSON payload with `from`, `to`, `reply_to`, `subject`, `html`, `text`, `headers`, and `tags`.
- **Error Mapping:**
  - `400 Bad Request`: Validation failure (malformed payload or invalid sender format).
  - `401 Unauthorized`: API key revoked, invalid, or missing.
  - `422 Unprocessable`: Recipient domain has no MX records or unverified sender domain.
  - `429 Rate Limit`: Upstream rate limit exceeded; marks delivery as transient retryable failure.
  - `5xx Server Error`: Resend service failure; triggers scheduled exponential backoff retry.
  - `TimeoutException`: Network boundary timeout mapped cleanly to transient failure.

### Secondary Candidate: AWS SES (`SesEmailProvider`)
- **Transport:** AWS Java SDK v2 (`software.amazon.awssdk:sesv2:2.29.51`), sharing dependencies with AWS S3 storage.
- **Client:** `SesV2Client` configured with bounded HTTP timeouts (15s), AWS regional endpoint, and credentials provider (`DefaultCredentialsProvider` with fallback to explicit key/secret).
- **Error Mapping:**
  - `LimitExceededException` / `TooManyRequestsException`: Rate limit or daily quota exceeded (transient failure).
  - `AccountSuspendedException` / `MailFromDomainNotVerifiedException`: Domain verification or IAM issue (permanent failure).
  - `BadRequestException`: Malformed message parameters (permanent failure).
  - `SesV2Exception` (5xx): AWS server issue (transient retryable failure).

---

## 4. Delivery Lifecycle & State Machine

```
               [Initiate Communication]
                         |
                         v
              +----------------------+
              |  Insert Local Record | (Status: PENDING)
              +----------------------+
                         |
                         v
              +----------------------+
              |   Provider Dispatch  |
              +----------------------+
                /                  \
   (Success / Disabled)         (Failure / Quota)
              /                      \
             v                        v
  +--------------------+    +--------------------+
  | Status: SENT       |    | Status: FAILED     |
  | Message ID captured|    | (Retryable or Term)|
  +--------------------+    +--------------------+
             |
             v  <--- Webhook Ingest (Resend / AWS SES SNS)
  +---------------------------------------------+
  |             Out-of-Order Defense            |
  | (Monotonic status guard: SENT -> DELIVERED) |
  | (Late SENT events cannot overwrite DELIVERED|
  |  or terminal BOUNCED / COMPLAINT records)   |
  +---------------------------------------------+
             |
     +-------+-------+
     |               |
     v               v
+-----------+  +-------------------+
| DELIVERED |  | BOUNCED/COMPLAINT | (Terminal failure; triggers alert)
+-----------+  +-------------------+
```

### Out-of-Order Webhook Delivery Defense
Because cloud delivery networks and SNS message dispatchers can deliver webhook events out of sequence (e.g. `delivered` arriving before `sent`, or duplicate `sent` events arriving after a `bounce`), `CommunicationDeliveryService` implements an idempotent monotonic status guard:
```java
// If current status is DELIVERED, ignore late SENT or PENDING
if (currentStatus == CommunicationDeliveryStatus.DELIVERED && 
    (newStatus == CommunicationDeliveryStatus.SENT || newStatus == CommunicationDeliveryStatus.PENDING)) {
    return;
}
// If current status is BOUNCED or COMPLAINT (terminal), do not allow downgrade
if (currentStatus == CommunicationDeliveryStatus.BOUNCED || currentStatus == CommunicationDeliveryStatus.COMPLAINT) {
    return;
}
```

---

## 5. Security & Escaping Standards

1. **HTML Entity Escaping:** All user-supplied variables (studio names, client names, project titles, notes) are sanitized through OWASP-recommended HTML character escaping (`&` $\to$ `&amp;`, `<` $\to$ `&lt;`, `>` $\to$ `&gt;`, `"` $\to$ `&quot;`, `'` $\to$ `&#x27;`).
2. **CRLF Header Injection Defense:** Recipient addresses, from addresses, reply-to addresses, and email subjects are strictly sanitized against carriage return (`\r`) and line feed (`\n`) characters to prevent header injection and BCC spam injection.
3. **Secret Masking:** In all debug and info logs, recipient email addresses are masked (`c***@example.com`), and all API keys, webhook secrets, and AWS credentials are strictly excluded from log statements.
4. **Canonical URL Base Normalization:** All action links and CTA buttons construct URLs using the configured canonical base URL (`app.canonical-base-url`), eliminating open redirect and Host-header poisoning risks.
5. **No Password Reset Vulnerability:** The platform does not implement email-based password reset tokens. All authentication occurs via OIDC.
6. **Privacy Export Safety:** Data export notifications do not attach ZIP archives directly to emails (which could be intercepted or bounce due to attachment limits). Instead, they provide a time-limited, authenticated link to the secure user settings portal.

---

## 6. Flyway & Database Verification

The database schema supporting transactional communications was inspected and confirmed at **Flyway V040**:
- Table `communication_deliveries` stores `id` (UUID), `channel` (EMAIL), `recipient`, `template_name`, `status`, `provider_message_id`, `provider_name`, `error_message`, `sent_at`, `delivered_at`, `created_at`, `updated_at`.
- Existing indexes (`idx_communication_deliveries_status`, `idx_communication_deliveries_provider_msg`) provide high-speed lookups for status transitions and webhook correlation.
- **Result:** No new database migration or schema modification was necessary for Phase 12E. Canonical Flyway baseline remains intact at `V040`.

---

## 7. Verification & Test Suite Summary

The email subsystem was verified using 40 dedicated unit and contract tests across 5 test suites:
- `ResendEmailProviderContractTest`: 11 passed (Success dispatch, 400, 401, 422, 429, 503, timeouts, missing ID, CRLF recipient, CRLF subject, unconfigured).
- `SesEmailProviderContractTest`: 8 passed (Success dispatch, 429 rate limit, unverified domain, bad request, 503 unavailable, client timeout, CRLF defense, unconfigured).
- `EmailDisabledWorkflowTest`: 5 passed (Studio invite with disabled provider, review invite with disabled provider, out-of-order delivered defense, out-of-order bounce defense, unknown message ID tolerance).
- `EmailSecurityAndEscapingTest`: 7 passed (XSS script escaping on studio invite, HTML escaping on review invite, privacy export escaping, CRLF header injection neutralization, multilingual Telugu/Hindi/Tamil UTF-8 preservation, large input bounds, URL base normalization).
- `ProductionConfigurationValidatorEmailTest`: 9 passed (Disabled provider clean pass in production, mock provider rejected in production, placeholder API keys rejected, invalid sender rejected, SES unconfigured rejected, SES valid passes, Resend valid passes).

Total: **40 Tests Run, 0 Failures, 0 Errors, 0 Skipped.**
