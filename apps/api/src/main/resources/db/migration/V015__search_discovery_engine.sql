-- ============================================================================
-- Phase 25 — Search & Discovery Engine Indexes (PostgreSQL 18)
-- ============================================================================

-- 1. Enable Trigram Extension for typo-tolerant fuzzy matching
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. Composite partial index for public-eligible project discovery
CREATE INDEX idx_studio_projects_discovery_pub ON studio_projects (
    project_status,
    visibility_status,
    category_code,
    city
) WHERE archived_at IS NULL;

-- 3. GIN Trigram index on project title for typo tolerance
CREATE INDEX idx_studio_projects_title_trgm ON studio_projects USING gin (title gin_trgm_ops);

-- 4. GIN Trigram index on studio name for typo-tolerant professional search
CREATE INDEX idx_designer_studios_name_trgm ON designer_studios USING gin (name gin_trgm_ops);

-- 5. Studio publication and discovery filter index
CREATE INDEX idx_designer_studios_discovery_pub ON designer_studios (
    publication_status,
    status,
    professional_type,
    city
);
