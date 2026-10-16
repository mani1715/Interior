# Phase 12E: Transactional Email Template Inventory

> **System Component:** `com.interior.platform.email.service.EmailTemplateService`  
> **Template Type Enum:** `com.interior.platform.email.domain.EmailTemplateType`  
> **Scope:** Strictly Transactional Operational Communications (No Marketing / Newsletters)

---

## 1. Approved Transactional Template Catalog

| Template Identifier (`EmailTemplateType`) | Business Trigger & Purpose | Target Recipient | Rendered Subject Line | Dynamic Content Variables | Escaped & Sanitized? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `STUDIO_TEAM_INVITATION` | Studio Owner/Admin invites a designer or team member | Invited Designer | `Invitation to join {studioName} on Elégance` | `studioName`, `inviterName`, `role`, `inviteUrl` | YES (HTML entities + CRLF defense) |
| `CLIENT_REVIEW_INVITATION` | Interior Designer invites a homeowner to review/approve project concepts | Client / Homeowner | `Review your interior design project: {projectName}` | `clientName`, `studioName`, `projectName`, `reviewUrl`, `note` | YES (HTML entities + CRLF defense) |
| `NEW_LEAD_ALERT` | Client submits inquiry or project request via public studio profile | Studio Admin / Owner | `New client inquiry from {clientName}` | `clientName`, `clientEmail`, `clientPhone`, `projectType`, `budget`, `message` | YES (HTML entities + CRLF defense) |
| `CLIENT_FEEDBACK_ALERT` | Client approves or comments on a design concept/iteration | Interior Designer | `New feedback on project: {projectName}` | `projectName`, `clientName`, `feedbackSummary`, `projectUrl` | YES (HTML entities + CRLF defense) |
| `SUBSCRIPTION_ACTIVATED` | Studio successfully subscribes to an Elégance tier (Pro / Studio) | Studio Owner | `Your Elégance subscription is active` | `studioName`, `planName`, `billingCycle`, `renewalDate` | YES (HTML entities + CRLF defense) |
| `PAYMENT_FAILED` | Recurring subscription invoice payment failed | Studio Owner | `Action required: Payment failed for your Elégance subscription` | `studioName`, `planName`, `retryDate`, `billingPortalUrl` | YES (HTML entities + CRLF defense) |
| `SUBSCRIPTION_RENEWED` | Recurring subscription payment successful | Studio Owner | `Receipt for your Elégance subscription renewal` | `studioName`, `planName`, `invoiceAmount`, `nextBillingDate`, `receiptUrl` | YES (HTML entities + CRLF defense) |
| `CANCEL_AT_PERIOD_END` | Studio requests subscription cancellation at end of current period | Studio Owner | `Confirmation: Subscription scheduled for cancellation` | `studioName`, `planName`, `accessEndDate` | YES (HTML entities + CRLF defense) |
| `SECURITY_ALERT` | Critical account security event (e.g. new device/session or role promotion) | Any User | `Security alert for your Elégance account` | `userName`, `eventType`, `eventTimestamp`, `ipAddress`, `securityPortalUrl` | YES (HTML entities + CRLF defense) |
| `PRIVACY_EXPORT_READY` | User requested GDPR / DPDP personal data export | Any User | `Your Elégance data export is ready` | `userName`, `downloadUrl`, `expiresInHours` | YES (HTML entities; No direct attachment) |
| `ACCOUNT_CLOSURE_CONFIRMATION`| User or admin closes studio/user account | Closed User | `Your Elégance account has been closed` | `userName`, `closureDate`, `retentionPolicySummary` | YES (HTML entities + CRLF defense) |

---

## 2. Rendering & Escaping Security Rules

1. **HTML Entity Sanitization:**
   All user-provided strings (including `studioName`, `clientName`, `projectName`, `note`, `feedbackSummary`) are filtered through `escapeHtml(...)`:
   - `&` becomes `&amp;`
   - `<` becomes `&lt;`
   - `>` becomes `&gt;`
   - `"` becomes `&quot;`
   - `'` becomes `&#x27;`
   This eliminates Cross-Site Scripting (XSS) vectors in web-based email clients.

2. **CRLF Header Injection Neutralization:**
   All subject lines, recipient mailboxes, and reply-to addresses are stripped of carriage returns (`\r`) and line feeds (`\n`).

3. **Multilingual UTF-8 Encoding:**
   Templates preserve non-Latin UTF-8 characters (e.g., Telugu, Hindi, Tamil, French, German) properly without breaking email clients or escaping non-ASCII typography.

4. **Canonical URL Base Normalization:**
   All links (e.g., `inviteUrl`, `reviewUrl`, `billingPortalUrl`, `downloadUrl`) are built using the application's configured canonical base URL (`app.canonical-base-url`), preventing Host header poisoning.

5. **Attachment-Free Privacy Exports:**
   Data export emails provide an authenticated, time-limited link to the user's secure account portal rather than attaching a raw ZIP archive directly to the email.
