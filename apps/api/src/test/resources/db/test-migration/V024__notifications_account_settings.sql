-- ============================================================================
-- V024: Notifications, Account Settings & Customer Inquiries (H2 Test Migration)
-- ============================================================================

-- 1. Users Schema Extensions
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url text NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deactivated_at timestamptz NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS deletion_requested_at timestamptz NULL;

-- 2. Customer Ownership Link on Studio Leads
ALTER TABLE studio_leads ADD COLUMN IF NOT EXISTS customer_user_id uuid NULL;
CREATE INDEX IF NOT EXISTS idx_studio_leads_customer_user_id ON studio_leads (customer_user_id, created_at DESC);

-- 3. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id uuid NOT NULL PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    studio_id uuid NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    type varchar(64) NOT NULL,
    title varchar(255) NOT NULL,
    message text NOT NULL,
    action_url varchar(512) NULL,
    read_at timestamptz NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    metadata text NULL
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_inbox ON notifications (user_id, created_at DESC);

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

-- 5. Platform Support & Feedback Table
CREATE TABLE IF NOT EXISTS platform_feedback (
    id uuid NOT NULL PRIMARY KEY,
    user_id uuid NULL,
    category varchar(64) NOT NULL,
    message text NOT NULL,
    contact_email varchar(255) NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_platform_feedback_created ON platform_feedback (created_at DESC);
