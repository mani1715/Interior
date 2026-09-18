package com.interior.platform.designers.domain;

import java.util.UUID;

public record StudioSpecialtyRecord(
        UUID id,
        UUID studioId,
        String specialtyCode,
        String specialtyName
) {}
