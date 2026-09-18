-- ============================================================================
-- Phase 08.1 — Studio Specialties Taxonomy & Onboarding Invariant Closure (H2 Test)
-- ============================================================================

-- 1. Studio Specialties (Normalized canonical design / aesthetic taxonomy mapping)
CREATE TABLE studio_specialties (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    specialty_code text NOT NULL,
    specialty_name text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_studio_specialty UNIQUE (studio_id, specialty_code)
);

CREATE INDEX idx_studio_specialties_studio ON studio_specialties(studio_id);
CREATE INDEX idx_studio_specialties_code ON studio_specialties(specialty_code);

-- 2. Canonical Initial Onboarding Completion Record (Idempotency & Concurrency Authority)
CREATE TABLE designer_onboarding_completions (
    user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE CASCADE,
    completed_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_onboarding_completions_studio ON designer_onboarding_completions(studio_id);

-- 3. Slug Claim Format Invariant (H2 compatible)
ALTER TABLE studio_slug_claims
    ADD CONSTRAINT chk_slug_format CHECK (REGEXP_LIKE(slug, '^[a-z0-9]+(-[a-z0-9]+)*$'));
