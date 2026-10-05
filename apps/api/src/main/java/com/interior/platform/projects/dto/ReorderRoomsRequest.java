package com.interior.platform.projects.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderRoomsRequest(
        @NotEmpty(message = "Room IDs cannot be empty")
        List<UUID> roomIds
) {}
