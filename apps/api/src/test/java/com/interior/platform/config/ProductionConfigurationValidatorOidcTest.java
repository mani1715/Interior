package com.interior.platform.config;

import com.interior.platform.media.config.MediaSecurityProperties;
import com.interior.platform.security.config.AuthSecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationValidatorOidcTest {

    @Test
    @DisplayName("Production profile rejects dev-auth when enabled")
    void testProductionRejectsDevAuth() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");
        env.setProperty("app.security.dev-auth-enabled", "true");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                env,
                new MediaSecurityProperties(),
                new AuthSecurityProperties()
        );

        // Set private field via reflection / test property
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "devAuthEnabled", true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("dev-auth-enabled MUST NOT be true in production"));
    }

    @Test
    @DisplayName("Production profile passes cleanly with deferred OIDC when no provider credentials exist")
    void testProductionPassesWithDeferredOidc() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");
        env.setProperty("spring.datasource.url", "jdbc:postgresql://postgres.cloud.internal:5432/interior_prod?sslmode=require");
        env.setProperty("app.storage.provider", "S3_COMPATIBLE");

        AuthSecurityProperties authProps = new AuthSecurityProperties();
        authProps.setSessionCookieSecure(true);

        MediaSecurityProperties mediaProps = new MediaSecurityProperties();
        mediaProps.setScanner("disabled");
        mediaProps.setRequireCleanScan(false);

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(env, mediaProps, authProps);
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "storageProvider", "S3_COMPATIBLE");
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "s3Endpoint", "https://r2.cloudflarestorage.com");
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "s3AccessKeyId", "r2-access-key");
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "s3SecretAccessKey", "r2-secret-key");
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "allowedOrigins", "https://interior.com");
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "sessionCookieSecure", true);

        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    @DisplayName("Production profile rejects incomplete OIDC provider with placeholder client ID")
    void testProductionRejectsPlaceholderOidcClientId() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");
        env.setProperty("spring.datasource.url", "jdbc:postgresql://postgres.cloud.internal:5432/interior_prod?sslmode=require");

        AuthSecurityProperties authProps = new AuthSecurityProperties();
        var google = new AuthSecurityProperties.OidcProviderProperties();
        google.setClientId("your-client-id-here");
        google.setClientSecret("real-secret-12345678");
        authProps.getOidc().getProviders().put("google", google);

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                env,
                new MediaSecurityProperties(),
                authProps
        );
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "sessionCookieSecure", true);
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "allowedOrigins", "https://interior.com");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("clientId is missing or placeholder"));
    }

    @Test
    @DisplayName("Production profile rejects incomplete OIDC provider missing client secret")
    void testProductionRejectsMissingOidcClientSecret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");
        env.setProperty("spring.datasource.url", "jdbc:postgresql://postgres.cloud.internal:5432/interior_prod?sslmode=require");

        AuthSecurityProperties authProps = new AuthSecurityProperties();
        var google = new AuthSecurityProperties.OidcProviderProperties();
        google.setClientId("valid-google-client-id.apps.googleusercontent.com");
        google.setClientSecret(""); // Missing!
        authProps.getOidc().getProviders().put("google", google);

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                env,
                new MediaSecurityProperties(),
                authProps
        );
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "sessionCookieSecure", true);
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "allowedOrigins", "https://interior.com");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("clientSecret is missing or placeholder"));
    }

    @Test
    @DisplayName("Production profile passes when valid full OIDC provider is configured")
    void testProductionPassesWithFullOidcConfig() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");
        env.setProperty("spring.datasource.url", "jdbc:postgresql://postgres.cloud.internal:5432/interior_prod?sslmode=require");

        AuthSecurityProperties authProps = new AuthSecurityProperties();
        var google = new AuthSecurityProperties.OidcProviderProperties();
        google.setClientId("123456789.apps.googleusercontent.com");
        google.setClientSecret("GOCSPX-valid-production-secret-999");
        google.setIssuer("https://accounts.google.com");
        google.setAuthorizationUri("https://accounts.google.com/o/oauth2/v2/auth");
        google.setTokenUri("https://oauth2.googleapis.com/token");
        google.setJwkSetUri("https://www.googleapis.com/oauth2/v3/certs");
        google.setRedirectUri("https://interior.com/api/v1/auth/callback");
        google.setScopes(List.of("openid", "email", "profile"));
        authProps.getOidc().getProviders().put("google", google);

        MediaSecurityProperties mediaProps = new MediaSecurityProperties();
        mediaProps.setScanner("disabled");
        mediaProps.setRequireCleanScan(false);

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(env, mediaProps, authProps);
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "sessionCookieSecure", true);
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "allowedOrigins", "https://interior.com");

        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }
}
