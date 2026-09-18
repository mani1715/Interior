-- ============================================================================
-- Phase 08 — Professional / Studio Onboarding Schema (H2 Test DB)
-- ============================================================================

-- 1. Extend designer_studios with aggregate profile & publication lock attributes
ALTER TABLE designer_studios
    ADD COLUMN professional_type text NOT NULL DEFAULT 'INTERIOR_STUDIO'
        CHECK (professional_type IN (
            'INDIVIDUAL_DESIGNER',
            'INTERIOR_STUDIO',
            'ARCHITECT',
            'ARCHITECTURE_STUDIO',
            'CUSTOM_FURNITURE_STUDIO',
            'WOODWORK_CABINETRY_PROFESSIONAL',
            'TURNKEY_CONTRACTOR'
        ));

ALTER TABLE designer_studios ADD COLUMN professional_title text NULL;
ALTER TABLE designer_studios ADD COLUMN tagline text NULL;
ALTER TABLE designer_studios ADD COLUMN experience_since_year smallint NULL;
ALTER TABLE designer_studios ADD COLUMN team_size text NULL;
ALTER TABLE designer_studios ADD COLUMN budget_range text NULL;
ALTER TABLE designer_studios ADD COLUMN address_line text NULL;
ALTER TABLE designer_studios ADD COLUMN city text NULL;
ALTER TABLE designer_studios ADD COLUMN district text NULL;
ALTER TABLE designer_studios ADD COLUMN state text NULL;
ALTER TABLE designer_studios ADD COLUMN postal_code text NULL;
ALTER TABLE designer_studios ADD COLUMN country text NOT NULL DEFAULT 'IN';
ALTER TABLE designer_studios ADD COLUMN travel_available boolean NOT NULL DEFAULT false;
ALTER TABLE designer_studios ADD COLUMN gst_registered boolean NOT NULL DEFAULT false;
ALTER TABLE designer_studios ADD COLUMN gst_number text NULL;
ALTER TABLE designer_studios ADD COLUMN publication_status text NOT NULL DEFAULT 'UNPUBLISHED'
    CHECK (publication_status IN ('UNPUBLISHED', 'PUBLISHED', 'ARCHIVED'));
ALTER TABLE designer_studios ADD COLUMN onboarding_completed_at timestamptz NULL;

-- 2. Studio Slug Registry (Reserved, Current, Aliases)
CREATE TABLE studio_slug_claims (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    slug text NOT NULL UNIQUE,
    state text NOT NULL CHECK (state IN ('RESERVED', 'CURRENT', 'ALIAS', 'RETIRED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_reserved_null_studio CHECK (
        (state = 'RESERVED' AND studio_id IS NULL) OR
        (state IN ('CURRENT', 'ALIAS', 'RETIRED') AND studio_id IS NOT NULL)
    )
);

CREATE INDEX uq_studio_current_slug ON studio_slug_claims(studio_id);
CREATE INDEX idx_studio_slug_claims_slug ON studio_slug_claims(slug);

-- Pre-seed reserved URL namespaces
INSERT INTO studio_slug_claims (slug, state) VALUES
('admin', 'RESERVED'),
('api', 'RESERVED'),
('auth', 'RESERVED'),
('account', 'RESERVED'),
('projects', 'RESERVED'),
('professionals', 'RESERVED'),
('categories', 'RESERVED'),
('locations', 'RESERVED'),
('dashboard', 'RESERVED'),
('settings', 'RESERVED'),
('support', 'RESERVED'),
('design-system', 'RESERVED'),
('onboarding', 'RESERVED'),
('signin', 'RESERVED'),
('signup', 'RESERVED'),
('login', 'RESERVED'),
('logout', 'RESERVED'),
('privacy', 'RESERVED'),
('terms', 'RESERVED'),
('about', 'RESERVED'),
('contact', 'RESERVED'),
('help', 'RESERVED'),
('pricing', 'RESERVED'),
('billing', 'RESERVED'),
('media', 'RESERVED'),
('explore', 'RESERVED'),
('search', 'RESERVED');

-- 3. Studio Contacts (Normalized explicit business contact channels)
CREATE TABLE studio_contacts (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    kind text NOT NULL CHECK (kind IN ('EMAIL', 'PHONE', 'WHATSAPP', 'WEBSITE', 'INSTAGRAM', 'FACEBOOK', 'OTHER_SOCIAL')),
    contact_value text NOT NULL,
    public_consent boolean NOT NULL DEFAULT false,
    sort_order integer NOT NULL DEFAULT 0,
    CONSTRAINT uq_studio_contact UNIQUE (studio_id, kind, contact_value)
);

CREATE INDEX idx_studio_contacts_studio ON studio_contacts(studio_id);

-- 4. Studio Services (Normalized service taxonomy offerings)
CREATE TABLE studio_services (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    service_code text NOT NULL,
    service_name text NOT NULL,
    CONSTRAINT uq_studio_service UNIQUE (studio_id, service_code)
);

CREATE INDEX idx_studio_services_studio ON studio_services(studio_id);

-- 5. Studio Service Areas (Normalized served cities & localities)
CREATE TABLE studio_service_areas (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    city_name text NOT NULL,
    locality text NULL,
    CONSTRAINT uq_studio_service_area UNIQUE (studio_id, city_name)
);

CREATE INDEX idx_studio_service_areas_studio ON studio_service_areas(studio_id);

-- 6. Designer Onboarding Drafts (Resilient multi-step progress)
CREATE TABLE designer_onboarding_drafts (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    user_id uuid NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    step integer NOT NULL DEFAULT 1 CHECK (step >= 1 AND step <= 8),
    draft_payload text NOT NULL,
    status text NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'BLOCKED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_onboarding_drafts_user ON designer_onboarding_drafts(user_id);
