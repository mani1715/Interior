-- ============================================================================
-- Phase 25.2 — Discovery Public Read Hardening (H2 Compatibility)
-- H2 dialect does not support PostgreSQL RLS policies or GIN expressions.
-- ============================================================================

-- No-op for H2 test profile
SELECT 1;
