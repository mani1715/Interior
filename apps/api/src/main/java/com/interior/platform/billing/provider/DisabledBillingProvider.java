package com.interior.platform.billing.provider;

import com.interior.platform.billing.domain.BillingPlanRecord;
import com.interior.platform.billing.domain.StudioSubscriptionRecord;
import com.interior.platform.billing.dto.CheckoutSessionResponse;
import com.interior.platform.designers.domain.StudioDetailRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnMissingBean(type = "com.interior.platform.billing.provider.CustomBillingProviderMarker")
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
            String cancelUrl
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
