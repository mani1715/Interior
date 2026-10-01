-- ============================================================================
-- Phase 29 — Admin + Production Hardening Migration (PostgreSQL 18)
-- ============================================================================

-- 1. Performance Indexes for Admin Queries & Audit Lookups
CREATE INDEX IF NOT EXISTS idx_audit_events_timestamp ON audit_events (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_action_time ON audit_events (action, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_users_status_created ON users (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_designer_studios_status_created ON designer_studios (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_studio_verifications_status_created ON studio_verifications (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_studio_reviews_status_submitted ON studio_reviews (status, submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_review_reports_status_created ON review_reports (status, created_at DESC);

-- 2. Public Read Hardening for Reviews (Studio active state & publication status gate)
DROP POLICY IF EXISTS public_read_studio_reviews ON studio_reviews;
CREATE POLICY public_read_studio_reviews ON studio_reviews
    FOR SELECT
    USING (
        status = 'PUBLISHED'
        AND EXISTS (
            SELECT 1 FROM designer_studios s
            WHERE s.id = studio_reviews.studio_id
              AND s.status = 'ACTIVE'
              AND s.publication_status = 'PUBLISHED'
        )
    );
