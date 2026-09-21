package com.interior.platform.seo.web;

import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.seo.dto.*;
import com.interior.platform.seo.service.SeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "SEO Engine", description = "Endpoints for studio publication, SEO settings, metadata, sitemaps, and public portfolio presentation")
public class SeoController {

    private final SeoService seoService;

    public SeoController(SeoService seoService) {
        this.seoService = seoService;
    }

    // ========================================================================
    // Authenticated Workspace Endpoints
    // ========================================================================

    @GetMapping("/seo/status")
    @Operation(summary = "Get SEO and publication status", description = "Retrieve current publication status, SEO checklist, and metadata diagnostics for the studio")
    public ResponseEntity<SeoStatusResponse> getSeoStatus(
            HttpServletRequest request,
            @RequestParam(value = "studioId", required = false) UUID studioId
    ) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return ResponseEntity.ok(seoService.getSeoStatus(actor, studioId));
    }

    @PostMapping("/seo/publish")
    @Operation(summary = "Publish studio portfolio", description = "Validate publication readiness and publish the studio portfolio to public discovery")
    public ResponseEntity<SeoStatusResponse> publishStudio(
            HttpServletRequest request,
            @RequestParam(value = "studioId", required = false) UUID studioId
    ) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return ResponseEntity.ok(seoService.publishStudio(actor, studioId));
    }

    @PostMapping("/seo/unpublish")
    @Operation(summary = "Unpublish studio portfolio", description = "Revert studio portfolio to unpublished draft state and revoke public discovery routing")
    public ResponseEntity<SeoStatusResponse> unpublishStudio(
            HttpServletRequest request,
            @RequestParam(value = "studioId", required = false) UUID studioId
    ) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return ResponseEntity.ok(seoService.unpublishStudio(actor, studioId));
    }

    @PutMapping("/seo/settings")
    @Operation(summary = "Update SEO settings", description = "Update custom meta title, meta description, and indexing preferences")
    public ResponseEntity<SeoStatusResponse> updateSeoSettings(
            HttpServletRequest request,
            @RequestParam(value = "studioId", required = false) UUID studioId,
            @Valid @RequestBody UpdateSeoSettingsRequest req
    ) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return ResponseEntity.ok(seoService.updateSeoSettings(actor, studioId, req));
    }

    // ========================================================================
    // Public Unauthenticated Endpoints
    // ========================================================================

    @GetMapping("/public/studios/{slug}")
    @Operation(summary = "Get public studio by slug", description = "Public-safe studio and portfolio presentation for published studios only")
    public ResponseEntity<PublicStudioDto> getPublicStudio(@PathVariable("slug") String slug) {
        return seoService.getPublicStudioBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/public/studios/{studioSlug}/projects/{projectSlug}")
    @Operation(summary = "Get public project detail", description = "Public-safe project detail with media derivatives for published studios only")
    public ResponseEntity<PublicProjectDetailDto> getPublicProject(
            @PathVariable("studioSlug") String studioSlug,
            @PathVariable("projectSlug") String projectSlug
    ) {
        return seoService.getPublicProject(studioSlug, projectSlug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/public/sitemap")
    @Operation(summary = "Get sitemap entries", description = "Dynamic XML sitemap entries containing published studios and public projects")
    public ResponseEntity<List<SitemapItemDto>> getSitemapEntries() {
        return ResponseEntity.ok(seoService.getSitemapData());
    }
}
