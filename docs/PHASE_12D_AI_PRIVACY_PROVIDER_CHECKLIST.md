# Elégance Platform: Phase 12D AI Data Privacy & Provider Checklist

## 1. Executive Summary & Privacy Principles

Customer and designer project imagery—such as residential floor plans, private living spaces, bespoke cabinetry details, and client furnishings—constitutes highly sensitive proprietary and personal data.

Under the Elégance platform data governance charter, **no customer or designer data may be used to train, retrain, or fine-tune public foundation AI models**, nor may provider logs retain unencrypted client images indefinitely.

---

## 2. Regulatory Alignment

This privacy framework complies with:
- **India Digital Personal Data Protection Act (DPDPA 2023)**: Purpose limitation, data minimization, consent management.
- **European Union General Data Protection Regulation (GDPR)**: Articles 28 (Processor obligations), 44–46 (International transfers), 32 (Security of processing).
- **California Consumer Privacy Act (CCPA / CPRA)**: Non-sale of consumer images and biometric protections.

---

## 3. Mandatory Provider Privacy & Compliance Checklist

Every external AI provider evaluated or onboarded during Phase 15 must satisfy all mandatory requirements below:

### Dimension 1: Model Training & Intellectual Property Exclusion
- [x] **Zero-Training Guarantee**: Provider contractually certifies that customer images, masks, and prompts are **never used** to train, fine-tune, evaluate, or improve foundation or proprietary models.
- [x] **Enterprise Account Exemption**: Consumer terms-of-service (which typically reserve training rights) are strictly avoided in favor of Enterprise Service Agreements (e.g., Google Cloud Vertex AI Enterprise Terms, Stability AI Enterprise Agreement).
- [x] **Intellectual Property Indemnification**: Provider provides IP infringement indemnification covering AI-generated visual outputs.

### Dimension 2: Data Retention & Ephemeral Processing
- [x] **Zero Permanent Provider Storage**: Source images and generated outputs are processed ephemerally in RAM or temporary scratch disks and purged immediately upon job completion or within <= 24 hours.
- [x] **Pre-Signed URL Expiration**: When using pre-signed storage URLs for provider ingest, the URL lifetime is constrained to <= 15 minutes.
- [x] **No Public CDN Caching**: Generated images must not be indexed by public search engine crawlers or cached in publicly discoverable CDNs.

### Dimension 3: PII & Metadata Minimization
- [x] **Prompt Sanitization**: Prompts sent to providers are stripped of customer names, residential physical addresses, studio contact numbers, or designer identifiers.
- [x] **EXIF / Geolocation Scrubbing**: All EXIF metadata (GPS coordinates, camera serial numbers, device metadata) is stripped from images before being transmitted to external AI endpoints.
- [x] **Tenant Token Isolation**: API keys and service accounts are isolated per environment (production vs staging).

### Dimension 4: Regional Data Sovereignty & Transfer
- [x] **Regional Processing Boundaries**: Provider endpoints are configured to route through authorized geographic regions:
  - India: `asia-south1` (Mumbai) / `asia-south2` (Delhi)
  - Europe: `europe-west3` (Frankfurt) / `europe-west1` (Belgium)
  - Americas: `us-central1` (Iowa) / `us-east4` (Virginia)
- [x] **Cross-Border Transfer Safeguards**: Standard Contractual Clauses (SCCs) and Data Transfer Impact Assessments (DTIAs) executed prior to routing EU/Indian resident imagery to US cloud endpoints.

### Dimension 5: Security Architecture & Protocol Enforcement
- [x] **Encryption in Transit**: TLS 1.2 or TLS 1.3 enforced for all provider REST calls with strict modern cipher suites.
- [x] **SSRF Boundary Enforcement**: All provider-returned URLs are filtered through `SsrfProtectionService` to prevent cloud infrastructure metadata harvesting.
- [x] **Malware Defense**: Provider outputs are scanned via `MalwareScanner` prior to storage persistence.
- [x] **Auditability**: All AI transactions are immutably logged in `ai_generation_cost_ledger` with timestamp, tenant ID, cost, and provider metadata.

---

## 4. Provider Compliance Audit Summary

| Evaluation Item | Google Cloud Vertex AI | Stability AI (Enterprise) | Replicate |
| :--- | :---: | :---: | :---: |
| Contractual Zero-Training Guarantee | **PASSED** (Enterprise default) | **PASSED** (With Enterprise tier) | Requires custom agreement |
| Ephemeral Media Processing | **PASSED** | **PASSED** | Varies by creator |
| Regional Data Pinning (EU / India) | **PASSED** (`asia-south1`, `europe-west3`) | Limited (US/Global CDN) | Limited (US-East/West) |
| Standard Contractual Clauses (SCC) | **PASSED** | **PASSED** | Available on Enterprise |
| SOC 2 Type II / ISO 27001 Certified | **PASSED** | **PASSED** | **PASSED** |
| Overall Compliance Readiness | **READY FOR PHASE 15** | **CONDITIONAL (Enterprise tier)**| **PHASE 16+ REVIEW** |
