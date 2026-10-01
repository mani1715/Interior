package com.interior.platform.admin;

import com.interior.platform.common.filter.RequestCorrelationFilter;
import com.interior.platform.common.filter.SecurityHeadersFilter;
import com.interior.platform.config.ProductionConfigurationValidator;
import com.interior.platform.email.domain.EmailMessage;
import com.interior.platform.email.domain.EmailSendResult;
import com.interior.platform.email.provider.DisabledEmailProvider;
import com.interior.platform.email.service.TransactionalEmailService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class ProductionHardeningTest {

    @Test
    @DisplayName("SecurityHeadersFilter emits security headers and enforces HSTS conditionally")
    void securityHeadersFilterEnforcesHstsOnHttpsOnly() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();

        // 1. Insecure HTTP request -> No HSTS header, but other security headers present
        MockHttpServletRequest httpReq = new MockHttpServletRequest("GET", "/api/v1/projects");
        httpReq.setSecure(false);
        MockHttpServletResponse httpRes = new MockHttpServletResponse();
        filter.doFilter(httpReq, httpRes, new MockFilterChain());

        assertEquals("nosniff", httpRes.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", httpRes.getHeader("X-Frame-Options"));
        assertEquals("strict-origin-when-cross-origin", httpRes.getHeader("Referrer-Policy"));
        assertNull(httpRes.getHeader("Strict-Transport-Security"));
        assertTrue(httpRes.getHeader("Content-Security-Policy").contains("default-src 'self'"));

        // 2. Secure HTTPS request -> HSTS header MUST be present
        MockHttpServletRequest httpsReq = new MockHttpServletRequest("GET", "/api/v1/projects");
        httpsReq.setSecure(true);
        MockHttpServletResponse httpsRes = new MockHttpServletResponse();
        filter.doFilter(httpsReq, httpsRes, new MockFilterChain());

        assertEquals("max-age=31536000; includeSubDomains", httpsRes.getHeader("Strict-Transport-Security"));
    }

    @Test
    @DisplayName("RequestCorrelationFilter attaches and propagates correlation ID in MDC")
    void requestCorrelationFilterMdcTracking() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/projects");
        req.addHeader("X-Request-Id", "corr-test-12345");
        MockHttpServletResponse res = new MockHttpServletResponse();

        final String[] mdcCapture = new String[1];
        FilterChain chain = (request, response) -> {
            mdcCapture[0] = MDC.get("requestId");
        };

        filter.doFilter(req, res, chain);

        assertEquals("corr-test-12345", res.getHeader("X-Request-Id"));
        assertEquals("corr-test-12345", mdcCapture[0]);
        // MDC must be cleaned up after request completes
        assertNull(MDC.get("requestId"));
    }

    @Test
    @DisplayName("DisabledEmailProvider truthfully reports NOT_CONFIGURED without throwing or faking delivery")
    void disabledEmailProviderReportsNotConfigured() {
        DisabledEmailProvider provider = new DisabledEmailProvider();
        TransactionalEmailService emailService = new TransactionalEmailService(provider);

        assertFalse(emailService.isConfigured());
        assertEquals("DISABLED", emailService.getProviderName());

        EmailSendResult result = emailService.send(new EmailMessage("test@example.com", "Test Subject", "Hello world"));
        assertNotNull(result);
        assertFalse(result.success());
        assertEquals("NOT_CONFIGURED", result.status());
    }

    @Test
    @DisplayName("ProductionConfigurationValidator rejects dev-auth when active profile is production")
    void productionConfigurationValidatorRejectsDevAuthInProd() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(prodEnv);

        // Reflection or setter simulation: in production with dev-auth enabled, it must throw IllegalStateException
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "devAuthEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "sessionCookieSecure", true);
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "allowedOrigins", "https://platform.com");

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    @DisplayName("ProductionConfigurationValidator rejects insecure session cookie in production")
    void productionConfigurationValidatorRejectsInsecureCookieInProd() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(prodEnv);
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "devAuthEnabled", false);
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "sessionCookieSecure", false);
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "allowedOrigins", "https://platform.com");

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }
}
