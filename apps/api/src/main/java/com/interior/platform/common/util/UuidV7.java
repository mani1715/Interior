package com.interior.platform.common.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Standard RFC 9562 UUIDv7 generator.
 * Encodes a 48-bit millisecond timestamp, 4-bit version (0111),
 * 12-bit rand_a, 2-bit variant (10), and 62-bit rand_b.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {}

    /**
     * Generates a new time-ordered UUIDv7 based on current system millisecond time.
     */
    public static UUID randomUuid() {
        return generate(System.currentTimeMillis());
    }

    /**
     * Generates a UUIDv7 using the specified millisecond timestamp.
     */
    public static UUID generate(long timestampMs) {
        byte[] randomBytes = new byte[10];
        RANDOM.nextBytes(randomBytes);

        // 48-bit timestamp + 4-bit version (0x7) + 12-bit rand_a
        long mostSigBits = (timestampMs & 0xFFFFFFFFFFFFL) << 16;
        mostSigBits |= 0x7000L; // version 7
        mostSigBits |= ((long) (randomBytes[0] & 0x0F)) << 8;
        mostSigBits |= ((long) (randomBytes[1] & 0xFF));

        // 2-bit variant (10) + 62-bit rand_b
        long leastSigBits = 0x8000000000000000L; // variant 2
        leastSigBits |= ((long) (randomBytes[2] & 0x3F)) << 56;
        leastSigBits |= ((long) (randomBytes[3] & 0xFF)) << 48;
        leastSigBits |= ((long) (randomBytes[4] & 0xFF)) << 40;
        leastSigBits |= ((long) (randomBytes[5] & 0xFF)) << 32;
        leastSigBits |= ((long) (randomBytes[6] & 0xFF)) << 24;
        leastSigBits |= ((long) (randomBytes[7] & 0xFF)) << 16;
        leastSigBits |= ((long) (randomBytes[8] & 0xFF)) << 8;
        leastSigBits |= ((long) (randomBytes[9] & 0xFF));

        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Checks if a given UUID is a version 7 UUID.
     */
    public static boolean isUuidV7(UUID uuid) {
        return uuid != null && uuid.version() == 7;
    }

    /**
     * Extracts the 48-bit Unix timestamp in milliseconds from a UUIDv7.
     */
    public static long extractTimestampMs(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        return uuid.getMostSignificantBits() >>> 16;
    }
}
