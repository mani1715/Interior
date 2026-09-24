package com.interior.platform.leads.web;

import com.interior.platform.leads.dto.PublicLeadSubmissionRequest;
import com.interior.platform.leads.dto.PublicLeadSubmissionResponse;
import com.interior.platform.leads.dto.PublicWhatsAppHandoffRequest;
import com.interior.platform.leads.dto.PublicWhatsAppHandoffResponse;
import com.interior.platform.leads.service.PublicLeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
@Tag(name = "Public Leads & Contact", description = "Endpoints for submitting public project/studio inquiries and initiating WhatsApp contact handoff")
public class PublicLeadController {

    private final PublicLeadService publicLeadService;

    public PublicLeadController(PublicLeadService publicLeadService) {
        this.publicLeadService = publicLeadService;
    }

    @PostMapping("/leads")
    @Operation(summary = "Submit public inquiry", description = "Public, unauthenticated lead submission for a studio or specific project with honeypot abuse protection and rate limiting.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inquiry successfully submitted"),
            @ApiResponse(responseCode = "400", description = "Invalid request, missing required consent, or invalid target"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<PublicLeadSubmissionResponse> submitLead(
            @Valid @RequestBody PublicLeadSubmissionRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        PublicLeadSubmissionResponse response = publicLeadService.submitInquiry(request, clientIp);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    @PostMapping("/contact/whatsapp-handoff")
    @Operation(summary = "Initiate WhatsApp handoff", description = "Generates a prefilled WhatsApp wa.me URL for visitor-initiated chat and records the handoff without falsely claiming delivery.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "WhatsApp handoff initiated successfully"),
            @ApiResponse(responseCode = "400", description = "WhatsApp contact not available or invalid target"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<PublicWhatsAppHandoffResponse> initiateWhatsAppHandoff(
            @Valid @RequestBody PublicWhatsAppHandoffRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        PublicWhatsAppHandoffResponse response = publicLeadService.initiateWhatsAppHandoff(request, clientIp);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
