-- ============================================================================
-- Phase 24 — AI Variations + History + Client Approval Schema (PostgreSQL 18)
-- ============================================================================

-- 1. Extend ai_visualization_jobs with lineage, shortlisting, and labeling
ALTER TABLE ai_visualization_jobs
    ADD COLUMN parent_job_id uuid NULL REFERENCES ai_visualization_jobs(id) ON DELETE SET NULL,
    ADD COLUMN root_job_id uuid NULL REFERENCES ai_visualization_jobs(id) ON DELETE SET NULL,
    ADD COLUMN is_shortlisted boolean NOT NULL DEFAULT false,
    ADD COLUMN is_studio_selected boolean NOT NULL DEFAULT false,
    ADD COLUMN concept_label varchar(100) NULL;

CREATE INDEX idx_ai_jobs_parent ON ai_visualization_jobs(parent_job_id);
CREATE INDEX idx_ai_jobs_root ON ai_visualization_jobs(studio_id, root_job_id);
CREATE INDEX idx_ai_jobs_shortlist ON ai_visualization_jobs(studio_id, is_shortlisted);

-- 2. Client Reviews Table
CREATE TABLE ai_client_reviews (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    project_id uuid NOT NULL,
    title varchar(150) NOT NULL,
    custom_message varchar(1000) NULL,
    token_hash bytea NOT NULL UNIQUE,
    status text NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'CLOSED', 'REVOKED')),
    include_original boolean NOT NULL DEFAULT false,
    expires_at timestamptz NOT NULL,
    current_approved_job_id uuid NULL REFERENCES ai_visualization_jobs(id) ON DELETE SET NULL,
    created_by uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_client_review_project FOREIGN KEY (project_id, studio_id)
        REFERENCES studio_projects(id, studio_id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_reviews_studio ON ai_client_reviews(studio_id);
CREATE INDEX idx_ai_reviews_project ON ai_client_reviews(studio_id, project_id);
CREATE INDEX idx_ai_reviews_status ON ai_client_reviews(studio_id, status);

-- 3. Client Review Sessions Table (Scoped, HttpOnly token exchange sessions)
CREATE TABLE ai_client_review_sessions (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    review_id uuid NOT NULL REFERENCES ai_client_reviews(id) ON DELETE CASCADE,
    session_token_hash bytea NOT NULL UNIQUE,
    csrf_token_hash bytea NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_review_sessions_review ON ai_client_review_sessions(review_id);
CREATE INDEX idx_ai_review_sessions_token ON ai_client_review_sessions(session_token_hash);

-- 4. Client Review Items Table (Explicitly shared concepts)
CREATE TABLE ai_client_review_items (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    review_id uuid NOT NULL REFERENCES ai_client_reviews(id) ON DELETE CASCADE,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    job_id uuid NOT NULL REFERENCES ai_visualization_jobs(id) ON DELETE CASCADE,
    media_id uuid NOT NULL,
    display_label varchar(100) NOT NULL,
    display_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_ai_review_item_media FOREIGN KEY (media_id, studio_id)
        REFERENCES media_assets(id, studio_id) ON DELETE CASCADE,
    CONSTRAINT uq_ai_review_item UNIQUE (review_id, job_id)
);

CREATE INDEX idx_ai_review_items_review ON ai_client_review_items(review_id);
CREATE INDEX idx_ai_review_items_studio ON ai_client_review_items(studio_id);

-- 5. Client Review Decisions Table (Immutable event-based approvals/change requests)
CREATE TABLE ai_client_review_decisions (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    review_id uuid NOT NULL REFERENCES ai_client_reviews(id) ON DELETE CASCADE,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    job_id uuid NOT NULL REFERENCES ai_visualization_jobs(id) ON DELETE CASCADE,
    decision text NOT NULL
        CHECK (decision IN ('APPROVED', 'CHANGES_REQUESTED', 'DECLINED')),
    client_name varchar(100) NULL,
    feedback varchar(1000) NULL,
    is_current boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_review_decisions_review ON ai_client_review_decisions(review_id);
CREATE INDEX idx_ai_review_decisions_current ON ai_client_review_decisions(review_id, is_current);
CREATE INDEX idx_ai_review_decisions_studio ON ai_client_review_decisions(studio_id);

-- 6. Client Review Comments Table (Targeted feedback thread)
CREATE TABLE ai_client_review_comments (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    review_id uuid NOT NULL REFERENCES ai_client_reviews(id) ON DELETE CASCADE,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    job_id uuid NULL REFERENCES ai_visualization_jobs(id) ON DELETE CASCADE,
    author_type text NOT NULL
        CHECK (author_type IN ('CLIENT', 'STUDIO')),
    author_name varchar(100) NOT NULL,
    comment_text varchar(1000) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_review_comments_review ON ai_client_review_comments(review_id);
CREATE INDEX idx_ai_review_comments_studio ON ai_client_review_comments(studio_id);

-- 7. Row-Level Security (RLS) for Tenant Isolation
ALTER TABLE ai_client_reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_client_reviews FORCE ROW LEVEL SECURITY;

ALTER TABLE ai_client_review_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_client_review_items FORCE ROW LEVEL SECURITY;

ALTER TABLE ai_client_review_decisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_client_review_decisions FORCE ROW LEVEL SECURITY;

ALTER TABLE ai_client_review_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_client_review_comments FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_ai_client_reviews ON ai_client_reviews;
CREATE POLICY tenant_isolation_ai_client_reviews ON ai_client_reviews
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

DROP POLICY IF EXISTS tenant_isolation_ai_client_review_items ON ai_client_review_items;
CREATE POLICY tenant_isolation_ai_client_review_items ON ai_client_review_items
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

DROP POLICY IF EXISTS tenant_isolation_ai_client_review_decisions ON ai_client_review_decisions;
CREATE POLICY tenant_isolation_ai_client_review_decisions ON ai_client_review_decisions
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

DROP POLICY IF EXISTS tenant_isolation_ai_client_review_comments ON ai_client_review_comments;
CREATE POLICY tenant_isolation_ai_client_review_comments ON ai_client_review_comments
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );
