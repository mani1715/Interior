package com.interior.platform.portfolio.dto;

import com.interior.platform.portfolio.domain.PortfolioTemplateKey;

public record InitializePortfolioRequest(
        PortfolioTemplateKey templateKey
) {}
