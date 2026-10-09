-- ============================================================================
-- Phase 7C: Communication Delivery Tracking & Notification Expansion
-- ============================================================================

-- 1. Expand CHECK constraint on notifications.type
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (type IN (
    'NEW_LEAD',
    'LEAD_ASSIGNED',
    'LEAD_FOLLOW_UP',
    'NEW_REVIEW',
    'REVIEW_RESPONSE',
    'REVIEW_INVITATION_READY',
    'VERIFICATION_UPDATE',
    'AI_GENERATION_COMPLETE',
    'AI_GENERATION_FAILED',
    'CLIENT_APPROVED_CONCEPT',
    'CLIENT_REQUESTED_CHANGES',
    'CLIENT_FEEDBACK_RECEIVED',
    'PORTFOLIO_PUBLISHED',
    'PORTFOLIO_ACTION_REQUIRED',
    'PROJECT_ACTION_REQUIRED',
    'PROJECT_PUBLISH_STATE_CHANGED',
    'TEAM_INVITATION',
    'STUDIO_INVITATION_ACCEPTED',
    'STUDIO_MEMBER_ADDED',
    'STUDIO_ROLE_CHANGED',
    'STUDIO_MEMBER_REMOVED',
    'SYSTEM_NOTICE'
));

-- 2. Truthful Communication Delivery Log Table
CREATE TABLE IF NOT EXISTS communication_deliveries (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    recipient_user_id uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    channel text NOT NULL CHECK (channel IN ('EMAIL', 'WHATSAPP', 'IN_APP')),
    event_type text NOT NULL,
    recipient text NOT NULL,
    subject_or_summary text NULL,
    status text NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'NOT_CONFIGURED', 'DELIVERED', 'RETRY_SCHEDULED')),
    provider text NOT NULL,
    provider_message_id text NULL,
    attempt_count int NOT NULL DEFAULT 0,
    max_attempts int NOT NULL DEFAULT 3,
    last_error text NULL,
    next_retry_at timestamptz NULL,
    idempotency_key text NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    delivered_at timestamptz NULL
);

CREATE INDEX IF NOT EXISTS idx_comm_deliveries_studio ON communication_deliveries (studio_id, created_at DESC) WHERE studio_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_comm_deliveries_recipient ON communication_deliveries (recipient_user_id, created_at DESC) WHERE recipient_user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_comm_deliveries_retry ON communication_deliveries (status, next_retry_at) WHERE status = 'RETRY_SCHEDULED';
CREATE INDEX IF NOT EXISTS idx_comm_deliveries_idempotency ON communication_deliveries (idempotency_key) WHERE idempotency_key IS NOT NULL;

-- 3. Row Level Security for Multi-Tenant Isolation
ALTER TABLE communication_deliveries ENABLE ROW LEVEL SECURITY;
ALTER TABLE communication_deliveries FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_communication_deliveries ON communication_deliveries;
CREATE POLICY tenant_isolation_communication_deliveries ON communication_deliveries
    USING (
        studio_id IS NULL
        OR studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
    );
