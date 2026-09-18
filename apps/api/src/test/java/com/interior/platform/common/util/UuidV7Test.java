package com.interior.platform.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UuidV7Test {

    @Test
    @DisplayName("Generated UUID has version 7 and variant 2 (RFC 9562 compliant)")
    void testUuidV7Compliance() {
        UUID uuid = UuidV7.randomUuid();

        assertNotNull(uuid);
        assertEquals(7, uuid.version(), "UUID version must be 7");
        assertEquals(2, uuid.variant(), "UUID variant must be 2 (IETF)");
        assertTrue(UuidV7.isUuidV7(uuid), "isUuidV7 must return true");
    }

    @Test
    @DisplayName("Extracted timestamp matches generation timestamp within reasonable delta")
    void testTimestampExtraction() {
        long before = System.currentTimeMillis();
        UUID uuid = UuidV7.randomUuid();
        long after = System.currentTimeMillis();

        long extracted = UuidV7.extractTimestampMs(uuid);

        assertTrue(extracted >= before, "Extracted timestamp should be >= before time");
        assertTrue(extracted <= after, "Extracted timestamp should be <= after time");
    }

    @Test
    @DisplayName("Deterministic timestamp generation accurately encodes given timestamp")
    void testDeterministicTimestamp() {
        long customTimestamp = 1774000000000L; // Future timestamp
        UUID uuid = UuidV7.generate(customTimestamp);

        assertEquals(7, uuid.version());
        assertEquals(2, uuid.variant());
        assertEquals(customTimestamp, UuidV7.extractTimestampMs(uuid));
    }

    @Test
    @DisplayName("High volume generation produces 10,000 unique UUIDv7 identifiers")
    void testUniquenessAcrossHighVolume() {
        int count = 10000;
        Set<UUID> set = new HashSet<>(count);

        for (int i = 0; i < count; i++) {
            UUID uuid = UuidV7.randomUuid();
            assertEquals(7, uuid.version());
            set.add(uuid);
        }

        assertEquals(count, set.size(), "All 10,000 UUIDv7 values must be unique");
    }

    @Test
    @DisplayName("Time-ordered monotonicity: sequentially generated UUIDs have non-decreasing timestamps")
    void testMonotonicity() throws InterruptedException {
        UUID uuid1 = UuidV7.randomUuid();
        Thread.sleep(5);
        UUID uuid2 = UuidV7.randomUuid();

        assertTrue(UuidV7.extractTimestampMs(uuid2) >= UuidV7.extractTimestampMs(uuid1));
    }
}
