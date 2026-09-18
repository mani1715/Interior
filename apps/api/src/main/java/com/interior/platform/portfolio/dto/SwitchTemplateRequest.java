package com.interior.platform.portfolio.dto;

import com.interior.platform.portfolio.domain.PortfolioTemplateKey;
import jakarta.validation.constraints.NotNull;

public record SwitchTemplateRequest(
        @NotNull(message = "Template key is required")
        PortfolioTemplateKey templateKey,

        @NotNull(message = "Aggregate version is required for concurrency control")
        Long version
) {}
