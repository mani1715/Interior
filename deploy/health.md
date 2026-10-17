# Health, Startup, Liveness & Readiness Contract

## 1. Overview & Security Isolation

The platform exposes standardized container health probes adhering to Kubernetes and container orchestrator conventions. 

> **SECURITY INVARIANT:** Internal detailed actuator health (`show-details=when-authorized`) is restricted. Only authorized administrative sessions or private cluster orchestrators may inspect deep component health. Public visitors receive binary status indicators.

---

## 2. Probe Mapping

| Probe Type | Service | Route | Target Readiness Dependency |
|---|---|---|---|
| **Liveness** | Backend API | `/api/v1/actuator/health/liveness` | JVM is responsive. Returns 200 `{"status":"UP"}`. Does not restart JVM if external optional providers are transiently unreachable. |
| **Readiness** | Backend API | `/api/v1/actuator/health/readiness` | PostgreSQL datasource connectivity and Flyway schema state. Returns 200 `{"status":"UP"}` when ready to accept HTTP traffic. |
| **Liveness/Readiness** | Frontend Web | `/health` | Next.js server runtime responsive. Returns 200 with HTML/JSON status. |
| **Metrics** | Backend API | `/api/v1/actuator/prometheus` | Prometheus scrape endpoint exposing Micrometer metrics. |

---

## 3. Probe Timings (Initial Engineering Sizing)

Based on verified local container measurements:

- **Startup Period:**
  - Backend API: `40s` (allows PostgreSQL connection initialization, Flyway migration audit, and Hibernate verification).
  - Frontend Web: `20s`.
  - ClamAV: `60s` (allows virus signature database loading into memory).
- **Check Interval:** `30s`.
- **Timeout:** `5s`.
- **Failure Threshold / Retries:** `3`.
