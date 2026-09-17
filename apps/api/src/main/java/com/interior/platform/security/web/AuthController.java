package com.interior.platform.security.web;

import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.service.SessionSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SessionSecurityService sessionSecurityService;

    public AuthController(SessionSecurityService sessionSecurityService) {
        this.sessionSecurityService = sessionSecurityService;
    }

    @GetMapping("/session")
    public ResponseEntity<ActorContext> getSession(
            @RequestAttribute(name = SecurityInterceptor.ACTOR_ATTRIBUTE, required = false) ActorContext actorContext) {
        ActorContext actor = actorContext != null ? actorContext : ActorContext.anonymous();
        return ResponseEntity.ok(actor);
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken() {
        String csrfToken = sessionSecurityService.generateCsrfToken();
        return ResponseEntity.ok(Map.of("csrfToken", csrfToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        boolean isSecure = request.isSecure();
        sessionSecurityService.clearSessionCookie(response, isSecure);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
