package com.interior.platform.media.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Opt-in live cloud integration test for Cloudflare R2 / S3-compatible object storage.
 * <p>
 * This test is skipped by default during normal builds. It is explicitly executed during
 * Phase 12A.2 when real Cloudflare R2 credentials are provided via environment variables
 * or system properties.
 * <p>
 * Isolation & Safety Guard:
 * All test objects are stored under {@code integration-tests/<run-id>/} and cleaned up in {@code finally}.
 * Never modifies or deletes any data outside this prefix.
 * Never logs credentials or secrets.
 */
@EnabledIfSystemProperty(named = "r2.live.test", matches = "true")
class R2LiveIntegrationTest {

    @Test
    @DisplayName("Verify Cloudflare R2 / S3 6-operation contract on live cloud endpoint")
    void testLiveR2StorageContract() {
        String endpoint = getEnvOrProp("S3_ENDPOINT");
        String accessKey = getEnvOrProp("S3_ACCESS_KEY_ID");
        String secretKey = getEnvOrProp("S3_SECRET_ACCESS_KEY");
        String privateBucket = getEnvOrProp("S3_BUCKET_PRIVATE");
        String regionStr = getEnvOrProp("S3_REGION", "auto");

        assertThat(endpoint)
                .as("S3_ENDPOINT must be set for live R2 integration test")
                .isNotBlank();
        assertThat(accessKey)
                .as("S3_ACCESS_KEY_ID must be set")
                .isNotBlank();
        assertThat(secretKey)
                .as("S3_SECRET_ACCESS_KEY must be set")
                .isNotBlank();
        assertThat(privateBucket)
                .as("S3_BUCKET_PRIVATE must be set")
                .isNotBlank();

        Region region = (regionStr == null || regionStr.isBlank() || regionStr.equalsIgnoreCase("auto"))
                ? Region.of("auto")
                : Region.of(regionStr);

        try (S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true)
                .build()) {

            String runId = UUID.randomUUID().toString();
            String isolatedPrefix = "integration-tests/" + runId + "/";
            String testKey1 = isolatedPrefix + "live-contract-test.txt";
            String testKey2 = isolatedPrefix + "live-contract-copy.txt";
            byte[] payload = ("Elégance R2 Live Test Payload: " + runId).getBytes(StandardCharsets.UTF_8);

            try {
                // 1. PutObject
                PutObjectRequest putReq = PutObjectRequest.builder()
                        .bucket(privateBucket)
                        .key(testKey1)
                        .contentType("text/plain; charset=utf-8")
                        .build();
                s3Client.putObject(putReq, RequestBody.fromBytes(payload));

                // 2. HeadObject
                HeadObjectRequest headReq = HeadObjectRequest.builder()
                        .bucket(privateBucket)
                        .key(testKey1)
                        .build();
                HeadObjectResponse headRes = s3Client.headObject(headReq);
                assertThat(headRes.contentLength()).isEqualTo(payload.length);
                assertThat(headRes.contentType()).contains("text/plain");

                // 3. GetObject
                GetObjectRequest getReq = GetObjectRequest.builder()
                        .bucket(privateBucket)
                        .key(testKey1)
                        .build();
                ResponseBytes<GetObjectResponse> getRes = s3Client.getObjectAsBytes(getReq);
                assertThat(getRes.asByteArray()).isEqualTo(payload);

                // 4. CopyObject
                CopyObjectRequest copyReq = CopyObjectRequest.builder()
                        .sourceBucket(privateBucket)
                        .sourceKey(testKey1)
                        .destinationBucket(privateBucket)
                        .destinationKey(testKey2)
                        .build();
                s3Client.copyObject(copyReq);

                // Verify copy via HeadObject
                HeadObjectResponse headCopyRes = s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(privateBucket)
                        .key(testKey2)
                        .build());
                assertThat(headCopyRes.contentLength()).isEqualTo(payload.length);

                // 5. DeleteObject
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(privateBucket)
                        .key(testKey1)
                        .build());
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(privateBucket)
                        .key(testKey2)
                        .build());

                // 6. Verify deletion (NoSuchKeyException)
                assertThatThrownBy(() -> s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(privateBucket)
                        .key(testKey1)
                        .build()))
                        .isInstanceOf(NoSuchKeyException.class);

            } finally {
                // Safety cleanup
                safeDelete(s3Client, privateBucket, testKey1);
                safeDelete(s3Client, privateBucket, testKey2);
            }
        }
    }

    private void safeDelete(S3Client s3Client, String bucket, String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception ignored) {
        }
    }

    private String getEnvOrProp(String name) {
        return getEnvOrProp(name, null);
    }

    private String getEnvOrProp(String name, String defaultValue) {
        String val = System.getProperty(name);
        if (val != null && !val.isBlank()) {
            return val;
        }
        val = System.getenv(name);
        if (val != null && !val.isBlank()) {
            return val;
        }
        return defaultValue;
    }
}
