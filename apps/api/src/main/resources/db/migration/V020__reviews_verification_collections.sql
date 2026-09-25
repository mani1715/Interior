-- ============================================================================
-- Phase 27 — Reviews + Verification + Collections Schema (PostgreSQL 18)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. REVIEWS DOMAIN
-- ----------------------------------------------------------------------------

-- 1.1 Review Invitations (Derives strictly from studio leads)
CREATE TABLE review_invitations (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    lead_id uuid NOT NULL,
    project_id uuid NULL,
    token_hash bytea NOT NULL UNIQUE,
    status text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'USED', 'EXPIRED', 'REVOKED')),
    expires_at timestamptz NOT NULL,
    created_by uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    revoked_at timestamptz NULL,
    used_at timestamptz NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 1 CHECK (version >= 1),
    CONSTRAINT fk_review_invitations_lead FOREIGN KEY (lead_id, studio_id)
        REFERENCES studio_leads(id, studio_id) ON DELETE CASCADE,
    CONSTRAINT fk_review_invitations_project FOREIGN KEY (project_id, studio_id)
        REFERENCES studio_projects(id, studio_id) ON DELETE SET NULL,
    CONSTRAINT uq_review_invitations_lead UNIQUE (studio_id, lead_id)
);

CREATE INDEX idx_review_invitations_studio ON review_invitations (studio_id, status, created_at DESC);
CREATE INDEX idx_review_invitations_token ON review_invitations (token_hash);

-- 1.2 Review Invitation Scoped Sessions (Token exchange HttpOnly cookies)
CREATE TABLE review_invitation_sessions (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    invitation_id uuid NOT NULL REFERENCES review_invitations(id) ON DELETE CASCADE,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    session_token_hash bytea NOT NULL UNIQUE,
    csrf_token_hash bytea NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_review_sessions_token ON review_invitation_sessions (session_token_hash);
CREATE INDEX idx_review_sessions_invitation ON review_invitation_sessions (invitation_id);

-- 1.3 Studio Reviews (Client authored, immutable to studio)
CREATE TABLE studio_reviews (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    lead_id uuid NOT NULL,
    project_id uuid NULL,
    review_invitation_id uuid NOT NULL REFERENCES review_invitations(id) ON DELETE CASCADE,
    rating integer NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title varchar(150) NULL,
    review_text text NOT NULL CHECK (char_length(review_text) >= 10 AND char_length(review_text) <= 2000),
    reviewer_display_name varchar(100) NOT NULL,
    display_name_mode text NOT NULL DEFAULT 'FIRST_NAME' CHECK (display_name_mode IN ('FIRST_NAME', 'INITIALS', 'ANONYMOUS')),
    status text NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('SUBMITTED', 'PUBLISHED', 'FLAGGED', 'REMOVED')),
    studio_response_text text NULL CHECK (studio_response_text IS NULL OR char_length(studio_response_text) <= 1500),
    studio_response_at timestamptz NULL,
    flagged_at timestamptz NULL,
    removed_at timestamptz NULL,
    submitted_at timestamptz NOT NULL DEFAULT now(),
    published_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 1 CHECK (version >= 1),
    CONSTRAINT uq_studio_reviews_invitation UNIQUE (review_invitation_id),
    CONSTRAINT uq_studio_reviews_lead UNIQUE (studio_id, lead_id),
    CONSTRAINT fk_studio_reviews_lead FOREIGN KEY (lead_id, studio_id)
        REFERENCES studio_leads(id, studio_id) ON DELETE RESTRICT,
    CONSTRAINT fk_studio_reviews_project FOREIGN KEY (project_id, studio_id)
        REFERENCES studio_projects(id, studio_id) ON DELETE SET NULL
);

CREATE INDEX idx_studio_reviews_studio_published ON studio_reviews (studio_id, status, published_at DESC);
CREATE INDEX idx_studio_reviews_project ON studio_reviews (project_id) WHERE project_id IS NOT NULL;

-- 1.4 Review Abuse Reports
CREATE TABLE review_reports (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    review_id uuid NOT NULL REFERENCES studio_reviews(id) ON DELETE CASCADE,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    reporter_user_id uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    reporter_ip varchar(45) NULL,
    reason text NOT NULL CHECK (reason IN ('SPAM', 'HARASSMENT', 'PERSONAL_INFO', 'NOT_CLIENT', 'OTHER')),
    details varchar(500) NULL,
    status text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'REVIEWED', 'DISMISSED')),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_review_reports_review ON review_reports (review_id, status);

-- ----------------------------------------------------------------------------
-- 2. VERIFICATION DOMAIN
-- ----------------------------------------------------------------------------

-- 2.1 Studio Verifications
CREATE TABLE studio_verifications (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE CONSTRAINT uq_studio_verifications_studio UNIQUE,
    status text NOT NULL DEFAULT 'NOT_SUBMITTED' CHECK (status IN (
        'NOT_SUBMITTED', 'PENDING', 'NEEDS_MORE_INFO', 'VERIFIED', 'REJECTED', 'REVERIFY_REQUIRED', 'EXPIRED'
    )),
    business_name text NOT NULL,
    professional_type text NOT NULL,
    registration_number varchar(100) NULL,
    gst_number varchar(50) NULL,
    website_domain varchar(255) NULL,
    notes text NULL,
    decision_reason text NULL,
    verified_at timestamptz NULL,
    expires_at timestamptz NULL,
    verified_snapshot jsonb NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 1 CHECK (version >= 1)
);

CREATE INDEX idx_studio_verifications_status ON studio_verifications (status);

-- 2.2 Studio Verification Documents (Private Evidence Storage)
CREATE TABLE studio_verification_documents (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    verification_id uuid NOT NULL REFERENCES studio_verifications(id) ON DELETE CASCADE,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    document_type text NOT NULL CHECK (document_type IN (
        'BUSINESS_REGISTRATION', 'GST_CERTIFICATE', 'PROFESSIONAL_LICENSE', 'OTHER'
    )),
    storage_key varchar(500) NOT NULL,
    original_filename varchar(255) NOT NULL,
    mime_type varchar(100) NOT NULL CHECK (mime_type IN ('application/pdf', 'image/jpeg', 'image/png')),
    file_size_bytes bigint NOT NULL CHECK (file_size_bytes > 0 AND file_size_bytes <= 10485760),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_studio_verification_docs ON studio_verification_documents (verification_id);

-- 2.3 Studio Verification Events (Append-only decision audit)
CREATE TABLE studio_verification_events (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    verification_id uuid NOT NULL REFERENCES studio_verifications(id) ON DELETE CASCADE,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    event_type text NOT NULL CHECK (event_type IN (
        'SUBMITTED', 'MORE_INFO_REQUESTED', 'DOCUMENT_ADDED', 'VERIFIED', 'REJECTED', 'REVERIFY_REQUIRED', 'EXPIRED'
    )),
    actor_user_id uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    reason text NULL,
    metadata jsonb NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_studio_verification_events ON studio_verification_events (verification_id, created_at ASC);

-- ----------------------------------------------------------------------------
-- 3. COLLECTIONS DOMAIN
-- ----------------------------------------------------------------------------

-- 3.1 User Collections (User-owned private inspiration boards)
CREATE TABLE user_collections (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    owner_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title varchar(100) NOT NULL,
    description varchar(500) NULL,
    is_default boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 1 CHECK (version >= 1)
);

CREATE INDEX idx_user_collections_owner ON user_collections (owner_user_id, updated_at DESC);
CREATE UNIQUE INDEX uq_user_default_collection ON user_collections (owner_user_id) WHERE (is_default = true);

-- 3.2 Collection Items
CREATE TABLE collection_items (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    collection_id uuid NOT NULL REFERENCES user_collections(id) ON DELETE CASCADE,
    owner_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id uuid NOT NULL REFERENCES studio_projects(id) ON DELETE CASCADE,
    note varchar(500) NULL,
    display_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_collection_project UNIQUE (collection_id, project_id)
);

CREATE INDEX idx_collection_items_order ON collection_items (collection_id, display_order ASC, created_at ASC);
CREATE INDEX idx_collection_items_project ON collection_items (project_id);

-- ----------------------------------------------------------------------------
-- 4. ROW LEVEL SECURITY (RLS)
-- ----------------------------------------------------------------------------

-- 4.1 Reviews RLS (Studio tenant isolation)
ALTER TABLE review_invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE review_invitations FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_review_invitations ON review_invitations
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

ALTER TABLE review_invitation_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE review_invitation_sessions FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_review_sessions ON review_invitation_sessions
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

ALTER TABLE studio_reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE studio_reviews FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_studio_reviews ON studio_reviews
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

ALTER TABLE review_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE review_reports FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_review_reports ON review_reports
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

-- 4.2 Verification RLS (Studio tenant isolation)
ALTER TABLE studio_verifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE studio_verifications FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_studio_verifications ON studio_verifications
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

ALTER TABLE studio_verification_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE studio_verification_documents FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_studio_verification_documents ON studio_verification_documents
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

ALTER TABLE studio_verification_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE studio_verification_events FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_studio_verification_events ON studio_verification_events
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

-- 4.3 Collections RLS (User ownership isolation, NOT studio tenant!)
ALTER TABLE user_collections ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_collections FORCE ROW LEVEL SECURITY;
CREATE POLICY user_isolation_user_collections ON user_collections
    FOR ALL
    USING (
        owner_user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

ALTER TABLE collection_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE collection_items FORCE ROW LEVEL SECURITY;
CREATE POLICY user_isolation_collection_items ON collection_items
    FOR ALL
    USING (
        owner_user_id = NULLIF(current_setting('app.current_user_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );
