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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
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
@Tag(name = "Authentication & Sessions", description = "Identity, OIDC authentication, and session management")
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
    @Operation(summary = "List configured identity providers", description = "Returns only actually configured OIDC providers and dev auth availability.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configured providers list returned")
    })
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
    @Operation(summary = "Initiate OIDC authentication", description = "Generates secure state, nonce, and PKCE challenge, returning 302 redirect to provider.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to provider authorization endpoint"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
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
    @Operation(summary = "OIDC callback handler (GET)", description = "Handles authorization code return, validates state and nonce, issues session cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to sanitized application return URL"),
            @ApiResponse(responseCode = "403", description = "Invalid or expired state / replay attack"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public void handleGetCallback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        processCallback(code, state, request, response);
    }

    @PostMapping("/callback")
    @Operation(summary = "OIDC callback handler (POST)", description = "Form-post callback handler for identity providers that post authorization response.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to sanitized application return URL"),
            @ApiResponse(responseCode = "403", description = "Invalid or expired state"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
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
    @Operation(summary = "Get current authenticated identity", description = "Returns active user profile, studio tenancy, roles, and assurance. Does not leak secrets.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Identity context returned")
    })
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
    @Operation(summary = "Retrieve fresh CSRF token", description = "Returns CSRF token with private no-store cache headers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSRF token returned")
    })
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
    @Operation(summary = "Logout current session", description = "Revokes current session in database, clears cookie. Requires X-CSRF-Token for authenticated sessions.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @ApiResponse(responseCode = "403", description = "CSRF token missing or invalid")
    })
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
    @Operation(summary = "Revoke all user sessions", description = "Revokes all sessions across all devices for the current user. Requires X-CSRF-Token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All active sessions revoked"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "CSRF token missing or invalid")
    })
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
    @Operation(summary = "Development persona login", description = "Authenticates as a predefined development persona. Strictly rejected outside dev/test profiles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated as persona"),
            @ApiResponse(responseCode = "403", description = "Dev authentication disabled in this environment"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
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
