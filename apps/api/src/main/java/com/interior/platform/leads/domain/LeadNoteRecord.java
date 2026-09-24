package com.interior.platform.leads.domain;

import java.time.Instant;
import java.util.UUID;

public record LeadNoteRecord(
    UUID id,
    UUID leadId,
    UUID studioId,
    UUID authorId,
    String content,
    Instant createdAt
) {}
