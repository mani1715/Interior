package com.interior.platform.verification.web;

import com.interior.platform.verification.dto.PublicVerificationBadgeDto;
import com.interior.platform.verification.service.StudioVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/studios")
@Tag(name = "Public Studio Verification", description = "Public endpoints for checking studio verification status and badges")
public class PublicVerificationController {

    private final StudioVerificationService verificationService;

    public PublicVerificationController(StudioVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping("/{slug}/verification")
    @Operation(summary = "Get public verification badge", description = "Returns public verification badge status for published studio.")
    public ResponseEntity<PublicVerificationBadgeDto> getVerificationBadge(@PathVariable("slug") String slug) {
        PublicVerificationBadgeDto badge = verificationService.getPublicBadgeBySlug(slug);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60, s-maxage=300, stale-while-revalidate=600")
                .body(badge);
    }
}
