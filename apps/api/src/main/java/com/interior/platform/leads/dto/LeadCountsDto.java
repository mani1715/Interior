package com.interior.platform.leads.dto;

public record LeadCountsDto(
    long total,
    long newLeads,
    long active,
    long won,
    long lost,
    long archived
) {}
