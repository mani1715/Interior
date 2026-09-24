package com.interior.platform.leads.service;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DisabledWhatsAppProvider implements WhatsAppProvider {

    @Override
    public String getProviderName() {
        return "DISABLED";
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public SendResult sendMessage(String toPhoneNormalized, String body, String idempotencyKey) {
        return SendResult.failed("PROVIDER_NOT_CONFIGURED", "WhatsApp managed messaging is not configured on this platform instance.");
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        return false;
    }

    @Override
    public Optional<WebhookStatusEvent> parseWebhookStatus(String payload) {
        return Optional.empty();
    }
}
