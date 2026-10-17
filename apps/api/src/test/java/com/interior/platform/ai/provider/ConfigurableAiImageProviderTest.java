package com.interior.platform.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.ai.domain.AiJobStatus;
import com.interior.platform.ai.domain.EditingMode;
import com.interior.platform.common.exception.AiProviderNotConfiguredException;
import com.interior.platform.common.service.SsrfProtectionService;
import com.interior.platform.common.util.UuidV7;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigurableAiImageProvider Stability Integration Tests")
class ConfigurableAiImageProviderTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<byte[]> httpResponse;

    private AiVisualizerProperties properties;
    private ConfigurableAiImageProvider provider;
    private byte[] sampleImageBytes;
    private byte[] sampleMaskBytes;

    @BeforeEach
    void setUp() throws IOException {
        properties = new AiVisualizerProperties();
        properties.setProvider("stability");
        properties.setApiKey("sk-mock-valid-key-for-test-contract-12345");
        properties.setEndpoint("https://api.stability.ai");
        properties.setModel("stability-v2beta");
        properties.setGrowMask(3);

        provider = new ConfigurableAiImageProvider(
                properties,
                new SsrfProtectionService(),
                httpClient,
                new ObjectMapper()
        );

        // Generate valid 100x100 JPEG sample image
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", baos);
        sampleImageBytes = baos.toByteArray();

        // Generate valid 100x100 PNG sample mask
        BufferedImage mask = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        mask.setRGB(50, 50, 0xFFFFFFFF);
        ByteArrayOutputStream maskBaos = new ByteArrayOutputStream();
        ImageIO.write(mask, "png", maskBaos);
        sampleMaskBytes = maskBaos.toByteArray();
    }

    private AiJobRecord createJob(EditingMode mode, String prompt) {
        return new AiJobRecord(
                UuidV7.randomUuid(),
                UuidV7.randomUuid(),
                UuidV7.randomUuid(),
                UuidV7.randomUuid(),
                null,
                "stability",
                null,
                prompt,
                null,
                AiJobStatus.PROCESSING,
                null,
                null,
                0,
                "test-idem-" + UUID.randomUUID(),
                UuidV7.randomUuid(),
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                0,
                true,
                mode,
                mode == EditingMode.PRECISION_MASK ? "studio/123/masks/mask.png" : null
        );
    }

    @Test
    @DisplayName("Quick Recolor calls /v2beta/stable-image/edit/search-and-recolor with multipart fields")
    void testQuickRecolorSuccess() throws Exception {
        AiJobRecord job = createJob(EditingMode.FULL_IMAGE, "Change the wardrobe to matte walnut brown finish");

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(sampleImageBytes);
        HttpHeaders headers = HttpHeaders.of(
                Map.of("Content-Type", List.of("image/png"), "seed", List.of("98765"), "finish-reason", List.of("SUCCESS")),
                (k, v) -> true
        );
        when(httpResponse.headers()).thenReturn(headers);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        ProviderGenerationResponse response = provider.submitGeneration(job, sampleImageBytes, "image/jpeg", null, null, List.of());

        assertTrue(response.isSuccess());
        assertNotNull(response.imageBytes());
        assertEquals("image/png", response.resultContentType());
        assertTrue(response.usageMetadata().contains("\"operation\":\"STABILITY_SEARCH_RECOLOR\""));
        assertTrue(response.usageMetadata().contains("\"credits\":5"));

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any());
        HttpRequest sent = requestCaptor.getValue();

        assertEquals("POST", sent.method());
        assertEquals("https://api.stability.ai/v2beta/stable-image/edit/search-and-recolor", sent.uri().toString());
        assertTrue(sent.headers().firstValue("Authorization").orElse("").startsWith("Bearer "));
        assertTrue(sent.headers().firstValue("Content-Type").orElse("").startsWith("multipart/form-data; boundary="));
        assertEquals("image/*", sent.headers().firstValue("Accept").orElse(""));
    }

    @Test
    @DisplayName("Precise Inpaint calls /v2beta/stable-image/edit/inpaint with binary mask")
    void testPreciseInpaintSuccess() throws Exception {
        AiJobRecord job = createJob(EditingMode.PRECISION_MASK, "Change only the masked cabinet doors to matte sage green");

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(sampleImageBytes);
        HttpHeaders headers = HttpHeaders.of(
                Map.of("Content-Type", List.of("image/png"), "seed", List.of("12345"), "finish-reason", List.of("SUCCESS")),
                (k, v) -> true
        );
        when(httpResponse.headers()).thenReturn(headers);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        ProviderGenerationResponse response = provider.submitGeneration(job, sampleImageBytes, "image/jpeg", sampleMaskBytes, "image/png", List.of());

        assertTrue(response.isSuccess());
        assertTrue(response.usageMetadata().contains("\"operation\":\"STABILITY_INPAINT\""));
        assertTrue(response.usageMetadata().contains("\"credits\":5"));

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any());
        HttpRequest sent = requestCaptor.getValue();

        assertEquals("https://api.stability.ai/v2beta/stable-image/edit/inpaint", sent.uri().toString());
    }

    @Test
    @DisplayName("Maps 400 Bad Request safely to AI_INVALID_REQUEST")
    void test400BadRequest() throws Exception {
        AiJobRecord job = createJob(EditingMode.FULL_IMAGE, "invalid prompt");

        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("{\"name\":\"bad_request\",\"message\":\"invalid dimensions\"}".getBytes(StandardCharsets.UTF_8));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        ProviderGenerationResponse response = provider.submitGeneration(job, sampleImageBytes, "image/jpeg", null, null, List.of());

        assertFalse(response.isSuccess());
        assertEquals("AI_INVALID_REQUEST", response.errorCode());
        assertFalse(response.isRetryable());
    }

    @Test
    @DisplayName("Maps 403 Safety/Moderation safely to AI_SAFETY_REJECTED")
    void test403Moderation() throws Exception {
        AiJobRecord job = createJob(EditingMode.FULL_IMAGE, "prompt with flagged keyword");

        when(httpResponse.statusCode()).thenReturn(403);
        when(httpResponse.body()).thenReturn("{\"name\":\"content_moderation\",\"message\":\"content_filtered safety trigger\"}".getBytes(StandardCharsets.UTF_8));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        ProviderGenerationResponse response = provider.submitGeneration(job, sampleImageBytes, "image/jpeg", null, null, List.of());

        assertFalse(response.isSuccess());
        assertEquals("AI_SAFETY_REJECTED", response.errorCode());
    }

    @Test
    @DisplayName("Maps 403 Authentication failure safely to AI_AUTH_FAILED")
    void test403AuthFailed() throws Exception {
        AiJobRecord job = createJob(EditingMode.FULL_IMAGE, "wardrobe in teak");

        when(httpResponse.statusCode()).thenReturn(403);
        when(httpResponse.body()).thenReturn("{\"name\":\"forbidden\",\"message\":\"invalid api key\"}".getBytes(StandardCharsets.UTF_8));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        ProviderGenerationResponse response = provider.submitGeneration(job, sampleImageBytes, "image/jpeg", null, null, List.of());

        assertFalse(response.isSuccess());
        assertEquals("AI_AUTH_FAILED", response.errorCode());
    }

    @Test
    @DisplayName("Maps 413 Payload Too Large safely to AI_PAYLOAD_TOO_LARGE")
    void test413PayloadTooLarge() throws Exception {
        AiJobRecord job = createJob(EditingMode.FULL_IMAGE, "wardrobe in oak");

        when(httpResponse.statusCode()).thenReturn(413);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        ProviderGenerationResponse response = provider.submitGeneration(job, sampleImageBytes, "image/jpeg", null, null, List.of());

        assertFalse(response.isSuccess());
        assertEquals("AI_PAYLOAD_TOO_LARGE", response.errorCode());
    }

    @Test
    @DisplayName("Maps 422 Unprocessable Entity safely to AI_UNPROCESSABLE_ENTITY")
    void test422Unprocessable() throws Exception {
        AiJobRecord job = createJob(EditingMode.FULL_IMAGE, "wardrobe in oak");

        when(httpResponse.statusCode()).thenReturn(422);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        ProviderGenerationResponse response = provider.submitGeneration(job, sampleImageBytes, "image/jpeg", null, null, List.of());

        assertFalse(response.isSuccess());
        assertEquals("AI_UNPROCESSABLE_ENTITY", response.errorCode());
    }

    @Test
    @DisplayName("Maps 429 Rate Limit safely to PROVIDER_QUOTA_REACHED")
    void test429RateLimit() throws Exception {
        AiJobRecord job = createJob(EditingMode.FULL_IMAGE, "wardrobe in oak");

        when(httpResponse.statusCode()).thenReturn(429);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        ProviderGenerationResponse response = provider.submitGeneration(job, sampleImageBytes, "image/jpeg", null, null, List.of());

        assertFalse(response.isSuccess());
        assertEquals("PROVIDER_QUOTA_REACHED", response.errorCode());
    }

    @Test
    @DisplayName("Maps 500 Server Error safely to PROVIDER_UNAVAILABLE")
    void test500ServerError() throws Exception {
        AiJobRecord job = createJob(EditingMode.FULL_IMAGE, "wardrobe in oak");

        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        ProviderGenerationResponse response = provider.submitGeneration(job, sampleImageBytes, "image/jpeg", null, null, List.of());

        assertFalse(response.isSuccess());
        assertEquals("PROVIDER_UNAVAILABLE", response.errorCode());
    }

    @Test
    @DisplayName("Maps Timeout Exception safely to PROVIDER_TIMEOUT")
    void testTimeoutException() throws Exception {
        AiJobRecord job = createJob(EditingMode.FULL_IMAGE, "wardrobe in oak");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("Request timed out"));

        ProviderGenerationResponse response = provider.submitGeneration(job, sampleImageBytes, "image/jpeg", null, null, List.of());

        assertFalse(response.isSuccess());
        assertEquals("PROVIDER_TIMEOUT", response.errorCode());
    }

    @Test
    @DisplayName("Rejects malformed image bytes returned on HTTP 200")
    void testMalformedImageResponse() throws Exception {
        AiJobRecord job = createJob(EditingMode.FULL_IMAGE, "wardrobe in oak");

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("corrupt non-image data".getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = HttpHeaders.of(
                Map.of("Content-Type", List.of("image/png")),
                (k, v) -> true
        );
        when(httpResponse.headers()).thenReturn(headers);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        ProviderGenerationResponse response = provider.submitGeneration(job, sampleImageBytes, "image/jpeg", null, null, List.of());

        assertFalse(response.isSuccess());
        assertEquals("MALFORMED_PROVIDER_OUTPUT", response.errorCode());
    }
}
