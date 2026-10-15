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
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Phase 12A.1 — S3/R2 Storage Contract Tests (6-Operation Set)")
class S3StorageContractTest {

    private S3StorageProperties properties;
    private S3Client mockS3Client;
    private S3Presigner mockPresigner;
    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new S3StorageProperties();
        properties.setProvider("S3_COMPATIBLE");
        properties.setPublicBaseUrl("https://media.elegance.interior");

        var s3 = properties.getS3();
        s3.setEndpoint("https://test-account.r2.cloudflarestorage.com");
        s3.setRegion("auto");
        s3.setAccessKeyId("test-access-key-id");
        s3.setSecretAccessKey("test-secret-access-key");
        s3.setPrivateBucket("test-private-media-bucket");
        s3.setPublicBucket("test-public-media-bucket");
        s3.setPathStyleAccess(true);

        mockS3Client = mock(S3Client.class);
        mockPresigner = mock(S3Presigner.class);

        storageService = new S3StorageService(properties, mockS3Client, mockPresigner);
    }

    @Test
    @DisplayName("Operation 1: Presigned PUT generates bounded 30-minute upload URL targeted to private bucket")
    void contract_presignedPutObject() throws Exception {
        UUID uploadIntentId = UUID.randomUUID();
        String quarantineKey = "pending/studio-1/intent-1/photo.jpg";

        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://test-account.r2.cloudflarestorage.com/test-private-media-bucket/" + quarantineKey + "?X-Amz-Signature=mock123"));
        when(mockPresigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        String url = storageService.generateUploadUrl(uploadIntentId, quarantineKey);

        assertNotNull(url);
        assertTrue(url.contains("X-Amz-Signature=mock123"));

        ArgumentCaptor<PutObjectPresignRequest> presignCaptor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(mockPresigner).presignPutObject(presignCaptor.capture());

        PutObjectPresignRequest captured = presignCaptor.getValue();
        assertEquals("test-private-media-bucket", captured.putObjectRequest().bucket());
        assertEquals(quarantineKey, captured.putObjectRequest().key());
        assertEquals(Duration.ofMinutes(30), captured.signatureDuration());
    }

    @Test
    @DisplayName("Operation 2: PutObject stores content with proper bucket, contentType, and public cache-control")
    void contract_putObject() {
        byte[] payload = "image binary data".getBytes(StandardCharsets.UTF_8);

        // 1. Private storage
        storageService.store("studio/s1/projects/p1/original/photo.jpg", payload, "image/jpeg");
        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(mockS3Client).putObject(putCaptor.capture(), any(RequestBody.class));

        assertEquals("test-private-media-bucket", putCaptor.getValue().bucket());
        assertEquals("studio/s1/projects/p1/original/photo.jpg", putCaptor.getValue().key());
        assertEquals("image/jpeg", putCaptor.getValue().contentType());
        assertNull(putCaptor.getValue().cacheControl()); // No public caching on private originals

        // 2. Public storage
        reset(mockS3Client);
        storageService.store("public/studio/s1/projects/p1/derivatives/card.webp", payload, "image/webp");
        verify(mockS3Client).putObject(putCaptor.capture(), any(RequestBody.class));

        assertEquals("test-public-media-bucket", putCaptor.getValue().bucket());
        assertEquals("public/studio/s1/projects/p1/derivatives/card.webp", putCaptor.getValue().key());
        assertEquals("image/webp", putCaptor.getValue().contentType());
        assertEquals("public, max-age=31536000, immutable", putCaptor.getValue().cacheControl());
    }

    @Test
    @DisplayName("Operation 3: GetObject loads bytes when present, returns null when NoSuchKeyException")
    void contract_getObject() {
        byte[] expected = "photo bytes".getBytes(StandardCharsets.UTF_8);

        // Success case
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(), expected
        );
        when(mockS3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        byte[] actual = storageService.load("studio/s1/original/img.jpg");
        assertArrayEquals(expected, actual);

        // Missing key case
        when(mockS3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("The specified key does not exist.").build());

        byte[] missing = storageService.load("studio/s1/original/missing.jpg");
        assertNull(missing);
    }

    @Test
    @DisplayName("Operation 4: HeadObject checks object existence (true when exists, false on 404/NoSuchKey)")
    void contract_headObject() {
        // Exists
        when(mockS3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());
        assertTrue(storageService.exists("studio/s1/original/img.jpg"));

        // Missing with NoSuchKeyException
        when(mockS3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Not Found").build());
        assertFalse(storageService.exists("studio/s1/original/notfound.jpg"));

        // Missing with S3Exception 404
        when(mockS3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow((S3Exception) S3Exception.builder().statusCode(404).message("Key Not Found").build());
        assertFalse(storageService.exists("studio/s1/original/notfound-404.jpg"));
    }

    @Test
    @DisplayName("Operation 5: CopyObject copies quarantine object to canonical destination and deletes quarantine")
    void contract_copyObject_and_move() {
        String source = "pending/s1/intent-1/asset.jpg";
        String dest = "studio/s1/projects/p1/original/asset.jpg";

        storageService.move(source, dest);

        ArgumentCaptor<CopyObjectRequest> copyCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(mockS3Client).copyObject(copyCaptor.capture());
        assertEquals("test-private-media-bucket", copyCaptor.getValue().sourceBucket());
        assertEquals(source, copyCaptor.getValue().sourceKey());
        assertEquals("test-private-media-bucket", copyCaptor.getValue().destinationBucket());
        assertEquals(dest, copyCaptor.getValue().destinationKey());

        ArgumentCaptor<DeleteObjectRequest> delCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(mockS3Client).deleteObject(delCaptor.capture());
        assertEquals("test-private-media-bucket", delCaptor.getValue().bucket());
        assertEquals(source, delCaptor.getValue().key());
    }

    @Test
    @DisplayName("Operation 6: DeleteObject issues deletion to correct bucket (idempotent)")
    void contract_deleteObject() {
        storageService.delete("studio/s1/original/file.jpg");

        ArgumentCaptor<DeleteObjectRequest> delCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(mockS3Client).deleteObject(delCaptor.capture());
        assertEquals("test-private-media-bucket", delCaptor.getValue().bucket());
        assertEquals("studio/s1/original/file.jpg", delCaptor.getValue().key());
    }

    @Test
    @DisplayName("Public URL resolution prepends configured CDN base URL cleanly")
    void contract_resolvePublicUrl() {
        String derivativeKey = "public/studio/s1/projects/p1/derivatives/hero.webp";
        String publicUrl = storageService.resolvePublicUrl(derivativeKey);
        assertEquals("https://media.elegance.interior/public/studio/s1/projects/p1/derivatives/hero.webp", publicUrl);
    }

    @Test
    @DisplayName("Unconfigured client throws IllegalStateException on store/load and returns safe defaults")
    void contract_unconfiguredHandling() {
        S3StorageService unconfigured = new S3StorageService(properties, null, null);
        assertFalse(unconfigured.isConfigured());
        assertFalse(unconfigured.exists("any/key"));

        assertThrows(IllegalStateException.class, () -> unconfigured.store("key", new byte[] { 1 }, "image/jpeg"));
        assertThrows(IllegalStateException.class, () -> unconfigured.load("key"));
        assertThrows(IllegalStateException.class, () -> unconfigured.move("src", "dst"));
        assertDoesNotThrow(() -> unconfigured.delete("key"));
    }
}
