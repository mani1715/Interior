package com.interior.platform.media.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "S3_COMPATIBLE")
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3StorageProperties properties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3StorageService(S3StorageProperties properties) {
        this.properties = properties;

        var s3Props = properties.getS3();
        if (s3Props.getEndpoint() == null || s3Props.getEndpoint().isBlank() ||
                s3Props.getAccessKeyId() == null || s3Props.getAccessKeyId().isBlank() ||
                s3Props.getSecretAccessKey() == null || s3Props.getSecretAccessKey().isBlank()) {
            log.warn("S3/R2 credentials not fully configured. Initializing unconfigured S3StorageService.");
            this.s3Client = null;
            this.s3Presigner = null;
            return;
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                s3Props.getAccessKeyId(),
                s3Props.getSecretAccessKey()
        );

        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(s3Props.isPathStyleAccess())
                .build();

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(s3Props.getEndpoint()))
                .region(Region.of(s3Props.getRegion() != null ? s3Props.getRegion() : "auto"))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Config)
                .build();

        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(s3Props.getEndpoint()))
                .region(Region.of(s3Props.getRegion() != null ? s3Props.getRegion() : "auto"))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Config)
                .build();

        log.info("Initialized S3/R2 StorageService connected to endpoint: {}, privateBucket: {}, publicBucket: {}",
                s3Props.getEndpoint(), s3Props.getPrivateBucket(), s3Props.getPublicBucket());
    }

    public S3StorageService(S3StorageProperties properties, S3Client s3Client, S3Presigner s3Presigner) {
        this.properties = properties;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    private String getBucketForKey(String key) {
        if (key != null && key.startsWith("public/")) {
            return properties.getS3().getPublicBucket();
        }
        return properties.getS3().getPrivateBucket();
    }

    private String getExtension(String contentType) {
        if (contentType == null) return "bin";
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "application/pdf" -> "pdf";
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
        if (s3Presigner == null) {
            return "/api/v1/media/upload/" + uploadIntentId;
        }

        try {
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(properties.getS3().getPrivateBucket())
                    .key(quarantineKey)
                    .build();

            PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(30))
                    .putObjectRequest(putReq)
                    .build();

            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignReq);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL for quarantine key {}", quarantineKey, e);
            return "/api/v1/media/upload/" + uploadIntentId;
        }
    }

    @Override
    public String resolvePublicUrl(String storageKey) {
        String base = properties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (storageKey.startsWith("/")) {
            return base + storageKey;
        }
        return base + "/" + storageKey;
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        if (s3Client == null) {
            throw new IllegalStateException("S3/R2 client is not configured for store operation");
        }
        String bucket = getBucketForKey(key);
        PutObjectRequest.Builder builder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key);
        if (contentType != null) {
            builder.contentType(contentType);
        }
        if (key.startsWith("public/")) {
            builder.cacheControl("public, max-age=31536000, immutable");
        }

        s3Client.putObject(builder.build(), RequestBody.fromBytes(content));
    }

    @Override
    public byte[] load(String key) {
        if (s3Client == null) {
            throw new IllegalStateException("S3/R2 client is not configured for load operation");
        }
        String bucket = getBucketForKey(key);
        try {
            GetObjectRequest req = GetObjectRequest.builder().bucket(bucket).key(key).build();
            return s3Client.getObjectAsBytes(req).asByteArray();
        } catch (NoSuchKeyException e) {
            log.debug("Object with key {} not found in bucket {}", key, bucket);
            return null;
        }
    }

    @Override
    public boolean exists(String key) {
        if (s3Client == null) {
            return false;
        }
        String bucket = getBucketForKey(key);
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }

    @Override
    public void move(String sourceKey, String destinationKey) {
        if (s3Client == null) {
            throw new IllegalStateException("S3/R2 client is not configured for move operation");
        }
        String sourceBucket = getBucketForKey(sourceKey);
        String destBucket = getBucketForKey(destinationKey);

        CopyObjectRequest copyReq = CopyObjectRequest.builder()
                .sourceBucket(sourceBucket)
                .sourceKey(sourceKey)
                .destinationBucket(destBucket)
                .destinationKey(destinationKey)
                .build();
        s3Client.copyObject(copyReq);

        DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                .bucket(sourceBucket)
                .key(sourceKey)
                .build();
        s3Client.deleteObject(delReq);
    }

    @Override
    public void delete(String key) {
        if (s3Client == null) {
            return;
        }
        String bucket = getBucketForKey(key);
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public boolean isConfigured() {
        return s3Client != null;
    }
}
