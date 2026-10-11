-- ============================================================================
-- Phase 9B: Transactional Email & Communication Delivery Productionization (Test H2)
-- ============================================================================

-- Index for retry scheduler in H2
CREATE INDEX IF NOT EXISTS idx_comm_deliveries_retry_scheduled
ON communication_deliveries (next_retry_at);
