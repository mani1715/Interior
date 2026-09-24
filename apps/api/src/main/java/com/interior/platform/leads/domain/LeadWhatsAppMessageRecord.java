package com.interior.platform.leads.domain;

import java.time.Instant;
import java.util.UUID;

public record LeadWhatsAppMessageRecord(
    UUID id,
    UUID studioId,
    UUID leadId,
    WhatsAppDirection direction,
    String provider,
    String providerMessageId,
    WhatsAppMessageStatus status,
    String body,
    String failureCode,
    Instant createdAt,
    Instant statusUpdatedAt
) {}
