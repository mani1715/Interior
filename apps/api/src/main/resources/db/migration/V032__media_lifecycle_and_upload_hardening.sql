-- ============================================================================
-- Phase 7D — Media Upload Lifecycle, Storage Quotas & Failure Recovery (V032)
-- ============================================================================

-- 1. Expand upload_intents status check constraint to include CANCELLED
ALTER TABLE upload_intents DROP CONSTRAINT IF EXISTS upload_intents_status_check;
ALTER TABLE upload_intents ADD CONSTRAINT upload_intents_status_check
    CHECK (status IN ('PENDING', 'COMMITTED', 'EXPIRED', 'FAILED', 'CANCELLED'));

-- 2. Add media_asset_id link on upload_intents for commit idempotency
ALTER TABLE upload_intents ADD COLUMN IF NOT EXISTS media_asset_id uuid NULL;

-- 3. Add upload_intent_id link on media_assets for traceability
ALTER TABLE media_assets ADD COLUMN IF NOT EXISTS upload_intent_id uuid NULL;

-- 4. Foreign keys and indexes for fast quota and intent resolution
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_upload_intents_media_asset'
    ) THEN
        ALTER TABLE upload_intents
            ADD CONSTRAINT fk_upload_intents_media_asset
            FOREIGN KEY (media_asset_id, studio_id)
            REFERENCES media_assets(id, studio_id)
            ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_media_assets_upload_intent'
    ) THEN
        ALTER TABLE media_assets
            ADD CONSTRAINT fk_media_assets_upload_intent
            FOREIGN KEY (upload_intent_id)
            REFERENCES upload_intents(id)
            ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_upload_intents_media_asset ON upload_intents (media_asset_id);
CREATE INDEX IF NOT EXISTS idx_media_assets_upload_intent ON media_assets (upload_intent_id);

-- Performance index for storage quota calculations
CREATE INDEX IF NOT EXISTS idx_media_assets_storage_quota
    ON media_assets (studio_id, file_size)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_upload_intents_storage_quota
    ON upload_intents (studio_id, expected_size_bytes, status, expires_at)
    WHERE status = 'PENDING';
