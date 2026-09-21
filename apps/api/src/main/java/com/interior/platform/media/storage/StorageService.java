package com.interior.platform.media.storage;

import java.util.UUID;

public interface StorageService {
    String generateQuarantineKey(UUID studioId, UUID uploadIntentId, UUID mediaAssetId, String contentType);
    
    String generateCanonicalOriginalKey(UUID studioId, UUID projectId, UUID mediaAssetId, String contentType);
    
    String generateDerivativeKey(UUID studioId, UUID projectId, UUID mediaAssetId, String variant, String format);

    String generateUploadUrl(UUID uploadIntentId, String quarantineKey);

    String resolvePublicUrl(String storageKey);

    void store(String key, byte[] content, String contentType);

    byte[] load(String key);

    boolean exists(String key);

    void move(String sourceKey, String destinationKey);

    void delete(String key);
}
