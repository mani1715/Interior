package com.interior.platform.reviews.dto;

import com.interior.platform.reviews.domain.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewReportRequest(
        @NotNull(message = "Report reason is required")
        ReportReason reason,

        @Size(max = 500, message = "Details cannot exceed 500 characters")
        String details
) {
}
