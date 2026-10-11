package com.interior.platform.security.mfa;

import com.interior.platform.security.config.AuthSecurityProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Standard RFC 6238 TOTP engine with AES-256-GCM secret encryption at rest.
 */
@Service
public class TotpService {

    private static final int SECRET_BYTE_LENGTH = 20; // 160 bits (recommended by RFC 6238)
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int GCM_IV_LENGTH = 12; // 96-bit nonce
    private static final int GCM_TAG_LENGTH = 128; // 128-bit authentication tag

    private final SecureRandom secureRandom = new SecureRandom();
    private final AuthSecurityProperties properties;

    public record TotpVerificationResult(boolean isValid, long matchedCounter) {
        public static TotpVerificationResult invalid() {
            return new TotpVerificationResult(false, -1);
        }
        public static TotpVerificationResult valid(long counter) {
            return new TotpVerificationResult(true, counter);
        }
    }

    public TotpService(AuthSecurityProperties properties) {
        this.properties = properties;
    }

    /**
     * Generates a cryptographically secure random 160-bit TOTP secret.
     */
    public byte[] generateSecret() {
        byte[] secret = new byte[SECRET_BYTE_LENGTH];
        secureRandom.nextBytes(secret);
        return secret;
    }

    /**
     * Builds standard otpauth URI for QR codes and authenticator apps.
     */
    public String buildOtpAuthUri(String userEmail, String base32Secret) {
        String issuer = properties.getMfa().getIssuer();
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8).replace("+", "%20");
        String encodedEmail = URLEncoder.encode(userEmail, StandardCharsets.UTF_8).replace("+", "%20");
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                encodedIssuer,
                encodedEmail,
                base32Secret,
                encodedIssuer,
                DIGITS,
                TIME_STEP_SECONDS
        );
    }

    /**
     * Calculates the 6-digit TOTP code for a given timestamp and counter.
     */
    public String generateCode(byte[] secret, long counter) {
        try {
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate TOTP code", e);
        }
    }

    /**
     * Verifies user-supplied TOTP code with clock skew tolerance and replay protection.
     */
    public TotpVerificationResult verifyCode(byte[] secret, String rawCode, Instant timestamp, Long lastUsedCounter) {
        if (rawCode == null || secret == null) {
            return TotpVerificationResult.invalid();
        }

        String cleanCode = rawCode.trim().replace(" ", "").replace("-", "");
        if (cleanCode.length() != DIGITS || !cleanCode.chars().allMatch(Character::isDigit)) {
            return TotpVerificationResult.invalid();
        }

        long currentCounter = timestamp.getEpochSecond() / TIME_STEP_SECONDS;
        int window = properties.getMfa().getAllowedClockSkewSteps();

        for (int i = -window; i <= window; i++) {
            long testCounter = currentCounter + i;
            if (lastUsedCounter != null && testCounter <= lastUsedCounter) {
                // Replay prevention: do not accept code for a counter equal to or prior to last used
                continue;
            }
            String expected = generateCode(secret, testCounter);
            if (MessageDigest.isEqual(cleanCode.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
                return TotpVerificationResult.valid(testCounter);
            }
        }

        return TotpVerificationResult.invalid();
    }

    /**
     * Encrypts the raw TOTP secret using AES-256-GCM for secure database persistence at rest.
     * Output format: "v{version}:{Base64(IV + Ciphertext + Tag)}"
     */
    public String encryptSecret(byte[] secret, int keyVersion) {
        try {
            byte[] keyBytes = getMasterKeyBytes();
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(secret);

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return "v" + keyVersion + ":" + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt TOTP secret", e);
        }
    }

    /**
     * Decrypts the stored AES-256-GCM encrypted TOTP secret.
     */
    public byte[] decryptSecret(String encryptedPayload) {
        if (encryptedPayload == null || !encryptedPayload.contains(":")) {
            throw new IllegalArgumentException("Invalid encrypted secret payload");
        }
        try {
            int colonIdx = encryptedPayload.indexOf(':');
            String payloadBase64 = encryptedPayload.substring(colonIdx + 1);
            byte[] combined = Base64.getDecoder().decode(payloadBase64);

            if (combined.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Payload too short");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            byte[] keyBytes = getMasterKeyBytes();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt TOTP secret", e);
        }
    }

    private byte[] getMasterKeyBytes() {
        String key = properties.getMfa().getEncryptionKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("MFA master encryption key is not configured");
        }
        try {
            // Derive a 256-bit key reliably using SHA-256
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return sha.digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
