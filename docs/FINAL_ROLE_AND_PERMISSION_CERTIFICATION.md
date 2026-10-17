# Final Role and Permission Certification
## Elégance — Interior Designer Platform

This document certifies the role-based access control (RBAC) matrix and tenant-isolation boundaries across all 8 user personas.

---

## 1. Role Definition & Privilege Matrix

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

## 2. In-Depth Boundary Testing Results

### A. Designer Member vs. Designer Admin
- **Test**: `DESIGNER_MEMBER` attempts to invite new members (`POST /api/v1/team/invitations`).
  - Result: Backend rejects with HTTP 403 `ACCESS_DENIED`. UI does not render invite button.
- **Test**: `DESIGNER_MEMBER` attempts to modify portfolio layout or billing plan.
  - Result: Backend rejects with HTTP 403 `STUDIO_ADMIN_REQUIRED`.

### B. Cross-Studio IDOR Isolation
- **Test**: Designer from Studio A attempts to access `GET /api/v1/projects/{studioB_projectId}`.
  - Result: PostgreSQL Row-Level Security (`app.current_studio_id`) filters query to 0 rows; Spring Boot returns HTTP 404 `PROJECT_NOT_FOUND`. Zero data leakage.
- **Test**: Designer from Studio A attempts to view CRM leads from Studio B (`GET /api/v1/leads/{studioB_leadId}`).
  - Result: HTTP 404 `LEAD_NOT_FOUND`.

### C. Admin & Super Admin Boundaries
- **Test**: `MODERATOR` attempts to alter platform operational state (`PUT /api/v1/admin/operations/state`).
  - Result: Rejected with HTTP 403 `SUPER_ADMIN_REQUIRED`.
- **Test**: Platform `ADMIN` attempts to modify operational circuit breakers.
  - Result: Rejected with HTTP 403 `SUPER_ADMIN_REQUIRED`.
- **Test**: Non-admin user attempts direct URL navigation to `/admin/*`.
  - Result: Client route guard redirects to `/` with "Access Denied" toast; backend endpoints reject API calls with HTTP 403.

### D. Multi-Studio Tab Independence
- Tested multi-studio switcher across separate browser tabs.
- `sessionStorage` and `X-Studio-Id` request header ensure Tab A context (Studio 1) never bleeds into Tab B context (Studio 2).
