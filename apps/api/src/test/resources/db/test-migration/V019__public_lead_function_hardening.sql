-- ============================================================================
-- Phase 26.2 — Security Definer + Public Lead Write Hardening (H2 Compatibility)
-- ============================================================================

-- Stored procedure submit_public_lead and PostgreSQL-specific role permissions
-- are managed natively in PostgreSQL. H2 test profile uses JDBC repository operations.
SELECT 1;
