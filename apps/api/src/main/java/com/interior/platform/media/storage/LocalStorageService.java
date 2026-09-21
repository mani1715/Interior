package com.interior.platform.media.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path storageBasePath;
    private final String publicBaseUrl;
    // In-memory fallback if disk is not used
    private final ConcurrentHashMap<String, byte[]> memoryStore = new ConcurrentHashMap<>();

    public LocalStorageService(
            @Value("${interior.storage.base-path:target/storage}") String basePath,
            @Value("${interior.storage.public-base-url:/api/v1/media/public}") String publicBaseUrl
    ) {
        this.storageBasePath = Paths.get(basePath).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl;
        try {
            Files.createDirectories(this.storageBasePath);
        } catch (IOException e) {
            log.warn("Could not create local storage directory at {}. In-memory fallback will be used.", storageBasePath, e);
        }
    }

    private String getExtension(String contentType) {
        if (contentType == null) return "bin";
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }

    @Override
    public String generateQuarantineKey(UUID studioId, UUID uploadIntentId, UUID mediaAssetId, String contentType) {
        return "pending/" + studioId + "/" + uploadIntentId + "/" + mediaAssetId + "." + getExtension(contentType);
    }

    @Override
    public String generateCanonicalOriginalKey(UUID studioId, UUID projectId, UUID mediaAssetId, String contentType) {
        return "studio/" + studioId + "/projects/" + projectId + "/original/" + mediaAssetId + "." + getExtension(contentType);
    }

    @Override
    public String generateDerivativeKey(UUID studioId, UUID projectId, UUID mediaAssetId, String variant, String format) {
        return "public/studio/" + studioId + "/projects/" + projectId + "/derivatives/" + variant.toLowerCase() + "_" + mediaAssetId + "." + format;
    }

    @Override
    public String generateUploadUrl(UUID uploadIntentId, String quarantineKey) {
        return "/api/v1/media/upload/" + uploadIntentId;
    }

    @Override
    public String resolvePublicUrl(String storageKey) {
        return publicBaseUrl + "/" + storageKey;
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        memoryStore.put(key, content);
        try {
            Path target = storageBasePath.resolve(key).normalize();
            if (target.startsWith(storageBasePath)) {
                Files.createDirectories(target.getParent());
                Files.write(target, content);
            }
        } catch (IOException e) {
            log.warn("Failed to write to local storage path for key {}, keeping in memory store", key, e);
        }
    }

    @Override
    public byte[] load(String key) {
        byte[] fromMemory = memoryStore.get(key);
        if (fromMemory != null) {
            return fromMemory;
        }
        try {
            Path target = storageBasePath.resolve(key).normalize();
            if (target.startsWith(storageBasePath) && Files.exists(target)) {
                return Files.readAllBytes(target);
            }
        } catch (IOException e) {
            log.warn("Failed to read from local storage path for key {}", key, e);
        }
        return null;
    }

    @Override
    public boolean exists(String key) {
        if (memoryStore.containsKey(key)) {
            return true;
        }
        Path target = storageBasePath.resolve(key).normalize();
        return target.startsWith(storageBasePath) && Files.exists(target);
    }

    @Override
    public void move(String sourceKey, String destinationKey) {
        byte[] content = load(sourceKey);
        if (content != null) {
            store(destinationKey, content, null);
            delete(sourceKey);
        } else {
            throw new IllegalArgumentException("Source key not found in storage: " + sourceKey);
        }
    }

    @Override
    public void delete(String key) {
        memoryStore.remove(key);
        try {
            Path target = storageBasePath.resolve(key).normalize();
            if (target.startsWith(storageBasePath) && Files.exists(target)) {
                Files.delete(target);
            }
        } catch (IOException e) {
            log.warn("Failed to delete local storage path for key {}", key, e);
        }
    }
}
