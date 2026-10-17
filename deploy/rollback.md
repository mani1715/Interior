# Production Rollback Decision Tree & Runbook

## 1. Rollback Principle

- **Application Rollback:** Highly preferred and instant. Reverting the immutable container image tag restores previous known-good state.
- **Database Downgrade:** NEVER automatic. Flyway undo migrations or manual reverse SQL are strictly prohibited without tested procedures. If a migration is backward-compatible (expand-contract), rolling back the application container is safe without touching the database schema.
- **Data Restoration:** If destructive schema corruption occurred, restore from the verified pre-release backup.

---

## 2. 4-Tier Rollback Decision Tree

```
                                 [INCIDENT DETECTED]
                                          |
        +---------------------------------+---------------------------------+
        |                                                                   |
        v                                                                   v
 [TIER A: App Defect, Schema Compatible]                [TIER B: Provider Configuration Defect]
        |                                                                   |
        | Action: Revert container image tags                               | Action: Revert invalid env vars or
        | to previous immutable Git SHA tag.                                | disable provider (set provider=disabled).
        |                                                                   |
        +---------------------------------+---------------------------------+
                                          |
        +---------------------------------+---------------------------------+
        |                                                                   |
        v                                                                   v
 [TIER C: Migration Failure During Boot]                 [TIER D: Destructive Data / Schema Bug]
        |                                                                   |
        | Action: Stop release immediately. Flyway                          | Action: Declare emergency incident.
        | transactional DDL aborted migration.                              | Enable READ_ONLY mode. Restore DB from
        | Schema remains intact. Previous app runs.                         | pre-release pg_dump -Fc backup snapshot.
```

---

## 3. Automated Rollback Triggers

An automated rollback must be initiated if any of the following conditions persist for > 3 minutes post-deployment:
1. **Readiness Probe Failure:** Backend replicas fail `/api/v1/actuator/health/readiness`.
2. **HTTP 5xx Error Spike:** 5xx response rate exceeds 1.0% of total inbound traffic.
3. **Database Connection Saturation:** Hikari connection pool wait times exceed 5,000 ms or active connections hit 100% pool capacity.
4. **Authentication Failure Spike:** Session verification or token decryption fails catastrophically.
5. **Cross-Tenant Bleeding:** Any RLS security boundary violation or query leak detected in audit logs.
