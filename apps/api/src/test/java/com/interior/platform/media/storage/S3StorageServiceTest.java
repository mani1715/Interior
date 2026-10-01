package com.interior.platform.media.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3StorageServiceTest {

    private S3StorageProperties properties;
    private S3Client mockS3Client;
    private S3Presigner mockPresigner;
    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new S3StorageProperties();
        properties.setProvider("S3_COMPATIBLE");
        properties.setPublicBaseUrl("https://media.interior.com");

        var s3 = properties.getS3();
        s3.setEndpoint("https://test.r2.cloudflarestorage.com");
        s3.setRegion("auto");
        s3.setAccessKeyId("test-access-key");
        s3.setSecretAccessKey("test-secret-key");
        s3.setPrivateBucket("interior-platform-media-private");
        s3.setPublicBucket("interior-platform-media-public");

        mockS3Client = mock(S3Client.class);
        mockPresigner = mock(S3Presigner.class);

        storageService = new S3StorageService(properties, mockS3Client, mockPresigner);
    }

    @Test
    @DisplayName("Keys follow canonical naming conventions and tenant isolation boundaries")
    void canonicalKeyGeneration() {
        UUID studioId = UUID.fromString("018d3b84-1234-7000-8000-000000000001");
        UUID uploadIntentId = UUID.fromString("018d3b84-1234-7000-8000-000000000002");
        UUID projectId = UUID.fromString("018d3b84-1234-7000-8000-000000000003");
        UUID assetId = UUID.fromString("018d3b84-1234-7000-8000-000000000004");

        String qKey = storageService.generateQuarantineKey(studioId, uploadIntentId, assetId, "image/jpeg");
        assertEquals("pending/018d3b84-1234-7000-8000-000000000001/018d3b84-1234-7000-8000-000000000002/018d3b84-1234-7000-8000-000000000004.jpg", qKey);

        String oKey = storageService.generateCanonicalOriginalKey(studioId, projectId, assetId, "image/png");
        assertEquals("studio/018d3b84-1234-7000-8000-000000000001/projects/018d3b84-1234-7000-8000-000000000003/original/018d3b84-1234-7000-8000-000000000004.png", oKey);

        String dKey = storageService.generateDerivativeKey(studioId, projectId, assetId, "CARD_WEBP", "webp");
        assertEquals("public/studio/018d3b84-1234-7000-8000-000000000001/projects/018d3b84-1234-7000-8000-000000000003/derivatives/card_webp_018d3b84-1234-7000-8000-000000000004.webp", dKey);
    }

    @Test
    @DisplayName("Generates direct presigned upload URL to private quarantine bucket")
    void presignedUploadUrl() throws Exception {
        UUID uploadIntentId = UUID.randomUUID();
        String quarantineKey = "pending/test/key.jpg";

        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://test.r2.cloudflarestorage.com/interior-platform-media-private/pending/test/key.jpg?signature=xyz"));
        when(mockPresigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        String url = storageService.generateUploadUrl(uploadIntentId, quarantineKey);
        assertTrue(url.contains("signature=xyz"));

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(mockPresigner).presignPutObject(captor.capture());
        assertEquals("interior-platform-media-private", captor.getValue().putObjectRequest().bucket());
        assertEquals(quarantineKey, captor.getValue().putObjectRequest().key());
    }

    @Test
    @DisplayName("Strict bucket routing: private keys route to private bucket, public keys route to public bucket")
    void strictBucketRouting() {
        byte[] content = "test content".getBytes(StandardCharsets.UTF_8);

        // 1. Private original upload
        storageService.store("studio/s1/projects/p1/original/a1.jpg", content, "image/jpeg");
        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockS3Client).putObject(putCaptor.capture(), any(RequestBody.class));
        assertEquals("interior-platform-media-private", putCaptor.getValue().bucket());
        assertNull(putCaptor.getValue().cacheControl());

        // 2. Public derivative upload
        reset(mockS3Client);
        storageService.store("public/studio/s1/projects/p1/derivatives/thumb.webp", content, "image/webp");
        verify(mockS3Client).putObject(putCaptor.capture(), any(RequestBody.class));
        assertEquals("interior-platform-media-public", putCaptor.getValue().bucket());
        assertEquals("public, max-age=31536000, immutable", putCaptor.getValue().cacheControl());
    }

    @Test
    @DisplayName("Resolve public URL prepends CDN base URL correctly")
    void resolvePublicUrl() {
        String derivativeKey = "public/studio/s1/projects/p1/derivatives/hero.webp";
        String publicUrl = storageService.resolvePublicUrl(derivativeKey);
        assertEquals("https://media.interior.com/public/studio/s1/projects/p1/derivatives/hero.webp", publicUrl);
    }

    @Test
    @DisplayName("Move copies to destination bucket and deletes from source bucket")
    void moveOperation() {
        String sourceKey = "pending/s1/u1/a1.jpg";
        String destKey = "studio/s1/projects/p1/original/a1.jpg";

        storageService.move(sourceKey, destKey);

        ArgumentCaptor<CopyObjectRequest> copyCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(mockS3Client).copyObject(copyCaptor.capture());
        assertEquals("interior-platform-media-private", copyCaptor.getValue().sourceBucket());
        assertEquals(sourceKey, copyCaptor.getValue().sourceKey());
        assertEquals("interior-platform-media-private", copyCaptor.getValue().destinationBucket());
        assertEquals(destKey, copyCaptor.getValue().destinationKey());

        ArgumentCaptor<DeleteObjectRequest> delCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(mockS3Client).deleteObject(delCaptor.capture());
        assertEquals("interior-platform-media-private", delCaptor.getValue().bucket());
        assertEquals(sourceKey, delCaptor.getValue().key());
    }
}
