package com.interior.platform.billing.provider;

import com.interior.platform.billing.domain.BillingPlanRecord;
import com.interior.platform.billing.domain.StudioSubscriptionRecord;
import com.interior.platform.billing.dto.CheckoutSessionResponse;
import com.interior.platform.designers.domain.StudioDetailRecord;

import java.util.Map;

public interface BillingProvider {

    /**
     * Unique identifier for this provider (e.g. 'NONE', 'RAZORPAY', 'STRIPE', 'FAKE').
     */
    String getProviderName();

    /**
     * Whether this billing provider is actively configured with valid API keys and webhooks.
     */
    boolean isConfigured();

    /**
     * Initiates a hosted checkout session with provider-managed checkout UI.
     */
    CheckoutSessionResponse createCheckoutSession(
            StudioDetailRecord studio,
            BillingPlanRecord plan,
            String successUrl,
            String cancelUrl
    );

    /**
     * Cancels an active external subscription at the billing provider.
     */
    void cancelSubscription(StudioSubscriptionRecord subscription, boolean atPeriodEnd);

    /**
     * Validates cryptographic signature and parses raw webhook payload.
     * Throws SecurityException or IllegalArgumentException on signature mismatch.
     */
    WebhookEvent parseWebhook(String rawPayload, Map<String, String> headers);
}
