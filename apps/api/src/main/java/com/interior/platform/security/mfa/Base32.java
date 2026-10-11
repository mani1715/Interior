package com.interior.platform.security.mfa;

import java.util.Arrays;

/**
 * Standard RFC 4648 Base32 encoder and decoder.
 */
public final class Base32 {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final byte[] DECODE_TABLE = new byte[128];

    static {
        Arrays.fill(DECODE_TABLE, (byte) -1);
        for (int i = 0; i < ALPHABET.length(); i++) {
            char c = ALPHABET.charAt(i);
            DECODE_TABLE[c] = (byte) i;
            DECODE_TABLE[Character.toLowerCase(c)] = (byte) i;
        }
    }

    private Base32() {}

    public static String encode(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                out.append(ALPHABET.charAt(index));
            }
        }

        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            out.append(ALPHABET.charAt(index));
        }

        return out.toString();
    }

    public static byte[] decode(String encoded) {
        if (encoded == null) {
            return new byte[0];
        }
        // Normalize: uppercase, remove spaces, hyphens, and padding '='
        String clean = encoded.replace("-", "")
                              .replace(" ", "")
                              .replace("=", "")
                              .trim()
                              .toUpperCase();

        if (clean.isEmpty()) {
            return new byte[0];
        }

        int buffer = 0;
        int bitsLeft = 0;
        byte[] output = new byte[clean.length() * 5 / 8];
        int count = 0;

        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (c >= DECODE_TABLE.length || DECODE_TABLE[c] == -1) {
                throw new IllegalArgumentException("Illegal character in Base32 string: " + c);
            }
            int val = DECODE_TABLE[c] & 0xFF;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }

        if (count == output.length) {
            return output;
        }
        return Arrays.copyOf(output, count);
    }
}
