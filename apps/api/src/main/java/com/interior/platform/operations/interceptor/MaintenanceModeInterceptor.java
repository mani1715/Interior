package com.interior.platform.operations.interceptor;

import com.interior.platform.operations.service.PlatformOperationsService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class MaintenanceModeInterceptor implements HandlerInterceptor {

    private final PlatformOperationsService operationsService;

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    public MaintenanceModeInterceptor(PlatformOperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if (SAFE_METHODS.contains(method)) {
            return true;
        }

        if (!operationsService.isMaintenanceReadOnly()) {
            return true;
        }

        String path = request.getRequestURI();

        // Allow explicit operational bypass paths for infrastructure/session/webhook recovery
        if (path.contains("/auth/logout") || path.contains("/public/webhooks") || path.contains("/actuator")) {
            return true;
        }

        // Dedicated administrative operational and control-plane remediation paths are permitted for platform administrators
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        if (actor != null && (actor.hasRole("ADMIN") || actor.hasRole("SUPER_ADMIN"))) {
            String normPath = path.startsWith("/api/v1") ? path.substring("/api/v1".length()) : path;
            if (normPath.startsWith("/admin/operations") || normPath.startsWith("/admin/mfa") || normPath.startsWith("/admin/audit")
                    || normPath.startsWith("/admin/users/") && normPath.endsWith("/status")
                    || normPath.startsWith("/admin/studios/") && normPath.endsWith("/moderation")) {
                return true;
            }
        }

        // Block with 503 Service Unavailable
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String message = operationsService.getOperationalState().maintenanceMessage();
        String safeMessage = (message != null && !message.isBlank()) ? message : "Platform is currently undergoing scheduled maintenance in read-only mode.";
        response.getWriter().write(String.format("{\"code\":\"MAINTENANCE_MODE_ACTIVE\",\"message\":\"%s\"}", safeMessage));
        return false;
    }
}
