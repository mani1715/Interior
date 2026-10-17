# Final Role and Permission Certification
## Elégance — Interior Designer Platform

This document certifies the canonical role-based access control (RBAC) matrix, studio membership authorization, and tenant-isolation boundaries across the platform.

---

## 1. Canonical Roles Source Truth

Executable source code establishes two distinct role tiers, enforced by database constraints and backend security services:

### A. Global Platform Identity Roles (`identity_roles`)
Enforced in database via `V001__security_identity_tenant_schema.sql` (`identity_roles.code` check constraint) and validated by `AuthorizationService.java`:
1. `SUPER_ADMIN` — Platform Super Administrator (full emergency control-plane, maintenance mode toggling, administrative overrides).
2. `ADMIN` — Platform Administrator (user suspension/reactivation, studio document verification, audit log review).
3. `MODERATOR` — Platform Content Moderator (review moderation queue, public feedback triage).
4. `DESIGNER` — Interior Designer (authenticated professional identity eligible to establish or lead a studio).
5. `DESIGNER_TEAM` — Studio Team Member (authenticated professional identity participating in studio work).
6. `CUSTOMER` — Public Customer / Client (baseline identity for homeowners and clients; project inquiries, moodboard collections).
7. `PUBLIC` — Unauthenticated Guest / Anonymous visitor (`ActorContext.anonymous()`).

### B. Studio Membership Roles (`studio_members`)
Enforced in database via `V027__canonicalize_identity_and_studio_roles.sql` (`studio_members.role` check constraint) and validated by `ActorContext.isStudioOwnerOrAdmin`:
1. `DESIGNER_ADMIN` — Studio Administrator / Owner (full administrative rights over the studio: settings, billing, team invites, portfolio publishing).
2. `DESIGNER_MEMBER` — Studio Member (operational access: view/edit projects, rooms, and media; cannot invite team members, alter billing, or publish portfolio).
- *Legacy Read Aliases*: `OWNER` and `ADMIN` are accepted for read compatibility and map to `DESIGNER_ADMIN`; `MEMBER` maps to `DESIGNER_MEMBER`.

### C. Human-Friendly Persona & Job Title Clarification
The following UI and business terms are human-readable display labels or team profile metadata, **NOT** backend authorization roles:
- **"Studio Owner" / "Studio Admin"** $\rightarrow$ UI display label for studio members with role `DESIGNER_ADMIN`.
- **"Team Member"** $\rightarrow$ UI display label for studio members with role `DESIGNER_MEMBER`.
- **"Client" / "Client User"** $\rightarrow$ UI persona label for users with platform role `CUSTOMER`.
- **"Lead Designer", "Junior Designer", "Procurement Specialist", "Finance Auditor"** $\rightarrow$ Studio team job titles and professional specialties stored in profile metadata, **not** RBAC permissions.
- **"Guest"** $\rightarrow$ Unauthenticated visitor (`PUBLIC`).

---

## 2. Canonical Privilege Matrix

| Resource / Endpoint Scope | PUBLIC | CUSTOMER | DESIGNER (NO STUDIO) | DESIGNER_MEMBER | DESIGNER_ADMIN | MODERATOR | ADMIN | SUPER_ADMIN |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Public Showcase & Search** | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW |
| **Submit Public Inquiries / Leads** | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW |
| **Submit Client Review via Token** | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW |
| **Customer Collections** | DENY | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW |
| **Customer Account & TOTP MFA** | DENY | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW | ALLOW |
| **Create Designer Studio (Onboarding)** | DENY | DENY | ALLOW | DENY (Member) | ALLOW (Add-on) | DENY | DENY | DENY |
| **View Studio Projects / Rooms** | DENY | DENY | DENY | ALLOW | ALLOW | DENY | DENY | DENY |
| **Edit Room Media / AI Concepts** | DENY | DENY | DENY | ALLOW | ALLOW | DENY | DENY | DENY |
| **Publish Project to Public Directory** | DENY | DENY | DENY | DENY (403) | ALLOW | DENY | DENY | DENY |
| **Edit & Publish Studio Portfolio** | DENY | DENY | DENY | DENY (403) | ALLOW | DENY | DENY | DENY |
| **Manage Studio Team & Invites** | DENY | DENY | DENY | DENY (403) | ALLOW | DENY | DENY | DENY |
| **Subscription & Billing Upgrades** | DENY | DENY | DENY | DENY (403) | ALLOW | DENY | DENY | DENY |
| **Moderate Reviews Queue** | DENY | DENY | DENY | DENY | DENY | ALLOW | ALLOW | ALLOW |
| **Verify Designer Studios** | DENY | DENY | DENY | DENY | DENY | DENY (403) | ALLOW | ALLOW |
| **Suspend / Reactivate User Accounts** | DENY | DENY | DENY | DENY | DENY | DENY (403) | ALLOW | ALLOW |
| **View Platform Audit Logs** | DENY | DENY | DENY | DENY | DENY | DENY (403) | ALLOW | ALLOW |
| **Platform Circuit Breakers (Maintenance)**| DENY | DENY | DENY | DENY | DENY | DENY (403) | DENY (403) | ALLOW |

---

## 3. Boundary Testing & Invariant Validation

### A. Studio Member vs. Studio Admin Boundary
- **Test**: `DESIGNER_MEMBER` attempts to invite new members (`POST /api/v1/team/invitations`).
  - Result: Backend rejects with HTTP 403 `ACCESS_DENIED`. UI does not render invite button.
- **Test**: `DESIGNER_MEMBER` attempts to modify portfolio layout or billing plan.
  - Result: Backend rejects with HTTP 403 `STUDIO_ADMIN_REQUIRED`.

### B. Cross-Studio IDOR Isolation
- **Test**: Designer from Studio A attempts to access `GET /api/v1/projects/{studioB_projectId}`.
  - Result: PostgreSQL Row-Level Security (`app.current_studio_id`) filters query to 0 rows; Spring Boot returns HTTP 404 `PROJECT_NOT_FOUND`. Zero data leakage.
- **Test**: Designer from Studio A attempts to view CRM leads from Studio B (`GET /api/v1/leads/{studioB_leadId}`).
  - Result: HTTP 404 `LEAD_NOT_FOUND`.

### C. Governance & Administrative Boundaries
- **Test**: `MODERATOR` attempts to alter platform operational state (`PUT /api/v1/admin/operations/state`).
  - Result: Rejected with HTTP 403 `SUPER_ADMIN_REQUIRED`.
- **Test**: Platform `ADMIN` attempts to modify operational circuit breakers.
  - Result: Rejected with HTTP 403 `SUPER_ADMIN_REQUIRED`.
- **Test**: Non-admin user attempts direct URL navigation to `/admin/*`.
  - Result: Client route guard redirects to `/` with "Access Denied" notice; backend endpoints reject API calls with HTTP 403.

### D. Multi-Studio Tab Independence
- Tested multi-studio switcher across separate browser tabs.
- `sessionStorage` (`elegance_active_studio_id`) and `X-Studio-Id` request header ensure Tab A context (Studio 1) never bleeds into Tab B context (Studio 2).
