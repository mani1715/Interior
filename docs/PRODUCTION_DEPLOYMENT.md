# Production Deployment Guide

## Overview

The Interior Design Platform comprises two main application layers and a relational database:
1. **Frontend**: Next.js 16 (React 19, TypeScript, Tailwind CSS) containerized with Node 20.
2. **Backend**: Spring Boot 3.4.3 (Java 21, JPA/Hibernate, Flyway, Actuator) containerized with Eclipse Temurin 21.
3. **Database**: PostgreSQL 16+ with Row-Level Security (RLS) and UUIDv7 primary keys.

---

## Environment Configuration

### Required Backend Environment Variables (`apps/api`)

| Variable | Description | Example / Default |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active profile | `production` |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://postgres.internal:5432/interiordb` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `app_user` |
| `SPRING_DATASOURCE_PASSWORD` | Database user password | `<secure_password>` |
| `APP_SECURITY_DEV_AUTH_ENABLED` | **Must be false in production** | `false` |
| `APP_SECURITY_SESSION_SECRET` | 32+ character HMAC secret for session signing | `<high_entropy_secret>` |
| `APP_SECURITY_ALLOWED_ORIGINS` | Comma-separated allowed CORS origins | `https://interiordesign.com` |
| `APP_SECURITY_COOKIE_SECURE` | Enforces Secure flag on auth cookies | `true` |
| `APP_STORAGE_LOCAL_BASE_DIR` | Filesystem mount point for assets if S3/R2 not active | `/app/storage` |
| `SERVER_PORT` | HTTP listening port | `8080` |

### Required Frontend Environment Variables (`apps/web`)

| Variable | Description | Example / Default |
|---|---|---|
| `NODE_ENV` | Node environment | `production` |
| `NEXT_PUBLIC_API_URL` | Public backend API URL | `https://api.interiordesign.com/api/v1` |
| `PORT` | Listening port | `3000` |

---

## Production Health Checks

### Backend Endpoints
- **Liveness Probe**: `GET /api/v1/actuator/health/liveness`
  - Returns `{"status":"UP"}` when the JVM process is alive and responding.
- **Readiness Probe**: `GET /api/v1/actuator/health/readiness`
  - Returns `{"status":"UP"}` only when the database pool is verified and migrations are applied.
- **Metrics**: `GET /api/v1/actuator/metrics` (secured / internal only).

### Frontend Endpoints
- **Health Check**: `GET /health`
  - Returns `{"status":"healthy","service":"web","timestamp":"..."}`.

---

## Container Deployment

### Using Docker Compose
```bash
docker compose -f docker-compose.yml up -d --build
```

### Kubernetes Readiness/Liveness Configuration
```yaml
livenessProbe:
  httpGet:
    path: /api/v1/actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 15
readinessProbe:
  httpGet:
    path: /api/v1/actuator/health/readiness
    port: 8080
  initialDelaySeconds: 20
  periodSeconds: 10
```

---

## Migration Policy

1. All database migrations are sequential Flyway scripts under `apps/api/src/main/resources/db/migration/`.
2. Migrations execute automatically on backend startup before serving traffic.
3. Zero-downtime guidelines:
   - Migrations must be strictly additive (new tables, new columns with defaults, concurrent indexes).
   - Column drops or renames must follow expand-and-contract patterns across subsequent deployment cycles.
