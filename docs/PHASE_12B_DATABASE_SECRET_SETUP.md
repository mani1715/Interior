# Phase 12B: Database Environment & Secret Configuration

## 1. Overview

This document defines the production database environment variables for the Elégance Interior Designer Platform.

> **CRITICAL SECURITY REQUIREMENT**:
> Database passwords, connection strings, and administrative tokens must NEVER be committed to Git. All production database credentials must be dynamically injected via secure environment variables or vault secret managers.

---

## 2. Configuration Inventory

| Variable Name | Required in Prod? | Default (Local Dev) | Example Value (Production) | Description |
| :--- | :--- | :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | Yes | `jdbc:postgresql://localhost:5433/interior_design_dev` | `jdbc:postgresql://db.cloud.internal:5432/elegance_prod?sslmode=require` | PostgreSQL JDBC connection URL |
| `SPRING_DATASOURCE_USERNAME` | Yes | `postgres` | `elegance_app_user` | Application runtime database user |
| `SPRING_DATASOURCE_PASSWORD` | Yes | `postgres` | `<STRONG_SECRET_PASSWORD>` | Database user authentication secret |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | No | `10` | `10` | Maximum connections in HikariCP pool |
| `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE` | No | `2` | `2` | Minimum idle connections maintained |
| `SPRING_DATASOURCE_HIKARI_LEAK_DETECTION_THRESHOLD` | No | `30000` | `30000` | Threshold (ms) for reporting connection leaks |

---

## 3. Local Development vs. Production Matrix

- **Local Development (`application.properties`)**:
  - Connects to local PostgreSQL instance (port 5433 or 5432).
  - Uses default developer credentials (`postgres`/`postgres`).
- **Production Mode (`application-production.properties`)**:
  - Requires `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`.
  - [`ProductionConfigurationValidator`](file:///c:/my%20projects/interior%20design/apps/api/src/main/java/com/interior/platform/config/ProductionConfigurationValidator.java) rejects missing URL or in-memory H2 databases.
  - Requires SSL encryption (`sslmode=require`) across public cloud boundaries.
