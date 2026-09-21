package com.interior.platform.projects.dto;

import jakarta.validation.constraints.NotNull;

public record ProjectActionRequest(
        @NotNull(message = "Version is required for concurrency control")
        Long version
) {}
