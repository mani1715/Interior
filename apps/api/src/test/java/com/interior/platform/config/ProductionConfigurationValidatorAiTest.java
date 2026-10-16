package com.interior.platform.config;

import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.media.config.MediaSecurityProperties;
import com.interior.platform.security.config.AuthSecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationValidatorAiTest {

    private ProductionConfigurationValidator createProductionValidator(AiVisualizerProperties aiProps) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");
        env.setProperty("spring.datasource.url", "jdbc:postgresql://db.cloud.internal:5432/interior_prod?sslmode=require");

        MediaSecurityProperties media = new MediaSecurityProperties();
        media.setScanner("disabled");
        media.setRequireCleanScan(false);

        AuthSecurityProperties auth = new AuthSecurityProperties();

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                env, media, auth, aiProps
        );

        ReflectionTestUtils.setField(validator, "devAuthEnabled", false);
        ReflectionTestUtils.setField(validator, "sessionCookieSecure", true);
        ReflectionTestUtils.setField(validator, "allowedOrigins", "https://elegance-design.com");
        ReflectionTestUtils.setField(validator, "storageProvider", "S3_COMPATIBLE");
        ReflectionTestUtils.setField(validator, "s3Endpoint", "https://r2.cloudflarestorage.com");
        ReflectionTestUtils.setField(validator, "s3AccessKeyId", "valid-r2-key");
        ReflectionTestUtils.setField(validator, "s3SecretAccessKey", "valid-r2-secret");

        return validator;
    }

    @Test
    @DisplayName("Should pass cleanly when AI is unconfigured or disabled in production")
    void validate_passesCleanlyWhenAiDisabledInProduction() {
        AiVisualizerProperties aiProps = new AiVisualizerProperties();
        aiProps.setEnabled(true);
        aiProps.setProvider("none");

        ProductionConfigurationValidator validator = createProductionValidator(aiProps);
        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    @DisplayName("Should fail fast in production if a mock or fake AI provider is selected")
    void validate_failsFastWhenMockAiProviderInProduction() {
        AiVisualizerProperties aiProps = new AiVisualizerProperties();
        aiProps.setEnabled(true);
        aiProps.setProvider("mock_configured");

        ProductionConfigurationValidator validator = createProductionValidator(aiProps);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("Mock AI provider ('mock_configured') MUST NOT be used in production"));
    }

    @Test
    @DisplayName("Should fail fast in production if AI API key is a placeholder")
    void validate_failsFastWhenAiApiKeyIsPlaceholder() {
        AiVisualizerProperties aiProps = new AiVisualizerProperties();
        aiProps.setEnabled(true);
        aiProps.setProvider("stability");
        aiProps.setApiKey("your-stability-api-key");

        ProductionConfigurationValidator validator = createProductionValidator(aiProps);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("AI provider 'stability' is enabled in production but apiKey is missing or placeholder"));
    }

    @Test
    @DisplayName("Should fail fast in production if AI API key is blank")
    void validate_failsFastWhenAiApiKeyIsEmpty() {
        AiVisualizerProperties aiProps = new AiVisualizerProperties();
        aiProps.setEnabled(true);
        aiProps.setProvider("replicate");
        aiProps.setApiKey("   ");

        ProductionConfigurationValidator validator = createProductionValidator(aiProps);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("AI provider 'replicate' is enabled in production but apiKey is missing or placeholder"));
    }

    @Test
    @DisplayName("Should fail fast in production if configured AI model is not on allowlist")
    void validate_failsFastWhenAiModelNotInAllowlist() {
        AiVisualizerProperties aiProps = new AiVisualizerProperties();
        aiProps.setEnabled(true);
        aiProps.setProvider("vertex");
        aiProps.setApiKey("valid-secret-token");
        aiProps.setAllowedModels(List.of("interior-diffusion-v1", "gemini-imagen-3"));
        aiProps.setModel("unauthorized-rogue-model");

        ProductionConfigurationValidator validator = createProductionValidator(aiProps);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("Configured AI model 'unauthorized-rogue-model' is not on permitted models allowlist"));
    }

    @Test
    @DisplayName("Should pass cleanly when AI is fully configured with valid non-placeholder parameters in production")
    void validate_passesCleanlyWhenAiConfiguredValidlyInProduction() {
        AiVisualizerProperties aiProps = new AiVisualizerProperties();
        aiProps.setEnabled(true);
        aiProps.setProvider("stability");
        aiProps.setApiKey("sk-live-actual-production-secret");
        aiProps.setModel("interior-diffusion-v1");
        aiProps.setAllowedModels(List.of("interior-diffusion-v1", "gemini-imagen-3"));

        ProductionConfigurationValidator validator = createProductionValidator(aiProps);
        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }
}
