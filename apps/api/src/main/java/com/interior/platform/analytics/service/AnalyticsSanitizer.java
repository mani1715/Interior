package com.interior.platform.analytics.service;

import com.interior.platform.analytics.domain.AnalyticsEventType;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

public final class AnalyticsSanitizer {

    private static final Set<String> ALLOWED_METADATA_KEYS = Set.of(
            "categoryCode",
            "sourceRoute",
            "deviceClass",
            "referrerDomain",
            "completionYear"
    );

    private AnalyticsSanitizer() {}

    public static String sanitizeReferrer(String rawReferrer) {
        if (rawReferrer == null || rawReferrer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(rawReferrer.trim());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            // Retain only safe domain/host; strictly discard paths and query tokens
            return host.toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    public static String sanitizeUrl(String rawUrl) {
        return sanitizeReferrer(rawUrl);
    }

    public static String sanitizeSource(String rawSource) {
        if (rawSource == null || rawSource.isBlank()) {
            return "direct";
        }
        String scrubbed = rawSource.replaceAll("[^a-zA-Z0-9_\\-\\.]", "").trim();
        return scrubbed.length() > 64 ? scrubbed.substring(0, 64) : scrubbed;
    }

    public static String sanitizeDeviceClass(String rawDeviceClass) {
        if (rawDeviceClass == null || rawDeviceClass.isBlank()) {
            return "UNKNOWN";
        }
        String upper = rawDeviceClass.trim().toUpperCase();
        return switch (upper) {
            case "MOBILE" -> "MOBILE";
            case "TABLET" -> "TABLET";
            case "DESKTOP" -> "DESKTOP";
            default -> "UNKNOWN";
        };
    }

    public static String hashSession(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    public static Map<String, Object> sanitizeMetadata(AnalyticsEventType eventType, Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> clean = new HashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String key = entry.getKey();
            if (ALLOWED_METADATA_KEYS.contains(key)) {
                Object val = entry.getValue();
                if (val instanceof String s) {
                    // Strip html tags and bound length to 64 chars
                    String scrubbed = s.replaceAll("<[^>]*>", "").trim();
                    if (!scrubbed.isEmpty() && scrubbed.length() <= 64) {
                        clean.put(key, scrubbed);
                    }
                } else if (val instanceof Number) {
                    clean.put(key, val);
                } else if (val instanceof Boolean) {
                    clean.put(key, val);
                }
            }
        }
        return clean;
    }
}
