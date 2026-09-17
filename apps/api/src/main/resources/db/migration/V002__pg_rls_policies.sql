-- ============================================================================
-- Row Level Security (RLS) Defense-in-Depth for PostgreSQL 18 Tenant Tables
-- ============================================================================

ALTER TABLE studio_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE studio_members FORCE ROW LEVEL SECURITY;

ALTER TABLE audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_events FORCE ROW LEVEL SECURITY;

ALTER TABLE outbox_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE outbox_events FORCE ROW LEVEL SECURITY;

-- Tenant Isolation Policies
CREATE POLICY tenant_isolation_studio_members ON studio_members
    FOR ALL
    USING (
        studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

CREATE POLICY tenant_isolation_audit_events ON audit_events
    FOR ALL
    USING (
        studio_id IS NULL 
        OR studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );

CREATE POLICY tenant_isolation_outbox_events ON outbox_events
    FOR ALL
    USING (
        studio_id IS NULL 
        OR studio_id = NULLIF(current_setting('app.current_studio_id', true), '')::uuid
        OR current_setting('app.is_admin', true) = 'true'
    );
