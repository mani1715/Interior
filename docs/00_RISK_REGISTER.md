# Phase 00 — Risk Register

2026-09-17. Prospective product risks, not observed vulnerabilities in the empty workspace. Likelihood/impact: 1 low, 2 medium, 3 high. Severity = likelihood × impact: 1–2 LOW, 3–4 MEDIUM, 6 HIGH, 9 CRITICAL. Owners are accountable modules until people are assigned in Phase 01. All risks are OPEN; a mitigation is a plan, not evidence of resolution. Security/publication gates apply before exposure even if Phase 29 later audits them.

| ID | Risk | Likelihood | Impact | Severity | Mitigation and closure evidence | Owner Module | Target Phase |
|---|---|---|---|---|---|---|---|
| R-01 | AI output inconsistency or altered room geometry | 3 | 3 | CRITICAL | Representative masked/reference evaluation, explicit concept labels, user review; no exact material/geometry promise | AI | 21–24 |
| R-02 | Uncontrolled AI cost or double charge | 3 | 3 | CRITICAL | Atomic reservations, limits, idempotency, uncertain-outcome reconciliation, budget alerts and retry tests | AI / billing | 03, 21, 28 |
| R-03 | AI provider outage, deprecation or terms change | 2 | 3 | HIGH | Capability adapter, deadlines, circuit breaker, provider review and failure drills | AI | 21 |
| R-04 | Original/derivative storage growth exceeds margin | 3 | 2 | HIGH | Track every variant/version, quotas, retention, orphan cleanup and representative storage-cost model | Media / billing | 19, 28 |
| R-05 | CDN bandwidth and invalidation costs | 3 | 2 | HIGH | Responsive delivery, cache policy, egress alerts and load/cost tests | Media / operations | 19, 29 |
| R-06 | Copyright violations/copied work | 3 | 2 | HIGH | Ownership attestation, reports, moderation/appeals and evidence retention; watermarks are not legal proof | Trust / admin | 18, 27, 29 |
| R-07 | Fake portfolios, projects or impersonation | 3 | 2 | HIGH | Provenance labels, reporting/takedown in V1; evidence-backed verification in V1.5 | Trust | 18, 27 |
| R-08 | Malicious uploads, decompression bombs or malware | 3 | 3 | CRITICAL | Private quarantine, allowlist and content validation, decoder sandbox/resource limits, hostile fixture tests | Media / security | 03, 19 |
| R-09 | Account takeover/recovery abuse | 2 | 3 | HIGH | Secure sessions, brute-force limits, verification/recovery controls and revocation tests | Identity | 03, 07 |
| R-10 | Admin compromise or excessive staff access | 2 | 3 | HIGH | Mandatory MFA, scoped permissions, step-up, monitored break-glass and privileged audit tests | Identity / admin | 03, 29 |
| R-11 | Cross-tenant object, worker or cache access | 3 | 3 | CRITICAL | Tenant-qualified queries/relations/jobs/cache keys, deny by default and two-tenant adversarial suites | All domain modules | 02–29 |
| R-12 | SEO duplicate content across facets/domains/themes | 3 | 2 | HIGH | Canonical policy, finite indexable routes, redirect registry and crawler assertions | SEO | 20, V1.5-DOMAIN |
| R-13 | Thin/generated pages harm discovery quality | 3 | 2 | HIGH | Editorial content eligibility, no bulk facet page generation and content audit before indexing | SEO / discovery | 06, 20, 25 |
| R-14 | Poor Core Web Vitals on image-heavy portfolios | 3 | 2 | HIGH | Payload budgets, responsive images, minimal JS, lab/field monitoring and 3,000-image dataset checks | Web / media | 04, 17, 19, 29 |
| R-15 | 3D makes mobile unusable or inaccessible | 3 | 2 | HIGH | Lazy optional enhancement, static fallback, reduced motion and weak-device tests | Web / design | 05, 17 |
| R-16 | Backups fail silently or cannot restore | 2 | 3 | HIGH | Alerts, encrypted PITR/copies and timed restore exercise meeting agreed RPO/RTO | Operations | 01, 29 |
| R-17 | Payment failure/replay/refund state divergence | 2 | 3 | HIGH | Signed webhook tests, replay deduplication, reconciliation, immutable ledger and refund scenarios | Billing | 28 |
| R-18 | Data loss from edits, cleanup or migrations | 2 | 3 | HIGH | Optimistic concurrency, immutable versions, staged migrations, reference-aware purge and recovery tests | Portfolios / data / media | 02, 10, 19, 29 |
| R-19 | Spam leads or phone-number abuse | 3 | 2 | HIGH | Rate limits, progressive abuse challenges, deduplication and restricted contact exposure | Leads | 03, 26 |
| R-20 | Fake/retaliatory reviews and badge abuse | 3 | 2 | HIGH | Project/client linkage, explicit verification states, appeals and moderation audit | Reviews / trust | 27 |
| R-21 | Custom-domain takeover, TLS failure or canonical drift | 2 | 3 | HIGH | Ownership verification, periodic revalidation, unique mapping, TLS monitoring and removal/reattach tests | Domains / SEO | V1.5-DOMAIN |
| R-22 | Clean originals exposed through CDN/optimizer | 2 | 3 | HIGH | Isolate origin permissions, short-lived private grants and direct/guessed/optimizer bypass tests | Media / security | 19 |
| R-23 | Watermark missing, invisible or stale | 2 | 3 | HIGH | Publication readiness gate, bounded visibility, recipe versioning and thumbnail/OG/export assertions | Media / portfolios | 19, 20 |
| R-24 | Private share tokens leak or survive revocation | 2 | 3 | HIGH | Hash tokens, scope/expiry, no-store/referrer controls and API/media revocation tests | Collaboration / AI | 24 |
| R-25 | Private household data sent to wrong provider/log | 2 | 3 | HIGH | Consent, minimum payload, redaction, vendor retention review and log/provider request inspection | Privacy / AI / operations | 03, 21, 29 |
| R-26 | Queue redelivery causes duplicate publication or credits | 3 | 2 | HIGH | Outbox, idempotent handlers, lease recovery and crash/replay tests | Workers / billing | 19, 21, 28 |
| R-27 | Six themes diverge in schema/accessibility | 3 | 2 | HIGH | Common versioned data, capability validation and shared adversarial theme dataset | Portfolios / design | 10–17 |
| R-28 | White Bronze CTA text fails contrast | 3 | 2 | HIGH | Charcoal text verified at 5.35:1; test every theme/state; icon/text semantics | Design | 04, 17 |
| R-29 | Interrupted mobile uploads lose work | 3 | 2 | HIGH | Resume/retry, durable drafts, progress/cancel and network-interruption tests | Media / mobile | 18, 19, 23 |
| R-30 | Scope ordering mistaken for production readiness | 3 | 2 | HIGH | Explicit 18–20 integration gates; distinguish V1 and V1.5 deliverables in 24/27/28 | Architecture | 01–29 |
| R-31 | Dependency/runtime mismatch or vulnerable supply chain | 2 | 3 | HIGH | Pin supported versions, lockfiles, SBOM/scans, patch process and reproducible builds | Architecture / security | 01, 03, 29 |
| R-32 | Deletion undone by restoring backups/history | 2 | 3 | HIGH | Tombstones, purge propagation, history-reference filtering and restore-with-deletion tests | Privacy / operations | 19, 29 |
| R-33 | Unverified AI facts published as real project claims | 2 | 2 | MEDIUM | Designer confirmation for assistant output, provenance and publish validation | AI / projects | V1.5-ASSIST |
| R-34 | Analytics violate privacy or misstate conversions | 2 | 2 | MEDIUM | Minimal events, consent review, attribution definitions and synthetic event checks | Analytics | 28, V1.5-ANALYTICS |
| R-35 | Root studio slug collides with platform route | 2 | 2 | MEDIUM | Reserved namespaces, normalized uniqueness and alias/redirect tests | Portfolios / SEO | 02, 08, 20 |
| R-36 | V1 launch exceeds budget because all six themes are required | 3 | 2 | HIGH | Milestone estimates in 01, phase-specific signoffs; pilot explicitly labeled incomplete V1 | Product / portfolios | 01, 11–17 |

Review at each implementation phase and before releases. Closing a risk requires a linked test, review or operational drill; accepting residual risk requires a recorded owner and rationale. No register item is closed by Phase 00 documentation alone.

## Phase02 data-model risk review — 2026-09-17

All36 existing risks remain **OPEN**. The seven Phase02 documents provide design mitigation, not runtime closure. Critical/high data risks and acceptance evidence:

| Existing risk | Phase02 design mitigation / remaining exposure | Owner / closure gate |
|---|---|---|
| R-11 CRITICAL — cross-tenant access | Composite tenant+parent FKs, FORCE RLS, verified transaction-local context, scoped public/customer/grant/admin functions. Settings can be forged by a compromised trusted connection; application policy remains primary | Security/data; real PostgreSQL pool/role/nested-ID tests03 and each feature |
| R-02 CRITICAL; R-17/R-26 HIGH — charges/replays | Locked usage accounts, immutable ledger/reservations, provider-account-env inbox uniqueness, durable AI submission intent/reconciliation and fan-out receipts | AI/billing; concurrent last-credit, callback/poll, replay/refund and uncertain-timeout tests21/28 |
| R-08 CRITICAL; R-22/R-23 HIGH — unsafe/public media | Private immutable source, typed usages/private-only references, finite recipe/variant manifest, versioned watermark and publication gate | Media; real decoder/IAM/OAC/optimizer/stamp/takedown tests19/20 |
| R-10/R-24/R-25 HIGH — privilege/private data | Expiring action/resource access cases, step-up/audit, hashed review grants and live gateway, private reference/mask/evidence access | Identity/AI/trust; wrong-role/case/token/revocation/provider payload tests03/24/27 |
| R-18 HIGH — publication or schema data loss | Root+parent composite deferred pointer constraints, sealed child graph, optimistic drafts, studio publication/reference lock, forward-only migration contracts | Portfolio/projects/data; publish/purge/reorder/rollback and clean/upgrade migration tests10/18/19 |
| R-04/R-32 HIGH — retention/orphans/resurrection | Explicit object versions/checksums, reference-aware deletion tasks, durable independent tombstone journal, ledger-backed byte holds | Media/privacy/operations; orphan inventory, backup pre-deletion restore and provider cleanup19/29 |
| R-16 HIGH — incomplete restore | Separate complete-backup principal, all-tenant RLS completeness test, external provider reconciliation before replay | Operations; timed isolated restore/PITR drill29 |
| R-12/R-21 HIGH — URL/domain drift | Owner-to-current aliases with permanent uniqueness; verified host registry later; live indexability separate from preference | SEO/designers; rename/alias/host-loss/canonical tests20/V1.5 |
| R-31 HIGH — executable qualification | Native PostgreSQL18 uuidv7 chosen and documented; actual Java25/Flyway/JDBC/PG compatibility, triggers/policies and runtime unavailable here | Architecture; qualified build/database harness before executable Phase03 validation |

No new critical design contradiction remains identified after conceptual review. UUIDv7 exposes approximate creation time and is not a secret; random bearer tokens remain separate. The94-table catalogue is a full-roadmap blueprint, not a requirement to deploy84 V1 tables in Phase03. Retention periods remain configurable proposals pending policy review; schema approval does not authorize production collection or paid launch. See [security review](02_DATA_OWNERSHIP_SECURITY.md) and [migration test gates](02_MIGRATION_STRATEGY.md).
