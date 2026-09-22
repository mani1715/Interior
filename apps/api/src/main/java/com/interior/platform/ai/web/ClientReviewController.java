package com.interior.platform.ai.web;

import com.interior.platform.ai.dto.*;
import com.interior.platform.ai.service.AiVisualizerService;
import com.interior.platform.security.config.AuthSecurityProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/client-review")
@Tag(name = "Client Review", description = "Public-facing client concept review session and decision endpoints")
public class ClientReviewController {

    private static final String SESSION_COOKIE_NAME = "review_session";

    private final AiVisualizerService aiVisualizerService;
    private final AuthSecurityProperties authSecurityProperties;

    public ClientReviewController(AiVisualizerService aiVisualizerService, AuthSecurityProperties authSecurityProperties) {
        this.aiVisualizerService = aiVisualizerService;
        this.authSecurityProperties = authSecurityProperties;
    }

    @PostMapping("/exchange")
    @Operation(summary = "Exchange raw review token for session", description = "Exchanges one-time/reusable URL token for an HttpOnly session cookie and CSRF token.")
    public ResponseEntity<ExchangeReviewTokenResponse> exchangeToken(
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @RequestBody ExchangeReviewTokenRequest req
    ) {
        String clientIp = getClientIp(request);
        ExchangeReviewSessionResult result = aiVisualizerService.exchangeReviewToken(req.token(), clientIp);

        attachReviewSessionCookie(request, response, result.sessionToken(), result.expiresAt());

        ExchangeReviewTokenResponse body = new ExchangeReviewTokenResponse(
                result.reviewPublicId(),
                result.csrfToken(),
                "/review/view/" + result.reviewPublicId()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    @GetMapping("/session")
    @Operation(summary = "Get current review presentation", description = "Fetches review details and concept items for the active review session.")
    public ResponseEntity<PublicClientReviewResponse> getSession(HttpServletRequest request) {
        String sessionToken = extractSessionToken(request);
        String clientIp = getClientIp(request);
        PublicClientReviewResponse res = aiVisualizerService.getPublicReview(sessionToken, clientIp);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(res);
    }

    @GetMapping("/media/{mediaId}")
    @Operation(summary = "Get watermarked review media preview", description = "Serves private concept derivatives strictly restricted to review participants.")
    public ResponseEntity<byte[]> getMedia(
            HttpServletRequest request,
            @PathVariable("mediaId") UUID mediaId
    ) {
        String sessionToken = extractSessionToken(request);
        byte[] mediaBytes = aiVisualizerService.getReviewMediaPreview(sessionToken, mediaId);

        String contentType = detectContentType(mediaBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(mediaBytes);
    }

    @PostMapping("/decisions")
    @Operation(summary = "Submit client approval or change request", description = "Records client decision on a concept with anti-CSRF protection.")
    public ResponseEntity<Void> submitDecision(
            HttpServletRequest request,
            @Valid @RequestBody SubmitClientDecisionRequest req
    ) {
        String sessionToken = extractSessionToken(request);
        String csrfToken = extractCsrfToken(request);
        String clientIp = getClientIp(request);

        aiVisualizerService.submitClientDecision(sessionToken, csrfToken, req, clientIp);

        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .build();
    }

    @PostMapping("/comments")
    @Operation(summary = "Submit client feedback comment", description = "Adds a client feedback comment to the review or specific concept.")
    public ResponseEntity<Void> submitComment(
            HttpServletRequest request,
            @Valid @RequestBody SubmitClientCommentRequest req
    ) {
        String sessionToken = extractSessionToken(request);
        String csrfToken = extractCsrfToken(request);
        String clientIp = getClientIp(request);

        aiVisualizerService.submitClientComment(sessionToken, csrfToken, req, clientIp);

        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .build();
    }

    private void attachReviewSessionCookie(HttpServletRequest request, HttpServletResponse response, String rawSessionToken, Instant expiresAt) {
        long maxAgeSeconds = Math.max(0, Duration.between(Instant.now(), expiresAt).getSeconds());
        boolean isSecure = request.isSecure() && authSecurityProperties.isSessionCookieSecure();

        response.addHeader(HttpHeaders.SET_COOKIE, String.format(
                "%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax%s",
                SESSION_COOKIE_NAME,
                rawSessionToken,
                maxAgeSeconds,
                isSecure ? "; Secure" : ""
        ));
    }

    private String extractSessionToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (SESSION_COOKIE_NAME.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    private String extractCsrfToken(HttpServletRequest request) {
        String token = request.getHeader("X-CSRF-Token");
        if (token == null || token.isBlank()) {
            token = request.getHeader("X-XSRF-TOKEN");
        }
        return token;
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    private String detectContentType(byte[] data) {
        if (data != null && data.length >= 8) {
            if ((data[0] & 0xFF) == 0x89 && (data[1] & 0xFF) == 0x50 &&
                (data[2] & 0xFF) == 0x4E && (data[3] & 0xFF) == 0x47) {
                return MediaType.IMAGE_PNG_VALUE;
            }
            if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
                return MediaType.IMAGE_JPEG_VALUE;
            }
            if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F') {
                return "image/webp";
            }
        }
        return MediaType.IMAGE_JPEG_VALUE;
    }
}
