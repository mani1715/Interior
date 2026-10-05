package com.interior.platform.projects.dto;

import com.interior.platform.projects.domain.RoomType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotNull(message = "Room type is required")
        RoomType roomType,

        @Size(max = 128, message = "Display name cannot exceed 128 characters")
        String displayName
) {}
