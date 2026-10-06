package com.interior.platform.seo.dto;

import java.util.List;
import java.util.UUID;

public record PublicRoomDto(
        UUID id,
        String roomType,
        String label,
        int displayOrder,
        PublicMediaDto coverPhoto,
        int eligiblePhotoCount,
        List<PublicMediaDto> photos
) {}
