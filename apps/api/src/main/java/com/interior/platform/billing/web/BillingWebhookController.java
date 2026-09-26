package com.interior.platform.billing.web;

import com.interior.platform.billing.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/public/webhooks/billing")
@Tag(name = "Billing Webhooks", description = "Public endpoints for payment provider webhook lifecycle events")
public class BillingWebhookController {

    private final BillingService billingService;

    public BillingWebhookController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/{provider}")
    @Operation(summary = "Handle billing provider webhook", description = "Verifies webhook signature, ensures event idempotency, and updates subscription/transaction records.")
    public ResponseEntity<Void> handleWebhook(
            @PathVariable("provider") String provider,
            @RequestBody(required = false) String rawBody,
            @RequestHeader Map<String, String> headers
    ) {
        if (rawBody == null || rawBody.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            boolean handled = billingService.handleWebhook(provider, rawBody, headers);
            if (handled) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (SecurityException e) {
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
