package com.interior.platform.leads.service;

import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.ConflictException;
import com.interior.platform.common.exception.ResourceNotFoundException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.leads.domain.*;
import com.interior.platform.leads.dto.SendWhatsAppMessageRequest;
import com.interior.platform.leads.dto.WhatsAppMessageDto;
import com.interior.platform.leads.dto.WhatsAppProviderStatusDto;
import com.interior.platform.leads.repository.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WhatsAppService {

    private final LeadRepository leadRepository;
    private final WhatsAppProvider whatsAppProvider;

    public WhatsAppService(LeadRepository leadRepository, WhatsAppProvider whatsAppProvider) {
        this.leadRepository = leadRepository;
        this.whatsAppProvider = whatsAppProvider;
    }

    public WhatsAppProviderStatusDto getProviderStatus(UUID studioId) {
        Optional<LeadRepository.PublicWhatsAppContact> contactOpt = leadRepository.findPublicWhatsAppContact(studioId);
        boolean handoffEnabled = contactOpt.isPresent();
        String publicPhone = contactOpt.map(LeadRepository.PublicWhatsAppContact::contactValue).orElse(null);

        String status = whatsAppProvider.isConfigured() ? "READY" : "NOT_CONFIGURED";
        return new WhatsAppProviderStatusDto(
                whatsAppProvider.isConfigured(),
                whatsAppProvider.getProviderName(),
                status,
                handoffEnabled,
                publicPhone
        );
    }

    @Transactional
    public WhatsAppMessageDto sendMessage(UUID studioId, UUID leadId, SendWhatsAppMessageRequest request, UUID actorId) {
        StudioLeadRecord lead = leadRepository.findById(studioId, leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        if (lead.whatsappConsentAt() == null) {
            throw new BadRequestException("Cannot send WhatsApp message: lead has not provided explicit WhatsApp consent.");
        }

        if (!whatsAppProvider.isConfigured()) {
            throw new BadRequestException("WhatsApp managed messaging is not configured on this platform instance.");
        }

        if (request.body() == null || request.body().isBlank()) {
            throw new BadRequestException("Message body cannot be empty");
        }

        UUID messageId = UuidV7.randomUuid();
        Instant now = Instant.now();

        // 1. Initial SUBMITTED record
        WhatsAppProvider.SendResult sendResult = whatsAppProvider.sendMessage(
                lead.phoneNormalized(),
                request.body().trim(),
                request.idempotencyKey()
        );

        WhatsAppMessageStatus finalStatus = sendResult.success() ? WhatsAppMessageStatus.SUBMITTED : WhatsAppMessageStatus.FAILED;

        LeadWhatsAppMessageRecord msgRecord = new LeadWhatsAppMessageRecord(
                messageId,
                studioId,
                leadId,
                WhatsAppDirection.OUTBOUND,
                whatsAppProvider.getProviderName(),
                sendResult.providerMessageId(),
                finalStatus,
                request.body().trim(),
                sendResult.errorCode(),
                now,
                now
        );

        leadRepository.saveWhatsAppMessage(msgRecord);

        // 2. Audit activity
        LeadActivityType activityType = sendResult.success() ? LeadActivityType.WHATSAPP_MESSAGE_SENT : LeadActivityType.WHATSAPP_MESSAGE_FAILED;
        String details = sendResult.success()
                ? "{\"providerMessageId\":\"" + sendResult.providerMessageId() + "\"}"
                : "{\"error\":\"" + sendResult.errorCode() + "\"}";

        leadRepository.saveActivity(new LeadActivityRecord(
                UuidV7.randomUuid(),
                leadId,
                studioId,
                actorId,
                activityType,
                details,
                now
        ));

        if (!sendResult.success()) {
            throw new BadRequestException("WhatsApp dispatch failed: " + sendResult.errorMessage());
        }

        return toDto(msgRecord);
    }

    @Transactional
    public boolean handleWebhook(String payload, String signature) {
        if (!whatsAppProvider.verifyWebhookSignature(payload, signature)) {
            return false;
        }

        Optional<WhatsAppProvider.WebhookStatusEvent> eventOpt = whatsAppProvider.parseWebhookStatus(payload);
        if (eventOpt.isEmpty()) {
            return true; // Acknowledged non-status event safely
        }

        WhatsAppProvider.WebhookStatusEvent event = eventOpt.get();
        Optional<LeadWhatsAppMessageRecord> msgOpt = leadRepository.findWhatsAppMessageByProviderId(
                whatsAppProvider.getProviderName(),
                event.providerMessageId()
        );

        if (msgOpt.isEmpty()) {
            return true; // Unknown provider message ID ignored safely
        }

        LeadWhatsAppMessageRecord msg = msgOpt.get();

        // Monotonic status progression: only update if transition is valid
        if (msg.status().canTransitionTo(event.status())) {
            leadRepository.updateWhatsAppMessageStatus(
                    msg.studioId(),
                    msg.id(),
                    event.status(),
                    event.failureCode()
            );

            LeadActivityType activityType = switch (event.status()) {
                case DELIVERED -> LeadActivityType.WHATSAPP_MESSAGE_DELIVERED;
                case READ -> LeadActivityType.WHATSAPP_MESSAGE_READ;
                case FAILED -> LeadActivityType.WHATSAPP_MESSAGE_FAILED;
                default -> null;
            };

            if (activityType != null) {
                leadRepository.saveActivity(new LeadActivityRecord(
                        UuidV7.randomUuid(),
                        msg.leadId(),
                        msg.studioId(),
                        null,
                        activityType,
                        "{\"providerMessageId\":\"" + event.providerMessageId() + "\"}",
                        Instant.now()
                ));
            }
        }

        return true;
    }

    public List<WhatsAppMessageDto> listMessages(UUID studioId, UUID leadId) {
        return leadRepository.listWhatsAppMessages(studioId, leadId).stream()
                .map(this::toDto)
                .toList();
    }

    private WhatsAppMessageDto toDto(LeadWhatsAppMessageRecord m) {
        return new WhatsAppMessageDto(
                m.id(),
                m.direction().name(),
                m.provider(),
                m.status().name(),
                m.body(),
                m.failureCode(),
                m.createdAt(),
                m.statusUpdatedAt()
        );
    }
}
