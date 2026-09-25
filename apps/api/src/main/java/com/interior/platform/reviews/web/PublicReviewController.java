package com.interior.platform.reviews.web;

import com.interior.platform.reviews.dto.*;
import com.interior.platform.reviews.service.ReviewInvitationService;
import com.interior.platform.reviews.service.ReviewService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/public")
@Tag(name = "Public Reviews", description = "Public-facing endpoints for studio review browsing, invitation token exchange, and submission")
public class PublicReviewController {

    public static final String SESSION_COOKIE_NAME = "review_session";

    private final ReviewInvitationService invitationService;
    private final ReviewService reviewService;

    public PublicReviewController(ReviewInvitationService invitationService, ReviewService reviewService) {
        this.invitationService = invitationService;
        this.reviewService = reviewService;
    }

    @GetMapping("/studios/{slug}/reviews")
    @Operation(summary = "Get published studio reviews", description = "Publicly browsable paginated list of reviews and aggregate metrics for a studio.")
    public ResponseEntity<PublicStudioReviewsResponse> getPublicReviews(
            @PathVariable("slug") String studioSlug,
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit,
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset
    ) {
        PublicStudioReviewsResponse response = reviewService.getPublicReviews(studioSlug, limit, offset);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60, s-maxage=300, stale-while-revalidate=600")
                .body(response);
    }

    @PostMapping("/reviews/exchange-token")
    @Operation(summary = "Exchange raw review token for scoped session", description = "Exchanges a raw invitation token for an HttpOnly scoped session cookie and redirects to token-free submission URL.")
    public ResponseEntity<ExchangeReviewTokenResponse> exchangeToken(
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @RequestBody ExchangeReviewTokenRequest req
    ) {
        ReviewInvitationService.ExchangeReviewSessionResult result = invitationService.exchangeToken(req.token());

        // Attach HttpOnly, Secure, SameSite=Lax session cookie
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE_NAME, result.sessionToken())
                .httpOnly(true)
                .secure(request.isSecure())
                .path("/")
                .maxAge(Duration.ofHours(24))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        ExchangeReviewTokenResponse body = new ExchangeReviewTokenResponse(
                result.invitationId(),
                result.csrfToken(),
                "/review/submit",
                result.studioName(),
                result.studioSlug(),
                result.clientFirstName()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    @PostMapping("/reviews/submit")
    @Operation(summary = "Submit genuine client review", description = "Submits client rating and review content using scoped review session.")
    public ResponseEntity<PublicStudioReviewDto> submitReview(
            HttpServletRequest request,
            @Valid @RequestBody SubmitReviewRequest req,
            @CookieValue(value = SESSION_COOKIE_NAME, required = false) String sessionCookie,
            @RequestHeader(value = "X-Review-Session", required = false) String sessionHeader
    ) {
        String sessionToken = resolveSessionToken(sessionCookie, sessionHeader, request);
        PublicStudioReviewDto submitted = reviewService.submitReview(sessionToken, req);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(submitted);
    }

    @PostMapping("/reviews/{id}/report")
    @Operation(summary = "Report review for moderation", description = "Allows visitors or authenticated users to flag a review for policy violations.")
    public ResponseEntity<Void> reportReview(
            HttpServletRequest request,
            @PathVariable("id") UUID reviewId,
            @Valid @RequestBody ReviewReportRequest req
    ) {
        ActorContext actor = extractActor(request);
        UUID reporterUserId = (actor != null && actor.isAuthenticated()) ? actor.userId() : null;
        String clientIp = getClientIp(request);

        reviewService.reportReview(reviewId, req, clientIp, reporterUserId);
        return ResponseEntity.noContent().build();
    }

    private String resolveSessionToken(String cookie, String header, HttpServletRequest request) {
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        if (cookie != null && !cookie.isBlank()) {
            return cookie.trim();
        }
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (SESSION_COOKIE_NAME.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    private ActorContext extractActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return actor != null ? actor : ActorContext.anonymous();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
