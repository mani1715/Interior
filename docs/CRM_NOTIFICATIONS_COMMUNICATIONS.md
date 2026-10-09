# CRM, Notifications & Communication Delivery Architecture
**Phase 7C Implementation Report & Architecture Specification**
**Product:** Elégance — Interior Designer Platform  
**Target Repository:** `c:\my projects\interior design`  
**Database Schema Version:** Flyway V031  

---

## 1. Executive Summary & Core Principle

In Phase 7C, Elégance establishes an operational, tenant-safe communication layer connecting CRM leads, team collaboration, client feedback, and notification delivery across the platform.

### Core Architectural Principle
A strict distinction is enforced across all domain workflows:
$$\text{EVENT OCCURRED} \neq \text{NOTIFICATION CREATED} \neq \text{EXTERNAL MESSAGE DELIVERED}$$

- **Event Occurred:** A business transaction succeeded in the database (e.g., client inquiry submitted, lead assigned, concept feedback submitted).
- **Notification Created:** An in-app notification record is persisted in the PostgreSQL database and broadcast to active studio members via Server-Sent Events (SSE).
- **External Message Delivered:** A third-party external channel (SMTP/SES or WhatsApp) has actually confirmed receipt and transmission of the message.

The system **never** reports "Email sent" or "WhatsApp sent" unless the underlying provider confirms it. When external providers are inactive or unconfigured, the system truthfully logs the delivery record with status `NOT_CONFIGURED`, maintaining an audit trail while never failing or rolling back the primary business transaction.

---

## 2. In-App Notification System Architecture

### 2.1 Authoritative Data Model
In-app notifications are stored in the authoritative `notifications` table, protected by Row-Level Security (RLS) ensuring strict tenant and recipient isolation.

```
+---------------------------------------------------------------+
|                      notifications                            |
+---------------------------------------------------------------+
| id              : UUID (v7 time-ordered PK)                   |
| studio_id       : UUID (FK designer_studios)                  |
| user_id         : UUID (FK users, recipient)                  |
| type            : VARCHAR(64) (NotificationType check)        |
| title           : VARCHAR(255)                                |
| message         : TEXT                                        |
| data_json       : JSONB (entity links, action routes)         |
| is_read         : BOOLEAN DEFAULT FALSE                       |
| read_at         : TIMESTAMPTZ NULL                            |
| created_at      : TIMESTAMPTZ DEFAULT NOW()                   |
+---------------------------------------------------------------+
```

### 2.2 Notification Types Expansion
Flyway migration `V031__communication_delivery_and_notification_expansion.sql` expanded the allowed notification types:
- `NEW_LEAD`: Dispatched to studio owners/admins when a public inquiry is submitted.
- `LEAD_ASSIGNED`: Dispatched to the assigned team member when a lead is delegated.
- `CLIENT_FEEDBACK_RECEIVED`: Dispatched when a client submits concept feedback.
- `CLIENT_APPROVED_CONCEPT`: Dispatched when a client approves an AI concept design.
- `CLIENT_REQUESTED_CHANGES`: Dispatched when a client requests revisions on a concept.
- `TEAM_INVITATION`: Dispatched to a team member upon joining or being invited.
- `REVIEW_INVITATION_READY`: Dispatched when a review invitation token session is ready for client dispatch.
- `PROJECT_PUBLISH_STATE_CHANGED`: Dispatched when project visibility changes.
- Studio lifecycle events: `STUDIO_MEMBER_ADDED`, `STUDIO_MEMBER_REMOVED`, `STUDIO_ROLE_CHANGED`, `VERIFICATION_UPDATE`.

### 2.3 Read Lifecycle & Self-Action Suppression
- **Unread Count:** Bounded, indexed query by `(user_id, studio_id, is_read)`.
- **Mark Read:** Single notification update (`markAsRead(id, user)`), verifying recipient ownership.
- **Mark All Read:** Batch update for all unread notifications belonging to the current user in the active studio.
- **Self-Action Suppression:** When an actor triggers an action (e.g., designer assigns a lead to themselves), the notification dispatch is suppressed to avoid redundant self-spam.

### 2.4 Real-Time SSE Streaming
- **Endpoints:** `GET /api/v1/workspace/notifications/stream`
- **Registry:** `SseConnectionRegistry` manages active `SseEmitter` instances keyed by user and studio context.
- **Heartbeat & Reconnection:** Automated 15-second heartbeat ping with keepalive comments. Reconnections gracefully restore stream without message duplication.

---

## 3. External Communication Delivery Pipeline

### 3.1 Data Model & Tenant Security
The `communication_deliveries` table tracks all external communication attempts:

```sql
CREATE TABLE communication_deliveries (
    id UUID PRIMARY KEY,
    studio_id UUID NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    recipient_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    channel VARCHAR(32) NOT NULL,            -- EMAIL, WHATSAPP, IN_APP
    event_type VARCHAR(64) NOT NULL,         -- NEW_LEAD, TEAM_INVITATION, etc.
    recipient VARCHAR(255) NOT NULL,         -- Email address or phone number
    subject_or_summary VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL,             -- PENDING, SENT, FAILED, NOT_CONFIGURED, DELIVERED
    provider VARCHAR(64) NOT NULL,           -- DISABLED, SES, WHATSAPP_DIRECT
    external_message_id VARCHAR(255),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    last_error TEXT,
    next_retry_at TIMESTAMPTZ,
    idempotency_key VARCHAR(255) UNIQUE,
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

- **Row-Level Security:** RLS is enabled and forced (`FORCE ROW LEVEL SECURITY`).
- **Isolation Policy:** Tenant isolation guarantees that cross-studio querying is rejected at the database engine level via `current_setting('app.current_studio_id', true)`.

### 3.2 Delivery Channels & Statuses
- **Channels:**
  - `EMAIL`: Handled via `TransactionalEmailService`.
  - `WHATSAPP`: Mode A (Direct `wa.me` customer-initiated) or Mode B (Managed provider).
  - `IN_APP`: In-app notification mirroring.
- **Statuses:**
  - `NOT_CONFIGURED`: Default when external provider credentials are not configured.
  - `PENDING`: Enqueued for dispatch.
  - `SENT`: Upstream provider confirmed message acceptance.
  - `FAILED`: Terminal failure after exhausting retry budget.
  - `RETRY_SCHEDULED`: Transient error; bounded retry scheduled.
  - `DELIVERED`: Delivery receipt confirmed (e.g., webhook callback).

### 3.3 Transaction Decoupling & Idempotency
- **Fail-Safe Decoupling:** External delivery attempts are wrapped in exception containment blocks. If SMTP fails or the provider returns an error, the caller's database transaction is **not** aborted. The failure is recorded in `communication_deliveries`.
- **Idempotency:** A unique `idempotency_key` (e.g., `team_invite_{inviteId}`, `review_invite_{inviteId}`) prevents duplicate deliveries on retried operations.

---

## 4. Email Template Service & Security Hardening

### 4.1 Injection Mitigation
The `EmailTemplateService` enforces strict security safeguards:
1. **XSS / HTML Injection Defense:** All dynamic parameters (`studioName`, `clientName`, `leadName`, `projectName`) are escaped using `HtmlUtils.htmlEscape(...)`.
2. **Host-Header Poisoning Defense:** Links and action URLs are constructed strictly against the canonical application base URL configured in application properties (`app.baseUrl`), never accepting raw client `Host` or `X-Forwarded-Host` headers.
3. **Dual-Part Content:** Generates both semantic HTML formatting and plaintext fallbacks.

### 4.2 Standard Templates
- `buildTeamInvitationEmail`: Studio team member invitations with direct accept links.
- `buildReviewInvitationEmail`: Client feedback and review invitation tokens.
- `buildNewLeadAlertEmail`: Owner notification on new CRM inquiry submission.
- `buildClientFeedbackNotificationEmail`: Designer notification on client review submission.

---

## 5. End-to-End Workflow Wiring

| Business Workflow | Trigger | In-App Notification | External Delivery Attempt | Status When Disabled |
|:---|:---|:---|:---|:---|
| **Public Lead Submission** | Visitor submits contact enquiry form | `NEW_LEAD` to studio owner | Alert email to studio owner | `NOT_CONFIGURED` |
| **Lead Assignment** | Studio admin assigns lead to designer | `LEAD_ASSIGNED` to assignee (suppressed if self) | None (In-App primary) | — |
| **Team Invitation** | Studio admin invites team member | None (Pending accept) | Invite email to invitee | `NOT_CONFIGURED` |
| **Review Invitation** | Studio creates review session for client | `REVIEW_INVITATION_READY` to creator | Invite email to client | `NOT_CONFIGURED` |
| **Client Feedback** | Client submits feedback / change request | `CLIENT_FEEDBACK_RECEIVED` or `CLIENT_REQUESTED_CHANGES` | Feedback email to designer | `NOT_CONFIGURED` |

---

## 6. Truthful Provider Status Matrix

| Channel | Current Provider Implementation | Configured? | Frontend / API Reporting |
|:---|:---|:---|:---|
| **In-App** | PostgreSQL `notifications` + SSE Stream | **YES (Authoritative)** | Real-time bell badge, unread list, SSE push |
| **Email** | `DisabledEmailProvider` | **NO** | `NOT_CONFIGURED` (truthful, never claims sent) |
| **WhatsApp** | `MODE_A_ACTIVE (Direct wa.me)` | **YES (Mode A)** | Client opens direct WhatsApp chat via `wa.me` link |
| **Managed WhatsApp**| `MODE_B_NOT_CONFIGURED` | **NO** | `NOT_CONFIGURED` (truthful) |
| **SMS / Push** | N/A | **NO** | Not implemented / unsupported |

---

## 7. Verification & Test Evidence

### 7.1 Backend Test Suite (Maven Surefire)
- Total Tests: **453**
- Passed: **453**
- Failures: **0**
- Errors: **0**
- Skipped: **0**

### 7.2 Dedicated PostgreSQL 18 RLS Security Suite
- Total Tests: **23**
- Passed: **23**
- Failures: **0**
- Errors: **0**
- Skipped: **0**
- Includes:
  - `PostgreSqlCommunicationDeliveryRlsTest`: 3 / 3 PASS (Cross-studio read isolation, tenant update isolation, cross-studio insert denial).
  - `PostgreSqlProjectRoomRlsTest`: 5 / 5 PASS.
  - `PostgreSqlReviewsVerificationCollectionsTest`: 4 / 4 PASS.
  - `PostgreSqlStudioTeamRlsTest`: 3 / 3 PASS.
  - `PostgreSqlLeadSecurityClosureTest`: 4 / 4 PASS.
  - `PostgreSqlAnalyticsBillingRlsTest`: 4 / 4 PASS.

### 7.3 Frontend Test Suite (Vitest)
- Test Files: **52 / 52 passed**
- Total Tests: **368 / 368 passed**
- Failures: **0**

### 7.4 TypeScript & Linting & Production Build
- `npm run typecheck`: **Clean (0 errors)**
- `npm run lint`: **Clean (0 errors)**
- `npm run build`: **Compiled successfully, 28/28 static pages generated**
