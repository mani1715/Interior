package com.interior.platform.leads.service;

import com.interior.platform.leads.domain.WhatsAppMessageStatus;
import java.util.Optional;

public interface WhatsAppProvider {

    String getProviderName();

    boolean isConfigured();

    SendResult sendMessage(String toPhoneNormalized, String body, String idempotencyKey);

    boolean verifyWebhookSignature(String payload, String signature);

    Optional<WebhookStatusEvent> parseWebhookStatus(String payload);

    record SendResult(
        boolean success,
        String providerMessageId,
        WhatsAppMessageStatus status,
        String errorCode,
        String errorMessage
    ) {
        public static SendResult submitted(String providerMessageId) {
            return new SendResult(true, providerMessageId, WhatsAppMessageStatus.SUBMITTED, null, null);
        }
        public static SendResult failed(String errorCode, String errorMessage) {
            return new SendResult(false, null, WhatsAppMessageStatus.FAILED, errorCode, errorMessage);
        }
    }

    record WebhookStatusEvent(
        String providerMessageId,
        WhatsAppMessageStatus status,
        String failureCode
    ) {}
}
