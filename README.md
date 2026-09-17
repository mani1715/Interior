# Interior Portfolio & Discovery Platform

Production startup platform combining Interior Designer Portfolio + SEO Discovery + AI Interior Visualizer + Project Discovery + Client Collaboration + Leads/CRM.

**Current State:** Phase 03 Security Foundation & Monorepo Application Implementation PASS.

---

## Workspace Monorepo Architecture

```
/
├── apps/
│   ├── web/           # Next.js 16.3.3, React 19.3, TypeScript 6.0.3, App Router
│   └── api/           # Java 25 (Temurin), Spring Boot 3.4.3, Flyway, PostgreSQL 18
├── infrastructure/    # Cloud deployment configurations
├── docs/              # System architecture & decision records
├── scripts/           # Local environment & build helpers
├── .github/workflows/ # GitHub Actions CI pipeline
├── .editorconfig
├── .gitignore
└── README.md
```

---

## Development Prerequisites

* **Java:** JDK 25 (Temurin-25.0.4.1+1-LTS)
* **Node.js:** Node 24.19.0 LTS (or Node 22+)
* **Database:** PostgreSQL 18 (port 5433, database `interior_design_dev`)

---

## Quick Start Commands

### Frontend (`apps/web`)

```bash
# Typecheck TypeScript
npm --prefix apps/web run typecheck

# Build Next.js Production Bundle
npm --prefix apps/web run build

# Start Development Server
npm --prefix apps/web run dev
```

### Backend (`apps/api`)

```bash
# Run Maven Unit & Integration Tests (JDK 25)
cd apps/api && ./mvnw test

# Run Flyway Migrations & Launch Spring Boot API Server
cd apps/api && ./mvnw spring-boot:run
```

### PostgreSQL 18 Migrations

```sql
-- Apply Flyway Versioned Migrations
V001__security_identity_tenant_schema.sql
V002__pg_rls_policies.sql
```

---

## Implemented Security Features (Phase 03)

* **Identity & Session Security:** Opaque SHA-256 hashed 256-bit entropy random tokens stored in database, served via `__Host-session` cookie (`HttpOnly`, `Secure`, `SameSite=Lax`).
* **CSRF Protection:** Synchronizer token pattern with mandatory `X-CSRF-Token` header verification on mutating HTTP requests.
* **Tenant Isolation:** Multi-tenant composite keys `(studio_id, id)` with PostgreSQL 18 Row Level Security (`FORCE ROW LEVEL SECURITY`). Tested cross-tenant denial between Studio A and Studio B.
* **Security Headers:** Enforced `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Strict-Transport-Security`, `Permissions-Policy`, and strict CSP.
