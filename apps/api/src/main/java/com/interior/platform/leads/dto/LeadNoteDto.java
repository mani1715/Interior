package com.interior.platform.leads.dto;

import java.time.Instant;
import java.util.UUID;

public record LeadNoteDto(
    UUID id,
    UUID authorId,
    String authorName,
    String content,
    Instant createdAt
) {}
