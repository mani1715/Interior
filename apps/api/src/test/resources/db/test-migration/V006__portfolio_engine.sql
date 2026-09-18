-- ============================================================================
-- Phase 10 — Portfolio Builder Engine Schema (H2 Test Compatible)
-- ============================================================================

-- 1. Portfolios (Primary portfolio entity per studio tenant)
CREATE TABLE portfolios (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL UNIQUE REFERENCES designer_studios(id) ON DELETE CASCADE,
    template_key text NOT NULL DEFAULT 'BASIC'
        CHECK (template_key IN ('BASIC', 'MODERN', 'LUXURY', 'ARCHITECTURAL', 'WARM_NATURAL', 'DARK_CINEMATIC')),
    status text NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'READY', 'UNPUBLISHED')),
    headline text NULL,
    subheadline text NULL,
    bio text NULL,
    design_philosophy text NULL,
    years_of_experience integer NULL
        CHECK (years_of_experience IS NULL OR (years_of_experience >= 0 AND years_of_experience <= 100)),
    primary_color text NULL
        CHECK (primary_color IS NULL OR REGEXP_LIKE(primary_color, '^#[0-9a-fA-F]{6}$')),
    secondary_color text NULL
        CHECK (secondary_color IS NULL OR REGEXP_LIKE(secondary_color, '^#[0-9a-fA-F]{6}$')),
    accent_color text NULL
        CHECK (accent_color IS NULL OR REGEXP_LIKE(accent_color, '^#[0-9a-fA-F]{6}$')),
    font_pairing text NOT NULL DEFAULT 'SYSTEM_SANS'
        CHECK (font_pairing IN ('SYSTEM_SANS', 'CLASSIC_SERIF', 'MODERN_CLEAN', 'EDITORIAL', 'WARM_EDITORIAL', 'BOLD_CINEMATIC')),
    version bigint NOT NULL DEFAULT 1
        CHECK (version >= 1),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_portfolios_id_studio UNIQUE (id, studio_id)
);

-- 2. Portfolio Sections (Modular ordered presentation blocks)
CREATE TABLE portfolio_sections (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    portfolio_id uuid NOT NULL,
    studio_id uuid NOT NULL,
    section_type text NOT NULL
        CHECK (section_type IN (
            'HERO', 'ABOUT', 'SERVICES', 'FEATURED_PROJECTS', 'PROJECT_GRID',
            'BEFORE_AFTER', 'BEFORE_AI_REALITY', 'DESIGN_PROCESS', 'TESTIMONIALS',
            'TEAM', 'AWARDS', 'PRESS', 'SERVICE_AREAS', 'FAQ', 'CONTACT', 'CTA',
            'VIDEO', 'CUSTOM_NOTE'
        )),
    display_order integer NOT NULL CHECK (display_order >= 0),
    is_visible boolean NOT NULL DEFAULT true,
    schema_version integer NOT NULL DEFAULT 1 CHECK (schema_version >= 1),
    content text NOT NULL DEFAULT '{}',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_sections_portfolio FOREIGN KEY (portfolio_id, studio_id)
        REFERENCES portfolios(id, studio_id) ON DELETE CASCADE,
    CONSTRAINT fk_sections_studio FOREIGN KEY (studio_id)
        REFERENCES designer_studios(id) ON DELETE CASCADE,
    CONSTRAINT uq_section_order UNIQUE (portfolio_id, display_order)
);

CREATE INDEX idx_portfolio_sections_portfolio ON portfolio_sections(portfolio_id);
CREATE INDEX idx_portfolio_sections_studio ON portfolio_sections(studio_id);
CREATE INDEX idx_portfolio_sections_type ON portfolio_sections(portfolio_id, section_type);

-- 3. Portfolio Historical Version Snapshots
CREATE TABLE portfolio_versions (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    portfolio_id uuid NOT NULL,
    studio_id uuid NOT NULL,
    version_number integer NOT NULL CHECK (version_number >= 1),
    label text NOT NULL,
    snapshot_payload text NOT NULL,
    created_by uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_versions_portfolio FOREIGN KEY (portfolio_id, studio_id)
        REFERENCES portfolios(id, studio_id) ON DELETE CASCADE,
    CONSTRAINT fk_versions_studio FOREIGN KEY (studio_id)
        REFERENCES designer_studios(id) ON DELETE CASCADE,
    CONSTRAINT uq_portfolio_version UNIQUE (portfolio_id, version_number)
);

CREATE INDEX idx_portfolio_versions_portfolio ON portfolio_versions(portfolio_id, created_at DESC);
CREATE INDEX idx_portfolio_versions_studio ON portfolio_versions(studio_id);
