-- ============================================================================
-- Phase 25 — Search & Discovery Engine Indexes & Policies (PostgreSQL 18)
-- ============================================================================

-- 1. Enable Trigram Extension for typo-tolerant fuzzy matching
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. Composite partial index for public-eligible project discovery
CREATE INDEX IF NOT EXISTS idx_studio_projects_discovery_pub ON studio_projects (
    project_status,
    visibility_status,
    category_code,
    city
) WHERE archived_at IS NULL;

-- 3. GIN Trigram index on project title for typo tolerance
CREATE INDEX IF NOT EXISTS idx_studio_projects_title_trgm ON studio_projects USING gin (title gin_trgm_ops);

-- 4. GIN Trigram index on studio name for typo-tolerant professional search
CREATE INDEX IF NOT EXISTS idx_designer_studios_name_trgm ON designer_studios USING gin (name gin_trgm_ops);

-- 5. Studio publication and discovery filter index
CREATE INDEX IF NOT EXISTS idx_designer_studios_discovery_pub ON designer_studios (
    publication_status,
    status,
    professional_type,
    city
);

-- 6. GIN Full-Text Search index for projects (title + description + city)
CREATE INDEX IF NOT EXISTS idx_studio_projects_fts ON studio_projects USING gin (
    to_tsvector('english', title || ' ' || coalesce(short_description, '') || ' ' || coalesce(city, ''))
);

-- 7. GIN Full-Text Search index for designer studios (name + tagline + city)
CREATE INDEX IF NOT EXISTS idx_designer_studios_fts ON designer_studios USING gin (
    to_tsvector('english', name || ' ' || coalesce(tagline, '') || ' ' || coalesce(city, ''))
);

-- 8. Row Level Security: Narrow SELECT policies for cross-tenant public discovery
DROP POLICY IF EXISTS public_read_studio_projects ON studio_projects;
CREATE POLICY public_read_studio_projects ON studio_projects
    FOR SELECT
    USING (
        project_status = 'READY'
        AND visibility_status = 'PORTFOLIO'
        AND archived_at IS NULL
    );

DROP POLICY IF EXISTS public_read_project_styles ON project_styles;
CREATE POLICY public_read_project_styles ON project_styles
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM studio_projects sp
            WHERE sp.id = project_styles.project_id
              AND sp.project_status = 'READY'
              AND sp.visibility_status = 'PORTFOLIO'
              AND sp.archived_at IS NULL
        )
    );

DROP POLICY IF EXISTS public_read_media_derivatives ON media_derivatives;
CREATE POLICY public_read_media_derivatives ON media_derivatives
    FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM media_assets ma
            JOIN studio_projects sp ON sp.id = ma.project_id
            WHERE ma.id = media_derivatives.media_id
              AND ma.visibility = 'PUBLIC'
              AND ma.media_type IN ('REAL_PROJECT', 'AI_CONCEPT', 'STUDIO_BRANDING')
              AND sp.project_status = 'READY'
              AND sp.visibility_status = 'PORTFOLIO'
              AND sp.archived_at IS NULL
        )
    );
