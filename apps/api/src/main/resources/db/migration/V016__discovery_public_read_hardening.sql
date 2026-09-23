-- ============================================================================
-- Phase 25.2 — Discovery Public Read RLS & Privacy Hardening (PostgreSQL 18)
-- ============================================================================

-- 1. DROP old permissive Phase 25.1 policies to ensure proper policy composition
DROP POLICY IF EXISTS public_read_studio_projects ON studio_projects;
DROP POLICY IF EXISTS public_read_project_styles ON project_styles;
DROP POLICY IF EXISTS public_read_media_derivatives ON media_derivatives;
DROP POLICY IF EXISTS public_read_media_assets ON media_assets;
DROP POLICY IF EXISTS public_read_designer_studios ON designer_studios;
DROP POLICY IF EXISTS tenant_isolation_designer_studios ON designer_studios;

-- 2. Designer Studios: Enable & Force RLS, establish tenant isolation & public-read gates
ALTER TABLE designer_studios ENABLE ROW LEVEL SECURITY;
ALTER TABLE designer_studios FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_designer_studios ON designer_studios
    FOR ALL
    USING (
        id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

CREATE POLICY public_read_designer_studios ON designer_studios
    FOR SELECT
    USING (
        publication_status = 'PUBLISHED'
        AND status = 'ACTIVE'
    );

-- 3. Studio Projects: Tightened correlated RLS public read policy
-- A project is discoverable ONLY if status = 'READY', visibility = 'PORTFOLIO',
-- not archived, AND its parent studio is PUBLISHED + ACTIVE.
CREATE POLICY public_read_studio_projects ON studio_projects
    FOR SELECT
    USING (
        project_status = 'READY'
        AND visibility_status = 'PORTFOLIO'
        AND archived_at IS NULL
        AND EXISTS (
            SELECT 1 FROM designer_studios s
            WHERE s.id = studio_projects.studio_id
              AND s.publication_status = 'PUBLISHED'
              AND s.status = 'ACTIVE'
        )
    );

-- 4. Project Styles: Scoped to public-eligible projects
CREATE POLICY public_read_project_styles ON project_styles
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM studio_projects sp
            JOIN designer_studios s ON s.id = sp.studio_id
            WHERE sp.id = project_styles.project_id
              AND sp.project_status = 'READY'
              AND sp.visibility_status = 'PORTFOLIO'
              AND sp.archived_at IS NULL
              AND s.publication_status = 'PUBLISHED'
              AND s.status = 'ACTIVE'
        )
    );

-- 5. Media Assets: Public-read gate enforcing ownership chain and privacy
ALTER TABLE media_assets ENABLE ROW LEVEL SECURITY;
ALTER TABLE media_assets FORCE ROW LEVEL SECURITY;

CREATE POLICY public_read_media_assets ON media_assets
    FOR SELECT
    USING (
        deleted_at IS NULL
        AND visibility != 'PRIVATE'
        AND media_type IN ('REAL_PROJECT', 'BEFORE', 'AFTER', 'AI_CONCEPT')
        AND EXISTS (
            SELECT 1 FROM studio_projects sp
            JOIN designer_studios s ON s.id = sp.studio_id
            WHERE sp.id = media_assets.project_id
              AND sp.project_status = 'READY'
              AND sp.visibility_status = 'PORTFOLIO'
              AND sp.archived_at IS NULL
              AND s.publication_status = 'PUBLISHED'
              AND s.status = 'ACTIVE'
        )
    );

-- 6. Media Derivatives: Public-read gate enforcing entire ownership chain
-- Derivative -> Media Asset -> Project -> Studio
CREATE POLICY public_read_media_derivatives ON media_derivatives
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM media_assets ma
            JOIN studio_projects sp ON sp.id = ma.project_id
            JOIN designer_studios s ON s.id = sp.studio_id
            WHERE ma.id = media_derivatives.media_id
              AND ma.deleted_at IS NULL
              AND ma.visibility != 'PRIVATE'
              AND ma.media_type IN ('REAL_PROJECT', 'BEFORE', 'AFTER', 'AI_CONCEPT')
              AND sp.project_status = 'READY'
              AND sp.visibility_status = 'PORTFOLIO'
              AND sp.archived_at IS NULL
              AND s.publication_status = 'PUBLISHED'
              AND s.status = 'ACTIVE'
        )
    );

-- 7. Studio Child Tables: Enable & Force RLS with public read gates for published studios
-- A. studio_services
ALTER TABLE studio_services ENABLE ROW LEVEL SECURITY;
ALTER TABLE studio_services FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_studio_services ON studio_services;
CREATE POLICY tenant_isolation_studio_services ON studio_services
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

DROP POLICY IF EXISTS public_read_studio_services ON studio_services;
CREATE POLICY public_read_studio_services ON studio_services
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM designer_studios s
            WHERE s.id = studio_services.studio_id
              AND s.publication_status = 'PUBLISHED'
              AND s.status = 'ACTIVE'
        )
    );

-- B. studio_specialties
ALTER TABLE studio_specialties ENABLE ROW LEVEL SECURITY;
ALTER TABLE studio_specialties FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_studio_specialties ON studio_specialties;
CREATE POLICY tenant_isolation_studio_specialties ON studio_specialties
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

DROP POLICY IF EXISTS public_read_studio_specialties ON studio_specialties;
CREATE POLICY public_read_studio_specialties ON studio_specialties
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM designer_studios s
            WHERE s.id = studio_specialties.studio_id
              AND s.publication_status = 'PUBLISHED'
              AND s.status = 'ACTIVE'
        )
    );

-- C. studio_service_areas
ALTER TABLE studio_service_areas ENABLE ROW LEVEL SECURITY;
ALTER TABLE studio_service_areas FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_studio_service_areas ON studio_service_areas;
CREATE POLICY tenant_isolation_studio_service_areas ON studio_service_areas
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

DROP POLICY IF EXISTS public_read_studio_service_areas ON studio_service_areas;
CREATE POLICY public_read_studio_service_areas ON studio_service_areas
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM designer_studios s
            WHERE s.id = studio_service_areas.studio_id
              AND s.publication_status = 'PUBLISHED'
              AND s.status = 'ACTIVE'
        )
    );

-- D. studio_contacts (Consent-gated)
ALTER TABLE studio_contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE studio_contacts FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_studio_contacts ON studio_contacts;
CREATE POLICY tenant_isolation_studio_contacts ON studio_contacts
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

DROP POLICY IF EXISTS public_read_studio_contacts ON studio_contacts;
CREATE POLICY public_read_studio_contacts ON studio_contacts
    FOR SELECT
    USING (
        public_consent = true
        AND EXISTS (
            SELECT 1 FROM designer_studios s
            WHERE s.id = studio_contacts.studio_id
              AND s.publication_status = 'PUBLISHED'
              AND s.status = 'ACTIVE'
        )
    );

-- E. studio_seo_settings
ALTER TABLE studio_seo_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE studio_seo_settings FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS public_read_studio_seo_settings ON studio_seo_settings;
CREATE POLICY public_read_studio_seo_settings ON studio_seo_settings
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM designer_studios s
            WHERE s.id = studio_seo_settings.studio_id
              AND s.publication_status = 'PUBLISHED'
              AND s.status = 'ACTIVE'
        )
    );

-- 8. FTS Strategy: Replace aggressive English stemming with 'simple' configuration
-- Preserves proper nouns, Indian city names, designer brands, and interior terminology
DROP INDEX IF EXISTS idx_studio_projects_fts;
CREATE INDEX idx_studio_projects_fts ON studio_projects USING gin (
    to_tsvector('simple', title || ' ' || coalesce(short_description, '') || ' ' || coalesce(city, ''))
);

DROP INDEX IF EXISTS idx_designer_studios_fts;
CREATE INDEX idx_designer_studios_fts ON designer_studios USING gin (
    to_tsvector('simple', name || ' ' || coalesce(tagline, '') || ' ' || coalesce(city, ''))
);
