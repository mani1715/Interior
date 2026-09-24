package com.interior.platform.leads.web;

import com.interior.platform.leads.service.WhatsAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/public/webhooks/whatsapp")
@Tag(name = "WhatsApp Webhook", description = "Provider webhook receiver for WhatsApp message status callbacks and reconciliation")
public class WhatsAppWebhookController {

    private final WhatsAppService whatsAppService;

    public WhatsAppWebhookController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @PostMapping
    @Operation(summary = "WhatsApp status webhook callback", description = "Receives delivery status updates from the configured WhatsApp provider and reconciles message state monotonically.")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload
    ) {
        boolean accepted = whatsAppService.handleWebhook(payload, signature);
        if (!accepted) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "invalid_signature"));
        }
        return ResponseEntity.ok(Map.of("status", "acknowledged"));
    }
}
