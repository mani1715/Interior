package com.interior.platform.billing.repository;

import com.interior.platform.billing.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillingRepository {

    Optional<BillingPlanRecord> findPlanByCode(String code);

    Optional<BillingPlanRecord> findPlanById(UUID id);

    List<BillingPlanRecord> listActivePlans();

    List<PlanEntitlementRecord> findEntitlementsByPlanId(UUID planId);

    Optional<StudioSubscriptionRecord> findActiveSubscription(UUID studioId);

    Optional<StudioSubscriptionRecord> findSubscriptionById(UUID id);

    Optional<StudioSubscriptionRecord> findSubscriptionByProviderSubscriptionId(String provider, String providerSubscriptionId);

    StudioSubscriptionRecord saveSubscription(StudioSubscriptionRecord subscription);

    BillingTransactionRecord saveTransaction(BillingTransactionRecord tx);

    List<BillingTransactionRecord> listTransactions(UUID studioId, int limit);

    boolean recordBillingEvent(BillingEventRecord event);
}
