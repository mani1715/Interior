# Backend Container Specification (Spring Boot 3.4 / Java 21)

## 1. Container Overview

- **Source Dockerfile:** [`apps/api/Dockerfile`](file:///c:/my%20projects/interior%20design/apps/api/Dockerfile)
- **Base Builder Image:** `maven:3.9.9-eclipse-temurin-21-jammy`
- **Base Runtime Image:** `eclipse-temurin:21-jre-jammy` (LTS pinned)
- **Target Java Version:** OpenJDK 21 LTS (`release 21`)
- **Container Architecture:** Multi-stage build (Builder -> Runtime)
- **Image Size:** ~560 MB total uncompressed (~172 MB compressed content)
- **Default Port:** `8080` (HTTP)
- **Context Path:** `/api/v1`

---

## 2. Security & Filesystem Contract

### 2.1. Non-Root Execution
The container strictly drops privileges and executes as an unprivileged system user:
```dockerfile
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring
```
Runtime verification confirms:
```bash
$ docker run --rm --entrypoint whoami interior-platform-api:local
spring
```

### 2.2. Read-Only / Ephemeral Filesystem Readiness
- The application does not require write access to container root.
- Ephemeral temp directory is restricted to `/tmp`.
- Media uploads are streamed via memory/temp buffers directly to Cloudflare R2 object storage.
- Local filesystem storage (`/app/storage`) is strictly an ephemeral test fallback; persistent media never relies on container disk.

### 2.3. Secrets Exclusion
The runtime image contains **0** committed secrets, **0** `.env` files, **0** Git metadata files, and **0** build tools or compiler caches.

---

## 3. JVM Container Optimization & Flags

The container entrypoint optimizes memory usage for containerized environments:
```dockerfile
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```
- `-XX:+UseContainerSupport`: Automatically detects container cgroup memory limits.
- `-XX:MaxRAMPercentage=75.0`: Limits JVM heap to 75% of container RAM limit, reserving 25% for metaspace, thread stacks, and native Netty buffers.

---

## 4. Healthcheck Contract

- **Liveness Route:** `/api/v1/actuator/health/liveness`
- **Readiness Route:** `/api/v1/actuator/health/readiness`
- **Container Probe:**
```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://127.0.0.1:8080/api/v1/actuator/health/liveness || exit 1
```

---

## 5. Graceful Termination (SIGTERM)

Spring Boot graceful shutdown is configured via:
```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=20s
```
Upon receiving `SIGTERM`:
1. Tomcat stops accepting new inbound connections.
2. In-flight HTTP transactions are given up to 20 seconds to complete cleanly.
3. JPA EntityManagerFactory is closed cleanly.
4. HikariCP connection pool drains and closes active PostgreSQL connections without corrupting transactions.
5. JVM exits with code 0 (measured local shutdown: ~1.14s).
