# Phase 12A: Containerized ClamAV Operations Runbook

## 1. Overview

The Elégance Interior Designer Platform integrates ClamAV for zero-trust malware scanning of all user-uploaded binaries. The ClamAV daemon (`clamd`) runs as an isolated microservice communicating with the API over private TCP streams via the `zINSTREAM` protocol.

---

## 2. Container Configuration

### 2.1. Docker Compose Specifications
In `docker-compose.yml` (development) and `docker-compose.prod.yml` (production):

```yaml
services:
  clamav:
    image: clamav/clamav:1.4.1
    restart: unless-stopped
    expose:
      - "3310"
    volumes:
      - clamav_data:/var/lib/clamav
    healthcheck:
      test: ["CMD-SHELL", "clamdscan /init || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    environment:
      - CLAMAV_NO_FRESHCLAMD=false
    networks:
      - interior_network

volumes:
  clamav_data:
```

### 2.2. Network Isolation Guard
- In production (`docker-compose.prod.yml`), ClamAV specifies `expose: - "3310"` rather than `ports: - "3310:3310"`.
- This ensures port 3310 is accessible **only** to containers attached to `interior_network` (such as the `api` service) and is never accessible from public interfaces or the host machine.

---

## 3. Operational Lifecycle

### 3.1. Starting the Service
```bash
# Start ClamAV in background
docker compose up -d clamav

# Check daemon startup and virus definition download
docker compose logs -f clamav
```
*Note*: During initial startup, `freshclam` downloads the ClamAV bytecode and daily definition databases. The container will report healthy once database initialization completes (typically 30–60 seconds).

### 3.2. Sizing and Memory Requirements
- **Memory**: Minimum 2.5 GB allocated RAM recommended. ClamAV loads the entire signature database into RAM.
- **CPU**: 1–2 cores recommended for high throughput streaming uploads.

---

## 4. Application Integration & Configuration

### 4.1. Properties
In `application-production.properties`:
```properties
app.media.security.malware-scanner=clamav
app.media.security.clamav.host=${CLAMAV_HOST:clamav}
app.media.security.clamav.port=${CLAMAV_PORT:3310}
app.media.security.clamav.connection-timeout-ms=${CLAMAV_CONNECT_TIMEOUT_MS:3000}
app.media.security.clamav.read-timeout-ms=${CLAMAV_READ_TIMEOUT_MS:10000}
```

### 4.2. Health Monitoring
Spring Boot Actuator exposes scanner health at `/actuator/health`:
```json
{
  "status": "UP",
  "components": {
    "malwareScanner": {
      "status": "UP",
      "details": {
        "provider": "clamav",
        "version": "ClamAV 1.4.1/27600/...",
        "mediaUploadReady": true
      }
    }
  }
}
```

### 4.3. Fail-Closed Policy & Troubleshooting
- **Scanner Down**: If ClamAV becomes unreachable, `ClamAvMalwareScanner` returns status `UNAVAILABLE`.
- **Upload Rejected**: `MediaService` catches non-clean results and immediately rejects the upload with an HTTP 503 / 400 error. No bytes are written to storage.
- **Troubleshooting Steps**:
  1. Inspect logs: `docker compose logs --tail=100 clamav`
  2. Verify container health: `docker ps --filter "name=clamav"`
  3. Verify internal connectivity from API container: `nc -zv clamav 3310`
