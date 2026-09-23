-- ============================================================================
-- Phase 25 — Search & Discovery Engine Indexes (H2 Compatibility)
-- ============================================================================

CREATE INDEX idx_studio_projects_discovery_pub ON studio_projects (
    project_status,
    visibility_status,
    category_code,
    city
);

CREATE INDEX idx_designer_studios_discovery_pub ON designer_studios (
    publication_status,
    status,
    professional_type,
    city
);
