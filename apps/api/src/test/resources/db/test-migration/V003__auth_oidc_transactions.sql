-- ============================================================================
-- Phase 07.1 — Durable OIDC Authentication Transactions (Test DB)
-- ============================================================================

CREATE TABLE auth_oidc_transactions (
    id uuid NOT NULL DEFAULT uuidv7() PRIMARY KEY,
    state text NOT NULL UNIQUE,
    nonce text NOT NULL,
    code_verifier text NOT NULL,
    provider_id text NOT NULL,
    return_url text NOT NULL,
    intent_role text,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz NULL
);

CREATE INDEX idx_auth_oidc_tx_state ON auth_oidc_transactions(state);
CREATE INDEX idx_auth_oidc_tx_expires ON auth_oidc_transactions(expires_at);
