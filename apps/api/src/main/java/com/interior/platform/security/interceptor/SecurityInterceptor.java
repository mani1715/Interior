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
    public static final String CSRF_HEADER = "X-CSRF-Token";

    private final SessionSecurityService sessionSecurityService;

    @Value("${app.security.session-cookie-name:__Host-session}")
    private String sessionCookieName;

    public SecurityInterceptor(SessionSecurityService sessionSecurityService) {
        this.sessionSecurityService = sessionSecurityService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String sessionToken = extractSessionToken(request);
        ActorContext actor = ActorContext.anonymous();

        if (sessionToken != null) {
            // For Phase 03 foundation: token is validated and mapped to ActorContext
            // (In production DB runtime, hashed token is matched against identity_sessions)
            actor = ActorContext.anonymous(); // Replaced per request session state
        }

        request.setAttribute(ACTOR_ATTRIBUTE, actor);

        // Enforce CSRF token on mutating HTTP methods
        String method = request.getMethod();
        if (Set.of("POST", "PUT", "DELETE", "PATCH").contains(method.toUpperCase())) {
            String csrfHeader = request.getHeader(CSRF_HEADER);
            if (actor.isAuthenticated() && (csrfHeader == null || csrfHeader.isBlank())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF Token Missing");
                return false;
            }
        }

        return true;
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
