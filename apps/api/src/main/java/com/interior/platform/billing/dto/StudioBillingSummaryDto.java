package com.interior.platform.billing.dto;

import com.interior.platform.billing.domain.BillingPlanRecord;
import com.interior.platform.billing.domain.BillingTransactionRecord;
import com.interior.platform.billing.domain.StudioSubscriptionRecord;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record StudioBillingSummaryDto(
        UUID studioId,
        BillingPlanRecord currentPlan,
        StudioSubscriptionRecord activeSubscription,
        Map<String, Object> effectiveEntitlements,
        String billingProviderStatus,
        boolean commercialCheckoutEnabled,
        List<BillingPlanDto> availablePlans,
        List<BillingTransactionRecord> recentTransactions
) {}
