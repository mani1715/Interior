# Final Role and Permission Certification
## Elégance — Interior Designer Platform

This document certifies the canonical role-based access control (RBAC) matrix, studio membership authorization, and tenant-isolation boundaries across the platform.

---

## 1. Executable Role Architecture Source Truth

Executable source code establishes two distinct role tiers, enforced by database constraints and backend security services:

### A. Global Platform Identity Roles (`identity_roles`)
Enforced in database via `V001__security_identity_tenant_schema.sql` (`identity_roles.code` check constraint) and validated by `AuthorizationService.java`:

#### Active Assignable Platform Roles (5 Roles):
1. `SUPER_ADMIN` — Platform Super Administrator (emergency control plane, platform maintenance mode toggling, system audit oversight).
2. `ADMIN` — Platform Administrator (user account lifecycle, studio verification review, operational audit monitoring).
3. `MODERATOR` — Platform Content Moderator (client review moderation queue, public testimonial approval/rejection).
4. `DESIGNER` — Interior Designer (assigned automatically upon completing professional studio onboarding via `ProfessionalOnboardingService`).
5. `CUSTOMER` — Public Customer / Client (assigned automatically to all new users upon OIDC registration via `OidcService` under least privilege).

#### Dormant / Legacy Identity Seed (1 Value):
- `DESIGNER_TEAM` — Legacy platform identity seed from `V001`.
  - **Status**: Dormant compatibility value in `identity_roles` check constraint.
  - **Permissions**: Confers no elevated platform privileges.
  - **Assignment**: Not assigned during normal studio team invitations (`StudioTeamServiceTest:239` explicitly verifies the invariant: *no global role escalation to `DESIGNER_TEAM`*). Team members remain `CUSTOMER` at the platform level and receive studio-scoped access via `studio_members`.

#### Anonymous Runtime Actor Context (Not a Persisted Role):
- `PUBLIC` — Instantiated strictly at runtime in unauthenticated requests via `ActorContext.anonymous()`.
  - **Status**: Not in `identity_roles`, never persisted in database, never assigned to any user account.

---

### B. Studio Membership Authorization Roles (`studio_members`)
Enforced in database via `V027__canonicalize_identity_and_studio_roles.sql` (`studio_members.role` check constraint) and validated by `ActorContext.isStudioOwnerOrAdmin`:

#### Canonical Studio Roles (2 Roles):
1. `DESIGNER_ADMIN` — Studio Administrator / Owner (full administrative authority over the studio: settings, billing, team invites, and portfolio publishing).
2. `DESIGNER_MEMBER` — Studio Member (operational access: view/edit studio projects, rooms, and media; cannot invite members, change billing, or publish portfolio).

#### Legacy Studio Read Aliases (Compatibility Only):
- `OWNER` $\rightarrow$ Evaluated as `DESIGNER_ADMIN` in `ActorContext.isStudioOwnerOrAdmin`.
- `ADMIN` $\rightarrow$ Evaluated as `DESIGNER_ADMIN` in `ActorContext.isStudioOwnerOrAdmin`.
- `MEMBER` $\rightarrow$ Evaluated as `DESIGNER_MEMBER` in `ActorContext.isStudioMember`.
- *Note*: All database writes canonicalize strictly to `DESIGNER_ADMIN` and `DESIGNER_MEMBER`.

---

### C. Human-Friendly Persona & Job Title Clarification
The following terms are UI display badges or studio profile job titles/metadata, **NOT** backend authorization roles:
- **"Studio Owner" / "Studio Admin"** $\rightarrow$ UI display label for studio members with role `DESIGNER_ADMIN`.
- **"Team Member"** $\rightarrow$ UI display label for studio members with role `DESIGNER_MEMBER`.
- **"Client" / "Client User"** $\rightarrow$ UI persona label for users with platform role `CUSTOMER`.
- **"Lead Designer", "Junior Designer", "Procurement Specialist", "Finance Auditor"** $\rightarrow$ Studio team job titles stored in member profile metadata, **not** RBAC roles.
- **"Guest"** $\rightarrow$ Anonymous visitor (`PUBLIC` runtime context).

---

## 2. Canonical Privilege Matrix

| Resource / Endpoint Scope | PUBLIC (Anonymous) | CUSTOMER | DESIGNER (No Studio) | DESIGNER_MEMBER | DESIGNER_ADMIN | MODERATOR | ADMIN | SUPER_ADMIN |
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

*Matrix Scope Note*: The 8 columns in this matrix represent all evaluated operational states: 1 anonymous visitor context (`PUBLIC`), 1 baseline customer role (`CUSTOMER`), 1 unaffiliated professional role (`DESIGNER`), 2 studio authorization tiers (`DESIGNER_MEMBER`, `DESIGNER_ADMIN`), and 3 governance roles (`MODERATOR`, `ADMIN`, `SUPER_ADMIN`).

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
