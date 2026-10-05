package com.interior.platform.workspace.dto;

import java.util.UUID;

public record CreateStudioResponse(
        UUID studioId,
        String name,
        String slug,
        String professionalType,
        String role,
        String message
) {
}
