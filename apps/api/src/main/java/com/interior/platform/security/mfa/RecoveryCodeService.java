package com.interior.platform.security.mfa;

import com.interior.platform.security.config.AuthSecurityProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Service managing cryptographically secure one-time recovery codes for account recovery.
 */
@Service
public class RecoveryCodeService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final AuthSecurityProperties properties;

    public record GeneratedRecoveryCodes(
        List<String> rawCodes,
        List<String> hashedCodes
    ) {}

    public RecoveryCodeService(AuthSecurityProperties properties) {
        this.properties = properties;
    }

    /**
     * Generates a batch of high-entropy recovery codes and their secure SHA-256 hashes.
     */
    public GeneratedRecoveryCodes generateBatch() {
        int count = properties.getMfa().getRecoveryCodesCount();
        List<String> rawCodes = new ArrayList<>(count);
        List<String> hashedCodes = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            byte[] bytes = new byte[8]; // 64 bits of entropy per code
            secureRandom.nextBytes(bytes);
            String hex = HexFormat.of().formatHex(bytes).toLowerCase();
            // Format as XXXX-XXXX-XXXX-XXXX
            String formatted = String.format("%s-%s-%s-%s",
                    hex.substring(0, 4),
                    hex.substring(4, 8),
                    hex.substring(8, 12),
                    hex.substring(12, 16));

            rawCodes.add(formatted);
            hashedCodes.add(hashCode(formatted));
        }

        return new GeneratedRecoveryCodes(rawCodes, hashedCodes);
    }

    /**
     * Hashes a recovery code for secure database persistence at rest.
     */
    public String hashCode(String rawCode) {
        if (rawCode == null) {
            throw new IllegalArgumentException("Recovery code cannot be null");
        }
        String clean = rawCode.trim().replace("-", "").replace(" ", "").toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(clean.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
