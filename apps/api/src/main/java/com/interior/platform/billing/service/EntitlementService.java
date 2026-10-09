package com.interior.platform.billing.service;

import com.interior.platform.billing.domain.*;
import com.interior.platform.billing.repository.BillingRepository;
import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.projects.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EntitlementService {

    public static final String BASE_PLAN_CODE = "BASE";
    public static final String STANDARD_PLAN_CODE = "STANDARD";

    private final BillingRepository billingRepository;
    private final ProjectRepository projectRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.interior.platform.media.repository.MediaRepository mediaRepository;

    public EntitlementService(BillingRepository billingRepository, ProjectRepository projectRepository) {
        this.billingRepository = billingRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Resolves the active billing plan for a studio.
     * Invariants:
     * - An active subscription resolves to its plan.
     * - If no active subscription exists, but the latest recorded subscription is commercial
     *   (e.g. EXPIRED, CANCELLED, PAST_DUE), fall back safely to STANDARD to prevent granting unlimited BASE access.
     * - Only studios without commercial subscription records fall back to internal legacy BASE.
     */
    public BillingPlanRecord getActivePlan(UUID studioId) {
        Optional<StudioSubscriptionRecord> activeSub = billingRepository.findActiveSubscription(studioId);
        if (activeSub.isPresent()) {
            return billingRepository.findPlanById(activeSub.get().planId())
                    .orElseGet(this::getStandardPlan);
        }

        Optional<StudioSubscriptionRecord> latestSub = billingRepository.findLatestSubscription(studioId);
        if (latestSub.isPresent()) {
            return getStandardPlan();
        }

        return getBasePlan();
    }

    /**
     * Retrieves the internal legacy BASE plan.
     */
    public BillingPlanRecord getBasePlan() {
        return billingRepository.findPlanByCode(BASE_PLAN_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Base plan configuration not found"));
    }

    /**
     * Retrieves the customer-facing STANDARD plan.
     */
    public BillingPlanRecord getStandardPlan() {
        return billingRepository.findPlanByCode(STANDARD_PLAN_CODE)
                .orElseGet(this::getBasePlan);
    }

    /**
     * Resolves effective entitlements for a studio.
     * Numeric null explicitly indicates unlimited capacity.
     */
    public Map<String, Object> getEffectiveEntitlements(UUID studioId) {
        BillingPlanRecord plan = getActivePlan(studioId);
        List<PlanEntitlementRecord> records = billingRepository.findEntitlementsByPlanId(plan.id());

        Map<String, Object> entitlements = new HashMap<>();
        for (PlanEntitlementRecord record : records) {
            if ("BOOLEAN".equalsIgnoreCase(record.valueType())) {
                entitlements.put(record.entitlementKey(), Boolean.TRUE.equals(record.booleanValue()));
            } else if ("NUMERIC".equalsIgnoreCase(record.valueType())) {
                entitlements.put(record.entitlementKey(), record.numericValue()); // null = unlimited
            }
        }
        return entitlements;
    }

    public boolean hasBooleanEntitlement(UUID studioId, EntitlementKey key) {
        Map<String, Object> entitlements = getEffectiveEntitlements(studioId);
        Object val = entitlements.get(key.name());
        return Boolean.TRUE.equals(val);
    }

    public Long getNumericLimit(UUID studioId, EntitlementKey key) {
        Map<String, Object> entitlements = getEffectiveEntitlements(studioId);
        Object val = entitlements.get(key.name());
        if (val instanceof Number n) {
            return n.longValue();
        }
        return null; // null represents unlimited
    }

    /**
     * Enforces project limit if current plan has a non-null numeric limit.
     */
    public void assertProjectCreationAllowed(UUID studioId) {
        Long limit = getNumericLimit(studioId, EntitlementKey.PROJECT_LIMIT);
        if (limit != null) {
            int currentCount = projectRepository.countProjects(studioId);
            if (currentCount >= limit) {
                throw new BadRequestException(
                        String.format("Project limit reached for your current plan (%d/%d). Upgrade your plan to create more projects.",
                                currentCount, limit)
                );
            }
        }
    }

    /**
     * Enforces portfolio photo quota per project (committed photos + reserved pending upload intents).
     */
    public void assertProjectPhotoQuotaAllowed(UUID studioId, UUID projectId, int incomingPhotosCount) {
        Long limit = getNumericLimit(studioId, EntitlementKey.PROJECT_PHOTO_LIMIT);
        if (limit != null && mediaRepository != null) {
            int committed = mediaRepository.countCommittedPortfolioPhotos(studioId, projectId);
            int reserved = mediaRepository.countPendingPortfolioUploadIntents(studioId, projectId);
            int currentTotal = committed + reserved;
            if (currentTotal + incomingPhotosCount > limit) {
                throw new BadRequestException(
                        String.format("Photo limit reached for this project (%d/%d). Upgrade your plan to add more portfolio photographs.",
                                currentTotal, limit)
                );
            }
        }
    }

    /**
     * Enforces storage quota (committed media assets and derivatives + reserved pending upload intents).
     */
    public void assertStorageQuotaAllowed(UUID studioId, long incomingBytes) {
        Long limit = getNumericLimit(studioId, EntitlementKey.STORAGE_LIMIT_BYTES);
        if (limit != null && mediaRepository != null) {
            long committed = mediaRepository.countCommittedStorageBytes(studioId);
            long pending = mediaRepository.countPendingStorageBytes(studioId);
            long currentTotal = committed + pending;
            if (currentTotal + incomingBytes > limit) {
                throw new BadRequestException(
                        String.format("Storage limit reached for your current plan (%d/%d bytes). Upgrade your plan for additional storage capacity.",
                                currentTotal, limit)
                );
            }
        }
    }

    public Long getStorageLimitBytes(UUID studioId) {
        return getNumericLimit(studioId, EntitlementKey.STORAGE_LIMIT_BYTES);
    }

    /**
     * Enforces Cinematic presentation mode selection and studio-level Cinematic project quota.
     */
    public void assertCinematicProjectAllowed(UUID studioId) {
        boolean cinematicEnabled = hasBooleanEntitlement(studioId, EntitlementKey.CINEMATIC_PORTFOLIO);
        if (!cinematicEnabled) {
            throw new BadRequestException("Cinematic project presentation is not included in your current plan. Upgrade to Pro to enable Cinematic presentation.");
        }
        Long limit = getNumericLimit(studioId, EntitlementKey.CINEMATIC_PROJECT_LIMIT);
        if (limit != null) {
            int currentCinematic = projectRepository.countCinematicProjects(studioId);
            if (currentCinematic >= limit) {
                throw new BadRequestException(
                        String.format("Cinematic project allocation limit reached (%d/%d projects). Switch an existing project to Standard or upgrade.",
                                currentCinematic, limit)
                );
            }
        }
    }
}
