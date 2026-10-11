# PRODUCTION MONITORING AND ALERTING REQUIREMENTS — ELÉGANCE

## 1. Service Level Indicators (SLIs) & Objectives (SLOs)

| Metric | Target / SLO | Measurement Window | Alert Severity |
|---|---|---|---|
| **API Availability** | $\ge 99.9\%$ successful HTTP requests (non-5xx, excluding planned 503 maintenance) | 30-day rolling | P1 / CRITICAL |
| **API Latency (p95)**| $< 350\text{ms}$ for standard metadata and retrieval queries | 5-minute rolling | P2 / HIGH |
| **API Latency (p99)**| $< 1000\text{ms}$ for standard endpoints | 5-minute rolling | P3 / MEDIUM |
| **Media Scanning Latency (p95)** | $< 5000\text{ms}$ to complete quarantine scan & ledger entry | 10-minute rolling | P2 / HIGH |
| **Error Budget Exhaustion** | $> 1\%$ non-maintenance 5xx rate | Over 5 minutes | P1 / CRITICAL |

---

## 2. Core Prometheus / Health Metrics

- `http_server_requests_seconds_count`: Total request throughput by URI pattern and HTTP status.
- `http_server_requests_seconds_max`: Latency ceiling per endpoint.
- `hikari_pool_connections_active`: Active PostgreSQL database connection utilization.
- `hikari_pool_connections_pending`: Queries queued waiting for database connection pool acquisition (Alert threshold: $> 5$ pending for $> 30\text{s}$).
- `operational_job_execution_failures_total`: Counter tracking failures in `operational_job_runs`.
- `media_security_scans_quarantined_total`: Rate of infected or suspicious files detected by the scanning pipeline.

---

## 3. Alerting Rules & Escalation Tiers

### P1 (Critical - Immediate Page)
- Service downtime or unhandled 5xx error rate $> 5\%$ for $> 2$ consecutive minutes.
- Database connection pool saturation ($\ge 95\%$ pool utilization).
- Advisory locking deadlocks preventing critical scheduled jobs (`SESSION_CLEANUP`, `UPLOAD_EXPIRY`).

### P2 (High - 30-minute Response)
- Media quarantine queue backlog or scanning latency $> 10\text{s}$ for $> 15$ minutes.
- Elevated 429 rate limit triggers exceeding typical baselines by $300\%$.
- External provider error spikes (Stripe, OpenAI, Resend).

### P3 (Medium - Next Business Day)
- Support ticket queue growth exceeding standard capacity thresholds.
- Non-critical job execution retries.
