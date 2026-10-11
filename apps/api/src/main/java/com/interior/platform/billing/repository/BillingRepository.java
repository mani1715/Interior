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

    Optional<StudioSubscriptionRecord> findLatestSubscription(UUID studioId);

    Optional<StudioSubscriptionRecord> findSubscriptionById(UUID id);

    Optional<StudioSubscriptionRecord> findSubscriptionByProviderSubscriptionId(String provider, String providerSubscriptionId);

    StudioSubscriptionRecord saveSubscription(StudioSubscriptionRecord subscription);

    BillingTransactionRecord saveTransaction(BillingTransactionRecord tx);

    List<BillingTransactionRecord> listTransactions(UUID studioId, int limit);

    List<BillingTransactionRecord> listTransactions(UUID studioId, int limit, int offset);

    boolean recordBillingEvent(BillingEventRecord event);

    BillingCheckoutSessionRecord saveCheckoutSession(BillingCheckoutSessionRecord session);

    Optional<BillingCheckoutSessionRecord> findCheckoutSessionById(UUID id);

    Optional<BillingCheckoutSessionRecord> findCheckoutSessionByIdempotencyKey(UUID studioId, String idempotencyKey);

    Optional<BillingCheckoutSessionRecord> findCheckoutSessionByProviderSessionId(String provider, String providerSessionId);

    void updateCheckoutSessionStatus(UUID id, String status, String providerSessionId);
}
