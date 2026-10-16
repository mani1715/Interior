package com.interior.platform.config;

import com.interior.platform.ai.config.AiVisualizerProperties;
import com.interior.platform.email.config.EmailProperties;
import com.interior.platform.media.config.MediaSecurityProperties;
import com.interior.platform.security.config.AuthSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Phase 12E — Production Configuration Validator Email Tests")
class ProductionConfigurationValidatorEmailTest {

    private Environment environment;
    private MediaSecurityProperties mediaSecurityProperties;
    private AuthSecurityProperties authSecurityProperties;
    private AiVisualizerProperties aiVisualizerProperties;
    private EmailProperties emailProperties;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"production"});
        when(environment.getProperty(eq("spring.datasource.url"), anyString())).thenReturn("jdbc:postgresql://localhost:5432/interior_prod");

        mediaSecurityProperties = new MediaSecurityProperties();
        mediaSecurityProperties.setScanner("disabled");

        authSecurityProperties = new AuthSecurityProperties();
        authSecurityProperties.setDevAuthEnabled(false);

        aiVisualizerProperties = new AiVisualizerProperties();
        aiVisualizerProperties.setEnabled(false);

        emailProperties = new EmailProperties();
    }

    private ProductionConfigurationValidator createValidator() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                environment,
                mediaSecurityProperties,
                authSecurityProperties,
                aiVisualizerProperties,
                emailProperties
        );
        ReflectionTestUtils.setField(validator, "devAuthEnabled", false);
        ReflectionTestUtils.setField(validator, "allowedOrigins", "https://interior.com");
        ReflectionTestUtils.setField(validator, "sessionCookieSecure", true);
        ReflectionTestUtils.setField(validator, "storageProvider", "S3_COMPATIBLE");
        ReflectionTestUtils.setField(validator, "s3Endpoint", "https://r2.cloudflarestorage.com");
        ReflectionTestUtils.setField(validator, "s3AccessKeyId", "valid-r2-key");
        ReflectionTestUtils.setField(validator, "s3SecretAccessKey", "valid-r2-secret");
        return validator;
    }

    @Test
    @DisplayName("Email Validator: Disabled provider passes cleanly in production (Phase 15 Pending)")
    void testDisabledEmailProviderCleanPass() {
        emailProperties.setProvider("disabled");
        ProductionConfigurationValidator validator = createValidator();
        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    @DisplayName("Email Validator: Mock/fake provider is strictly rejected in production")
    void testFakeEmailProviderRejectedInProduction() {
        emailProperties.setProvider("fake");
        ProductionConfigurationValidator validator = createValidator();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("Mock email provider"));
    }

    @Test
    @DisplayName("Email Validator: Resend with empty API key is rejected in production")
    void testResendEmptyKeyRejected() {
        emailProperties.setProvider("resend");
        emailProperties.setFromAddress("notifications@interior.design");
        emailProperties.getResend().setApiKey("");

        ProductionConfigurationValidator validator = createValidator();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("apiKey is missing or placeholder"));
    }

    @Test
    @DisplayName("Email Validator: Resend with placeholder API key is rejected in production")
    void testResendPlaceholderKeyRejected() {
        emailProperties.setProvider("resend");
        emailProperties.setFromAddress("notifications@interior.design");
        emailProperties.getResend().setApiKey("placeholder_resend_key");

        ProductionConfigurationValidator validator = createValidator();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("apiKey is missing or placeholder"));
    }

    @Test
    @DisplayName("Email Validator: Resend with missing fromAddress is rejected in production")
    void testResendMissingFromAddressRejected() {
        emailProperties.setProvider("resend");
        emailProperties.getResend().setApiKey("re_prod_valid_token_123");
        emailProperties.setFromAddress("");

        ProductionConfigurationValidator validator = createValidator();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("fromAddress is missing or placeholder"));
    }

    @Test
    @DisplayName("Email Validator: Valid Resend configuration passes cleanly in production")
    void testResendValidPass() {
        emailProperties.setProvider("resend");
        emailProperties.getResend().setApiKey("re_prod_valid_token_123");
        emailProperties.setFromAddress("notifications@interior.design");

        ProductionConfigurationValidator validator = createValidator();
        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    @DisplayName("Email Validator: SES with missing fromAddress is rejected in production")
    void testSesMissingFromAddressRejected() {
        emailProperties.setProvider("ses");
        emailProperties.getSes().setRegion("us-east-1");
        emailProperties.setFromAddress("");

        ProductionConfigurationValidator validator = createValidator();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("fromAddress is missing or placeholder"));
    }

    @Test
    @DisplayName("Email Validator: Valid SES configuration passes cleanly in production")
    void testSesValidPass() {
        emailProperties.setProvider("ses");
        emailProperties.getSes().setRegion("us-east-1");
        emailProperties.setFromAddress("notifications@interior.design");

        ProductionConfigurationValidator validator = createValidator();
        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    @DisplayName("Email Validator: Rogue unsupported email provider is rejected in production")
    void testUnsupportedProviderRejected() {
        emailProperties.setProvider("sendgrid");

        ProductionConfigurationValidator validator = createValidator();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("Unsupported production email provider"));
    }
}
