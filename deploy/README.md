# Elégance — Deployment & Production Release Package

> **CRITICAL RELEASE POLICY NOTICE:**
> **APPLICATION STATUS:** APPLICATION RELEASE CANDIDATE VERIFIED.
> **DEPLOYMENT STATUS:** NO LIVE DEPLOYMENT PERFORMED.
> **REAL CLOUD PROVIDERS:** DEFERRED TO PHASE 15 (PRE-LAUNCH ACTIVATION).
> **FINAL PRODUCT BRAND & DOMAIN:** DEFERRED TO PHASE 16 (PRE-LAUNCH DOMAIN PURCHASE & GO-LIVE).
> **LEGAL STATUS:** LEGAL REVIEW REQUIRED BEFORE PUBLIC PRODUCTION.

---

## 1. Directory Purpose

This directory contains deployment-neutral, reproducible, and auditable production release specifications for the Elégance Interior Designer Platform. 

These specifications define the contract between the compiled application artifacts and any target hosting environment (e.g., bare VPS, container orchestrator, managed containers, or hybrid cloud) without locking the platform into a proprietary vendor or performing premature deployment.

---

## 2. Release Artifacts & Inventory

| Document | Purpose |
|---|---|
| [`topology.md`](./topology.md) | Architectural topology: Edge -> Web -> API -> PostgreSQL + R2 + ClamAV |
| [`environment.schema.md`](./environment.schema.md) | Canonical environment variable schema, classifications, defaults, and validator constraints |
| [`backend.container.md`](./backend.container.md) | Spring Boot 3.4 / Java 21 non-root OCI container runtime specification |
| [`frontend.container.md`](./frontend.container.md) | Next.js 16.3.8 Standalone non-root OCI container runtime specification |
| [`database.md`](./database.md) | PostgreSQL 15–18 engine compatibility, Flyway V001–V040 migrations, and connection budgeting |
| [`storage-r2.md`](./storage-r2.md) | Cloudflare R2 S3-compatible object storage topology and least-privilege IAM permissions |
| [`clamav.md`](./clamav.md) | ClamAV TCP streaming daemon deployment and fail-closed malware security specification |
| [`health.md`](./health.md) | Healthcheck, startup, liveness, readiness, and metrics contract |
| [`release-order.md`](./release-order.md) | Deterministic 8-step production rollout execution order |
| [`rollback.md`](./rollback.md) | 4-tier incident rollback decision tree and recovery procedures |
| [`release-manifest.json`](./release-manifest.json) | Immutable machine-readable release manifest linking Git SHA, image tags, and schema versions |

---

## 3. Guiding Principles

1. **Deterministic Reproducibility:** Every release artifact is linked to an exact Git commit SHA.
2. **Configuration-Driven:** All network origins, database connections, and provider endpoints are supplied at runtime via environment variables.
3. **Secret Safety:** Zero credentials or API keys are embedded in source code, Git history, container layers, or frontend bundles.
4. **Fail-Closed Security:** Unconfigured or misconfigured components fail safe. Mock or fake providers are strictly prohibited in the `production` profile.
5. **Vendor Neutrality:** Standard OCI containers, HTTP/1.1 & HTTP/2 reverse proxy contracts, and standard PostgreSQL protocols prevent vendor lock-in.
