-- ============================================================================
-- Phase 9B: Transactional Email & Communication Delivery Productionization
-- Expanded delivery statuses (BOUNCED, COMPLAINT)
-- ============================================================================

-- Expand CHECK constraint on communication_deliveries.status
ALTER TABLE communication_deliveries DROP CONSTRAINT IF EXISTS communication_deliveries_status_check;
ALTER TABLE communication_deliveries ADD CONSTRAINT communication_deliveries_status_check CHECK (status IN (
    'PENDING',
    'SENT',
    'FAILED',
    'NOT_CONFIGURED',
    'DELIVERED',
    'RETRY_SCHEDULED',
    'BOUNCED',
    'COMPLAINT'
));

-- Index for retry scheduler: quickly claim pending retries
CREATE INDEX IF NOT EXISTS idx_comm_deliveries_retry_scheduled
ON communication_deliveries (next_retry_at)
WHERE status = 'RETRY_SCHEDULED';
