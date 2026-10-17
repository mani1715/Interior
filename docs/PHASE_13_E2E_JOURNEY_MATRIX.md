# Phase 13 — End-to-End User Journey Matrix

**Product:** Elégance — Interior Designer Platform  
**Phase:** 13 (Whole-Product End-to-End Certification & Release-Candidate Validation)  
**Status:** CERTIFIED  
**Date:** 2026-10-17  

---

## 1. Persona-Driven Journey Catalog

### Journey 1: Public Anonymous Visitor Journey
- **Actor:** `PUBLIC_VISITOR` (Unauthenticated)
- **Path:** `/` -> `/projects` -> `/projects/[slug]` -> `/professionals` -> `/professionals/[slug]` -> `/contact`
- **Actions Verified:**
  1. Loads editorial split hero homepage (`BrandHero`, 180svh desktop motion, non-pinned mobile fallback).
  2. Executes keyword search ("Living Room", "Minimalist") across public projects.
  3. Navigates to verified studio profile (`/professionals/studio-atelier`).
  4. Explores space showcase with sticky room pills and room-scoped photo viewer.
  5. Opens full-screen photo viewer, tests 3x zoom and contact sheet toggle.
  6. Submits design inquiry via `EnquirySheet` (honeypot field empty, valid CSRF).
  7. Attempts accessing unpublished or suspended studio -> receives clean HTTP 404.
  8. SEO verification: title, description, canonical link, OpenGraph tags, and robots noindex on non-public pages.
- **Verdict:** **PASS**

### Journey 2: New Customer Onboarding & Authentication Journey
- **Actor:** `CUSTOMER` (`dev.customer@platform.local`)
- **Path:** `/sign-in` -> `/auth/dev-login` -> `/account`
- **Actions Verified:**
  1. Initiates login with return URL parameter (`returnUrl=/projects`).
  2. Authenticates through identity provider; receives secure `__Host-session` cookie (`HttpOnly`, `SameSite=Lax`, `Path=/`, `Secure`).
  3. Verifies post-login redirection sanitizes return URL (prevents open redirects to `//evil.com` or `javascript:`).
  4. Loads `/account` profile page; views active cryptographic sessions.
  5. Explores inspiration collections (`/account/collections`); creates private moodboard.
  6. Saves public project media into private collection.
- **Verdict:** **PASS**

### Journey 3: Designer Without Studio (Onboarding Wizard)
- **Actor:** `DESIGNER` (No active studio membership)
- **Path:** `/onboarding/professional` -> `/workspace`
- **Actions Verified:**
  1. Designer logs in with zero studio memberships.
  2. Directed immediately to `/onboarding/professional` (never lands on a broken or blank dashboard).
  3. Completes studio registration form (business name, slug, specialties, city).
  4. Backend creates `designer_studios` record, assigns user `DESIGNER_ADMIN` role in `studio_memberships`, resolves default `STANDARD` entitlement tier.
  5. Studio initialized in `UNPUBLISHED` state by default.
  6. Redirects into fresh workspace dashboard (`/workspace`) with helpful onboarding empty-state cards.
- **Verdict:** **PASS**

### Journey 4: Designer Admin Studio Operations Journey
- **Actor:** `DESIGNER_ADMIN` (Studio A: `Studio Atelier`)
- **Path:** `/workspace` -> `/workspace/projects` -> `/workspace/media` -> `/workspace/ai` -> `/workspace/leads` -> `/workspace/billing`
- **Actions Verified:**
  1. **Workspace Dashboard:** Real aggregate metrics load cleanly (0 fake data).
  2. **Project Lifecycle:** Creates new project "Penthouse Suite", adds description, budget, styles.
  3. **Room Hierarchy:** Creates 3 rooms (`Living Room`, `Master Bedroom`, `Kitchen`), sets room covers.
  4. **Safe Room Deletion:** Deletes a room; photos remain intact in media library (`room_id = NULL`), preventing orphan media assets.
  5. **Media Upload Pipeline:** Uploads high-res interior photos; ClamAV malware scan executes; commits to `READY` state.
  6. **Reticle Focal Points:** Calibrates `focal_x` and `focal_y` in `FocalPointModal` with live preview.
  7. **Presentation Mode:** Selects template (Architectural / Warm Natural); tests Cinematic presentation mode toggle.
  8. **Publication Gate:** Runs `/projects/{id}/publish-check`; resolves publishable criteria; publishes project.
  9. **CRM Leads:** Receives public customer inquiry, updates status to `CONTACTED`, adds private designer note.
  10. **Verified Reviews:** Generates 256-bit cryptographic review invitation token; client submits review via token.
- **Verdict:** **PASS**

### Journey 5: Multi-Studio Tenancy & Tab Isolation Journey
- **Actor:** Multi-Studio Designer (`dev.designer@platform.local`, member of Studio A and Studio B)
- **Path:** `/workspace` (Studio A in Tab 1, Studio B in Tab 2)
- **Actions Verified:**
  1. Tab 1 selects `Studio A` -> `sessionStorage` stores Studio A ID; API requests include `X-Studio-Id: studio-a`.
  2. Tab 2 selects `Studio B` -> `sessionStorage` stores Studio B ID; API requests include `X-Studio-Id: studio-b`.
  3. Refreshing Tab 1 preserves Studio A context without cross-tab interference.
  4. Forged `X-Studio-Id` test: User in Studio A attempts sending `X-Studio-Id: studio-c` (unassociated studio); backend throws **HTTP 403 Forbidden**.
  5. IDOR test: Studio A attempts fetching Studio B project UUID -> backend throws **HTTP 404 / 403**.
- **Verdict:** **PASS**

### Journey 6: AI Visualizer Studio Journey
- **Actor:** `DESIGNER_ADMIN` / `DESIGNER_MEMBER`
- **Path:** `/workspace/ai`
- **Actions Verified:**
  1. Selects owned project photo as base canvas.
  2. Opens precision mask editor; paints inpainting target area on canvas; tests clear and undo.
  3. Inputs design prompt ("Scandinavian minimalist oak cabinetry with brass accents").
  4. Submits generation job -> decrements studio daily quota (20/day UTC).
  5. Local deterministic AI provider processes job -> produces private derivative image.
  6. Inspects before/after interactive comparison slider.
  7. Provider disabled simulation -> UI renders truthful "AI Visualizer temporarily unavailable" with HTTP 503 while platform health remains `UP`.
- **Verdict:** **PASS**

### Journey 7: Commercial Billing & Downgrade Safety Journey
- **Actor:** `DESIGNER_ADMIN` (Billing Owner)
- **Path:** `/workspace/billing`
- **Actions Verified:**
  1. Views current plan (`STANDARD`) and quota utilization bars.
  2. Inspects commercial catalog: STANDARD (10 proj/15 photos), PREMIUM (20 proj/25 photos), PRO (20 proj/30 photos/5 cinematic).
  3. Confirms internal `BASE` plan is strictly non-purchasable.
  4. Clicks "Upgrade to Pro" -> renders truthful checkout boundary modal ("Payment provider activation scheduled for Phase 15").
  5. Simulates plan downgrade with over-quota assets -> **Zero Data Loss Invariant** confirmed (existing projects and media remain intact; restricts only new creations).
- **Verdict:** **PASS**

### Journey 8: Platform Moderation Journey
- **Actor:** `MODERATOR` (`dev.moderator@platform.local`)
- **Path:** `/admin/reviews` -> `/admin/verification`
- **Actions Verified:**
  1. Moderator logs in with MFA assurance.
  2. Opens review moderation queue; inspects flagged customer reviews.
  3. Approves clean review -> review becomes publicly visible on studio portfolio.
  4. Opens verification document queue; inspects business registration proof; approves studio verification.
  5. Attempts accessing platform operational flags or user suspension -> **HTTP 403 Forbidden** (moderator boundary enforced).
- **Verdict:** **PASS**

### Journey 9: Platform Administrator & Operations Journey
- **Actor:** `SUPER_ADMIN` (`dev.superadmin@platform.local`)
- **Path:** `/admin` -> `/admin/users` -> `/admin/studios` -> `/admin/operations` -> `/admin/audit`
- **Actions Verified:**
  1. Mandatory MFA gate enforced before granting access to `/admin`.
  2. User Administration: Views user directory, suspends abusive account, confirms suspension audit record.
  3. Studio Administration: Views global studios, freezes spam studio.
  4. Operational Modes: Toggles `READ_ONLY` maintenance mode -> public reads continue uninterrupted, write mutations blocked with HTTP 503. Restores to `NORMAL`.
  5. Audit Stream: Inspects tamper-evident security audit log with timestamps, actor IDs, and sanitized metadata (zero passwords, zero tokens).
- **Verdict:** **PASS**

---

## 2. Journey Verification Summary

```
================================================================================
TOTAL E2E JOURNEYS AUDITED: 9
JOURNEYS PASSING:           9 / 9 (100%)
ACTORS CERTIFIED:           8 (Public, Customer, Designer, Admin, Moderator, etc.)
CROSS-TENANT LEAKS:         0
DATA INTEGRITY ANOMALIES:   0
================================================================================
```
