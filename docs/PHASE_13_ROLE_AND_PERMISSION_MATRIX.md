# Phase 13 — Authoritative Role & Permission Matrix

**Product:** Elégance — Interior Designer Platform  
**Phase:** 13 (Whole-Product End-to-End Certification & Release-Candidate Validation)  
**Status:** CERTIFIED  
**Date:** 2026-10-17  

---

> [!IMPORTANT]
> **CANONICAL ACTOR SOURCE TRUTH**  
> The Elégance platform defines two layers of authorization:
> 1. **Global Platform Roles:** `PUBLIC_VISITOR`, `CUSTOMER`, `DESIGNER`, `MODERATOR`, `ADMIN`, `SUPER_ADMIN`.
> 2. **Studio Tenancy Roles:** `DESIGNER_ADMIN` (Owner/Lead Architect), `DESIGNER_MEMBER` (Staff Designer/Collaborator).
> *(Legacy aliases `OWNER`, `ADMIN`, `MEMBER`, `DESIGNER_TEAM` are supported for read/backward-compatibility only).*
> 
> Photographer terminology (`photographer`, `photographer_admin`, `photography_studio`) is strictly prohibited.

---

## 1. Master Permissions Grid Across Actors

| Capability / Action | Public Visitor | Customer | Designer (No Studio) | Designer Member (Studio A) | Designer Admin (Studio A) | Moderator | Admin | Super Admin |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Browse Homepage & Discovery** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **View Published Portfolios** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Submit Public Inquiries (Leads)** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Submit Client Reviews (via Token)** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Save Private Collections** | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Manage Account & Cryptographic Sessions** | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Enroll & Verify TOTP MFA** | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Create New Studio (Onboarding)** | ❌ | ❌ | ✅ | ✅ (Additional) | ✅ (Additional) | ❌ | ❌ | ❌ |
| **Access Workspace Dashboard** | ❌ | ❌ | ❌ | ✅ (Studio A only) | ✅ (Studio A only) | ❌ | ❌ | ❌ |
| **View Studio CRM Leads** | ❌ | ❌ | ❌ | ✅ (Studio A only) | ✅ (Studio A only) | ❌ | ❌ | ❌ |
| **Create / Edit Projects & Rooms** | ❌ | ❌ | ❌ | ✅ (Studio A only) | ✅ (Studio A only) | ❌ | ❌ | ❌ |
| **Upload & Manage Media Assets** | ❌ | ❌ | ❌ | ✅ (Studio A only) | ✅ (Studio A only) | ❌ | ❌ | ❌ |
| **Execute AI Visualizer Jobs** | ❌ | ❌ | ❌ | ✅ (Studio A only) | ✅ (Studio A only) | ❌ | ❌ | ❌ |
| **Publish / Unpublish Projects** | ❌ | ❌ | ❌ | ❌ | ✅ (Studio A only) | ❌ | ❌ | ❌ |
| **Manage Studio Business Profile** | ❌ | ❌ | ❌ | ❌ | ✅ (Studio A only) | ❌ | ❌ | ❌ |
| **Manage Team Members & Invites** | ❌ | ❌ | ❌ | ❌ | ✅ (Studio A only) | ❌ | ❌ | ❌ |
| **Manage Billing & Subscriptions** | ❌ | ❌ | ❌ | ❌ | ✅ (Studio A only) | ❌ | ❌ | ❌ |
| **Access Cross-Studio Data (Studio B)** | ❌ | ❌ | ❌ | ❌ (DENIED) | ❌ (DENIED) | ❌ | ❌ | ❌ |
| **Access Platform Admin Console** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ (Queues only) | ✅ | ✅ |
| **Moderate Flagged Reviews** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| **Audit Studio Verification Documents** | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| **Suspend / Reactivate User Accounts** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| **Freeze / Suspend Studio Tenancies** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| **Inspect Immutable Security Audit Logs** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| **Toggle Global Maintenance Mode** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| **Adjust Operational Feature Flags** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 2. Multi-Tenancy & Studio Boundary Rules

1. **Strict Tenant Separation:**
   - A `DESIGNER_ADMIN` or `DESIGNER_MEMBER` belonging to `Studio A` has zero read or write visibility into `Studio B`.
   - Every database query for tenant resources filters by `studio_id` enforced both in Spring JPA and PostgreSQL Row-Level Security (`current_setting('app.current_studio_id')`).
   - Forged HTTP `X-Studio-Id` headers targeting studios where the authenticated user has no active membership are rejected with **HTTP 403 Forbidden**.
2. **Tab Isolation Invariant:**
   - Active studio state is persisted in browser `sessionStorage` and component memory.
   - Opening Studio A in Tab 1 and Studio B in Tab 2 ensures completely independent studio contexts without cross-tab session contamination (zero `localStorage` active-studio usage).
3. **Control Plane Independence:**
   - Platform `ADMIN` and `SUPER_ADMIN` roles operate at the control plane level (`/admin/*`) and **never** impersonate or inherit studio tenancy memberships implicitly.
   - Direct studio tenant operations require explicit ownership or explicit audit-logged administrative intervention.

---

## 3. Mandatory Multi-Factor Authentication (MFA) Policy

1. **Administrative Roles (`ADMIN`, `SUPER_ADMIN`):**
   - MFA is **MANDATORY** to access `/admin/*` endpoints.
   - Sessions authenticated without high assurance (MFA or hardware security assertion) are challenged with HTTP 403 step-up before any administrative mutation is permitted.
2. **Moderator Role (`MODERATOR`):**
   - Allowed access to review and verification moderation queues; step-up required for destructive rejection actions.
3. **Standard Users (`CUSTOMER`, `DESIGNER`):**
   - MFA is optional but encouraged. Enrolled users require TOTP verification during login.
   - Backup recovery codes are single-use, stored as bcrypt/SHA-256 hashes, and can be regenerated at any time with active session confirmation.
