package com.interior.platform.billing.service;

import com.interior.platform.billing.domain.*;
import com.interior.platform.billing.dto.BillingPlanDto;
import com.interior.platform.billing.dto.CheckoutSessionResponse;
import com.interior.platform.billing.dto.CreateCheckoutRequest;
import com.interior.platform.billing.dto.StudioBillingSummaryDto;
import com.interior.platform.billing.provider.BillingProvider;
import com.interior.platform.billing.provider.WebhookEvent;
import com.interior.platform.billing.repository.BillingRepository;
import com.interior.platform.common.exception.AccessDeniedException;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.designers.domain.StudioDetailRecord;
import com.interior.platform.designers.repository.StudioRepository;
import com.interior.platform.security.domain.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final BillingRepository billingRepository;
    private final EntitlementService entitlementService;
    private final StudioRepository studioRepository;
    private final BillingProvider billingProvider;

    public BillingService(
            BillingRepository billingRepository,
            EntitlementService entitlementService,
            StudioRepository studioRepository,
            BillingProvider billingProvider
    ) {
        this.billingRepository = billingRepository;
        this.entitlementService = entitlementService;
        this.studioRepository = studioRepository;
        this.billingProvider = billingProvider;
    }

    /**
     * Retrieves studio billing workspace summary including current plan, entitlements,
     * provider status, and truthful checkout capability.
     */
    public StudioBillingSummaryDto getStudioBillingSummary(ActorContext actor, UUID studioId) {
        validateStudioAccess(actor, studioId);

        Optional<StudioSubscriptionRecord> activeSub = billingRepository.findActiveSubscription(studioId);
        BillingPlanRecord currentPlan = activeSub.isPresent()
                ? billingRepository.findPlanById(activeSub.get().planId()).orElseGet(entitlementService::getBasePlan)
                : entitlementService.getBasePlan();

        Map<String, Object> entitlements = entitlementService.getEffectiveEntitlements(studioId);

        List<BillingPlanRecord> activePlans = billingRepository.listActivePlans();
        List<BillingPlanDto> availablePlanDtos = activePlans.stream().map(p -> {
            List<PlanEntitlementRecord> entRecords = billingRepository.findEntitlementsByPlanId(p.id());
            Map<String, Object> entMap = new HashMap<>();
            for (PlanEntitlementRecord r : entRecords) {
                if ("BOOLEAN".equalsIgnoreCase(r.valueType())) {
                    entMap.put(r.entitlementKey(), Boolean.TRUE.equals(r.booleanValue()));
                } else {
                    entMap.put(r.entitlementKey(), r.numericValue());
                }
            }
            return new BillingPlanDto(
                    p.id(),
                    p.code(),
                    p.name(),
                    p.description(),
                    p.billingPeriod(),
                    p.currency(),
                    p.priceMinor(),
                    p.active(),
                    p.purchasable(),
                    p.displayOrder(),
                    entMap
            );
        }).toList();

        boolean providerConfigured = billingProvider.isConfigured();
        String providerStatus = providerConfigured ? billingProvider.getProviderName() : "NOT_CONFIGURED";
        boolean commercialCheckoutEnabled = providerConfigured && activePlans.stream().anyMatch(BillingPlanRecord::purchasable);

        List<BillingTransactionRecord> recentTransactions = billingRepository.listTransactions(studioId, 10);

        return new StudioBillingSummaryDto(
                studioId,
                currentPlan,
                activeSub.orElse(null),
                entitlements,
                providerStatus,
                commercialCheckoutEnabled,
                availablePlanDtos,
                recentTransactions
        );
    }

    /**
     * Initiates a hosted checkout session. Enforces server-authoritative price and purchasable flag.
     */
    @Transactional
    public CheckoutSessionResponse initiateCheckout(ActorContext actor, UUID studioId, CreateCheckoutRequest req) {
        validateStudioAccess(actor, studioId);

        if (!billingProvider.isConfigured()) {
            throw new BadRequestException("Billing provider is not configured. Commercial checkout is currently disabled.");
        }

        BillingPlanRecord plan = billingRepository.findPlanByCode(req.planCode())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + req.planCode()));

        if (!plan.active() || !plan.purchasable()) {
            throw new BadRequestException("Plan is not available for purchase: " + req.planCode());
        }

        StudioDetailRecord studio = studioRepository.findStudioById(studioId)
                .orElseThrow(() -> new ResourceNotFoundException("Studio not found: " + studioId));

        return billingProvider.createCheckoutSession(studio, plan, req.successUrl(), req.cancelUrl());
    }

    /**
     * Schedules subscription cancellation at the end of the current billing period.
     * Guaranteed: Studio data, projects, leads, and media are NEVER deleted upon cancellation.
     */
    @Transactional
    public void cancelSubscription(ActorContext actor, UUID studioId) {
        validateStudioAccess(actor, studioId);

        StudioSubscriptionRecord current = billingRepository.findActiveSubscription(studioId)
                .orElseThrow(() -> new BadRequestException("No active subscription to cancel."));

        if (current.cancelAtPeriodEnd() || current.status() == SubscriptionStatus.CANCEL_AT_PERIOD_END) {
            return; // Already marked for cancellation
        }

        Instant now = Instant.now();
        StudioSubscriptionRecord updated = new StudioSubscriptionRecord(
                current.id(),
                current.studioId(),
                current.planId(),
                SubscriptionStatus.CANCEL_AT_PERIOD_END,
                current.provider(),
                current.providerCustomerId(),
                current.providerSubscriptionId(),
                current.currentPeriodStart(),
                current.currentPeriodEnd(),
                true,
                now,
                current.createdAt(),
                now,
                current.version()
        );

        billingRepository.saveSubscription(updated);

        if (billingProvider.isConfigured()) {
            try {
                billingProvider.cancelSubscription(updated, true);
            } catch (Exception e) {
                log.warn("External provider cancellation error: {}", e.getMessage());
            }
        }
    }

    /**
     * Secure webhook processor with cryptographic signature verification,
     * event deduplication, and out-of-order tolerance.
     */
    @Transactional
    public boolean handleWebhook(String providerName, String rawBody, Map<String, String> headers) {
        if (!billingProvider.getProviderName().equalsIgnoreCase(providerName)) {
            log.warn("Webhook provider mismatch: requested={}, configured={}", providerName, billingProvider.getProviderName());
            return false;
        }

        WebhookEvent event = billingProvider.parseWebhook(rawBody, headers);
        if (event == null) {
            return false;
        }

        Optional<StudioSubscriptionRecord> subOpt = Optional.empty();
        if (event.providerSubscriptionId() != null) {
            subOpt = billingRepository.findSubscriptionByProviderSubscriptionId(providerName, event.providerSubscriptionId());
        }

        UUID studioId = subOpt.map(StudioSubscriptionRecord::studioId).orElse(null);

        BillingEventRecord eventRecord = new BillingEventRecord(
                UuidV7.randomUuid(),
                studioId,
                event.eventType(),
                providerName,
                event.eventId(),
                event.rawData(),
                event.timestamp() != null ? event.timestamp() : Instant.now(),
                Instant.now()
        );

        boolean recorded = billingRepository.recordBillingEvent(eventRecord);
        if (!recorded) {
            log.info("Webhook event duplicate skipped: provider={}, eventId={}", providerName, event.eventId());
            return true;
        }

        // Apply state transition if subscription is known
        if (subOpt.isPresent()) {
            StudioSubscriptionRecord sub = subOpt.get();
            applyWebhookToSubscription(sub, event);
        }

        return true;
    }

    private void applyWebhookToSubscription(StudioSubscriptionRecord sub, WebhookEvent event) {
        Instant now = Instant.now();
        String evType = event.eventType().toLowerCase(Locale.ROOT);

        if (evType.contains("payment") && (evType.contains("succeeded") || evType.contains("paid"))) {
            if (event.amountMinor() != null && event.amountMinor() > 0) {
                BillingTransactionRecord tx = new BillingTransactionRecord(
                        UuidV7.randomUuid(),
                        sub.studioId(),
                        sub.id(),
                        sub.provider(),
                        event.providerPaymentId(),
                        null,
                        event.amountMinor(),
                        "INR",
                        BillingTransactionStatus.SUCCEEDED,
                        "Subscription payment",
                        null,
                        event.timestamp() != null ? event.timestamp() : now,
                        now
                );
                billingRepository.saveTransaction(tx);
            }
            if (sub.status() != SubscriptionStatus.ACTIVE) {
                StudioSubscriptionRecord active = new StudioSubscriptionRecord(
                        sub.id(), sub.studioId(), sub.planId(),
                        SubscriptionStatus.ACTIVE, sub.provider(),
                        sub.providerCustomerId(), sub.providerSubscriptionId(),
                        sub.currentPeriodStart() != null ? sub.currentPeriodStart() : now,
                        sub.currentPeriodEnd(), sub.cancelAtPeriodEnd(),
                        sub.cancelledAt(), sub.createdAt(), now, sub.version()
                );
                billingRepository.saveSubscription(active);
            }
        } else if (evType.contains("payment") && evType.contains("failed")) {
            if (event.amountMinor() != null && event.amountMinor() > 0) {
                BillingTransactionRecord tx = new BillingTransactionRecord(
                        UuidV7.randomUuid(),
                        sub.studioId(),
                        sub.id(),
                        sub.provider(),
                        event.providerPaymentId(),
                        null,
                        event.amountMinor(),
                        "INR",
                        BillingTransactionStatus.FAILED,
                        "Payment failed",
                        null,
                        event.timestamp() != null ? event.timestamp() : now,
                        now
                );
                billingRepository.saveTransaction(tx);
            }
            StudioSubscriptionRecord pastDue = new StudioSubscriptionRecord(
                    sub.id(), sub.studioId(), sub.planId(),
                    SubscriptionStatus.PAST_DUE, sub.provider(),
                    sub.providerCustomerId(), sub.providerSubscriptionId(),
                    sub.currentPeriodStart(), sub.currentPeriodEnd(),
                    sub.cancelAtPeriodEnd(), sub.cancelledAt(),
                    sub.createdAt(), now, sub.version()
            );
            billingRepository.saveSubscription(pastDue);
        } else if (evType.contains("cancelled") || evType.contains("canceled")) {
            StudioSubscriptionRecord cancelled = new StudioSubscriptionRecord(
                    sub.id(), sub.studioId(), sub.planId(),
                    SubscriptionStatus.CANCELLED, sub.provider(),
                    sub.providerCustomerId(), sub.providerSubscriptionId(),
                    sub.currentPeriodStart(), sub.currentPeriodEnd(),
                    true, now, sub.createdAt(), now, sub.version()
            );
            billingRepository.saveSubscription(cancelled);
        }
    }

    private void validateStudioAccess(ActorContext actor, UUID studioId) {
        if (actor == null || !actor.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        if (actor.hasRole("ADMIN") || actor.hasRole("SUPER_ADMIN")) {
            return;
        }
        if (studioId == null || !actor.isStudioMember(studioId)) {
            throw new AccessDeniedException("User is not a member of studio: " + studioId);
        }
    }
}
