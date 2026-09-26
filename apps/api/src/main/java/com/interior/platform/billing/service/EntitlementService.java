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

    private final BillingRepository billingRepository;
    private final ProjectRepository projectRepository;

    public EntitlementService(BillingRepository billingRepository, ProjectRepository projectRepository) {
        this.billingRepository = billingRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Resolves the active billing plan for a studio, falling back to internal BASE plan.
     */
    public BillingPlanRecord getActivePlan(UUID studioId) {
        Optional<StudioSubscriptionRecord> activeSub = billingRepository.findActiveSubscription(studioId);
        if (activeSub.isPresent()) {
            return billingRepository.findPlanById(activeSub.get().planId())
                    .orElseGet(this::getBasePlan);
        }
        return getBasePlan();
    }

    /**
     * Retrieves the internal non-commercial BASE plan.
     */
    public BillingPlanRecord getBasePlan() {
        return billingRepository.findPlanByCode(BASE_PLAN_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Base plan configuration not found"));
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
                        String.format("Project limit reached for your current plan (%d/%d). Upgrade to create more projects.",
                                currentCount, limit)
                );
            }
        }
    }
}
