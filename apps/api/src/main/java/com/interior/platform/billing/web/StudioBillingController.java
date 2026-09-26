package com.interior.platform.billing.web;

import com.interior.platform.billing.dto.CheckoutSessionResponse;
import com.interior.platform.billing.dto.CreateCheckoutRequest;
import com.interior.platform.billing.dto.StudioBillingSummaryDto;
import com.interior.platform.billing.service.BillingService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/studio/billing")
@Tag(name = "Studio Billing & Subscriptions", description = "Workspace subscription, plan entitlements, and billing management")
public class StudioBillingController {

    private final BillingService billingService;

    public StudioBillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    @Operation(summary = "Get studio billing summary", description = "Retrieves active plan, effective entitlements, provider configuration status, and recent transactions.")
    public ResponseEntity<StudioBillingSummaryDto> getBillingSummary(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam, actor);

        StudioBillingSummaryDto summary = billingService.getStudioBillingSummary(actor, studioId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-cache, no-store, must-revalidate")
                .body(summary);
    }

    @PostMapping("/checkout")
    @Operation(summary = "Initiate subscription checkout", description = "Creates a secure checkout session with the billing provider.")
    public ResponseEntity<CheckoutSessionResponse> initiateCheckout(
            HttpServletRequest request,
            @Valid @RequestBody CreateCheckoutRequest checkoutReq,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam, actor);

        CheckoutSessionResponse response = billingService.initiateCheckout(actor, studioId, checkoutReq);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store, max-age=0")
                .body(response);
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel subscription at period end", description = "Schedules subscription cancellation at the end of the current billing cycle. Studio data is never deleted.")
    public ResponseEntity<Void> cancelSubscription(
            HttpServletRequest request,
            @RequestHeader(value = "X-Studio-Id", required = false) String studioIdHeader,
            @RequestParam(value = "studioId", required = false) UUID studioIdParam
    ) {
        ActorContext actor = extractActor(request);
        UUID studioId = resolveRequestedStudioId(studioIdHeader, studioIdParam, actor);

        billingService.cancelSubscription(actor, studioId);

        return ResponseEntity.noContent()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-cache, no-store, must-revalidate")
                .build();
    }

    private ActorContext extractActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return actor != null ? actor : ActorContext.anonymous();
    }

    private UUID resolveRequestedStudioId(String header, UUID param, ActorContext actor) {
        if (param != null) return param;
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (actor != null && actor.activeStudioId() != null) {
            return actor.activeStudioId();
        }
        return null;
    }
}
