# ELÉGANCE DATA RETENTION & ANONYMIZATION MATRIX

| Data Category | Table(s) | Retention Period | Deletion / Anonymization Policy | Legal Basis / Regulatory Context |
|---|---|---|---|---|
| **User Identity & Profile** | `users` | Active until user request or account closure | Immediately anonymized upon deletion (`display_name = 'Deleted User'`, `email = NULL`, `phone = NULL`, `avatar_url = NULL`, `status = 'DELETED'`). | GDPR Art 17 / DPDP / CCPA Right to Erasure |
| **Authentication Bindings** | `identity_external_identities` | Active lifespan | Hard deleted upon deletion request. | Authentication credentials security |
| **Multi-Factor Secrets** | `identity_mfa_methods`, `identity_recovery_codes` | Active lifespan | Hard deleted upon deletion request. | Zero retention of cryptographic key material |
| **Active Sessions** | `identity_sessions` | Idle 30m / Max 24h | Hard deleted immediately on account closure or deletion. | Session assurance & access revocation |
| **Private Collections / Moodboards** | `user_collections` | Lifetime of user account | Hard deleted upon user deletion. | User content privacy |
| **Notifications Inbox** | `notifications` | 90 days default | Hard deleted upon user deletion. | Operational message lifecycle |
| **Customer Inquiries / Leads** | `studio_leads` | 3 years | Retained for studio CRM records with anonymized `customer_user_id` linkage. | Commercial record keeping |
| **Studio Records & Portfolios** | `designer_studios`, `studio_projects`, `project_rooms`, `media_assets` | Lifetime of studio | Unpublished/Archived on studio closure; preserved under studio tenant for collaborative team attribution. | Business continuity & Intellectual property |
| **Billing & Commercial Transactions** | `billing_transactions`, `billing_checkout_sessions`, `studio_subscriptions` | 7 years minimum | Retained with immutable ledger references; never hard deleted. | Statutory financial compliance & GST/Tax laws |
| **AI Usage & Cost Ledgers** | `ai_usage_events`, `ai_cost_ledger` | 5 years | Retained with studio scoping for multi-currency reconciliation; user references pseudonymized. | Financial audit & provider cost verification |
| **Audit Trails & Security Events** | `audit_events` | 5 years | Immutable; user actor IDs preserved as tombstoned historical references. | Security forensics & regulatory audit defense |
| **Legal Compliance Holds** | `privacy_legal_holds` | Until released by Compliance Officer | Overrides and blocks all deletion/closure requests until active status is released. | Litigation hold defense & legal compliance |
