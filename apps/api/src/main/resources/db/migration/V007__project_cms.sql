-- ============================================================================
-- Phase 18 — Project CMS Schema (PostgreSQL)
-- ============================================================================

-- 1. Studio Projects Table
CREATE TABLE studio_projects (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    slug text NOT NULL,
    title text NOT NULL,
    short_description text NULL,
    full_description text NULL,
    category_code text NOT NULL
        CHECK (category_code IN (
            'LIVING_ROOM', 'TV_UNIT', 'BEDROOM', 'WARDROBE', 'MODULAR_KITCHEN',
            'POOJA_UNIT', 'CROCKERY_UNIT', 'STUDY_UNIT', 'FALSE_CEILING',
            'WALL_PANELS', 'SHOE_RACK', 'OFFICE', 'COMMERCIAL',
            'CUSTOM_FURNITURE', 'COMPLETE_HOME_INTERIOR'
        )),
    project_status text NOT NULL DEFAULT 'DRAFT'
        CHECK (project_status IN ('DRAFT', 'READY', 'ARCHIVED')),
    visibility_status text NOT NULL DEFAULT 'PRIVATE'
        CHECK (visibility_status IN ('PRIVATE', 'PORTFOLIO')),
    featured boolean NOT NULL DEFAULT false,
    display_order integer NOT NULL DEFAULT 0 CHECK (display_order >= 0),
    city text NULL,
    district text NULL,
    state text NULL,
    country text NOT NULL DEFAULT 'IN',
    property_type text NULL
        CHECK (property_type IS NULL OR property_type IN (
            'APARTMENT', 'INDEPENDENT_HOUSE', 'VILLA', 'OFFICE',
            'RETAIL', 'RESTAURANT', 'COMMERCIAL', 'OTHER'
        )),
    project_scope text NULL
        CHECK (project_scope IS NULL OR project_scope IN (
            'FULL_INTERIOR', 'PARTIAL_INTERIOR', 'SINGLE_ROOM',
            'CUSTOM_FURNITURE', 'WOODWORK', 'RENOVATION',
            'ARCHITECTURAL', 'TURNKEY', 'OTHER'
        )),
    completion_year integer NULL
        CHECK (completion_year IS NULL OR (completion_year >= 1990 AND completion_year <= 2100)),
    budget_visibility text NOT NULL DEFAULT 'HIDDEN'
        CHECK (budget_visibility IN ('HIDDEN', 'RANGE', 'STARTING_FROM')),
    budget_min numeric(12,2) NULL CHECK (budget_min IS NULL OR budget_min >= 0),
    budget_max numeric(12,2) NULL CHECK (budget_max IS NULL OR budget_max >= 0),
    currency text NOT NULL DEFAULT 'INR',
    client_name_visibility text NOT NULL DEFAULT 'HIDDEN'
        CHECK (client_name_visibility IN ('HIDDEN', 'DISPLAY')),
    client_display_name text NULL,
    area_value numeric(10,2) NULL CHECK (area_value IS NULL OR area_value >= 0),
    area_unit text NULL
        CHECK (area_unit IS NULL OR area_unit IN ('SQ_FT', 'SQ_M')),
    internal_notes text NULL,
    version bigint NOT NULL DEFAULT 1 CHECK (version >= 1),
    created_by uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    archived_at timestamptz NULL,
    CONSTRAINT uq_studio_projects_id_studio UNIQUE (id, studio_id),
    CONSTRAINT uq_studio_projects_studio_slug UNIQUE (studio_id, slug)
);

-- 2. Project Styles Join Table
CREATE TABLE project_styles (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    project_id uuid NOT NULL,
    studio_id uuid NOT NULL,
    style_code text NOT NULL
        CHECK (style_code IN (
            'MODERN_MINIMALIST', 'WARM_CONTEMPORARY', 'INDIAN_TRADITIONAL',
            'NEO_CLASSICAL', 'SCANDINAVIAN', 'INDUSTRIAL',
            'LUXURY_ECLECTIC', 'BIOPHILIC'
        )),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_project_styles_project FOREIGN KEY (project_id, studio_id)
        REFERENCES studio_projects(id, studio_id) ON DELETE CASCADE,
    CONSTRAINT fk_project_styles_studio FOREIGN KEY (studio_id)
        REFERENCES designer_studios(id) ON DELETE CASCADE,
    CONSTRAINT uq_project_styles_code UNIQUE (project_id, style_code)
);

-- Indexes
CREATE INDEX idx_studio_projects_studio ON studio_projects(studio_id);
CREATE INDEX idx_studio_projects_status ON studio_projects(studio_id, project_status);
CREATE INDEX idx_studio_projects_visibility ON studio_projects(studio_id, visibility_status);
CREATE INDEX idx_studio_projects_featured ON studio_projects(studio_id, featured);
CREATE INDEX idx_studio_projects_order ON studio_projects(studio_id, display_order);
CREATE INDEX idx_project_styles_project ON project_styles(project_id);
CREATE INDEX idx_project_styles_code ON project_styles(style_code);

-- Row Level Security (RLS) Defense-in-Depth for PostgreSQL
ALTER TABLE studio_projects ENABLE ROW LEVEL SECURITY;
ALTER TABLE studio_projects FORCE ROW LEVEL SECURITY;

ALTER TABLE project_styles ENABLE ROW LEVEL SECURITY;
ALTER TABLE project_styles FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_studio_projects ON studio_projects;
CREATE POLICY tenant_isolation_studio_projects ON studio_projects
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

DROP POLICY IF EXISTS tenant_isolation_project_styles ON project_styles;
CREATE POLICY tenant_isolation_project_styles ON project_styles
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );
