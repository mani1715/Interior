package com.interior.platform.security.interceptor;

import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.service.SessionSecurityService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    public static final String ACTOR_ATTRIBUTE = "actorContext";
    public static final String SESSION_ATTRIBUTE = "sessionRecord";
    public static final String CSRF_HEADER = "X-CSRF-Token";

    private final SessionSecurityService sessionSecurityService;

    @Value("${app.security.session-cookie-name:__Host-session}")
    private String sessionCookieName = "__Host-session";

    @org.springframework.beans.factory.annotation.Autowired
    public SecurityInterceptor(SessionSecurityService sessionSecurityService) {
        this.sessionSecurityService = sessionSecurityService;
    }

    public void setSessionCookieName(String sessionCookieName) {
        if (sessionCookieName != null) {
            this.sessionCookieName = sessionCookieName;
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object existingActor = request.getAttribute(ACTOR_ATTRIBUTE);
        if (existingActor instanceof ActorContext preset && preset.isAuthenticated()) {
            return true;
        }

        String sessionToken = extractSessionToken(request);
        ActorContext actor = ActorContext.anonymous();
        com.interior.platform.security.domain.SessionRecord session = null;

        if (sessionToken != null && !sessionToken.isBlank()) {
            var validatedOpt = sessionSecurityService.validateSession(sessionToken);
            if (validatedOpt.isPresent()) {
                var vs = validatedOpt.get();
                session = vs.session();
                var user = vs.user();
                var roles = vs.roles().isEmpty() ? Set.of("CUSTOMER") : vs.roles();
                var memberships = vs.studioMemberships();
                java.util.UUID requestedStudioId = resolveRequestedStudioId(request);
                var activeMembership = memberships.isEmpty() ? null : memberships.get(0);
                boolean isAdminPath = request.getRequestURI() != null &&
                        (request.getRequestURI().startsWith("/admin") || request.getRequestURI().startsWith("/api/v1/admin"));
                if (requestedStudioId != null && !isAdminPath) {
                    var matched = memberships.stream()
                            .filter(m -> m.studioId().equals(requestedStudioId))
                            .findFirst();
                    if (matched.isPresent()) {
                        activeMembership = matched.get();
                    } else {
                        // User is not an active member of requested studio -> reject immediately
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: you are not a member of the requested studio");
                        return false;
                    }
                }
                java.util.UUID studioId = activeMembership != null ? activeMembership.studioId() : null;
                String studioRole = activeMembership != null ? activeMembership.role() : null;

                Set<String> permissions = derivePermissions(roles, studioRole);

                actor = new ActorContext(
                        user.id(),
                        user.displayName(),
                        user.email(),
                        user.status(),
                        roles,
                        permissions,
                        studioId,
                        studioRole,
                        session.assurance(),
                        true
                );
            }
        }

        request.setAttribute(ACTOR_ATTRIBUTE, actor);
        if (session != null) {
            request.setAttribute(SESSION_ATTRIBUTE, session);
        }

        if (actor.isAuthenticated()) {
            if (response.getHeader("Cache-Control") == null) {
                response.setHeader("Cache-Control", "no-store, private");
            }
        }

        // Enforce CSRF token on mutating HTTP methods for authenticated sessions
        String method = request.getMethod();
        if (Set.of("POST", "PUT", "DELETE", "PATCH").contains(method.toUpperCase())) {
            if (actor.isAuthenticated()) {
                String csrfHeader = request.getHeader(CSRF_HEADER);
                if (csrfHeader == null || csrfHeader.isBlank()) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF Token Missing");
                    return false;
                }
                if (session == null || !sessionSecurityService.verifyCsrfToken(csrfHeader, session)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF Token");
                    return false;
                }
            }
        }

        return true;
    }

    private Set<String> derivePermissions(Set<String> roles, String studioRole) {
        java.util.HashSet<String> perms = new java.util.HashSet<>();
        for (String role : roles) {
            switch (role.toUpperCase()) {
                case "SUPER_ADMIN" -> perms.addAll(Set.of("*", "system:admin", "user:manage", "studio:manage", "project:write", "project:read"));
                case "ADMIN" -> perms.addAll(Set.of("system:admin", "user:manage", "studio:manage", "project:write", "project:read", "content:moderate"));
                case "MODERATOR" -> perms.addAll(Set.of("content:moderate", "review:moderate", "project:read"));
                case "DESIGNER" -> perms.addAll(Set.of("project:read", "project:write", "studio:read", "studio:write", "lead:read", "lead:write"));
                case "DESIGNER_TEAM" -> perms.addAll(Set.of("project:read", "project:write", "studio:read"));
                case "CUSTOMER" -> perms.addAll(Set.of("project:read", "moodboard:read", "moodboard:write", "inquiry:create"));
            }
        }
        if (studioRole != null) {
            switch (studioRole.toUpperCase()) {
                case "DESIGNER_ADMIN", "OWNER", "ADMIN" ->
                        perms.addAll(Set.of("project:read", "project:write", "studio:read", "studio:write", "lead:read", "lead:write", "team:manage"));
                case "DESIGNER_MEMBER", "MEMBER" ->
                        perms.addAll(Set.of("project:read", "project:write", "studio:read"));
            }
        }
        return perms;
    }

    private java.util.UUID resolveRequestedStudioId(HttpServletRequest request) {
        String param = request.getParameter("studioId");
        if (param != null && !param.isBlank()) {
            try {
                return java.util.UUID.fromString(param.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        String header = request.getHeader("X-Studio-Id");
        if (header != null && !header.isBlank()) {
            try {
                return java.util.UUID.fromString(header.trim());
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    private String extractSessionToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (sessionCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
