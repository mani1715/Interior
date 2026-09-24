-- ============================================================================
-- Phase 26.1 — Leads Security Hardening (H2 Compatibility)
-- ============================================================================

-- 1. Ensure unique constraint on idempotency_key for studio_leads
CREATE INDEX IF NOT EXISTS uq_studio_leads_idempotency
    ON studio_leads (studio_id, idempotency_key);

-- 2. Add idempotency_key to lead_whatsapp_messages
ALTER TABLE lead_whatsapp_messages ADD COLUMN IF NOT EXISTS idempotency_key text NULL;

CREATE INDEX IF NOT EXISTS uq_lead_whatsapp_msg_idempotency
    ON lead_whatsapp_messages (studio_id, idempotency_key);
