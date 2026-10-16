# Phase 12H — Performance & Load Testing Operational Runbook

**Product:** Elégance — Interior Designer Platform  
**Target Audience:** Platform Engineers, QA Engineers, DevOps  
**Phase:** 12H  

---

## 1. Quick Start Guide

### 1.1 Verify Pre-requisites
Ensure Node.js (v18+) and PowerShell are available in your path:
```powershell
node -v
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
Run the full stepped benchmark suite (1 -> 10 -> 25 -> 50 -> 100 concurrent workers):
```powershell
node performance/runner.js --all --baseUrl=http://localhost:8080
```

### 2.2 Individual Scenarios

#### Scenario 1: Public Portfolio Browsing
Simulates unauthenticated visitors exploring discovery and public studio portfolios:
```powershell
node performance/public-browse.js --concurrency=20 --duration=15 --baseUrl=http://localhost:8080
```

#### Scenario 2: Authenticated Designer Reads
Simulates logged-in designers loading dashboard, projects, team, leads CRM, and analytics:
```powershell
node performance/authenticated-read.js --concurrency=15 --duration=10 --baseUrl=http://localhost:8080
```

#### Scenario 3: Concurrent Mutations / Writes
Simulates simultaneous lead inquiries and project modifications:
```powershell
node performance/authenticated-write.js --concurrency=10 --duration=10 --baseUrl=http://localhost:8080
```

#### Scenario 4: Mixed Workload (80% Reads / 20% Writes)
Simulates realistic production traffic distribution:
```powershell
node performance/mixed-workload.js --concurrency=50 --duration=20 --baseUrl=http://localhost:8080
```

#### Scenario 5: Media & Upload Intent Workload
Evaluates storage quota calculations and intent reservations:
```powershell
node performance/media-workload.js --concurrency=10 --duration=10 --baseUrl=http://localhost:8080
```

#### Scenario 6: Soak & Extended Resource Stability
Runs extended traffic while sampling JVM heap memory and HikariCP active connections:
```powershell
node performance/soak.js --concurrency=15 --duration=60 --sampleInterval=5 --baseUrl=http://localhost:8080
```

---

## 3. Remote Target Safety Switch

> [!CAUTION]
> Load tests default strictly to `localhost` / `127.0.0.1`. Attempting to run against any non-local hostname without the safety override flag will abort immediately with exit code 1.

To run against an authorized staging environment:
```powershell
$env:ALLOW_REMOTE_LOAD_TEST="true"
node performance/runner.js --baseUrl=https://staging-api.elegance.internal --concurrency=25 --duration=15
$env:ALLOW_REMOTE_LOAD_TEST=""
```

---

## 4. Key Metrics & Interpretation

| Metric | Healthy Threshold (Local) | Meaning / Remediation |
|:---|:---:|:---|
| **Public p50 Latency** | $< 25\text{ ms}$ | Median public read speed. If high, check missing database indices. |
| **Public p95 Latency** | $< 100\text{ ms}$ | 95th percentile public read speed. If high, check connection pool saturation. |
| **Error Rate** | $0.00\%$ | Percentage of 5xx server errors. Unhandled exceptions require immediate investigation. |
| **Throughput** | $> 250\text{ req/s}$ | Total transactions per second completed by the local server. |
| **Active DB Connections** | $\le 10$ | Must never exceed `maximum-pool-size`. Must return to `minimum-idle: 2` after test. |

---

## 5. Troubleshooting & FAQ

### Issue: `SQLTransientConnectionException: Connection is not available, request timed out after 20000ms`
- **Cause:** Concurrency exceeded database pool capacity, or transactions are holding connections during external I/O.
- **Check:** Ensure all external HTTP calls (AI image generation, email sending) execute outside `@Transactional` blocks.
- **Tuning:** In production (Phase 15), adjust `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` in accordance with PostgreSQL `max_connections`.

### Issue: `BadRequestException: Project limit reached for current plan`
- **Explanation:** This is expected quota enforcement behavior when testing near boundary limits (e.g. 10 projects on STANDARD plan). It is logged as a quota rejection, not a system failure.

---

## 6. Phase 15 Handoff: Production Cloud Load Testing

Upon deployment to cloud infrastructure in Phase 15:
1. Re-run `performance/runner.js --all` against the staging URL with `ALLOW_REMOTE_LOAD_TEST=true`.
2. Validate CDN caching efficiency for public media assets and static JSON endpoints.
3. Verify horizontal autoscaling triggers under synthetic load $> 500\text{ req/s}$.
4. Establish formal production capacity SLAs with real hosting metrics.
