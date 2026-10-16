package com.interior.platform.billing.provider;

import com.interior.platform.billing.domain.BillingPlanRecord;
import com.interior.platform.billing.domain.StudioSubscriptionRecord;
import com.interior.platform.billing.dto.CheckoutSessionResponse;
import com.interior.platform.designers.domain.StudioDetailRecord;

import java.util.Map;

/**
 * Disabled billing provider implementation.
 * Ensures zero external network requests occur when billing is disabled or pending Phase 15 activation.
 * Commercial checkout requests fail-fast with an informative error.
 * Webhook parsing fails-fast with UnsupportedOperationException to guarantee zero state mutation.
 */
public class DisabledBillingProvider implements BillingProvider {

    @Override
    public String getProviderName() {
        return "NONE";
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public CheckoutSessionResponse createCheckoutSession(
            StudioDetailRecord studio,
            BillingPlanRecord plan,
            String successUrl,
            String cancelUrl,
            String idempotencyKey
    ) {
        throw new IllegalStateException("Billing provider is not configured. Commercial checkout is disabled.");
    }

    @Override
    public void cancelSubscription(StudioSubscriptionRecord subscription, boolean atPeriodEnd) {
        // No-op for unconfigured provider
    }

    @Override
    public WebhookEvent parseWebhook(String rawPayload, Map<String, String> headers) {
        throw new UnsupportedOperationException("Webhooks are disabled when billing provider is not configured.");
    }
}
