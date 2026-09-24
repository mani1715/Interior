package com.interior.platform.leads.domain;

public enum LeadActivityType {
    LEAD_CREATED("Inquiry Received"),
    STATUS_CHANGED("Status Updated"),
    ASSIGNED("Team Member Assigned"),
    NOTE_ADDED("Internal Note Added"),
    FOLLOW_UP_CHANGED("Follow-up Date Scheduled"),
    WHATSAPP_HANDOFF_OPENED("WhatsApp Contact Opened"),
    WHATSAPP_MESSAGE_SENT("WhatsApp Message Sent"),
    WHATSAPP_MESSAGE_DELIVERED("WhatsApp Message Delivered"),
    WHATSAPP_MESSAGE_READ("WhatsApp Message Read"),
    WHATSAPP_MESSAGE_FAILED("WhatsApp Message Failed"),
    WON("Lead Marked Won"),
    LOST("Lead Marked Lost"),
    ARCHIVED("Lead Archived");

    private final String displayName;

    LeadActivityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
