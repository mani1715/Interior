package com.interior.platform.security.web;

import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.UnauthorizedException;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.domain.AuthMeResponse;
import com.interior.platform.security.domain.SessionRecord;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.repository.SecurityRepository;
import com.interior.platform.security.service.AuditService;
import com.interior.platform.security.service.DevAuthService;
import com.interior.platform.security.service.OidcService;
import com.interior.platform.security.service.RateLimiterService;
import com.interior.platform.security.service.SessionSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SessionSecurityService sessionSecurityService;
    private final SecurityRepository securityRepository;
    private final OidcService oidcService;
    private final RateLimiterService rateLimiterService;
    private final AuditService auditService;
    private final Optional<DevAuthService> devAuthService;

    public AuthController(
            SessionSecurityService sessionSecurityService,
            SecurityRepository securityRepository,
            OidcService oidcService,
            RateLimiterService rateLimiterService,
            AuditService auditService,
            Optional<DevAuthService> devAuthService
    ) {
        this.sessionSecurityService = sessionSecurityService;
        this.securityRepository = securityRepository;
        this.oidcService = oidcService;
        this.rateLimiterService = rateLimiterService;
        this.auditService = auditService;
        this.devAuthService = devAuthService;
    }

    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProviders() {
        var providers = oidcService.getAvailableProviders();
        boolean isDevAuthEnabled = devAuthService.isPresent();
        List<?> devPersonas = isDevAuthEnabled ? devAuthService.get().getAvailablePersonas() : List.of();

        return ResponseEntity.ok(Map.of(
                "providers", providers,
                "devAuthEnabled", isDevAuthEnabled,
                "devPersonas", devPersonas
        ));
    }

    @GetMapping("/login")
    public void login(
            @RequestParam(name = "provider", defaultValue = "google") String provider,
            @RequestParam(name = "returnUrl", defaultValue = "/") String returnUrl,
            @RequestParam(name = "role", defaultValue = "CUSTOMER") String role,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String clientIp = getClientIp(request);
        rateLimiterService.acquire("login:" + clientIp, 15, Duration.ofMinutes(1));

        var authResponse = oidcService.initiateLogin(provider, returnUrl, role);
        response.sendRedirect(authResponse.authorizationUrl());
    }

    @GetMapping("/callback")
    public void handleGetCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        processCallback(code, state, request, response);
    }

    @PostMapping("/callback")
    public void handlePostCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        processCallback(code, state, request, response);
    }

    private void processCallback(
            String code,
            String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String clientIp = getClientIp(request);
        rateLimiterService.acquire("callback:" + clientIp, 20, Duration.ofMinutes(1));

        if (code == null || state == null) {
            response.sendRedirect("/auth/error?error=missing_parameters");
            return;
        }

        try {
            var authResult = oidcService.handleCallback(code, state);
            var sessionResult = sessionSecurityService.createAndPersistSession(
                    authResult.user().id(),
                    authResult.assurance(),
                    request.getHeader("User-Agent"),
                    response
            );

            auditService.record(
                    authResult.user().id(),
                    null,
                    "USER_LOGIN_OIDC",
                    "SESSION",
                    sessionResult.sessionRecord().id().toString(),
                    Map.of("returnUrl", authResult.returnUrl()),
                    clientIp,
                    request.getHeader("User-Agent")
            );

            String target = OidcService.sanitizeReturnUrl(authResult.returnUrl());
            response.sendRedirect(target);
        } catch (Exception e) {
            response.sendRedirect("/auth/error?error=" + e.getClass().getSimpleName());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> getCurrentUser(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        if (actor == null || !actor.isAuthenticated()) {
            return ResponseEntity.ok(AuthMeResponse.unauthenticated());
        }

        var memberships = securityRepository.getStudioMemberships(actor.userId());
        var studioSummaries = memberships.stream()
                .map(m -> new AuthMeResponse.StudioSummary(m.studioId(), m.studioName(), m.studioSlug(), m.role()))
                .toList();

        AuthMeResponse response = AuthMeResponse.authenticated(
                actor.userId(),
                actor.displayName(),
                actor.email(),
                "ACTIVE",
                actor.platformRoles(),
                actor.permissions(),
                actor.activeStudioId(),
                actor.activeStudioRole(),
                studioSummaries,
                actor.assurance()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletRequest request) {
        SessionRecord session = (SessionRecord) request.getAttribute(SecurityInterceptor.SESSION_ATTRIBUTE);
        String csrfToken;
        if (session != null) {
            csrfToken = sessionSecurityService.refreshSessionCsrfToken(session.id());
        } else {
            csrfToken = sessionSecurityService.generateCsrfToken();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .body(Map.of("csrfToken", csrfToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response) {
        SessionRecord session = (SessionRecord) request.getAttribute(SecurityInterceptor.SESSION_ATTRIBUTE);
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);

        if (session != null) {
            sessionSecurityService.revokeSession(session.id());
            auditService.record(
                    session.userId(),
                    actor != null ? actor.activeStudioId() : null,
                    "USER_LOGOUT",
                    "SESSION",
                    session.id().toString(),
                    Map.of(),
                    getClientIp(request),
                    request.getHeader("User-Agent")
            );
        }

        sessionSecurityService.clearSessionCookie(response);
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    @PostMapping("/revoke-all")
    public ResponseEntity<Map<String, Object>> revokeAll(HttpServletRequest request, HttpServletResponse response) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        if (actor == null || !actor.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required to revoke sessions");
        }

        sessionSecurityService.revokeAllUserSessions(actor.userId());
        sessionSecurityService.clearSessionCookie(response);

        auditService.record(
                actor.userId(),
                actor.activeStudioId(),
                "SESSION_REVOKE_ALL",
                "USER",
                actor.userId().toString(),
                Map.of(),
                getClientIp(request),
                request.getHeader("User-Agent")
        );

        return ResponseEntity.ok(Map.of("success", true, "message", "All active sessions revoked"));
    }

    @PostMapping("/dev-login")
    public ResponseEntity<AuthMeResponse> devLogin(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (devAuthService.isEmpty()) {
            throw new AccessDeniedException("Development authentication is disabled in this environment");
        }

        String clientIp = getClientIp(request);
        rateLimiterService.acquire("dev-login:" + clientIp, 20, Duration.ofMinutes(1));

        String persona = body != null ? body.get("persona") : null;
        var authResult = devAuthService.get().loginPersona(persona);

        var sessionResult = sessionSecurityService.createAndPersistSession(
                authResult.user().id(),
                authResult.assurance(),
                "Dev Persona (" + persona + ")",
                response
        );

        auditService.record(
                authResult.user().id(),
                null,
                "DEV_LOGIN",
                "SESSION",
                sessionResult.sessionRecord().id().toString(),
                Map.of("persona", persona != null ? persona : "unknown"),
                clientIp,
                request.getHeader("User-Agent")
        );

        var memberships = securityRepository.getStudioMemberships(authResult.user().id());
        var studioSummaries = memberships.stream()
                .map(m -> new AuthMeResponse.StudioSummary(m.studioId(), m.studioName(), m.studioSlug(), m.role()))
                .toList();

        var roles = securityRepository.getUserRoles(authResult.user().id());
        var activeStudio = memberships.isEmpty() ? null : memberships.get(0).studioId();
        var activeStudioRole = memberships.isEmpty() ? null : memberships.get(0).role();

        AuthMeResponse me = AuthMeResponse.authenticated(
                authResult.user().id(),
                authResult.user().displayName(),
                authResult.user().email(),
                authResult.user().status(),
                roles,
                java.util.Set.of(),
                activeStudio,
                activeStudioRole,
                studioSummaries,
                authResult.assurance()
        );

        return ResponseEntity.ok(me);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
