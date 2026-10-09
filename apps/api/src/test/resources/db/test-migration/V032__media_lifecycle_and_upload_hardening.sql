-- ============================================================================
-- Phase 7D — Media Upload Lifecycle, Storage Quotas & Failure Recovery (Test H2)
-- ============================================================================

ALTER TABLE upload_intents ADD COLUMN IF NOT EXISTS media_asset_id uuid NULL;
ALTER TABLE media_assets ADD COLUMN IF NOT EXISTS upload_intent_id uuid NULL;

CREATE INDEX IF NOT EXISTS idx_upload_intents_media_asset ON upload_intents (media_asset_id);
CREATE INDEX IF NOT EXISTS idx_media_assets_upload_intent ON media_assets (upload_intent_id);
