package com.interior.platform.projects.dto;

import java.util.List;

public record ProjectPublishCheckResponse(
        boolean isPublishable,
        List<String> blockers,
        boolean isProjectReady,
        boolean isStudioActive,
        boolean hasPublicMedia
) {}
