package com.interior.platform.billing.dto;

public record CheckoutSessionResponse(
        String sessionId,
        String checkoutUrl,
        String provider,
        String planCode,
        long amountMinor,
        String currency
) {}
