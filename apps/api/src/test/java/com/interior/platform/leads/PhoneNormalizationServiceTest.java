package com.interior.platform.leads;

import com.interior.platform.common.exception.BadRequestException;
import com.interior.platform.leads.service.PhoneNormalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PhoneNormalizationService Unit Tests")
class PhoneNormalizationServiceTest {

    private PhoneNormalizationService service;

    @BeforeEach
    void setUp() {
        service = new PhoneNormalizationService();
    }

    @Test
    @DisplayName("India 10-digit mobile number starting with 6-9 is normalized to +91")
    void testIndian10Digit() {
        assertEquals("+919876543210", service.normalize("9876543210"));
        assertEquals("+918765432109", service.normalize("8765432109"));
        assertEquals("+917654321098", service.normalize("7654321098"));
        assertEquals("+916543210987", service.normalize("6543210987"));
    }

    @Test
    @DisplayName("India 11-digit mobile number with leading zero is normalized to +91")
    void testIndian11DigitLeadingZero() {
        assertEquals("+919876543210", service.normalize("09876543210"));
        assertEquals("+918765432109", service.normalize("08765432109"));
    }

    @Test
    @DisplayName("India 12-digit mobile number starting with 91 is normalized to +91")
    void testIndian12DigitWithCountryCode() {
        assertEquals("+919876543210", service.normalize("919876543210"));
    }

    @Test
    @DisplayName("International E.164 formats with + are preserved")
    void testInternationalE164() {
        assertEquals("+919876543210", service.normalize("+919876543210"));
        assertEquals("+14155552671", service.normalize("+14155552671"));
        assertEquals("+447911123456", service.normalize("+447911123456"));
    }

    @Test
    @DisplayName("Punctuation, spaces, and dashes are stripped cleanly")
    void testPunctuationStripping() {
        assertEquals("+919876543210", service.normalize("+91 98765-43210"));
        assertEquals("+919876543210", service.normalize("(+91) 98765 43210"));
        assertEquals("+919876543210", service.normalize("98765.43210"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abc", "12345", "+123", "5551234567", "invalid-phone"})
    @DisplayName("Invalid phone numbers throw BadRequestException")
    void testInvalidPhoneNumbers(String invalid) {
        assertThrows(BadRequestException.class, () -> service.normalize(invalid));
    }

    @Test
    @DisplayName("Null phone number throws BadRequestException")
    void testNullPhone() {
        assertThrows(BadRequestException.class, () -> service.normalize(null));
    }

    @Test
    @DisplayName("Phone masking conceals middle digits for privacy")
    void testMasking() {
        String masked = service.mask("+919876543210");
        assertTrue(masked.startsWith("+91"));
        assertTrue(masked.endsWith("3210"));
        assertTrue(masked.contains("••••"));

        assertEquals("••••••", service.mask("12345"));
        assertEquals("••••••", service.mask(null));
    }
}
