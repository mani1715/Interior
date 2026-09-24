package com.interior.platform.leads.dto;

public record WhatsAppProviderStatusDto(
    boolean configured,
    String providerName,
    String status,
    boolean publicHandoffEnabled,
    String publicWhatsAppNumber
) {}
