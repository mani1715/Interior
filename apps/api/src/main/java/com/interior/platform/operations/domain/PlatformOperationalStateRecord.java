package com.interior.platform.operations.domain;

import java.time.Instant;
import java.util.UUID;

public record PlatformOperationalStateRecord(
        String id,
        String maintenanceMode,
        String maintenanceMessage,
        boolean uploadsEnabled,
        boolean aiEnabled,
        boolean registrationsEnabled,
        UUID updatedBy,
        Instant updatedAt
) {}
