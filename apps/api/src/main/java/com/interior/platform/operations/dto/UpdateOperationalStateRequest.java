package com.interior.platform.operations.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateOperationalStateRequest(
        @Pattern(regexp = "NORMAL|READ_ONLY", message = "maintenanceMode must be NORMAL or READ_ONLY")
        String maintenanceMode,
        String maintenanceMessage,
        Boolean uploadsEnabled,
        Boolean aiEnabled,
        Boolean registrationsEnabled
) {}
