-- ============================================================================
-- Phase 26 — Leads + CRM + WhatsApp Schema (H2 Compatibility)
-- ============================================================================

-- 1. Studio Leads Table
CREATE TABLE studio_leads (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    project_id uuid NULL,
    source text NOT NULL CHECK (source IN (
        'PROJECT_DISCOVERY',
        'PROFESSIONAL_DISCOVERY',
        'PUBLIC_PROJECT',
        'PUBLIC_PORTFOLIO',
        'DIRECT_INQUIRY',
        'WHATSAPP_HANDOFF'
    )),
    status text NOT NULL DEFAULT 'NEW' CHECK (status IN (
        'NEW',
        'CONTACTED',
        'QUALIFIED',
        'SITE_VISIT_PLANNED',
        'IN_DISCUSSION',
        'WON',
        'LOST',
        'ARCHIVED'
    )),
    name text NOT NULL,
    phone_normalized text NOT NULL,
    email_normalized text NULL,
    city text NULL,
    project_category text NULL,
    budget_range text NULL,
    message text NOT NULL,
    preferred_contact_channel text NULL CHECK (
        preferred_contact_channel IS NULL OR preferred_contact_channel IN ('PHONE', 'WHATSAPP', 'EMAIL', 'ANY')
    ),
    contact_consent_at timestamptz NOT NULL DEFAULT now(),
    whatsapp_consent_at timestamptz NULL,
    assigned_user_id uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    next_follow_up_at timestamptz NULL,
    lost_reason text NULL,
    possible_duplicate boolean NOT NULL DEFAULT false,
    idempotency_key text NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 1 CHECK (version >= 1),
    archived_at timestamptz NULL,
    CONSTRAINT uq_studio_leads_id_studio UNIQUE (id, studio_id),
    CONSTRAINT fk_studio_leads_project FOREIGN KEY (project_id, studio_id)
        REFERENCES studio_projects(id, studio_id) ON DELETE SET NULL
);

CREATE INDEX idx_studio_leads_inbox ON studio_leads (studio_id, status, created_at DESC);
CREATE INDEX idx_studio_leads_assigned ON studio_leads (studio_id, assigned_user_id);
CREATE INDEX idx_studio_leads_follow_up ON studio_leads (studio_id, next_follow_up_at);
CREATE INDEX idx_studio_leads_dedup ON studio_leads (studio_id, phone_normalized, created_at DESC);
CREATE INDEX idx_studio_leads_idempotency ON studio_leads (studio_id, idempotency_key);

-- 2. Lead Activity Table
CREATE TABLE lead_activities (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    lead_id uuid NOT NULL,
    studio_id uuid NOT NULL,
    actor_id uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    activity_type text NOT NULL CHECK (activity_type IN (
        'LEAD_CREATED',
        'STATUS_CHANGED',
        'ASSIGNED',
        'NOTE_ADDED',
        'FOLLOW_UP_CHANGED',
        'WHATSAPP_HANDOFF_OPENED',
        'WHATSAPP_MESSAGE_SENT',
        'WHATSAPP_MESSAGE_DELIVERED',
        'WHATSAPP_MESSAGE_READ',
        'WHATSAPP_MESSAGE_FAILED',
        'WON',
        'LOST',
        'ARCHIVED'
    )),
    details text NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_lead_activities_lead FOREIGN KEY (lead_id, studio_id)
        REFERENCES studio_leads(id, studio_id) ON DELETE CASCADE
);

CREATE INDEX idx_lead_activities_lead ON lead_activities (lead_id, created_at ASC);
CREATE INDEX idx_lead_activities_studio ON lead_activities (studio_id, created_at DESC);

-- 3. Lead Internal Notes Table
CREATE TABLE lead_notes (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    lead_id uuid NOT NULL,
    studio_id uuid NOT NULL,
    author_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    content text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_lead_notes_lead FOREIGN KEY (lead_id, studio_id)
        REFERENCES studio_leads(id, studio_id) ON DELETE CASCADE
);

CREATE INDEX idx_lead_notes_lead ON lead_notes (lead_id, created_at ASC);

-- 4. WhatsApp Messages Table
CREATE TABLE lead_whatsapp_messages (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    lead_id uuid NOT NULL,
    direction text NOT NULL CHECK (direction IN ('OUTBOUND', 'INBOUND')),
    provider text NOT NULL DEFAULT 'DISABLED',
    provider_message_id text NULL,
    status text NOT NULL CHECK (status IN ('QUEUED', 'SUBMITTED', 'SENT', 'DELIVERED', 'READ', 'FAILED')),
    body text NOT NULL,
    failure_code text NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    status_updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_lead_whatsapp_messages_lead FOREIGN KEY (lead_id, studio_id)
        REFERENCES studio_leads(id, studio_id) ON DELETE CASCADE
);

CREATE INDEX idx_whatsapp_messages_lead ON lead_whatsapp_messages (lead_id, created_at ASC);
CREATE INDEX idx_whatsapp_messages_provider_msg ON lead_whatsapp_messages (provider, provider_message_id);
