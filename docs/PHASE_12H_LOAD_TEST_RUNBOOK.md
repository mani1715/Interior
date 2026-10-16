# Phase 12H — Performance & Load Testing Operational Runbook

**Product:** Elégance — Interior Designer Platform  
**Target Audience:** Platform Engineers, QA Engineers, DevOps  
**Phase:** 12H & 12H.1  

---

## 1. Quick Start Guide

### 1.1 Verify Pre-requisites
Ensure Node.js (v18+), PostgreSQL 18.3, and Java 21 are available:
```powershell
node -v
java -version
```

### 1.2 Run Comprehensive Performance Readiness Verification
Execute the automated readiness verification harness:
```powershell
cd "C:\my projects\interior design"
powershell -ExecutionPolicy Bypass -File scripts/verify-performance-readiness.ps1
```
This runs:
1. Concurrency, quota, and race condition JUnit tests (12/12).
2. Load test scripts syntax validation (8/8 scripts).
3. Remote target safety switch verification.
4. Studio row lock audit (`FOR UPDATE`).
5. HikariCP connection pool settings audit.
6. PostgreSQL advisory lock singleton verification.

---

## 2. Running Load Test Scenarios

All load testing scripts are located in the `performance/` directory.

### 2.1 Master Benchmark Runner
Run the stepped benchmark suite (1 -> 10 -> 25 -> 50 -> 100 concurrent workers):
```powershell
node performance/runner.js --all --baseUrl=http://localhost:8080/api/v1
```

### 2.2 Individual Scenarios

#### Scenario 1: Public Portfolio Browsing (100 VU Sustained Stress)
Simulates 100 concurrent unauthenticated visitors exploring discovery and public studio portfolios:
```powershell
node performance/public-browse.js --concurrency=100 --duration=60 --baseUrl=http://localhost:8080/api/v1
```

#### Scenario 2: Authenticated Designer Reads
Simulates logged-in designers loading dashboard, projects, team, leads CRM, and analytics:
```powershell
node performance/authenticated-read.js --concurrency=15 --duration=10 --baseUrl=http://localhost:8080/api/v1
```

#### Scenario 3: Concurrent Mutations / Writes
Simulates simultaneous lead inquiries and project modifications:
```powershell
node performance/authenticated-write.js --concurrency=10 --duration=10 --baseUrl=http://localhost:8080/api/v1
```

#### Scenario 4: Mixed Workload (80% Reads / 20% Writes)
Simulates realistic production traffic distribution:
```powershell
node performance/mixed-workload.js --concurrency=50 --duration=20 --baseUrl=http://localhost:8080/api/v1
```

#### Scenario 5: Media & Upload Intent Workload
Evaluates storage quota calculations and intent reservations:
```powershell
node performance/media-workload.js --concurrency=10 --duration=10 --baseUrl=http://localhost:8080/api/v1
```

#### Scenario 6: 20-Minute Soak & Resource Stability Verification
Runs a genuine 20-minute mixed workload (70% public reads, 20% authenticated reads, 10% mutations) while sampling JVM heap memory, live threads, HikariCP pool, and PostgreSQL system connections:
```powershell
node performance/soak.js --concurrency=20 --duration=1200 --sampleInterval=30 --baseUrl=http://localhost:8080/api/v1
```

---

## 3. Remote Target Safety Switch

> [!CAUTION]
> Load tests default strictly to `localhost` / `127.0.0.1`. Attempting to run against any non-local hostname without the safety override flag will abort immediately with exit code 1.

To run against an authorized staging environment:
```powershell
$env:ALLOW_REMOTE_LOAD_TEST="true"
node performance/runner.js --baseUrl=https://staging-api.elegance.internal/api/v1 --concurrency=25 --duration=15
$env:ALLOW_REMOTE_LOAD_TEST=""
```

---

## 4. Key Metrics & Interpretation

| Metric | Healthy Threshold (Local) | Meaning / Remediation |
|:---|:---:|:---|
| **Public p50 Latency** | $< 35\text{ ms}$ | Median public read speed. If high, check missing database indices. |
| **Public p95 Latency** | $< 300\text{ ms}$ | 95th percentile public read speed under 100 VUs. If high, check connection pool saturation. |
| **Error Rate** | $< 0.50\%$ | Percentage of unhandled 5xx server errors under 100-VU burst. |
| **Throughput** | $> 350\text{ req/s}$ | Total transactions per second completed by the local server under 100 VUs. |
| **Active DB Connections** | $\le 30$ | Must never exceed `maximum-pool-size`. Must return to `0.0` in idle cooldown. |

---

## 5. Troubleshooting & FAQ

### Issue: `SQLTransientConnectionException: Connection is not available, request timed out after 20000ms`
- **Cause:** Concurrency exceeded database pool capacity, or transactions are holding connections during external I/O.
- **Check:** Ensure all external HTTP calls (AI image generation, email sending) execute outside `@Transactional` blocks.
- **Tuning:** Hikari pool size has been configured to `maximum-pool-size: 30` with `minimum-idle: 10`.

### Issue: `BadRequestException: Project limit reached for current plan`
- **Explanation:** This is expected quota enforcement behavior when testing near boundary limits (e.g. 10 projects on STANDARD plan). It is logged as a quota rejection, not a system failure.

---

## 6. Phase 15 Handoff: Production Cloud Load Testing

Upon deployment to cloud infrastructure in Phase 15:
1. Re-run `performance/runner.js --all` against the staging URL with `ALLOW_REMOTE_LOAD_TEST=true`.
2. Validate CDN caching efficiency for public media assets and static JSON endpoints.
3. Verify horizontal autoscaling triggers under synthetic load $> 500\text{ req/s}$.
4. Establish formal production capacity SLAs with real hosting metrics.
