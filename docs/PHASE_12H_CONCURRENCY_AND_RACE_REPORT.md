# Phase 12H — Concurrency, Race Condition & Idempotency Report

**Product:** Elégance — Interior Designer Platform  
**Phase:** 12H (Performance, Concurrency, Load, Resilience & Whole-Product Failure Testing)  
**Status:** COMPLETE (Zero Race Vulnerabilities Detected)  
**Date:** 2026-10-16  

---

## 1. Concurrency Architecture & Row-Level Locking

To prevent race conditions without reducing cross-tenant parallelism, Elégance employs fine-grained studio row locking for quota mutations:

```sql
SELECT id FROM designer_studios WHERE id = ? FOR UPDATE;
```

### Key Architectural Properties
1. **Studio-Scoped Serialization:** Concurrent mutations targeting Studio A lock only Studio A's row in `designer_studios`.
2. **Zero Cross-Studio Contention:** Operations for Studio B execute concurrently without delay or blocking from Studio A.
3. **Transaction-Scoped Release:** Locks are automatically acquired at the start of the quota verification within `@Transactional` boundaries and released upon commit or rollback.
4. **Zero Quota Drift:** Quota counts (`countProjects`, `countCommittedPortfolioPhotos`, `countPendingStorageBytes`) are evaluated *after* acquiring the row lock, guaranteeing linearizable reads and zero time-of-check to time-of-use (TOCTOU) races.

---

## 2. Race Condition Verification Matrix

All 12 test methods in `apps/api/src/test/java/com/interior/platform/performance/ConcurrencyAndResilienceTest.java` passed cleanly with 0 failures and 0 errors:

| Race Test Scenario | Concurrency (Threads) | Pre-State | Target Boundary | Expected Outcome | Verified Database State | Result |
|:---|:---:|:---:|:---:|:---|:---|:---:|
| **Project Creation Cap Race** | 15 parallel threads | 9 projects | STANDARD limit: 10 | Exactly 1 success, 14 rejections | `countProjects == 10` (never 11) | **PASS** |
| **Photo Quota Cap Race** | 10 parallel threads | 14 photos | STANDARD limit: 15 | Exactly 1 success, 9 rejections | `committed + pending == 15` | **PASS** |
| **Storage Quota Boundary Race** | 10 parallel threads | Limit - 10MB | STANDARD limit: 5GB | Exactly 1 success (6MB), 9 rejections | `committed + pending == Limit - 4MB` | **PASS** |
| **Cinematic Mode Cap Race** | 10 parallel threads | 4 Cinematic | PRO limit: 5 | Exactly 1 success, 9 rejections | `countCinematicProjects == 5` | **PASS** |
| **BASE Fallback Protection** | Serial & Concurrent | Sub CANCELLED | Fallback tier | Falls back to STANDARD (10 projects) | Unmetered BASE plan strictly prevented | **PASS** |
| **Team Invitation Token Reuse** | 10 parallel threads | 1 pending token | 1 membership | Exactly 1 acceptance, 9 deduplicated | `studio_members` count == 1 | **PASS** |
| **Billing Webhook Idempotency** | 10 parallel threads | 0 events | 1 event record | Exactly 1 insertion, 9 deduplicated | `billing_events` count == 1 | **PASS** |
| **Scheduled Job Singleton Lock** | 2 parallel nodes | 0 runs | Singleton run | 1 node acquires advisory lock, 1 skips | Zero duplicate job executions | **PASS** |
| **Maintenance Mode Transition** | Read/Write mix | Normal mode | Read-only mode | Writes fail immediately (503), reads succeed | Gauge toggles 0 <-> 1 instantaneously | **PASS** |
| **Connection Pool Exhaustion** | 20 parallel threads | 10 conns max | 100 queries | All 100 queries complete within timeout | Zero thread leaks, pool fully recovers | **PASS** |
| **Post-Load Data Integrity** | Database audit | Stressed state | 0 anomalies | Zero negative bytes, zero orphan rows | Clean relational integrity | **PASS** |
| **Metric Cardinality Bounding** | 50 metric bursts | High volume | Low cardinality | Zero UUIDs, emails, or URLs as tags | High-volume metrics strictly bounded | **PASS** |

---

## 3. Quota Race Mechanics & Verification Details

### 3.1 Project Creation Boundary Race
- **Rule:** Under the STANDARD subscription, a studio is entitled to a maximum of 10 projects.
- **Race Condition Tested:** A studio with 9 projects issues 15 concurrent project creation requests simultaneously using `CountDownLatch`.
- **Enforcement:** `EntitlementService.assertProjectCreationAllowed(studioId)` locks the studio row via `billingRepository.lockStudioForQuota(studioId)` before evaluating `projectRepository.countProjects(studioId)`.
- **Result:** Exactly 1 thread successfully creates Project #10. The remaining 14 threads receive `BadRequestException: "Project limit reached for current plan. Upgrade required."` Database verification confirms exactly 10 projects exist.

### 3.2 Storage Quota Boundary Race
- **Rule:** Storage quota must never be oversubscribed or result in negative available bytes.
- **Race Condition Tested:** A studio has 10 MB remaining before reaching its 5 GB plan storage cap. 10 concurrent threads simultaneously request upload intents of 6 MB each.
- **Enforcement:** `EntitlementService.assertStorageQuotaAllowed(studioId, 6_000_000L)` locks the studio row and checks `committedBytes + pendingBytes + requestedBytes <= limit`.
- **Result:** Exactly 1 intent succeeds (allocating 6 MB, leaving 4 MB). The subsequent 9 threads fail because `committed + pending + 6MB > limit`. Total storage reserved remains strictly $\le 5\text{GB}$.

### 3.3 Team Invitation Single-Use Concurrency
- **Rule:** A team member invitation token must only be accepted once, preventing multiple user memberships or corrupted state.
- **Race Condition Tested:** 10 concurrent requests present the same raw invitation token simultaneously.
- **Enforcement:** `JdbcStudioTeamRepository.acceptInvitation` executes an atomic conditional update:
  ```sql
  UPDATE studio_member_invitations 
  SET status = 'ACCEPTED', updated_at = now() 
  WHERE id = ? AND status = 'PENDING'
  ```
  Coupled with the unique index on `studio_members(studio_id, user_id)`.
- **Result:** Exactly 1 acceptance succeeds. Exactly 1 membership row exists. 0 duplicate records.

### 3.4 Webhook Idempotency Concurrency
- **Rule:** Multiple identical webhook events delivered concurrently from payment providers (e.g. network retry bursts) must never duplicate ledger transactions or trigger duplicate provisioning.
- **Race Condition Tested:** 10 concurrent threads deliver the same `(provider, provider_event_id)` pair.
- **Enforcement:** `uq_billing_events_provider_event` unique index enforces database-level conflict detection:
  ```sql
  INSERT INTO billing_events (...) VALUES (...) 
  ON CONFLICT (provider, provider_event_id) DO NOTHING
  ```
- **Result:** Exactly 1 insertion returns `true`, 9 return `false` (deduplicated). Exactly 1 event stored in `billing_events`.

---

## 4. Concurrency Verdict

The Elégance backend is **strictly concurrency-safe and race-proof**. All commercial boundaries, quotas, single-use tokens, and webhooks enforce linearizable guarantees at the database transaction layer.
