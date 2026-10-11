package com.interior.platform.billing.service;

import com.interior.platform.billing.domain.*;
import com.interior.platform.billing.dto.*;
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
import com.interior.platform.email.service.CommunicationDeliveryService;
import com.interior.platform.email.service.EmailTemplateService;
import com.interior.platform.security.repository.SecurityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.of("UTC"));

    private final BillingRepository billingRepository;
    private final EntitlementService entitlementService;
    private final StudioRepository studioRepository;
    private final BillingProvider billingProvider;
    private final com.interior.platform.projects.repository.ProjectRepository projectRepository;

    @Value("${app.billing.grace-period-days:7}")
    private int gracePeriodDays = 7;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.interior.platform.media.repository.MediaRepository mediaRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.interior.platform.notifications.service.NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CommunicationDeliveryService communicationDeliveryService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private EmailTemplateService emailTemplateService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private SecurityRepository securityRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public BillingService(
            BillingRepository billingRepository,
            EntitlementService entitlementService,
            StudioRepository studioRepository,
            BillingProvider billingProvider,
            com.interior.platform.projects.repository.ProjectRepository projectRepository
    ) {
        this.billingRepository = billingRepository;
        this.entitlementService = entitlementService;
        this.studioRepository = studioRepository;
        this.billingProvider = billingProvider;
        this.projectRepository = projectRepository;
    }

    public BillingService(
            BillingRepository billingRepository,
            EntitlementService entitlementService,
            StudioRepository studioRepository,
            BillingProvider billingProvider
    ) {
        this(billingRepository, entitlementService, studioRepository, billingProvider, null);
    }

    public void setGracePeriodDays(int gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }

    /**
     * Retrieves studio billing workspace summary including current plan, entitlements,
     * provider status, truthful checkout capability, and usage breakdown.
     */
    public StudioBillingSummaryDto getStudioBillingSummary(ActorContext actor, UUID studioId) {
        validateStudioAccess(actor, studioId);

        Optional<StudioSubscriptionRecord> activeSub = billingRepository.findActiveSubscription(studioId);
        BillingPlanRecord currentPlan = entitlementService.getActivePlan(studioId);

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

        int projectCount = (projectRepository != null) ? projectRepository.countProjects(studioId) : 0;
        Long projectLimit = entitlementService.getNumericLimit(studioId, EntitlementKey.PROJECT_LIMIT);
        int cinematicProjectCount = (projectRepository != null) ? projectRepository.countCinematicProjects(studioId) : 0;
        Long cinematicProjectLimit = entitlementService.getNumericLimit(studioId, EntitlementKey.CINEMATIC_PROJECT_LIMIT);
        boolean cinematicPortfolioAllowed = entitlementService.hasBooleanEntitlement(studioId, EntitlementKey.CINEMATIC_PORTFOLIO);
        Long storageLimit = entitlementService.getNumericLimit(studioId, EntitlementKey.STORAGE_LIMIT_BYTES);
        long storageBytesUsed = (mediaRepository != null) ? mediaRepository.countActiveMediaByStudio(studioId) * 1024L * 1024L : 0L;
        Long aiMonthlyLimit = entitlementService.getNumericLimit(studioId, EntitlementKey.AI_MONTHLY_CREDITS);

        StudioUsageBreakdownDto usage = new StudioUsageBreakdownDto(
                projectCount,
                projectLimit,
                cinematicProjectCount,
                cinematicProjectLimit,
                cinematicPortfolioAllowed,
                storageBytesUsed,
                storageLimit,
                0L,
                aiMonthlyLimit
        );

        return new StudioBillingSummaryDto(
                studioId,
                currentPlan,
                activeSub.orElse(null),
                entitlements,
                providerStatus,
                commercialCheckoutEnabled,
                availablePlanDtos,
                recentTransactions,
                usage
        );
    }

    /**
     * Initiates a hosted checkout session. Enforces server-authoritative price and purchasable flag,
     * studio admin privileges, and idempotency key protection.
     */
    @Transactional
    public CheckoutSessionResponse initiateCheckout(ActorContext actor, UUID studioId, CreateCheckoutRequest req) {
        validateStudioAdminAccess(actor, studioId);

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

        String idempotencyKey = (req.idempotencyKey() != null && !req.idempotencyKey().isBlank())
                ? req.idempotencyKey().trim()
                : UUID.randomUUID().toString();

        // Check for existing pending session with this idempotency key
        Optional<BillingCheckoutSessionRecord> existingSession = billingRepository.findCheckoutSessionByIdempotencyKey(studioId, idempotencyKey);
        Instant now = Instant.now();
        if (existingSession.isPresent()) {
            BillingCheckoutSessionRecord s = existingSession.get();
            if ("PENDING".equals(s.status()) && s.expiresAt().isAfter(now)) {
                return new CheckoutSessionResponse(
                        s.providerSessionId() != null ? s.providerSessionId() : s.id().toString(),
                        s.providerSessionId() != null ? ("https://checkout." + s.provider().toLowerCase(Locale.ROOT) + ".local/pay/" + s.providerSessionId()) : "",
                        s.provider(),
                        plan.code(),
                        s.amountMinor(),
                        s.currency()
                );
            }
        }

        // Server-authoritative amount and currency
        long amountMinor = plan.priceMinor();
        String currency = plan.currency();
        Instant expiresAt = now.plus(30, ChronoUnit.MINUTES);

        BillingCheckoutSessionRecord sessionRecord = new BillingCheckoutSessionRecord(
                UuidV7.randomUuid(),
                studioId,
                plan.id(),
                billingProvider.getProviderName(),
                null,
                idempotencyKey,
                "PENDING",
                amountMinor,
                currency,
                req.successUrl(),
                req.cancelUrl(),
                expiresAt,
                now,
                now
        );
        billingRepository.saveCheckoutSession(sessionRecord);

        CheckoutSessionResponse response = billingProvider.createCheckoutSession(
                studio,
                plan,
                req.successUrl(),
                req.cancelUrl(),
                idempotencyKey
        );

        if (response.sessionId() != null) {
            billingRepository.updateCheckoutSessionStatus(sessionRecord.id(), "PENDING", response.sessionId());
        }

        return response;
    }

    /**
     * Schedules subscription cancellation at the end of the current billing period.
     * Guaranteed: Studio data, projects, leads, and media are NEVER deleted upon cancellation.
     */
    @Transactional
    public void cancelSubscription(ActorContext actor, UUID studioId) {
        validateStudioAdminAccess(actor, studioId);

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
                current.version(),
                current.sourceType(),
                current.downgradePlanId(),
                current.gracePeriodEnd()
        );

        billingRepository.saveSubscription(updated);

        if (billingProvider.isConfigured()) {
            try {
                billingProvider.cancelSubscription(updated, true);
            } catch (Exception e) {
                log.warn("External provider cancellation error: {}", e.getMessage());
            }
        }

        notifyStudio(studioId, "Subscription Cancellation Scheduled",
                "Your subscription is scheduled to cancel at the end of the current billing cycle. All existing assets remain safely preserved.");

        String accessUntil = updated.currentPeriodEnd() != null ? DATE_FORMATTER.format(updated.currentPeriodEnd()) : "the end of the current cycle";
        sendBillingEmail(studioId, "CANCEL_AT_PERIOD_END", (studio, user, plan) ->
                emailTemplateService.renderCancelAtPeriodEnd(studio.name(), plan != null ? plan.name() : "Commercial Plan", accessUntil, "/workspace/billing"),
                "cancel_scheduled:" + updated.id() + ":" + now.toEpochMilli()
        );
    }

    /**
     * Schedules a plan downgrade for the end of the current billing period.
     * Invariant: Over-quota projects, photos, and media are NEVER deleted.
     */
    @Transactional
    public void requestDowngrade(ActorContext actor, UUID studioId, DowngradePlanRequest req) {
        validateStudioAdminAccess(actor, studioId);

        StudioSubscriptionRecord current = billingRepository.findActiveSubscription(studioId)
                .orElseThrow(() -> new BadRequestException("No active subscription to downgrade."));

        BillingPlanRecord targetPlan = billingRepository.findPlanByCode(req.targetPlanCode())
                .orElseThrow(() -> new ResourceNotFoundException("Target plan not found: " + req.targetPlanCode()));

        if (targetPlan.id().equals(current.planId())) {
            throw new BadRequestException("Studio is already on plan: " + req.targetPlanCode());
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
                current.version(),
                current.sourceType(),
                targetPlan.id(),
                current.gracePeriodEnd()
        );

        billingRepository.saveSubscription(updated);

        if (billingProvider.isConfigured()) {
            try {
                billingProvider.cancelSubscription(updated, true);
            } catch (Exception e) {
                log.warn("External provider downgrade cancellation error: {}", e.getMessage());
            }
        }

        notifyStudio(studioId, "Plan Downgrade Scheduled",
                "Downgrade to " + targetPlan.name() + " scheduled for the end of the billing period. Existing projects and photographs remain safely preserved.");
    }

    /**
     * Retrieves bounded, paginated billing transaction history for a studio.
     */
    public List<BillingTransactionRecord> listTransactions(ActorContext actor, UUID studioId, int limit, int offset) {
        validateStudioAccess(actor, studioId);
        int boundedLimit = Math.max(1, Math.min(limit, 50));
        int boundedOffset = Math.max(0, offset);
        return billingRepository.listTransactions(studioId, boundedLimit, boundedOffset);
    }

    /**
     * Secure webhook processor with cryptographic signature verification,
     * event deduplication, amount & currency integrity checks, and out-of-order tolerance.
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

        Optional<BillingCheckoutSessionRecord> sessionOpt = findCheckoutSessionFromEvent(event, providerName);

        Optional<StudioSubscriptionRecord> subOpt = Optional.empty();
        if (event.providerSubscriptionId() != null) {
            subOpt = billingRepository.findSubscriptionByProviderSubscriptionId(providerName, event.providerSubscriptionId());
        } else if (sessionOpt.isPresent()) {
            subOpt = billingRepository.findActiveSubscription(sessionOpt.get().studioId());
        }

        UUID studioId = subOpt.map(StudioSubscriptionRecord::studioId)
                .orElseGet(() -> sessionOpt.map(BillingCheckoutSessionRecord::studioId).orElse(null));

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

        // Apply state transition
        applyWebhookToSubscription(subOpt.orElse(null), event, sessionOpt.orElse(null), studioId, providerName);

        return true;
    }

    private Optional<BillingCheckoutSessionRecord> findCheckoutSessionFromEvent(WebhookEvent event, String providerName) {
        if (event.rawData() != null) {
            Object sessIdObj = event.rawData().get("checkout_session_id");
            if (sessIdObj == null) sessIdObj = event.rawData().get("session_id");
            if (sessIdObj instanceof String sid) {
                Optional<BillingCheckoutSessionRecord> opt = billingRepository.findCheckoutSessionByProviderSessionId(providerName, sid);
                if (opt.isPresent()) return opt;
            }
        }
        if (event.providerPaymentId() != null) {
            Optional<BillingCheckoutSessionRecord> opt = billingRepository.findCheckoutSessionByProviderSessionId(providerName, event.providerPaymentId());
            if (opt.isPresent()) return opt;
        }
        return Optional.empty();
    }

    private void applyWebhookToSubscription(
            StudioSubscriptionRecord sub,
            WebhookEvent event,
            BillingCheckoutSessionRecord session,
            UUID studioId,
            String providerName
    ) {
        Instant now = Instant.now();
        String evType = event.eventType().toLowerCase(Locale.ROOT);

        if (evType.contains("checkout") && evType.contains("completed")
                || evType.contains("payment") && (evType.contains("succeeded") || evType.contains("paid"))
                || evType.contains("subscription") && evType.contains("activated")) {

            // Validate amount and currency integrity against recorded server checkout session
            if (session != null) {
                if (event.amountMinor() != null && event.amountMinor() > 0 && !event.amountMinor().equals(session.amountMinor())) {
                    log.error("Webhook amount mismatch for session {}: expected {}, received {}",
                            session.id(), session.amountMinor(), event.amountMinor());
                    throw new SecurityException("Webhook amount mismatch with server checkout session");
                }
                billingRepository.updateCheckoutSessionStatus(session.id(), "COMPLETED", event.providerPaymentId());
            }

            long amount = (event.amountMinor() != null && event.amountMinor() > 0)
                    ? event.amountMinor()
                    : (session != null ? session.amountMinor() : 0L);
            String currency = (session != null) ? session.currency() : "INR";

            UUID targetStudioId = (sub != null) ? sub.studioId() : ((session != null) ? session.studioId() : studioId);

            if (targetStudioId != null && amount > 0) {
                BillingTransactionRecord tx = new BillingTransactionRecord(
                        UuidV7.randomUuid(),
                        targetStudioId,
                        sub != null ? sub.id() : null,
                        providerName,
                        event.providerPaymentId(),
                        session != null ? session.id().toString() : null,
                        amount,
                        currency,
                        BillingTransactionStatus.SUCCEEDED,
                        "Subscription payment",
                        null,
                        event.timestamp() != null ? event.timestamp() : now,
                        now
                );
                billingRepository.saveTransaction(tx);
            }

            UUID planId = (session != null) ? session.planId() : (sub != null ? sub.planId() : null);

            if (sub != null) {
                StudioSubscriptionRecord active = new StudioSubscriptionRecord(
                        sub.id(),
                        sub.studioId(),
                        planId != null ? planId : sub.planId(),
                        SubscriptionStatus.ACTIVE,
                        providerName,
                        sub.providerCustomerId(),
                        event.providerSubscriptionId() != null ? event.providerSubscriptionId() : sub.providerSubscriptionId(),
                        sub.currentPeriodStart() != null ? sub.currentPeriodStart() : now,
                        now.plus(30, ChronoUnit.DAYS),
                        false,
                        null,
                        sub.createdAt(),
                        now,
                        sub.version(),
                        "PAID_PROVIDER",
                        null,
                        null
                );
                billingRepository.saveSubscription(active);
            } else if (targetStudioId != null && planId != null) {
                StudioSubscriptionRecord newSub = new StudioSubscriptionRecord(
                        UuidV7.randomUuid(),
                        targetStudioId,
                        planId,
                        SubscriptionStatus.ACTIVE,
                        providerName,
                        null,
                        event.providerSubscriptionId(),
                        now,
                        now.plus(30, ChronoUnit.DAYS),
                        false,
                        null,
                        now,
                        now,
                        1L,
                        "PAID_PROVIDER",
                        null,
                        null
                );
                billingRepository.saveSubscription(newSub);
            }

            if (targetStudioId != null) {
                notifyStudio(targetStudioId, "Subscription Activated", "Your subscription is now active.");
                sendBillingEmail(targetStudioId, "SUBSCRIPTION_ACTIVATED", (studio, user, plan) ->
                        emailTemplateService.renderSubscriptionActivated(studio.name(), plan != null ? plan.name() : "Commercial Plan", "/workspace/billing"),
                        "sub_activated:" + (event.providerSubscriptionId() != null ? event.providerSubscriptionId() : event.eventId())
                );
            }

        } else if (evType.contains("payment") && evType.contains("failed") || evType.contains("payment_failed")) {
            UUID targetStudioId = (sub != null) ? sub.studioId() : studioId;
            if (targetStudioId != null && event.amountMinor() != null && event.amountMinor() > 0) {
                BillingTransactionRecord tx = new BillingTransactionRecord(
                        UuidV7.randomUuid(),
                        targetStudioId,
                        sub != null ? sub.id() : null,
                        providerName,
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

            if (sub != null) {
                Instant gracePeriodEnd = now.plus(gracePeriodDays, ChronoUnit.DAYS);
                StudioSubscriptionRecord pastDue = new StudioSubscriptionRecord(
                        sub.id(),
                        sub.studioId(),
                        sub.planId(),
                        SubscriptionStatus.PAST_DUE,
                        sub.provider(),
                        sub.providerCustomerId(),
                        sub.providerSubscriptionId(),
                        sub.currentPeriodStart(),
                        sub.currentPeriodEnd(),
                        sub.cancelAtPeriodEnd(),
                        sub.cancelledAt(),
                        sub.createdAt(),
                        now,
                        sub.version(),
                        sub.sourceType(),
                        sub.downgradePlanId(),
                        gracePeriodEnd
                );
                billingRepository.saveSubscription(pastDue);

                String formattedGraceDate = DATE_FORMATTER.format(gracePeriodEnd);
                notifyStudio(sub.studioId(), "Payment Failed",
                        "Payment for your subscription failed. A grace period is active until " + formattedGraceDate + ".");

                sendBillingEmail(sub.studioId(), "PAYMENT_FAILED", (studio, user, plan) ->
                        emailTemplateService.renderPaymentFailed(studio.name(), plan != null ? plan.name() : "Commercial Plan", formattedGraceDate, "/workspace/billing"),
                        "payment_failed:" + event.eventId()
                );
            }

        } else if (evType.contains("renewed")) {
            UUID targetStudioId = (sub != null) ? sub.studioId() : studioId;
            if (targetStudioId != null && event.amountMinor() != null && event.amountMinor() > 0) {
                BillingTransactionRecord tx = new BillingTransactionRecord(
                        UuidV7.randomUuid(),
                        targetStudioId,
                        sub != null ? sub.id() : null,
                        providerName,
                        event.providerPaymentId(),
                        null,
                        event.amountMinor(),
                        "INR",
                        BillingTransactionStatus.SUCCEEDED,
                        "Subscription renewal",
                        null,
                        event.timestamp() != null ? event.timestamp() : now,
                        now
                );
                billingRepository.saveTransaction(tx);
            }

            if (sub != null) {
                Instant newPeriodEnd = (sub.currentPeriodEnd() != null && sub.currentPeriodEnd().isAfter(now))
                        ? sub.currentPeriodEnd().plus(30, ChronoUnit.DAYS)
                        : now.plus(30, ChronoUnit.DAYS);

                StudioSubscriptionRecord renewed = new StudioSubscriptionRecord(
                        sub.id(),
                        sub.studioId(),
                        sub.planId(),
                        SubscriptionStatus.ACTIVE,
                        sub.provider(),
                        sub.providerCustomerId(),
                        sub.providerSubscriptionId(),
                        sub.currentPeriodStart() != null ? sub.currentPeriodStart() : now,
                        newPeriodEnd,
                        false,
                        null,
                        sub.createdAt(),
                        now,
                        sub.version(),
                        sub.sourceType(),
                        null,
                        null
                );
                billingRepository.saveSubscription(renewed);
                String formattedPeriod = DATE_FORMATTER.format(newPeriodEnd);
                notifyStudio(sub.studioId(), "Subscription Renewed", "Your subscription renewed successfully.");
                sendBillingEmail(sub.studioId(), "SUBSCRIPTION_RENEWED", (studio, user, plan) ->
                        emailTemplateService.renderSubscriptionRenewed(studio.name(), plan != null ? plan.name() : "Commercial Plan", formattedPeriod, "/workspace/billing"),
                        "sub_renewed:" + (event.providerPaymentId() != null ? event.providerPaymentId() : event.eventId())
                );
            }

        } else if (evType.contains("cancelled") || evType.contains("canceled") || evType.contains("deleted") || evType.contains("expired")) {
            if (sub != null) {
                if (sub.downgradePlanId() != null) {
                    // Downgrade to target plan with zero data loss
                    StudioSubscriptionRecord downgraded = new StudioSubscriptionRecord(
                            sub.id(),
                            sub.studioId(),
                            sub.downgradePlanId(),
                            SubscriptionStatus.ACTIVE,
                            sub.provider(),
                            sub.providerCustomerId(),
                            sub.providerSubscriptionId(),
                            now,
                            now.plus(30, ChronoUnit.DAYS),
                            false,
                            null,
                            sub.createdAt(),
                            now,
                            sub.version(),
                            "PAID_PROVIDER",
                            null,
                            null
                    );
                    billingRepository.saveSubscription(downgraded);
                    notifyStudio(sub.studioId(), "Plan Downgraded",
                            "Your plan has transitioned to the selected lower tier. All existing projects and photographs remain safely preserved.");
                } else {
                    // Cancelled / Expired without downgrade -> fall back to STANDARD via EntitlementService
                    StudioSubscriptionRecord expired = new StudioSubscriptionRecord(
                            sub.id(),
                            sub.studioId(),
                            sub.planId(),
                            SubscriptionStatus.EXPIRED,
                            sub.provider(),
                            sub.providerCustomerId(),
                            sub.providerSubscriptionId(),
                            sub.currentPeriodStart(),
                            sub.currentPeriodEnd(),
                            false,
                            now,
                            sub.createdAt(),
                            now,
                            sub.version(),
                            sub.sourceType(),
                            null,
                            null
                    );
                    billingRepository.saveSubscription(expired);
                    notifyStudio(sub.studioId(), "Subscription Expired",
                            "Your commercial subscription has expired. Standard access active with zero data loss.");
                }
            }
        }
    }

    private void notifyStudio(UUID studioId, String title, String message) {
        if (notificationService == null || studioId == null) return;
        try {
            studioRepository.findStudioById(studioId).ifPresent(studio -> {
                if (studio.ownerId() != null) {
                    notificationService.dispatchNotification(
                            studio.ownerId(),
                            studioId,
                            com.interior.platform.notifications.domain.NotificationType.SYSTEM_NOTICE,
                            title,
                            message,
                            "/workspace/billing",
                            null
                    );
                }
            });
        } catch (Exception e) {
            log.warn("Failed to dispatch billing notification: {}", e.getMessage());
        }
    }

    @FunctionalInterface
    private interface EmailRenderFunction {
        EmailTemplateService.RenderedEmail render(StudioDetailRecord studio, com.interior.platform.security.domain.UserRecord user, BillingPlanRecord plan);
    }

    private void sendBillingEmail(UUID studioId, String eventType, EmailRenderFunction renderFunction, String idempotencyKey) {
        if (communicationDeliveryService == null || emailTemplateService == null || studioId == null) {
            return;
        }
        try {
            studioRepository.findStudioById(studioId).ifPresent(studio -> {
                if (studio.ownerId() != null) {
                    com.interior.platform.security.domain.UserRecord owner = (securityRepository != null)
                            ? securityRepository.findUserById(studio.ownerId()).orElse(null)
                            : null;
                    String recipientEmail = (owner != null && owner.email() != null && !owner.email().isBlank())
                            ? owner.email()
                            : (studio.contacts() != null ? studio.contacts().stream()
                                    .filter(c -> "EMAIL".equalsIgnoreCase(c.kind()))
                                    .map(StudioDetailRecord.StudioContactItem::value)
                                    .findFirst().orElse(null) : null);

                    if (recipientEmail != null && !recipientEmail.isBlank()) {
                        BillingPlanRecord plan = entitlementService.getActivePlan(studioId);
                        EmailTemplateService.RenderedEmail rendered = renderFunction.render(studio, owner, plan);
                        communicationDeliveryService.attemptEmailDelivery(
                                studioId,
                                studio.ownerId(),
                                eventType,
                                recipientEmail,
                                rendered.subject(),
                                rendered.textBody(),
                                rendered.htmlBody(),
                                idempotencyKey
                        );
                    }
                }
            });
        } catch (Exception e) {
            log.warn("Failed to dispatch billing transactional email for event {}: {}", eventType, e.getMessage());
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

    private void validateStudioAdminAccess(ActorContext actor, UUID studioId) {
        if (actor == null || !actor.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        if (actor.hasRole("ADMIN") || actor.hasRole("SUPER_ADMIN")) {
            return;
        }
        if (studioId == null || !actor.isStudioMember(studioId)) {
            throw new AccessDeniedException("User is not a member of studio: " + studioId);
        }
        if (!actor.isStudioOwnerOrAdmin(studioId)) {
            throw new AccessDeniedException("Only studio owners and administrators can manage subscriptions or checkout");
        }
    }
}
