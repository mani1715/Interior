package com.interior.platform.security.service;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.domain.UserRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.web.AuthController;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DevAuthSecurityTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(SecurityRepository.class, () -> mock(SecurityRepository.class))
            .withUserConfiguration(DevAuthService.class);

    @Test
    @DisplayName("DevAuthService MUST NOT activate under production profile, even if dev-auth-enabled is true")
    void testDevAuthDisabledUnderProductionProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=production", "app.security.dev-auth-enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(DevAuthService.class));
    }

    @Test
    @DisplayName("DevAuthService MUST NOT activate under staging profile, even if dev-auth-enabled is true")
    void testDevAuthDisabledUnderStagingProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=staging", "app.security.dev-auth-enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(DevAuthService.class));
    }

    @Test
    @DisplayName("DevAuthService MUST NOT activate under default/no profile, even if dev-auth-enabled is true")
    void testDevAuthDisabledUnderDefaultProfile() {
        contextRunner
                .withPropertyValues("app.security.dev-auth-enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(DevAuthService.class));
    }

    @Test
    @DisplayName("DevAuthService MUST NOT activate under dev profile if dev-auth-enabled is false or omitted")
    void testDevAuthDisabledWhenFlagIsFalseOrOmitted() {
        contextRunner
                .withPropertyValues("spring.profiles.active=dev", "app.security.dev-auth-enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DevAuthService.class));

        contextRunner
                .withPropertyValues("spring.profiles.active=dev")
                .run(context -> assertThat(context).doesNotHaveBean(DevAuthService.class));
    }

    @Test
    @DisplayName("DevAuthService ONLY activates when explicitly in dev/test profile AND dev-auth-enabled=true")
    void testDevAuthEnabledUnderDevAndTestProfilesWithFlag() {
        contextRunner
                .withPropertyValues("spring.profiles.active=dev", "app.security.dev-auth-enabled=true")
                .run(context -> assertThat(context).hasSingleBean(DevAuthService.class));

        contextRunner
                .withPropertyValues("spring.profiles.active=test", "app.security.dev-auth-enabled=true")
                .run(context -> assertThat(context).hasSingleBean(DevAuthService.class));
    }

    @Test
    @DisplayName("AuthController strictly rejects dev-login when DevAuthService is absent")
    void testDevLoginRejectedWhenDisabled() {
        SecurityRepository securityRepository = mock(SecurityRepository.class);
        java.time.Clock clock = java.time.Clock.systemUTC();
        var props = new com.interior.platform.security.config.AuthSecurityProperties();
        SessionSecurityService sessionSecurityService = new SessionSecurityService(props, securityRepository, clock);
        RateLimiterService rateLimiterService = new RateLimiterService(clock);
        AuditService auditService = new AuditService(securityRepository, new com.fasterxml.jackson.databind.ObjectMapper());
        OidcService oidcService = new OidcService(props, new OidcTransactionStore(securityRepository, clock), securityRepository, auditService, clock);

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
                () -> controller.devLogin(Map.of("persona", "customer"), mock(HttpServletRequest.class), mock(HttpServletResponse.class))
        );

        assertTrue(ex.getMessage().contains("Development authentication is disabled"));
    }

    @Test
    @DisplayName("SecurityInterceptor strictly ignores arbitrary X-User-Id, X-Role, and X-Studio-Id headers")
    void testArbitraryHeadersIgnored() throws Exception {
        SecurityRepository securityRepository = mock(SecurityRepository.class);
        var props = new com.interior.platform.security.config.AuthSecurityProperties();
        SessionSecurityService sessionSecurityService = new SessionSecurityService(props, securityRepository, java.time.Clock.systemUTC());
        SecurityInterceptor interceptor = new SecurityInterceptor(sessionSecurityService);
        ReflectionTestUtils.setField(interceptor, "sessionCookieName", "__Host-session");

        HttpServletRequest unauthenticatedRequest = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // Attacker attempts to spoof identity via request headers
        when(unauthenticatedRequest.getHeader("X-User-Id")).thenReturn(UUID.randomUUID().toString());
        when(unauthenticatedRequest.getHeader("X-Role")).thenReturn("SUPER_ADMIN");
        when(unauthenticatedRequest.getHeader("X-Studio-Id")).thenReturn(UUID.randomUUID().toString());
        when(unauthenticatedRequest.getCookies()).thenReturn(null);
        when(unauthenticatedRequest.getMethod()).thenReturn("GET");

        final ActorContext[] capturedActor = new ActorContext[1];
        org.mockito.Mockito.doAnswer(invocation -> {
            if (SecurityInterceptor.ACTOR_ATTRIBUTE.equals(invocation.getArgument(0))) {
                capturedActor[0] = invocation.getArgument(1);
            }
            return null;
        }).when(unauthenticatedRequest).setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());

        boolean allowed = interceptor.preHandle(unauthenticatedRequest, response, new Object());
        assertTrue(allowed);
        assertNotNull(capturedActor[0]);
        assertFalse(capturedActor[0].isAuthenticated(), "Actor MUST NOT be authenticated via headers");
        assertEquals(Set.of("PUBLIC"), capturedActor[0].platformRoles(), "Roles MUST NOT be populated from headers");
        assertFalse(capturedActor[0].platformRoles().contains("SUPER_ADMIN"));

        // Now test authenticated CUSTOMER attempting privilege escalation via header injection
        HttpServletRequest authenticatedRequest = mock(HttpServletRequest.class);
        String rawToken = sessionSecurityService.generateOpaqueSessionToken();
        byte[] tokenHash = sessionSecurityService.hashToken(rawToken);
        String rawCsrf = sessionSecurityService.generateCsrfToken();
        byte[] csrfHash = sessionSecurityService.hashToken(rawCsrf);

        Cookie cookie = new Cookie("__Host-session", rawToken);
        when(authenticatedRequest.getCookies()).thenReturn(new Cookie[]{cookie});
        when(authenticatedRequest.getMethod()).thenReturn("GET");
        when(authenticatedRequest.getHeader("X-Role")).thenReturn("SUPER_ADMIN");
        when(authenticatedRequest.getHeader("X-User-Id")).thenReturn(UUID.randomUUID().toString());

        UUID legitimateUserId = UUID.randomUUID();
        SessionRecord session = new SessionRecord(UUID.randomUUID(), legitimateUserId, tokenHash, csrfHash,
                Instant.now(), "PASSWORD", Instant.now(), Instant.now().plusSeconds(1800), Instant.now().plusSeconds(43200), null, "device");
        UserRecord user = new UserRecord(legitimateUserId, "Legit Customer", "customer@example.com", null, "ACTIVE", Instant.now(), Instant.now(), 0L);

        when(securityRepository.findSessionByTokenHash(tokenHash)).thenReturn(Optional.of(session));
        when(securityRepository.findUserById(legitimateUserId)).thenReturn(Optional.of(user));
        when(securityRepository.getUserRoles(legitimateUserId)).thenReturn(Set.of("CUSTOMER"));
        when(securityRepository.getStudioMemberships(legitimateUserId)).thenReturn(List.of());

        final ActorContext[] capturedAuthActor = new ActorContext[1];
        org.mockito.Mockito.doAnswer(invocation -> {
            if (SecurityInterceptor.ACTOR_ATTRIBUTE.equals(invocation.getArgument(0))) {
                capturedAuthActor[0] = invocation.getArgument(1);
            }
            return null;
        }).when(authenticatedRequest).setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());

        boolean authAllowed = interceptor.preHandle(authenticatedRequest, response, new Object());
        assertTrue(authAllowed);
        assertNotNull(capturedAuthActor[0]);
        assertTrue(capturedAuthActor[0].isAuthenticated());
        assertEquals(legitimateUserId, capturedAuthActor[0].userId());
        assertEquals(Set.of("CUSTOMER"), capturedAuthActor[0].platformRoles(), "Role MUST remain CUSTOMER despite spoofed header");
        assertFalse(capturedAuthActor[0].platformRoles().contains("SUPER_ADMIN"), "Injected SUPER_ADMIN role MUST be ignored");
    }
}
