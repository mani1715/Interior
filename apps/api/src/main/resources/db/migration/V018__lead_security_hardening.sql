-- ============================================================================
-- Phase 26.1 — Leads + PII + RLS + WhatsApp Security Hardening (PostgreSQL 18)
-- ============================================================================

-- 1. Ensure idempotency unique constraint on studio_leads
DROP INDEX IF EXISTS idx_studio_leads_idempotency;
CREATE UNIQUE INDEX IF NOT EXISTS uq_studio_leads_idempotency
    ON studio_leads (studio_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- 2. Add idempotency tracking to managed WhatsApp messages
ALTER TABLE lead_whatsapp_messages ADD COLUMN IF NOT EXISTS idempotency_key text NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_lead_whatsapp_msg_idempotency
    ON lead_whatsapp_messages (studio_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- 3. Harden RLS policies on studio_leads
-- Drop old policy
DROP POLICY IF EXISTS tenant_isolation_studio_leads ON studio_leads;
DROP POLICY IF EXISTS public_insert_studio_leads ON studio_leads;

-- Policy 3a: Studio member tenant isolation (SELECT, UPDATE, DELETE)
CREATE POLICY tenant_isolation_studio_leads ON studio_leads
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    )
    WITH CHECK (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

-- Policy 3b: Safe public lead insertion under forced RLS
-- Allows unauthenticated visitors to insert ONLY if:
--   - Target studio is PUBLISHED and ACTIVE
--   - If project_id is supplied, project belongs to studio, is READY, PORTFOLIO, and not archived
--   - status is forced to NEW
--   - assigned_user_id is NULL
--   - archived_at is NULL
--   - source is a recognized public attribution
CREATE POLICY public_insert_studio_leads ON studio_leads
    FOR INSERT
    WITH CHECK (
        status = 'NEW'
        AND assigned_user_id IS NULL
        AND archived_at IS NULL
        AND source IN ('PROJECT_DISCOVERY', 'PROFESSIONAL_DISCOVERY', 'PUBLIC_PROJECT', 'PUBLIC_PORTFOLIO', 'DIRECT_INQUIRY', 'WHATSAPP_HANDOFF')
        AND EXISTS (
            SELECT 1 FROM designer_studios s
            WHERE s.id = studio_leads.studio_id
              AND s.publication_status = 'PUBLISHED'
              AND s.status = 'ACTIVE'
        )
        AND (
            project_id IS NULL
            OR EXISTS (
                SELECT 1 FROM studio_projects sp
                WHERE sp.id = studio_leads.project_id
                  AND sp.studio_id = studio_leads.studio_id
                  AND sp.project_status = 'READY'
                  AND sp.visibility_status = 'PORTFOLIO'
                  AND sp.archived_at IS NULL
            )
        )
    );

-- 4. Harden RLS policies on lead_activities (Immutable Audit Log)
DROP POLICY IF EXISTS tenant_isolation_lead_activities ON lead_activities;
DROP POLICY IF EXISTS tenant_select_lead_activities ON lead_activities;
DROP POLICY IF EXISTS tenant_insert_lead_activities ON lead_activities;
DROP POLICY IF EXISTS public_insert_lead_activities ON lead_activities;
DROP POLICY IF EXISTS admin_delete_lead_activities ON lead_activities;

-- 4a: Studio members can SELECT audit activities
CREATE POLICY tenant_select_lead_activities ON lead_activities
    FOR SELECT
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

-- 4b: Studio members can INSERT audit activities
CREATE POLICY tenant_insert_lead_activities ON lead_activities
    FOR INSERT
    WITH CHECK (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

-- 4c: Public visitor lead creation can record initial activity
CREATE POLICY public_insert_lead_activities ON lead_activities
    FOR INSERT
    WITH CHECK (
        actor_id IS NULL
        AND activity_type IN ('LEAD_CREATED', 'WHATSAPP_HANDOFF_OPENED')
        AND EXISTS (
            SELECT 1 FROM studio_leads sl
            WHERE sl.id = lead_activities.lead_id
              AND sl.studio_id = lead_activities.studio_id
        )
    );

-- 4d: No UPDATE policy exists on lead_activities (guarantees immutability)
-- Only system admin can DELETE if ever needed
CREATE POLICY admin_delete_lead_activities ON lead_activities
    FOR DELETE
    USING (current_setting('app.is_admin', true) = 'true');

-- 5. Database function for safe public inquiry submission (alternative defense-in-depth)
CREATE OR REPLACE FUNCTION public.submit_public_lead(
    p_studio_slug text,
    p_project_slug text,
    p_name text,
    p_phone_normalized text,
    p_email_normalized text,
    p_city text,
    p_project_category text,
    p_budget_range text,
    p_message text,
    p_preferred_channel text,
    p_whatsapp_consent boolean,
    p_idempotency_key text,
    p_lead_id uuid DEFAULT uuidv7(),
    p_activity_id uuid DEFAULT uuidv7()
)
RETURNS TABLE (
    lead_id uuid,
    studio_id uuid,
    studio_name text,
    reference_number text
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_studio RECORD;
    v_project_id uuid := NULL;
    v_source text;
    v_now timestamptz := clock_timestamp();
    v_existing_id uuid;
    v_whatsapp_consent_at timestamptz := NULL;
BEGIN
    -- 1. Validate Target Studio (must be PUBLISHED + ACTIVE)
    SELECT id, name, slug INTO v_studio
    FROM designer_studios
    WHERE slug = trim(lower(p_studio_slug))
      AND publication_status = 'PUBLISHED'
      AND status = 'ACTIVE';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'This professional is not currently accepting inquiries.' USING ERRCODE = 'P0001';
    END IF;

    -- 2. Validate Target Project (if supplied: must belong to studio, READY, PORTFOLIO, unarchived)
    IF p_project_slug IS NOT NULL AND trim(p_project_slug) <> '' THEN
        SELECT id INTO v_project_id
        FROM studio_projects
        WHERE studio_id = v_studio.id
          AND slug = trim(p_project_slug)
          AND project_status = 'READY'
          AND visibility_status = 'PORTFOLIO'
          AND archived_at IS NULL;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'This professional is not currently accepting inquiries.' USING ERRCODE = 'P0001';
        END IF;

        v_source := 'PUBLIC_PROJECT';
    ELSE
        v_source := 'PUBLIC_PORTFOLIO';
    END IF;

    -- 3. Idempotency Check
    IF p_idempotency_key IS NOT NULL AND trim(p_idempotency_key) <> '' THEN
        SELECT id INTO v_existing_id
        FROM studio_leads
        WHERE studio_id = v_studio.id
          AND idempotency_key = trim(p_idempotency_key);

        IF FOUND THEN
            RETURN QUERY SELECT
                v_existing_id,
                v_studio.id,
                v_studio.name,
                ('INQ-' || upper(substring(v_existing_id::text, 1, 8)));
            RETURN;
        END IF;
    END IF;

    IF p_whatsapp_consent IS TRUE THEN
        v_whatsapp_consent_at := v_now;
    END IF;

    -- 4. Insert Lead Record
    INSERT INTO studio_leads (
        id, studio_id, project_id, source, status, name,
        phone_normalized, email_normalized, city, project_category,
        budget_range, message, preferred_contact_channel,
        contact_consent_at, whatsapp_consent_at, assigned_user_id,
        next_follow_up_at, lost_reason, possible_duplicate, idempotency_key,
        created_at, updated_at, version, archived_at
    ) VALUES (
        p_lead_id, v_studio.id, v_project_id, v_source, 'NEW', trim(p_name),
        p_phone_normalized, nullif(trim(lower(p_email_normalized)), ''), nullif(trim(p_city), ''), nullif(trim(p_project_category), ''),
        nullif(trim(p_budget_range), ''), trim(p_message), nullif(trim(upper(p_preferred_channel)), ''),
        v_now, v_whatsapp_consent_at, NULL,
        NULL, NULL, false, nullif(trim(p_idempotency_key), ''),
        v_now, v_now, 1, NULL
    );

    -- 5. Insert Immutable Activity Audit
    INSERT INTO lead_activities (
        id, lead_id, studio_id, actor_id, activity_type, details, created_at
    ) VALUES (
        p_activity_id, p_lead_id, v_studio.id, NULL, 'LEAD_CREATED',
        jsonb_build_object('source', v_source)::text, v_now
    );

    RETURN QUERY SELECT
        p_lead_id,
        v_studio.id,
        v_studio.name,
        ('INQ-' || upper(substring(p_lead_id::text, 1, 8)));
END;
$$;

-- Revoke default public execute and grant only to authenticated/current DB roles
REVOKE ALL ON FUNCTION public.submit_public_lead FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.submit_public_lead TO CURRENT_USER;
