# Admin & Platform Operations Guide

## Overview

The Admin Console (`/admin`) provides platform operators with centralized operational control over users, studios, professional verification, review moderation, and system audit logs.

Access is restricted exclusively to authenticated users possessing platform roles:
- `ADMIN`
- `SUPER_ADMIN`

Platform administrators do **not** become members of studios simply by possessing administrative privileges.

---

## 1. Accessing the Admin Console

1. Navigate to `/admin`.
2. The route layout inspects current user roles:
   - If not authenticated, redirects to `/sign-in`.
   - If authenticated without `ADMIN` or `SUPER_ADMIN` role, displays an access denied boundary.

---

## 2. Operations Modules

### A. Operations Dashboard (`/admin`)
- Displays real platform counters: Active Studios, Active Users, Verification Pending, Reviews Requiring Moderation, Media Count, Storage Usage.
- Displays the live stream of recent system audit activities.

### B. User Management (`/admin/users`)
- Lists users with registration timestamps, roles, and status (`ACTIVE`, `SUSPENDED`, `DISABLED`).
- **Account Status Transition**: Admins can change status with an audited reason. Changes are reversible.
- Never hard-deletes users.

### C. Studio Management (`/admin/studios`)
- Lists design studios with publication state, verification status, and project counts.
- **Studio Suspension**: Admins can suspend offending studios. Suspended studios are hidden from public discovery and have restricted workspace access.

### D. Verification Reviews (`/admin/verification`)
- Review submitted designer credentials (registration numbers, GST numbers, uploaded documentation).
- Actions:
  - **Approve**: Sets verification status to `VERIFIED` and marks studio badge across the platform.
  - **Reject**: Sets status to `REJECTED` with a required operational reason recorded in the database.

### E. Review Moderation (`/admin/reviews`)
- Inspect client reviews flagged by community reports or automated checks.
- **Moderation Invariant**: Admins cannot alter client star ratings or edit text. Admins can only change status to `REMOVED`, `FLAGGED`, or `PUBLISHED`.
- Removing a review automatically excludes it from the studio's truthful aggregate rating calculations.

### F. Immutable Audit Log (`/admin/audit`)
- Real-time search and inspection of append-only audit trail records.
- Logs actor ID, action type, target entity, IP address, and timestamp.
