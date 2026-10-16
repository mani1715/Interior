package com.interior.platform.email.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Validates Svix / Resend cryptographic webhook signatures.
 * Protocol:
 *  - Headers: svix-id, svix-timestamp, svix-signature (or webhook-id, webhook-timestamp, webhook-signature)
 *  - Secret: format 'whsec_<base64>' or raw secret
 *  - Signed payload: "${svix-id}.${svix-timestamp}.${body}"
 *  - Signature format: space-separated 'v1,<base64-signature>'
 *  - Enforces replay tolerance window (default: 300 seconds)
 *  - Constant-time signature comparison
 */
@Component
public class ResendWebhookSignatureValidator {

    private static final Logger log = LoggerFactory.getLogger(ResendWebhookSignatureValidator.class);
    private static final long DEFAULT_TOLERANCE_SECONDS = 300L; // 5 minutes

    public boolean isValid(String rawBody, Map<String, String> headers, String secret) {
        return isValid(rawBody, headers, secret, DEFAULT_TOLERANCE_SECONDS);
    }

    public boolean isValid(String rawBody, Map<String, String> headers, String secret, long toleranceSeconds) {
        if (rawBody == null || headers == null || secret == null || secret.isBlank()) {
            return false;
        }

        String msgId = getHeaderIgnoreCase(headers, "svix-id", "webhook-id");
        String timestampStr = getHeaderIgnoreCase(headers, "svix-timestamp", "webhook-timestamp");
        String signatureHeader = getHeaderIgnoreCase(headers, "svix-signature", "webhook-signature");

        if (msgId == null || msgId.isBlank() || timestampStr == null || timestampStr.isBlank() || signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Resend webhook rejected: missing required Svix headers (id, timestamp, or signature)");
            return false;
        }

        // Validate timestamp to prevent replay attacks
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            log.warn("Resend webhook rejected: invalid timestamp format: {}", timestampStr);
            return false;
        }

        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > toleranceSeconds) {
            log.warn("Resend webhook rejected: timestamp outside replay window (timestamp={}, now={}, diff={})",
                    timestamp, now, Math.abs(now - timestamp));
            return false;
        }

        // Decode secret key
        byte[] keyBytes;
        try {
            if (secret.startsWith("whsec_")) {
                keyBytes = Base64.getDecoder().decode(secret.substring(6));
            } else {
                // If not starting with whsec_, attempt base64 decode; fallback to raw UTF-8 bytes
                try {
                    keyBytes = Base64.getDecoder().decode(secret);
                } catch (IllegalArgumentException e) {
                    keyBytes = secret.getBytes(StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            log.error("Failed to decode Resend webhook signing secret: {}", e.getMessage());
            return false;
        }

        // Compute HMAC-SHA256
        String toSign = msgId + "." + timestampStr + "." + rawBody;
        String expectedSignature;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] hash = mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8));
            expectedSignature = Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error calculating Resend webhook HMAC: {}", e.getMessage());
            return false;
        }

        // Compare against signature candidates (e.g. 'v1,sig1 v1,sig2')
        String[] signatureParts = signatureHeader.split(" ");
        for (String part : signatureParts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("v1,")) {
                String candidate = trimmed.substring(3);
                if (MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8))) {
                    return true;
                }
            }
        }

        log.warn("Resend webhook rejected: signature mismatch");
        return false;
    }

    private String getHeaderIgnoreCase(Map<String, String> headers, String... candidates) {
        for (String candidate : candidates) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (candidate.equalsIgnoreCase(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }
}
