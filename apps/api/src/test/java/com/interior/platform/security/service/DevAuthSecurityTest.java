package com.interior.platform.security.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.web.AuthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DevAuthSecurityTest {

    @Test
    @DisplayName("AuthController strictly rejects dev-login when DevAuthService is absent")
    void testDevLoginRejectedWhenDisabled() {
        SecurityRepository securityRepository = mock(SecurityRepository.class);
        java.time.Clock clock = java.time.Clock.systemUTC();
        var props = new com.interior.platform.security.config.AuthSecurityProperties();
        SessionSecurityService sessionSecurityService = new SessionSecurityService(props, securityRepository, clock);
        RateLimiterService rateLimiterService = new RateLimiterService(clock);
        AuditService auditService = new AuditService(securityRepository, new com.fasterxml.jackson.databind.ObjectMapper());
        OidcService oidcService = new OidcService(props, new OidcTransactionStore(clock), securityRepository, auditService, clock);

        // Production-like configuration: Optional.empty() for DevAuthService
        AuthController controller = new AuthController(
                sessionSecurityService,
                securityRepository,
                oidcService,
                rateLimiterService,
                auditService,
                Optional.empty()
        );

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> controller.devLogin(Map.of("persona", "customer"), mock(jakarta.servlet.http.HttpServletRequest.class), mock(jakarta.servlet.http.HttpServletResponse.class))
        );

        assertTrue(ex.getMessage().contains("Development authentication is disabled"));
    }
}
