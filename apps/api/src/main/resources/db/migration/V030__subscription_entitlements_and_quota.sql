-- ============================================================================
-- Phase 30 — Subscription Entitlements & Capacity Quotas (Phase 5)
-- ============================================================================
-- 1. Seed customer-facing plans: STANDARD, PREMIUM, PRO
--    - purchasable = false, price = 0 (no live checkout yet, prices unapproved)
-- 2. Seed entitlements for STANDARD, PREMIUM, PRO
--    - STANDARD: 10 projects, 15 photos/project, STANDARD only, 0 cinematic
--    - PREMIUM:  20 projects, 25 photos/project, STANDARD only, 0 cinematic
--    - PRO:      20 projects, 30 photos/project, STANDARD + CINEMATIC, 5 cinematic
-- 3. Update legacy BASE plan:
--    - Explicit non-null numeric entitlements for PHOTO & CINEMATIC limit
-- 4. Extend media_assets with is_portfolio_enrolled boolean
--    - Defaults to true for REAL_PROJECT, BEFORE, AFTER
--    - Sets false for REFERENCE, CLIENT_PRIVATE, and AI_CONCEPT
-- 5. Indexes for fast quota evaluation and concurrency safety
-- ============================================================================

-- 1. Extend media_assets with is_portfolio_enrolled
ALTER TABLE media_assets
    ADD COLUMN is_portfolio_enrolled boolean NOT NULL DEFAULT true;

-- Update existing rows: REFERENCE, CLIENT_PRIVATE, and AI_CONCEPT default to unenrolled
UPDATE media_assets
    SET is_portfolio_enrolled = false
    WHERE media_type IN ('REFERENCE', 'CLIENT_PRIVATE', 'AI_CONCEPT');

-- Performance index for project portfolio photo count
CREATE INDEX idx_media_assets_portfolio_enrolled
    ON media_assets (studio_id, project_id, is_portfolio_enrolled)
    WHERE deleted_at IS NULL;

-- Performance index for pending upload intent reservations
CREATE INDEX idx_upload_intents_pending_quota
    ON upload_intents (studio_id, project_id, status, media_type)
    WHERE status = 'PENDING';

-- 2. Seed Customer-Facing Commercial Plans
-- STANDARD (Display order 1)
INSERT INTO billing_plans (
    id, code, name, description, billing_period, currency, price_minor, active, purchasable, display_order, provider_price_id, created_at, updated_at
) VALUES (
    '01923000-0000-7000-8000-000000000002',
    'STANDARD',
    'Standard',
    'Essential portfolio presence for independent interior designers and boutique studios.',
    'MONTHLY',
    'INR',
    0,
    true,
    false,
    1,
    NULL,
    now(),
    now()
);

-- PREMIUM (Display order 2)
INSERT INTO billing_plans (
    id, code, name, description, billing_period, currency, price_minor, active, purchasable, display_order, provider_price_id, created_at, updated_at
) VALUES (
    '01923000-0000-7000-8000-000000000003',
    'PREMIUM',
    'Premium',
    'Expanded project and media capacity for growing design studios.',
    'MONTHLY',
    'INR',
    0,
    true,
    false,
    2,
    NULL,
    now(),
    now()
);

-- PRO (Display order 3)
INSERT INTO billing_plans (
    id, code, name, description, billing_period, currency, price_minor, active, purchasable, display_order, provider_price_id, created_at, updated_at
) VALUES (
    '01923000-0000-7000-8000-000000000004',
    'PRO',
    'Pro',
    'Maximum capacity with cinematic project presentation for distinguished design practices.',
    'MONTHLY',
    'INR',
    0,
    true,
    false,
    3,
    NULL,
    now(),
    now()
);

-- 3. Seed Entitlements for BASE (Legacy snapshot additions)
INSERT INTO plan_entitlements (id, plan_id, entitlement_key, value_type, boolean_value, numeric_value, created_at) VALUES
('01923000-0000-7000-8000-000000000017', '01923000-0000-7000-8000-000000000001', 'PROJECT_PHOTO_LIMIT', 'NUMERIC', NULL, NULL, now()),
('01923000-0000-7000-8000-000000000018', '01923000-0000-7000-8000-000000000001', 'CINEMATIC_PORTFOLIO', 'BOOLEAN', true, NULL, now()),
('01923000-0000-7000-8000-000000000019', '01923000-0000-7000-8000-000000000001', 'CINEMATIC_PROJECT_LIMIT', 'NUMERIC', NULL, NULL, now());

-- 4. Seed Entitlements for STANDARD
INSERT INTO plan_entitlements (id, plan_id, entitlement_key, value_type, boolean_value, numeric_value, created_at) VALUES
('01923000-0000-7000-8000-000000000020', '01923000-0000-7000-8000-000000000002', 'PORTFOLIO_PUBLISH', 'BOOLEAN', true, NULL, now()),
('01923000-0000-7000-8000-000000000021', '01923000-0000-7000-8000-000000000002', 'PROJECT_LIMIT', 'NUMERIC', NULL, 10, now()),
('01923000-0000-7000-8000-000000000022', '01923000-0000-7000-8000-000000000002', 'PROJECT_PHOTO_LIMIT', 'NUMERIC', NULL, 15, now()),
('01923000-0000-7000-8000-000000000023', '01923000-0000-7000-8000-000000000002', 'CINEMATIC_PORTFOLIO', 'BOOLEAN', false, NULL, now()),
('01923000-0000-7000-8000-000000000024', '01923000-0000-7000-8000-000000000002', 'CINEMATIC_PROJECT_LIMIT', 'NUMERIC', NULL, 0, now()),
('01923000-0000-7000-8000-000000000025', '01923000-0000-7000-8000-000000000002', 'STORAGE_LIMIT_BYTES', 'NUMERIC', NULL, 5368709120, now()), -- 5 GB
('01923000-0000-7000-8000-000000000026', '01923000-0000-7000-8000-000000000002', 'AI_MONTHLY_CREDITS', 'NUMERIC', NULL, 20, now()),
('01923000-0000-7000-8000-000000000027', '01923000-0000-7000-8000-000000000002', 'LEADS_CRM', 'BOOLEAN', true, NULL, now()),
('01923000-0000-7000-8000-000000000028', '01923000-0000-7000-8000-000000000002', 'ANALYTICS_BASIC', 'BOOLEAN', true, NULL, now()),
('01923000-0000-7000-8000-000000000029', '01923000-0000-7000-8000-000000000002', 'ANALYTICS_ADVANCED', 'BOOLEAN', false, NULL, now());

-- 5. Seed Entitlements for PREMIUM
INSERT INTO plan_entitlements (id, plan_id, entitlement_key, value_type, boolean_value, numeric_value, created_at) VALUES
('01923000-0000-7000-8000-000000000030', '01923000-0000-7000-8000-000000000003', 'PORTFOLIO_PUBLISH', 'BOOLEAN', true, NULL, now()),
('01923000-0000-7000-8000-000000000031', '01923000-0000-7000-8000-000000000003', 'PROJECT_LIMIT', 'NUMERIC', NULL, 20, now()),
('01923000-0000-7000-8000-000000000032', '01923000-0000-7000-8000-000000000003', 'PROJECT_PHOTO_LIMIT', 'NUMERIC', NULL, 25, now()),
('01923000-0000-7000-8000-000000000033', '01923000-0000-7000-8000-000000000003', 'CINEMATIC_PORTFOLIO', 'BOOLEAN', false, NULL, now()),
('01923000-0000-7000-8000-000000000034', '01923000-0000-7000-8000-000000000003', 'CINEMATIC_PROJECT_LIMIT', 'NUMERIC', NULL, 0, now()),
('01923000-0000-7000-8000-000000000035', '01923000-0000-7000-8000-000000000003', 'STORAGE_LIMIT_BYTES', 'NUMERIC', NULL, 21474836480, now()), -- 20 GB
('01923000-0000-7000-8000-000000000036', '01923000-0000-7000-8000-000000000003', 'AI_MONTHLY_CREDITS', 'NUMERIC', NULL, 50, now()),
('01923000-0000-7000-8000-000000000037', '01923000-0000-7000-8000-000000000003', 'LEADS_CRM', 'BOOLEAN', true, NULL, now()),
('01923000-0000-7000-8000-000000000038', '01923000-0000-7000-8000-000000000003', 'ANALYTICS_BASIC', 'BOOLEAN', true, NULL, now()),
('01923000-0000-7000-8000-000000000039', '01923000-0000-7000-8000-000000000003', 'ANALYTICS_ADVANCED', 'BOOLEAN', true, NULL, now());

-- 6. Seed Entitlements for PRO
INSERT INTO plan_entitlements (id, plan_id, entitlement_key, value_type, boolean_value, numeric_value, created_at) VALUES
('01923000-0000-7000-8000-000000000040', '01923000-0000-7000-8000-000000000004', 'PORTFOLIO_PUBLISH', 'BOOLEAN', true, NULL, now()),
('01923000-0000-7000-8000-000000000041', '01923000-0000-7000-8000-000000000004', 'PROJECT_LIMIT', 'NUMERIC', NULL, 20, now()),
('01923000-0000-7000-8000-000000000042', '01923000-0000-7000-8000-000000000004', 'PROJECT_PHOTO_LIMIT', 'NUMERIC', NULL, 30, now()),
('01923000-0000-7000-8000-000000000043', '01923000-0000-7000-8000-000000000004', 'CINEMATIC_PORTFOLIO', 'BOOLEAN', true, NULL, now()),
('01923000-0000-7000-8000-000000000044', '01923000-0000-7000-8000-000000000004', 'CINEMATIC_PROJECT_LIMIT', 'NUMERIC', NULL, 5, now()),
('01923000-0000-7000-8000-000000000045', '01923000-0000-7000-8000-000000000004', 'STORAGE_LIMIT_BYTES', 'NUMERIC', NULL, 53687091200, now()), -- 50 GB
('01923000-0000-7000-8000-000000000046', '01923000-0000-7000-8000-000000000004', 'AI_MONTHLY_CREDITS', 'NUMERIC', NULL, 150, now()),
('01923000-0000-7000-8000-000000000047', '01923000-0000-7000-8000-000000000004', 'LEADS_CRM', 'BOOLEAN', true, NULL, now()),
('01923000-0000-7000-8000-000000000048', '01923000-0000-7000-8000-000000000004', 'ANALYTICS_BASIC', 'BOOLEAN', true, NULL, now()),
('01923000-0000-7000-8000-000000000049', '01923000-0000-7000-8000-000000000004', 'ANALYTICS_ADVANCED', 'BOOLEAN', true, NULL, now());
