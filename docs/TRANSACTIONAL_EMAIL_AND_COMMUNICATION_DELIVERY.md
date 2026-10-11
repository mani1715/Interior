# Transactional Email and Communication Delivery Architecture — Phase 9B

## 1. Executive Summary

Elégance implements a provider-neutral, truthful, observable transactional communication system. Live email infrastructure, third-party credentials, and direct marketing email systems are strictly forbidden at this phase. All transactional communications are dispatched via an interface abstraction (`EmailProvider`), defaulting to `DisabledEmailProvider` which returns `NOT_CONFIGURED` without failing or rolling back critical business operations.

For development, testing, and automated verification, `FakeEmailProvider` provides deterministic simulation of delivery acceptances, transient failures, permanent mailbox errors, and upstream exceptions.

Every dispatch attempt is immutably logged to the database table `communication_deliveries`, protected by PostgreSQL 18 Row Level Security (RLS) ensuring strict studio multi-tenant isolation.

---

## 2. Decision Record: Commercial Billing Grace Period

### Commercial Grace Period Audit Finding
In Phase 9A, `BillingService.java` contained a hardcoded 7-day grace period calculation (`now.plus(7, ChronoUnit.DAYS)`). This was identified during the Phase 9B audit as an unverified business assumption.

### Resolution & Server-Configurable Architecture
1. **Configurable Property**: Introduced `app.billing.grace-period-days` (defaulting to 7 days) injectable into `BillingService`.
2. **Server-Computed Date Formatting**: Transactional email templates render the server-computed exact date (`dd MMM yyyy`, e.g. `18 Oct 2026`) rather than hardcoding arbitrary day numbers into customer-facing communication copies.
3. **Formal Notification**: Marked as `COMMERCIAL GRACE PERIOD DECISION REQUIRED` for stakeholder approval prior to commercial release.

---

## 3. Communication Delivery State Machine

The delivery state machine is enforced by PostgreSQL database check constraints and Java domain enums (`DeliveryStatus`):

```
                        ┌──────────────┐
                        │   PENDING    │
                        └──────┬───────┘
                               │
                ┌──────────────┼──────────────┐
                ▼              ▼              ▼
       ┌────────────────┐ ┌─────────┐ ┌────────────────┐
       │ NOT_CONFIGURED │ │  SENT   │ │RETRY_SCHEDULED │
       └────────────────┘ └────┬────┘ └───────┬────────┘
                               │              │ (Backoff Retry)
                               │              ▼
                               │        ┌───────────┐
                               │        │  FAILED   │
                               │        └───────────┘
                ┌──────────────┼──────────────┐
                ▼              ▼              ▼
         ┌───────────┐   ┌───────────┐  ┌───────────┐
         │ DELIVERED │   │  BOUNCED  │  │ COMPLAINT │
         └───────────┘   └───────────┘  └───────────┘
```

| Status | Description | Terminal? |
|---|---|---|
| `PENDING` | Created prior to dispatch attempt. | No |
| `NOT_CONFIGURED` | Provider is disabled or unconfigured (`DisabledEmailProvider`). Never fakes delivery. | Yes |
| `SENT` | Successfully accepted by upstream provider/relay. | No |
| `DELIVERED` | Confirmed delivered to recipient mailbox via webhook event. | Yes |
| `RETRY_SCHEDULED` | Upstream returned a transient retryable error; exponential backoff scheduled. | No |
| `FAILED` | Permanent failure or maximum retries exhausted. | Yes |
| `BOUNCED` | Upstream provider reported bounce event (e.g. 550 mailbox unavailable). | Yes |
| `COMPLAINT` | Recipient marked email as spam / abuse complaint. | Yes |

---

## 4. Bounded Exponential Backoff & Multi-Instance Safety

### Concurrency Lock
Scheduled background retries via `CommunicationRetryScheduler` claim pending batches using PostgreSQL row-level locks:
```sql
SELECT * FROM communication_deliveries
WHERE status = 'RETRY_SCHEDULED'
  AND next_retry_at <= :now
ORDER BY next_retry_at ASC
LIMIT :batchSize
FOR UPDATE SKIP LOCKED;
```
This guarantees that multiple concurrent Spring Boot nodes or pods never process or double-dispatch the same delivery attempt.

### Backoff Formula
$$\text{delaySeconds} = \min\left(86400, \text{baseDelay} \times 2^{\text{attemptCount} - 1}\right)$$
- Base delay: `app.email.base-retry-delay-seconds` (default: 60s).
- Maximum retries: `app.email.max-retries` (default: 3).
- Hard ceiling: 24 hours (86,400s).

---

## 5. Security Invariants & Anti-Abuse Controls

1. **CRLF Header Injection Defense**: Every recipient email and subject header is scanned for `\r` and `\n` characters before dispatch. Malicious requests are rejected immediately and logged truthfully with zero impact on the caller.
2. **Strict HTML Escaping**: All dynamic variables (studio names, user names, role titles, project titles, client names) are escaped with Spring's `HtmlUtils.htmlEscape` before interpolation into email templates.
3. **Canonical Action URLs**: Relative paths are resolved strictly against the configured `app.baseUrl` (default: `http://localhost:3000`), never trusting incoming HTTP request headers like `Host` or `X-Forwarded-Host`.
4. **Token Security & Expiry**: Invitation tokens use cryptographically secure 256-bit random values. Expired or invalid tokens return generic responses preventing email or user enumeration.
5. **No Marketing / Bulk Emails**: Elégance contains zero marketing lists, zero newsletter systems, and zero bulk email blast features.
6. **Zero Tracking Pixels**: All rendered templates are pure semantic HTML with no 1x1 GIF tracking pixels or open-tracking redirectors.
7. **Zero Secret Logging**: Tokens, credentials, and webhook secrets are never logged in cleartext.

---

## 6. Provider Selection Status: Required Commercial Decisions

No live third-party email provider is selected or configured. Live credentials must NEVER be committed to this repository. The following items require formal commercial sign-off prior to production launch:

1. **Email Provider Selection Required**:
   - Evaluate options: Amazon SES (cost-effective for scale), Resend (developer-friendly DX), Postmark (dedicated transactional reputation).
   - Domain DNS authentication requirements: DKIM, SPF, DMARC (`p=reject`), and BIMI.
2. **Commercial Grace Period Duration**:
   - Recommended: 7 days.
   - Alternatives: 3 days, 14 days.
   - Entitlement behavior during grace: Read/write access preserved, visual warnings displayed in studio workspace.
3. **Support & Inbound Contacts**:
   - `support@elegance.design`
   - `billing@elegance.design`
   - `security@elegance.design`
