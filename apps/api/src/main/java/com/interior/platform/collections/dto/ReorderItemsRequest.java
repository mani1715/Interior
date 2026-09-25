package com.interior.platform.collections.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderItemsRequest(
        @NotNull(message = "Item IDs list is required")
        List<UUID> itemIds
) {
}
