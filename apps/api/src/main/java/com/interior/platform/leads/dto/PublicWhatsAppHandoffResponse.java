package com.interior.platform.leads.dto;

public record PublicWhatsAppHandoffResponse(
    String whatsappUrl,
    String businessName,
    String notice
) {}
