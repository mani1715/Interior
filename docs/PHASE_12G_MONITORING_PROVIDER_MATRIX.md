# Elégance — Phase 12G: Monitoring & Observability Provider Decision Matrix

**Document:** Comparative Evaluation Matrix for Future Telemetry & Monitoring Providers  
**Platform:** Elégance — Interior Designer Platform  
**Phase:** 12G Observability & Operational Readiness  

---

## 1. Provider Evaluation Matrix

> **STATUS:** **DECISION DEFERRED TO PHASE 15 (PRE-LAUNCH ACTIVATION).**  
> No commercial accounts, paid subscriptions, or vendor-locked credentials have been created.

| Provider Option | Structured Logs | Prometheus Metrics | OpenTelemetry Traces | Frontend Error Tracking | Alerting & Incident Routing | Regional Latency (India / AP) | Cost Model | Architecture Fit |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---|:---|
| **Hosting-Native Only** (e.g. Railway / Render + Vercel) | Good (Basic stdout search) | Basic (CPU/RAM only) | None | Basic (Console output) | Webhook / Email | Native to hosting region | Free / Included in compute tier | **High for MVP launch**; lowest operational overhead. |
| **Grafana Cloud** (Loki + Mimir + Tempo) | Excellent (Loki structured JSON indexing) | Native (Prometheus remote write) | Native (OTLP endpoint) | Moderate (Requires Faro or Sentry) | Excellent (Grafana OnCall) | Strong (AWS Mumbai / Singapore regions) | Free tier generous; usage-based scaling | **Top candidate for open-standards observability** (OTel/Prometheus compatible). |
| **Better Stack** (Logtail + Better Uptime) | Excellent (ClickHouse-based instant log search) | Basic (Heartbeat & latency) | Limited | None (Requires external error tracker) | Excellent (Built-in incident management, SMS, Call) | Good (Global Edge CDN) | Predictable flat-rate tiers | **Top candidate for uptime monitoring & on-call paging** paired with simple log search. |
| **Sentry** | Basic (Breadcrumbs only) | Limited (Performance transactions) | Basic | **Industry Standard** (Source maps, session replay, release health) | Good (Slack/Email integration) | Moderate (US / EU data centers) | Developer tier generous | **Recommended dedicated companion for frontend + backend exception tracking**. |
| **Datadog** | Excellent | Excellent | Excellent | Excellent | Excellent | Strong (AP regions) | High / Complex per-host + per-feature billing | Powerful enterprise suite; high cost risk for early-stage startup. |
| **New Relic** | Very Good | Very Good | Very Good | Very Good | Very Good | Good | Consumption-based (100 GB/month free) | Full-stack all-in-one; steeper learning curve than Grafana Cloud. |

---

## 2. Separation of Concerns Recommendation

For a cost-conscious, highly resilient production launch, the platform architecture supports a clean two-tier separation:
1. **Application Error Tracking & Source Maps:** **Sentry** (specialized in stack trace deduplication, React error boundaries, and release tracking).
2. **Infrastructure Telemetry, Logs & Uptime:** **Hosting-Native** (e.g. Railway/Vercel) + **Better Stack** (for external HTTP probes and uptime alerts) OR **Grafana Cloud** (if deep OTel tracing and long-term Prometheus metrics retention are needed).

---

## 3. Activation Roadmap (Phase 15 Milestone)

When Phase 15 hosting and infrastructure are provisioned:
1. Review production traffic volume and operational budget.
2. Select between Hosting-Native vs. Grafana Cloud vs. Better Stack.
3. Configure OTLP endpoint and authentication via environment variables:
   - `management.otlp.metrics.export.url`
   - `management.otlp.tracing.endpoint`
4. Connect frontend Sentry DSN in Next.js config if error tracking is enabled.
5. Set alert routing channels (Engineering on-call, Slack `#alerts-prod`).
