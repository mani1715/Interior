package com.interior.platform.designers.service;

import com.interior.platform.designers.dto.SlugCheckResponse;
import com.interior.platform.designers.repository.StudioRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SlugValidationService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static final Set<String> RESERVED_SLUGS = Set.of(
            "admin", "api", "auth", "account", "projects", "professionals",
            "categories", "locations", "dashboard", "settings", "support",
            "design-system", "onboarding", "signin", "signup", "login",
            "logout", "privacy", "terms", "about", "contact", "help",
            "pricing", "billing", "media", "explore", "search", "app",
            "public", "static", "assets", "health", "metrics", "workspace"
    );

    private final StudioRepository studioRepository;

    public SlugValidationService(StudioRepository studioRepository) {
        this.studioRepository = studioRepository;
    }

    /**
     * Generates a safe, normalized ASCII slug from a business name.
     */
    public String generateSlug(String name) {
        if (name == null || name.isBlank()) {
            return "studio";
        }

        // 1. Normalize unicode diacritics
        String normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");

        // 2. Lowercase and replace non-alphanumerics with hyphens
        String lower = withoutDiacritics.toLowerCase(Locale.ROOT);
        String slug = NON_ALPHANUMERIC.matcher(lower).replaceAll("-");

        // 3. Trim leading/trailing hyphens
        slug = slug.replaceAll("^-+|-+$", "");

        // 4. Fallback if input was purely non-Latin (e.g. Indic scripts) without ASCII representation
        if (slug.length() < 3) {
            String hash = Integer.toHexString(Math.abs(name.hashCode())).toLowerCase(Locale.ROOT);
            slug = "studio-" + (hash.length() > 6 ? hash.substring(0, 6) : hash);
        }

        // 5. Enforce max length 64
        if (slug.length() > 64) {
            slug = slug.substring(0, 64).replaceAll("-+$", "");
        }

        return slug;
    }

    /**
     * Validates a candidate slug for syntax, reserved namespaces, and database availability.
     */
    public SlugCheckResponse checkAvailability(String candidateSlug) {
        if (candidateSlug == null || candidateSlug.isBlank()) {
            return SlugCheckResponse.unavailable("", "Slug cannot be empty", null);
        }

        String slug = candidateSlug.trim().toLowerCase(Locale.ROOT);

        if (slug.length() < 3) {
            return SlugCheckResponse.unavailable(slug, "Slug must be at least 3 characters long", null);
        }

        if (slug.length() > 64) {
            return SlugCheckResponse.unavailable(slug, "Slug cannot exceed 64 characters", null);
        }

        if (!SLUG_PATTERN.matcher(slug).matches()) {
            return SlugCheckResponse.unavailable(slug, "Slug may only contain lowercase letters, numbers, and hyphens without consecutive or trailing hyphens", null);
        }

        if (RESERVED_SLUGS.contains(slug)) {
            String suggested = slug + "-studio";
            return SlugCheckResponse.unavailable(slug, "This handle is a reserved platform keyword", suggested);
        }

        if (studioRepository.isSlugClaimed(slug)) {
            String suggested = findAvailableAlternative(slug);
            return SlugCheckResponse.unavailable(slug, "This handle is already taken by another studio", suggested);
        }

        return SlugCheckResponse.available(slug);
    }

    private String findAvailableAlternative(String baseSlug) {
        for (int i = 2; i <= 99; i++) {
            String candidate = baseSlug + "-" + i;
            if (!RESERVED_SLUGS.contains(candidate) && !studioRepository.isSlugClaimed(candidate)) {
                return candidate;
            }
        }
        return baseSlug + "-design";
    }
}
