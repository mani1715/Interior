package com.interior.platform.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.ai.domain.AiJobRecord;
import com.interior.platform.ai.domain.CostSource;
import com.interior.platform.ai.domain.EditingMode;
import com.interior.platform.common.exception.AiProviderNotConfiguredException;
import com.interior.platform.common.service.SsrfProtectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Component
@Primary
public class ConfigurableAiImageProvider implements AiImageProvider {

    private static final Logger log = LoggerFactory.getLogger(ConfigurableAiImageProvider.class);

    private final AiVisualizerProperties properties;
    private final SsrfProtectionService ssrfProtectionService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ConfigurableAiImageProvider(AiVisualizerProperties properties) {
        this(properties, new SsrfProtectionService(), null, new ObjectMapper());
    }

    public ConfigurableAiImageProvider(AiVisualizerProperties properties, SsrfProtectionService ssrfProtectionService) {
        this(properties, ssrfProtectionService, null, new ObjectMapper());
    }

    @Autowired
    public ConfigurableAiImageProvider(
            AiVisualizerProperties properties,
            @Autowired(required = false) SsrfProtectionService ssrfProtectionService,
            @Autowired(required = false) HttpClient httpClient,
            @Autowired(required = false) ObjectMapper objectMapper
    ) {
        this.properties = properties != null ? properties : new AiVisualizerProperties();
        this.ssrfProtectionService = ssrfProtectionService != null ? ssrfProtectionService : new SsrfProtectionService();
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(5, this.properties.getTimeoutSeconds())))
                .build();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public String getProviderKey() {
        return properties.getProvider() != null && !properties.getProvider().isBlank()
                ? properties.getProvider().toLowerCase().trim()
                : "none";
    }

    @Override
    public boolean isConfigured() {
        String provider = properties.getProvider();
        if (provider == null || provider.isBlank() || "none".equalsIgnoreCase(provider)) {
            return false;
        }
        String key = properties.getApiKey();
        return key != null && !key.isBlank() && !key.equalsIgnoreCase("unconfigured") && !isPlaceholder(key);
    }

    @Override
    public boolean supportsReferenceImages() {
        return properties.isSupportsReferenceImages();
    }

    @Override
    public int getMaxReferenceImages() {
        return properties.getMaxReferenceImages();
    }

    @Override
    public boolean supportsMaskEditing() {
        return properties.isSupportsMaskEditing();
    }

    @Override
    public ProviderGenerationResponse submitGeneration(
            AiJobRecord job,
            byte[] inputImageBytes,
            String inputContentType,
            List<AiGenerationReference> references
    ) {
        return submitGeneration(job, inputImageBytes, inputContentType, null, null, references);
    }

    @Override
    public ProviderGenerationResponse submitGeneration(
            AiJobRecord job,
            byte[] inputImageBytes,
            String inputContentType,
            byte[] maskBytes,
            String maskContentType,
            List<AiGenerationReference> references
    ) {
        if (!isConfigured()) {
            throw new AiProviderNotConfiguredException("AI generation provider is not configured for this environment.");
        }

        if (job.editingMode() == EditingMode.PRECISION_MASK && !supportsMaskEditing()) {
            return ProviderGenerationResponse.failure("MASK_EDITING_UNSUPPORTED", "The configured AI model does not support precision mask editing.");
        }

        String endpoint = properties.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new AiProviderNotConfiguredException("AI generation provider endpoint is missing or invalid.");
        }

        // Validate model allowlist
        String configuredModel = properties.getModel();
        if (properties.getAllowedModels() != null && !properties.getAllowedModels().isEmpty()
                && !properties.getAllowedModels().contains(configuredModel)) {
            return ProviderGenerationResponse.failure("MODEL_NOT_PERMITTED", "Configured model '" + configuredModel + "' is not on the permitted models allowlist.");
        }

        boolean hasMask = (maskBytes != null && maskBytes.length > 0);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Elegance-Platform-AI/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(job, references, hasMask)))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                String ct = response.headers().firstValue("Content-Type").orElse("image/jpeg");
                long costMinor = properties.getEstimatedCostPerGenerationMinor();
                String currency = properties.getPricingCurrency();

                // Direct binary image response
                if (ct.startsWith("image/")) {
                    byte[] body = response.body();
                    if (!isValidImageBytes(body)) {
                        return ProviderGenerationResponse.failure("MALFORMED_PROVIDER_OUTPUT", "Provider returned invalid or malformed image data.");
                    }
                    return ProviderGenerationResponse.immediateSuccess(
                            body,
                            ct,
                            "{\"model\":\"" + escapeJson(configuredModel) + "\"}",
                            costMinor,
                            currency,
                            CostSource.CALCULATED
                    );
                }

                // JSON response handling (URL output or Base64 payload)
                if (ct.contains("application/json") || ct.contains("text/json")) {
                    return parseJsonResponse(response.body(), configuredModel, costMinor, currency, job.id().toString());
                }

                return ProviderGenerationResponse.asyncStarted("job-" + job.id(), "{\"model\":\"" + escapeJson(configuredModel) + "\"}");

            } else if (statusCode == 401 || statusCode == 403) {
                log.error("AI provider authentication rejected for job {} (status {})", job.id(), statusCode);
                return ProviderGenerationResponse.failure("AI_AUTH_FAILED", "AI provider authentication failed. Check credentials.");
            } else if (statusCode == 429) {
                log.warn("AI generation provider rate limit reached for job {} (status 429)", job.id());
                return ProviderGenerationResponse.failure("PROVIDER_QUOTA_REACHED", "AI generation provider quota reached. Please try again later.");
            } else if (statusCode >= 500) {
                log.warn("AI generation provider server error for job {} (status {})", job.id(), statusCode);
                return ProviderGenerationResponse.failure("PROVIDER_UNAVAILABLE", "AI generation provider temporarily unavailable (status " + statusCode + ").");
            } else if (statusCode == 400) {
                String bodyStr = new String(response.body(), StandardCharsets.UTF_8).toLowerCase();
                if (bodyStr.contains("safety") || bodyStr.contains("moderation") || bodyStr.contains("nsfw") || bodyStr.contains("policy") || bodyStr.contains("blocked")) {
                    return ProviderGenerationResponse.failure("AI_SAFETY_REJECTED", "AI generation request rejected by provider safety filters.");
                }
                return ProviderGenerationResponse.failure("AI_INVALID_REQUEST", "AI generation request rejected by provider (status 400).");
            } else {
                return ProviderGenerationResponse.failure("GENERATION_REJECTED", "AI generation request rejected by provider (status " + statusCode + ").");
            }

        } catch (HttpTimeoutException e) {
            log.warn("AI generation request timed out for job {}", job.id());
            return ProviderGenerationResponse.failure("PROVIDER_TIMEOUT", "AI generation provider timed out. Retrying may succeed.");
        } catch (Exception e) {
            log.error("AI generation provider call failed for job {}: {}", job.id(), e.getMessage());
            return ProviderGenerationResponse.failure("PROVIDER_ERROR", "AI generation provider network error.");
        }
    }

    private ProviderGenerationResponse parseJsonResponse(
            byte[] responseBytes,
            String configuredModel,
            long costMinor,
            String currency,
            String jobId
    ) {
        try {
            JsonNode root = objectMapper.readTree(responseBytes);

            // 1. Look for output URL
            String outputUrl = null;
            if (root.has("output_url")) {
                outputUrl = root.get("output_url").asText();
            } else if (root.has("image_url")) {
                outputUrl = root.get("image_url").asText();
            } else if (root.has("url")) {
                outputUrl = root.get("url").asText();
            } else if (root.has("output") && root.get("output").isArray() && !root.get("output").isEmpty()) {
                outputUrl = root.get("output").get(0).asText();
            }

            if (outputUrl != null && !outputUrl.isBlank()) {
                URI uri = URI.create(outputUrl.trim());
                if (!"https".equalsIgnoreCase(uri.getScheme())) {
                    log.warn("Rejecting non-HTTPS provider output URL: {}", outputUrl);
                    return ProviderGenerationResponse.failure("MALFORMED_PROVIDER_OUTPUT", "Provider output URL must use HTTPS.");
                }
                ssrfProtectionService.validateTargetUri(uri);
                byte[] downloaded = ssrfProtectionService.downloadBoundedImage(outputUrl, 50 * 1024 * 1024L);
                if (!isValidImageBytes(downloaded)) {
                    return ProviderGenerationResponse.failure("MALFORMED_PROVIDER_OUTPUT", "Downloaded provider image is corrupt or unrecognized.");
                }
                return ProviderGenerationResponse.immediateSuccess(
                        downloaded,
                        "image/jpeg",
                        "{\"model\":\"" + escapeJson(configuredModel) + "\",\"source_url_domain\":\"" + escapeJson(uri.getHost()) + "\"}",
                        costMinor,
                        currency,
                        CostSource.CALCULATED
                );
            }

            // 2. Look for base64 payload
            String b64 = null;
            if (root.has("b64_json")) {
                b64 = root.get("b64_json").asText();
            } else if (root.has("base64")) {
                b64 = root.get("base64").asText();
            } else if (root.has("image_base64")) {
                b64 = root.get("image_base64").asText();
            }

            if (b64 != null && !b64.isBlank()) {
                if (b64.contains(",")) {
                    b64 = b64.substring(b64.indexOf(",") + 1);
                }
                byte[] decoded = Base64.getDecoder().decode(b64.trim());
                if (!isValidImageBytes(decoded)) {
                    return ProviderGenerationResponse.failure("MALFORMED_PROVIDER_OUTPUT", "Decoded base64 image data is corrupt or unrecognized.");
                }
                return ProviderGenerationResponse.immediateSuccess(
                        decoded,
                        "image/jpeg",
                        "{\"model\":\"" + escapeJson(configuredModel) + "\"}",
                        costMinor,
                        currency,
                        CostSource.CALCULATED
                );
            }

            // 3. Fallback to async task identifier if present
            String remoteJobId = root.has("id") ? root.get("id").asText() : jobId;
            return ProviderGenerationResponse.asyncStarted(remoteJobId, "{\"model\":\"" + escapeJson(configuredModel) + "\"}");

        } catch (Exception e) {
            log.warn("Failed to parse provider JSON response: {}", e.getMessage());
            return ProviderGenerationResponse.failure("MALFORMED_PROVIDER_OUTPUT", "Unable to parse provider JSON response.");
        }
    }

    private boolean isValidImageBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return false;
        // JPEG magic bytes: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return true;
        }
        // PNG magic bytes: 89 50 4E 47
        if ((bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50 && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47) {
            return true;
        }
        // WebP magic bytes: RIFF....WEBP
        if ((bytes[0] & 0xFF) == 'R' && (bytes[1] & 0xFF) == 'I' && (bytes[2] & 0xFF) == 'F' && (bytes[3] & 0xFF) == 'F'
                && (bytes[8] & 0xFF) == 'W' && (bytes[9] & 0xFF) == 'E' && (bytes[10] & 0xFF) == 'B' && (bytes[11] & 0xFF) == 'P') {
            return true;
        }
        return false;
    }

    @Override
    public ProviderStatusResponse checkStatus(String providerJobId) {
        if (!isConfigured()) {
            throw new AiProviderNotConfiguredException("AI generation provider is not configured for this environment.");
        }
        return ProviderStatusResponse.processing(providerJobId);
    }

    @Override
    public boolean cancel(String providerJobId) {
        if (!isConfigured() || providerJobId == null) {
            return false;
        }
        return true;
    }

    private String buildRequestBody(AiJobRecord job, List<AiGenerationReference> references, boolean hasMask) {
        String escapedPrompt = escapeJson(job.prompt());
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"model\":\"").append(escapeJson(properties.getModel())).append("\",");
        sb.append("\"prompt\":\"").append(escapedPrompt).append("\",");
        sb.append("\"preserve_structure\":").append(job.preserveStructure()).append(",");
        sb.append("\"editing_mode\":\"").append(job.editingMode() != null ? job.editingMode().name() : "FULL_IMAGE").append("\",");
        sb.append("\"has_mask\":").append(hasMask).append(",");
        sb.append("\"num_outputs\":1,");
        sb.append("\"references\":[");
        if (references != null && !references.isEmpty()) {
            for (int i = 0; i < references.size(); i++) {
                if (i > 0) sb.append(",");
                AiGenerationReference ref = references.get(i);
                sb.append("{");
                sb.append("\"purpose\":\"").append(ref.purpose() != null ? ref.purpose().name() : "GENERAL_STYLE").append("\",");
                sb.append("\"label\":\"").append(ref.label() != null ? escapeJson(ref.label()) : "").append("\",");
                sb.append("\"instruction\":\"").append(ref.instruction() != null ? escapeJson(ref.instruction()) : "").append("\"");
                sb.append("}");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private boolean isPlaceholder(String val) {
        if (val == null) return true;
        String lower = val.toLowerCase().trim();
        return lower.startsWith("your-") || lower.contains("placeholder") || lower.equals("change-me");
    }
}
