package com.interior.platform.leads.dto;

import java.time.Instant;
import java.util.UUID;

public record WhatsAppMessageDto(
    UUID id,
    String direction,
    String provider,
    String status,
    String body,
    String failureCode,
    Instant createdAt,
    Instant statusUpdatedAt
) {}
