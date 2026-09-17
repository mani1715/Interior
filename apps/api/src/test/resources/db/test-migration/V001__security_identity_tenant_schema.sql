-- ============================================================================
-- H2 Test Database Migration (Schema Parity with PostgreSQL 18 V001)
-- ============================================================================

-- 1. Users Table
CREATE TABLE users (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    display_name text NOT NULL,
    email text UNIQUE,
    phone text,
    status text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACTIVE','SUSPENDED','DELETED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 2. Identity External Bindings (OIDC)
CREATE TABLE identity_external_identities (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    issuer text NOT NULL,
    subject text NOT NULL,
    linked_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_external_identity UNIQUE (issuer, subject)
);

-- 3. Platform Roles
CREATE TABLE identity_roles (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    code text NOT NULL UNIQUE CHECK (code IN ('CUSTOMER','DESIGNER','DESIGNER_TEAM','MODERATOR','ADMIN','SUPER_ADMIN')),
    name text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Seed Default Platform Roles
INSERT INTO identity_roles (code, name) VALUES
('CUSTOMER', 'Public Customer'),
('DESIGNER', 'Interior Designer Studio Owner'),
('DESIGNER_TEAM', 'Studio Team Member'),
('MODERATOR', 'Platform Content Moderator'),
('ADMIN', 'Platform Administrator'),
('SUPER_ADMIN', 'Platform Super Administrator');

-- 4. User Platform Roles
CREATE TABLE identity_user_roles (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role_id uuid NOT NULL REFERENCES identity_roles(id) ON DELETE RESTRICT,
    granted_at timestamptz NOT NULL DEFAULT now(),
    revoked_at timestamptz NULL,
    CONSTRAINT uq_user_role UNIQUE (user_id, role_id)
);

-- 5. Designer Studios (Tenants)
CREATE TABLE designer_studios (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    name text NOT NULL,
    slug text NOT NULL UNIQUE,
    owner_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('DRAFT','ACTIVE','SUSPENDED','DELETED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);

-- 6. Studio Memberships
CREATE TABLE studio_members (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NOT NULL REFERENCES designer_studios(id) ON DELETE RESTRICT,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role text NOT NULL CHECK (role IN ('OWNER','ADMIN','MEMBER')),
    granted_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_studio_member UNIQUE (studio_id, user_id)
);

-- 7. Application Sessions (Opaque SHA-256 Hashed Tokens)
CREATE TABLE identity_sessions (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    token_hash bytea NOT NULL UNIQUE,
    csrf_hash bytea NOT NULL,
    auth_time timestamptz NOT NULL DEFAULT now(),
    assurance text NOT NULL DEFAULT 'PASSWORD',
    last_seen_at timestamptz NOT NULL DEFAULT now(),
    idle_expires_at timestamptz NOT NULL,
    absolute_expires_at timestamptz NOT NULL,
    revoked_at timestamptz NULL,
    device_label text
);

-- 8. Audit Events
CREATE TABLE audit_events (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NULL REFERENCES designer_studios(id) ON DELETE RESTRICT,
    actor_id uuid NULL REFERENCES users(id) ON DELETE RESTRICT,
    action text NOT NULL,
    resource_type text NOT NULL,
    resource_id uuid NULL,
    request_id text NOT NULL,
    details text,
    timestamp timestamptz NOT NULL DEFAULT now()
);

-- 9. Transactional Outbox Events
CREATE TABLE outbox_events (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    studio_id uuid NULL REFERENCES designer_studios(id) ON DELETE RESTRICT,
    event_type text NOT NULL,
    payload text NOT NULL,
    status text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','DISPATCHED','FAILED')),
    created_at timestamptz NOT NULL DEFAULT now()
);
