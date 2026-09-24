package com.interior.platform.leads.service;

import com.interior.platform.common.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class PhoneNormalizationService {

    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    /**
     * Normalizes a raw phone number into strict E.164 representation.
     * India-first UX: auto-prefixes +91 for valid 10-digit Indian mobile numbers (starting with 6,7,8,9).
     */
    public String normalize(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new BadRequestException("Phone number is required");
        }

        // Strip spaces, dashes, dots, parentheses
        String cleaned = rawPhone.replaceAll("[\\s\\-\\.\\(\\)]", "");

        // Indian 10-digit format (e.g., 9876543210)
        if (cleaned.matches("^[6-9]\\d{9}$")) {
            return "+91" + cleaned;
        }

        // Indian 11-digit leading zero format (e.g., 09876543210)
        if (cleaned.matches("^0[6-9]\\d{9}$")) {
            return "+91" + cleaned.substring(1);
        }

        // Indian 12-digit format with country code but no plus (e.g., 919876543210)
        if (cleaned.matches("^91[6-9]\\d{9}$")) {
            return "+" + cleaned;
        }

        // International E.164 format with plus (e.g., +919876543210 or +12125551234)
        if (cleaned.startsWith("+")) {
            if (E164_PATTERN.matcher(cleaned).matches()) {
                return cleaned;
            }
        }

        throw new BadRequestException("Invalid phone number format: " + rawPhone + ". Please provide a valid phone number with country code.");
    }

    /**
     * Masks phone number for privacy in list views (e.g., +91 ••••• •4321).
     */
    public String mask(String normalizedPhone) {
        if (normalizedPhone == null || normalizedPhone.length() < 7) {
            return "••••••";
        }
        int len = normalizedPhone.length();
        String prefix = normalizedPhone.substring(0, Math.min(3, len));
        String suffix = normalizedPhone.substring(Math.max(0, len - 4));
        return prefix + " ••••• •" + suffix;
    }
}
