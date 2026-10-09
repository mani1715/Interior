package com.interior.platform.admin.dto;

import java.time.Instant;

public record OperationalHealthDto(
        String database,
        String storageProvider,
        String aiProvider,
        String emailProvider,
        String whatsappMode,
        Instant timestamp
) {}
