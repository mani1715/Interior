package com.interior.platform.seo.dto;

import java.util.List;
import java.util.UUID;

public record PublicStudioSummaryDto(
        UUID id,
        String slug,
        String name,
        String professionalType,
        String professionalTitle,
        String city,
        String state,
        List<PublicContactDto> contacts
) {}
