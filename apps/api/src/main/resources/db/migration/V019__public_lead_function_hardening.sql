-- ============================================================================
-- Phase 26.2 — Security Definer + Public Lead Write Hardening (PostgreSQL 18)
-- ============================================================================

-- 1. Create dedicated non-superuser role for submit_public_lead execution
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'lead_ingest_role') THEN
        CREATE ROLE lead_ingest_role WITH NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOINHERIT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'test_rls_public_user') THEN
        CREATE ROLE test_rls_public_user WITH LOGIN PASSWORD 'test_pass' NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
    END IF;
END
$$;

-- 2. Revoke CREATE on schema public from PUBLIC to prevent search-path shadowing
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO lead_ingest_role;
GRANT USAGE ON SCHEMA public TO test_rls_public_user;

-- 3. Grants for lead_ingest_role (strictly minimum required for function execution)
GRANT SELECT ON public.designer_studios, public.studio_projects TO lead_ingest_role;
GRANT SELECT, INSERT ON public.studio_leads, public.lead_activities TO lead_ingest_role;

-- 4. Revoke direct table privileges from PUBLIC and test_rls_public_user
REVOKE ALL ON public.studio_leads FROM PUBLIC, test_rls_public_user;
REVOKE ALL ON public.lead_activities FROM PUBLIC, test_rls_public_user;
REVOKE ALL ON public.lead_notes FROM PUBLIC, test_rls_public_user;
REVOKE ALL ON public.lead_whatsapp_messages FROM PUBLIC, test_rls_public_user;

-- 5. Harden RLS policies on studio_leads and lead_activities for lead_ingest_role
DROP POLICY IF EXISTS public_insert_studio_leads ON studio_leads;
DROP POLICY IF EXISTS public_insert_lead_activities ON lead_activities;
DROP POLICY IF EXISTS ingest_select_studio_leads ON studio_leads;
DROP POLICY IF EXISTS ingest_insert_studio_leads ON studio_leads;
DROP POLICY IF EXISTS ingest_insert_lead_activities ON lead_activities;

-- Ingest policies strictly for lead_ingest_role
CREATE POLICY ingest_select_studio_leads ON studio_leads
    FOR SELECT
    TO lead_ingest_role
    USING (true);

CREATE POLICY ingest_insert_studio_leads ON studio_leads
    FOR INSERT
    TO lead_ingest_role
    WITH CHECK (
        status = 'NEW'
        AND assigned_user_id IS NULL
        AND archived_at IS NULL
        AND source IN ('PROJECT_DISCOVERY', 'PROFESSIONAL_DISCOVERY', 'PUBLIC_PROJECT', 'PUBLIC_PORTFOLIO', 'DIRECT_INQUIRY', 'WHATSAPP_HANDOFF')
    );

CREATE POLICY ingest_insert_lead_activities ON lead_activities
    FOR INSERT
    TO lead_ingest_role
    WITH CHECK (
        actor_id IS NULL
        AND activity_type IN ('LEAD_CREATED', 'WHATSAPP_HANDOFF_OPENED')
    );

-- 6. Fully schema-qualified, hardened SECURITY DEFINER function with fixed search_path
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
SET search_path = pg_catalog, public
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
    SELECT s.id, s.name, s.slug INTO v_studio
    FROM public.designer_studios s
    WHERE s.slug = trim(lower(p_studio_slug))
      AND s.publication_status = 'PUBLISHED'
      AND s.status = 'ACTIVE';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'This professional is not currently accepting inquiries.' USING ERRCODE = 'P0001';
    END IF;

    -- 2. Validate Target Project (if supplied: must belong to studio, READY, PORTFOLIO, unarchived)
    IF p_project_slug IS NOT NULL AND trim(p_project_slug) <> '' THEN
        SELECT sp.id INTO v_project_id
        FROM public.studio_projects sp
        WHERE sp.studio_id = v_studio.id
          AND sp.slug = trim(p_project_slug)
          AND sp.project_status = 'READY'
          AND sp.visibility_status = 'PORTFOLIO'
          AND sp.archived_at IS NULL;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'This professional is not currently accepting inquiries.' USING ERRCODE = 'P0001';
        END IF;

        v_source := 'PUBLIC_PROJECT';
    ELSE
        v_source := 'PUBLIC_PORTFOLIO';
    END IF;

    -- 3. Idempotency Check
    IF p_idempotency_key IS NOT NULL AND trim(p_idempotency_key) <> '' THEN
        SELECT sl.id INTO v_existing_id
        FROM public.studio_leads sl
        WHERE sl.studio_id = v_studio.id
          AND sl.idempotency_key = trim(p_idempotency_key);

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

    -- 4. Insert Lead Record (server-owned status, null assignment, no archive)
    INSERT INTO public.studio_leads (
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

    -- 5. Insert Immutable Activity Audit (strictly LEAD_CREATED, actor_id NULL)
    INSERT INTO public.lead_activities (
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

-- 7. Change owner to dedicated non-superuser role
ALTER FUNCTION public.submit_public_lead OWNER TO lead_ingest_role;

-- 8. Revoke all execution from PUBLIC and grant only to authorized roles
REVOKE ALL ON FUNCTION public.submit_public_lead FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.submit_public_lead TO lead_ingest_role, test_rls_public_user, CURRENT_USER;
