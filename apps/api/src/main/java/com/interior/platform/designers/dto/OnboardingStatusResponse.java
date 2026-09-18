package com.interior.platform.designers.dto;

import java.util.UUID;

public record OnboardingStatusResponse(
        String status,
        int currentStep,
        String draftPayload,
        StudioSummary studio,
        boolean isDesigner
) {
    public record StudioSummary(
            UUID id,
            String name,
            String slug,
            String professionalType,
            String status,
            String publicationStatus,
            String role
    ) {}

    public static OnboardingStatusResponse notStarted() {
        return new OnboardingStatusResponse("NOT_STARTED", 1, null, null, false);
    }

    public static OnboardingStatusResponse inProgress(int step, String draftPayload) {
        return new OnboardingStatusResponse("IN_PROGRESS", step, draftPayload, null, false);
    }

    public static OnboardingStatusResponse completed(StudioSummary studio) {
        return new OnboardingStatusResponse("COMPLETED", 8, null, studio, true);
    }

    public static OnboardingStatusResponse blocked(String reason) {
        return new OnboardingStatusResponse("BLOCKED", 0, reason, null, false);
    }
}
