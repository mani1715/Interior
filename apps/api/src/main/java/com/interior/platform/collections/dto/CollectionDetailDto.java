package com.interior.platform.collections.dto;

import java.util.List;

public record CollectionDetailDto(
        UserCollectionDto collection,
        List<CollectionItemDto> items
) {
}
