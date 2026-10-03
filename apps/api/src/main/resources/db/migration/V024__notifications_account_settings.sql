-- ============================================================================
-- Product Completion Round 2 — Persistent Notification Center, Account Settings & Customer Inquiries
-- ============================================================================

-- 1. Users Schema Extensions
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url text NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deactivated_at timestamptz NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deletion_requested_at timestamptz NULL;

-- 2. Customer Ownership Link on Studio Leads
ALTER TABLE studio_leads ADD COLUMN IF NOT EXISTS customer_user_id uuid NULL REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_studio_leads_customer_user_id ON studio_leads (customer_user_id, created_at DESC) WHERE customer_user_id IS NOT NULL;

-- Customer can read inquiries they submitted
DROP POLICY IF EXISTS customer_select_studio_leads ON studio_leads;
CREATE POLICY customer_select_studio_leads ON studio_leads
    FOR SELECT
    USING (
        customer_user_id IS NOT NULL
        AND customer_user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
    );

-- 3. Persistent Notifications Center Table
CREATE TABLE IF NOT EXISTS notifications (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    studio_id uuid NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    type text NOT NULL CHECK (type IN (
        'NEW_LEAD',
        'LEAD_FOLLOW_UP',
        'NEW_REVIEW',
        'REVIEW_RESPONSE',
        'VERIFICATION_UPDATE',
        'AI_GENERATION_COMPLETE',
        'AI_GENERATION_FAILED',
        'CLIENT_APPROVED_CONCEPT',
        'CLIENT_REQUESTED_CHANGES',
        'PORTFOLIO_PUBLISHED',
        'PORTFOLIO_ACTION_REQUIRED',
        'PROJECT_ACTION_REQUIRED',
        'SYSTEM_NOTICE'
    )),
    title text NOT NULL,
    message text NOT NULL,
    action_url text NULL,
    read_at timestamptz NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    metadata jsonb NULL
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_inbox ON notifications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications (user_id, created_at DESC) WHERE read_at IS NULL;

ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_notifications ON notifications;
CREATE POLICY tenant_isolation_notifications ON notifications
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

-- 4. Notification Preferences Table
CREATE TABLE IF NOT EXISTS notification_preferences (
    user_id uuid NOT NULL PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    in_app_enabled boolean NOT NULL DEFAULT true,
    email_enabled boolean NOT NULL DEFAULT false,
    whatsapp_enabled boolean NOT NULL DEFAULT false,
    lead_notifications boolean NOT NULL DEFAULT true,
    review_notifications boolean NOT NULL DEFAULT true,
    ai_notifications boolean NOT NULL DEFAULT true,
    system_notifications boolean NOT NULL DEFAULT true,
    updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE notification_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_preferences FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_notification_preferences ON notification_preferences;
CREATE POLICY tenant_isolation_notification_preferences ON notification_preferences
    USING (user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid);

-- 5. Platform Support & Feedback Table
CREATE TABLE IF NOT EXISTS platform_feedback (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    user_id uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    category text NOT NULL CHECK (category IN ('BUG', 'INCORRECT_INFO', 'PRIVACY_CONCERN', 'GENERAL', 'FEATURE_REQUEST')),
    message text NOT NULL,
    contact_email text NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_platform_feedback_created ON platform_feedback (created_at DESC);
