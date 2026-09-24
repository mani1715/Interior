package com.interior.platform.leads.service;

import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.common.exception.RateLimitExceededException;
import com.interior.platform.common.util.UuidV7;
import com.interior.platform.leads.domain.*;
import com.interior.platform.leads.dto.PublicLeadSubmissionRequest;
import com.interior.platform.leads.dto.PublicLeadSubmissionResponse;
import com.interior.platform.leads.dto.PublicWhatsAppHandoffRequest;
import com.interior.platform.leads.dto.PublicWhatsAppHandoffResponse;
import com.interior.platform.leads.repository.LeadRepository;
import com.interior.platform.security.service.RateLimiterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PublicLeadService {

    private final LeadRepository leadRepository;
    private final PhoneNormalizationService phoneNormalizationService;
    private final RateLimiterService rateLimiterService;

    public PublicLeadService(
            LeadRepository leadRepository,
            PhoneNormalizationService phoneNormalizationService,
            RateLimiterService rateLimiterService
    ) {
        this.leadRepository = leadRepository;
        this.phoneNormalizationService = phoneNormalizationService;
        this.rateLimiterService = rateLimiterService;
    }

    @Transactional
    public PublicLeadSubmissionResponse submitInquiry(PublicLeadSubmissionRequest req, String clientIp) {
        // 1. Abuse Protection: Honeypot check
        if (req.website_hp() != null && !req.website_hp().isBlank()) {
            // Silently discard bot submission
            return new PublicLeadSubmissionResponse("INQ-BOT001", "Inquiry received successfully.", "Interior Studio");
        }

        // 2. Abuse Protection: IP Rate Limiting (5 inquiries per 10 minutes per IP)
        String rateLimitKey = "lead_inq_" + (clientIp != null ? clientIp : "unknown");
        rateLimiterService.acquire(rateLimitKey, 5, Duration.ofMinutes(10));

        // 3. Target Studio Validation
        if (req.targetStudioSlug() == null || req.targetStudioSlug().isBlank()) {
            throw new BadRequestException("This professional is not currently accepting inquiries.");
        }

        LeadRepository.PublicStudioTarget studio = leadRepository.findPublicStudioBySlug(req.targetStudioSlug().trim())
                .orElseThrow(() -> new BadRequestException("This professional is not currently accepting inquiries."));

        // 4. Target Project Validation (if supplied)
        UUID projectId = null;
        String projectTitle = null;
        if (req.targetProjectSlug() != null && !req.targetProjectSlug().isBlank()) {
            LeadRepository.PublicProjectTarget project = leadRepository.findPublicProjectBySlug(studio.id(), req.targetProjectSlug().trim())
                    .orElseThrow(() -> new BadRequestException("This professional is not currently accepting inquiries."));
            projectId = project.id();
            projectTitle = project.title();
        }

        // 5. Idempotency Check
        if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            Optional<StudioLeadRecord> existing = leadRepository.findByIdempotencyKey(studio.id(), req.idempotencyKey().trim());
            if (existing.isPresent()) {
                String refNum = "INQ-" + existing.get().id().toString().substring(0, 8).toUpperCase();
                return new PublicLeadSubmissionResponse(refNum, "Inquiry received successfully.", studio.name());
            }
        }

        // 6. Data Normalization & Sanitization
        String phoneNormalized = phoneNormalizationService.normalize(req.phone());
        String emailNormalized = (req.email() != null && !req.email().isBlank()) ? req.email().trim().toLowerCase() : null;
        String sanitizedName = sanitizeText(req.name(), 100);
        String sanitizedMessage = sanitizeText(req.message(), 2000);
        String sanitizedCity = (req.city() != null && !req.city().isBlank()) ? sanitizeText(req.city(), 100) : null;
        String category = (req.projectCategory() != null && !req.projectCategory().isBlank()) ? req.projectCategory().trim() : null;
        String budget = (req.budgetRange() != null && !req.budgetRange().isBlank()) ? req.budgetRange().trim() : null;

        PreferredContactChannel channel = null;
        if (req.preferredContactChannel() != null && !req.preferredContactChannel().isBlank()) {
            try {
                channel = PreferredContactChannel.valueOf(req.preferredContactChannel().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        // 7. Server-Derived Source Attribution
        LeadSource source = (projectId != null) ? LeadSource.PUBLIC_PROJECT : LeadSource.PUBLIC_PORTFOLIO;

        // 8. Possible Duplicate Check (within past 48 hours)
        Instant duplicateWindow = Instant.now().minus(Duration.ofHours(48));
        boolean possibleDuplicate = leadRepository.findRecentDuplicate(studio.id(), phoneNormalized, emailNormalized, duplicateWindow).isPresent();

        // 9. Persist Lead
        UUID leadId = UuidV7.randomUuid();
        Instant now = Instant.now();
        Instant whatsappConsentAt = req.whatsappConsent() ? now : null;

        StudioLeadRecord leadRecord = new StudioLeadRecord(
                leadId,
                studio.id(),
                projectId,
                source,
                LeadStatus.NEW,
                sanitizedName,
                phoneNormalized,
                emailNormalized,
                sanitizedCity,
                category,
                budget,
                sanitizedMessage,
                channel,
                now,
                whatsappConsentAt,
                null,
                null,
                null,
                possibleDuplicate,
                req.idempotencyKey() != null ? req.idempotencyKey().trim() : null,
                now,
                now,
                1L,
                null
        );

        leadRepository.save(leadRecord);

        // 10. Audit Activity (LEAD_CREATED)
        String details = (projectTitle != null)
                ? "{\"projectTitle\":\"" + escapeJson(projectTitle) + "\",\"source\":\"" + source.name() + "\"}"
                : "{\"source\":\"" + source.name() + "\"}";

        leadRepository.saveActivity(new LeadActivityRecord(
                UuidV7.randomUuid(),
                leadId,
                studio.id(),
                null,
                LeadActivityType.LEAD_CREATED,
                details,
                now
        ));

        String referenceNumber = "INQ-" + leadId.toString().substring(0, 8).toUpperCase();
        return new PublicLeadSubmissionResponse(referenceNumber, "Inquiry received successfully.", studio.name());
    }

    @Transactional
    public PublicWhatsAppHandoffResponse initiateWhatsAppHandoff(PublicWhatsAppHandoffRequest req, String clientIp) {
        // 1. Rate Limiting (10 WhatsApp handoffs per 5 minutes per IP)
        String rateLimitKey = "wa_handoff_" + (clientIp != null ? clientIp : "unknown");
        rateLimiterService.acquire(rateLimitKey, 10, Duration.ofMinutes(5));

        // 2. Target Studio Validation
        LeadRepository.PublicStudioTarget studio = leadRepository.findPublicStudioBySlug(req.targetStudioSlug().trim())
                .orElseThrow(() -> new BadRequestException("This professional is not currently accepting inquiries."));

        // 3. Studio Public WhatsApp Consent Verification
        LeadRepository.PublicWhatsAppContact contact = leadRepository.findPublicWhatsAppContact(studio.id())
                .orElseThrow(() -> new BadRequestException("WhatsApp contact is not enabled by this professional. Please use the Send Inquiry form."));

        // 4. Target Project (optional)
        String projectTitle = null;
        if (req.targetProjectSlug() != null && !req.targetProjectSlug().isBlank()) {
            projectTitle = leadRepository.findPublicProjectBySlug(studio.id(), req.targetProjectSlug().trim())
                    .map(LeadRepository.PublicProjectTarget::title)
                    .orElse(null);
        }

        // 5. Compose Prefilled Message (Truthful, safe, context-aware)
        String prefillText;
        if (req.customMessage() != null && !req.customMessage().isBlank()) {
            prefillText = sanitizeText(req.customMessage(), 500);
        } else if (projectTitle != null) {
            prefillText = "Hi " + studio.name() + ", I found your \"" + projectTitle + "\" project on Interior Platform. I would like to discuss an interior project with you.";
        } else {
            prefillText = "Hi " + studio.name() + ", I found your studio on Interior Platform. I would like to discuss an interior project with you.";
        }

        // Clean digits for wa.me URL (remove plus, spaces, dashes)
        String cleanDigits = contact.contactValue().replaceAll("[^0-9]", "");
        String encodedText = URLEncoder.encode(prefillText, StandardCharsets.UTF_8).replace("+", "%20");
        String whatsappUrl = "https://wa.me/" + cleanDigits + "?text=" + encodedText;

        // 6. Record Handoff Attribution if visitor provided contact details or context
        if (req.visitorPhone() != null && !req.visitorPhone().isBlank()) {
            try {
                String phoneNorm = phoneNormalizationService.normalize(req.visitorPhone());
                UUID leadId = UuidV7.randomUuid();
                Instant now = Instant.now();
                String visitorName = req.visitorName() != null ? sanitizeText(req.visitorName(), 100) : "WhatsApp Visitor";

                StudioLeadRecord lead = new StudioLeadRecord(
                        leadId,
                        studio.id(),
                        null,
                        LeadSource.WHATSAPP_HANDOFF,
                        LeadStatus.NEW,
                        visitorName,
                        phoneNorm,
                        null,
                        null,
                        null,
                        null,
                        prefillText,
                        PreferredContactChannel.WHATSAPP,
                        now,
                        now,
                        null,
                        null,
                        null,
                        false,
                        null,
                        now,
                        now,
                        1L,
                        null
                );
                leadRepository.save(lead);
                leadRepository.saveActivity(new LeadActivityRecord(
                        UuidV7.randomUuid(),
                        leadId,
                        studio.id(),
                        null,
                        LeadActivityType.WHATSAPP_HANDOFF_OPENED,
                        "{\"handoff\":\"USER_INITIATED\"}",
                        now
                ));
            } catch (Exception ignored) {
                // If phone is invalid, handoff still opens without corrupting lead DB
            }
        }

        return new PublicWhatsAppHandoffResponse(
                whatsappUrl,
                studio.name(),
                "WhatsApp handoff link generated. Opening WhatsApp..."
        );
    }

    private String sanitizeText(String input, int maxLength) {
        if (input == null) return "";
        // Strip HTML tags and control characters
        String stripped = input.replaceAll("<[^>]*>", "").trim();
        if (stripped.length() > maxLength) {
            return stripped.substring(0, maxLength);
        }
        return stripped;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}
