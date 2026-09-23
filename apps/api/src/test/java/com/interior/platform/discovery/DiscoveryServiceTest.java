package com.interior.platform.discovery;

import com.interior.platform.common.exception.RateLimitExceededException;
import com.interior.platform.discovery.dto.*;
import com.interior.platform.discovery.repository.DiscoveryRepository;
import com.interior.platform.discovery.service.DiscoveryService;
import com.interior.platform.security.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DiscoveryServiceTest {

    private DiscoveryRepository discoveryRepository;
    private RateLimiterService rateLimiterService;
    private DiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        discoveryRepository = mock(DiscoveryRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-23T10:00:00Z"), ZoneOffset.UTC);
        rateLimiterService = new RateLimiterService(clock);
        discoveryService = new DiscoveryService(discoveryRepository, rateLimiterService);
    }

    @Test
    @DisplayName("Input sanitization: collapses whitespace and caps at 100 chars")
    void testInputSanitization() {
        String longQuery = "   modern    minimalist      " + "a".repeat(120);
        DiscoverySearchParams raw = new DiscoverySearchParams(
                longQuery, " LIVING_ROOM ", "  MODERN_MINIMALIST  ", "  Bengaluru ", null, null, null, null, "relevance", 20, 0, null
        );

        when(discoveryRepository.searchProjects(any())).thenReturn(List.of());
        when(discoveryRepository.countProjects(any())).thenReturn(0L);
        when(discoveryRepository.getFacets(any())).thenReturn(new DiscoveryFacetsDto(List.of(), List.of(), List.of(), List.of()));

        discoveryService.searchProjects(raw);

        ArgumentCaptor<DiscoverySearchParams> captor = ArgumentCaptor.forClass(DiscoverySearchParams.class);
        verify(discoveryRepository).searchProjects(captor.capture());

        DiscoverySearchParams sanitized = captor.getValue();
        assertNotNull(sanitized.q());
        assertTrue(sanitized.q().length() <= 100);
        assertFalse(sanitized.q().contains("   "));
        assertEquals("LIVING_ROOM", sanitized.category());
        assertEquals("MODERN_MINIMALIST", sanitized.style());
        assertEquals("Bengaluru", sanitized.city());
    }

    @Test
    @DisplayName("Pagination bounds: safe limit default 12, max 50; offset max 1000")
    void testPaginationBounds() {
        DiscoverySearchParams rawUnder = new DiscoverySearchParams(null, null, null, null, null, null, null, null, null, -5, -10, null);
        assertEquals(12, rawUnder.getSafeLimit());
        assertEquals(0, rawUnder.getSafeOffset());

        DiscoverySearchParams rawOver = new DiscoverySearchParams(null, null, null, null, null, null, null, null, null, 150, 5000, null);
        assertEquals(50, rawOver.getSafeLimit());
        assertEquals(1000, rawOver.getSafeOffset());
    }

    @Test
    @DisplayName("Suggestions rate limiting: 20 req/min succeeds, 21st throws 429")
    void testSuggestionsRateLimiting() {
        when(discoveryRepository.getSuggestions(anyString(), anyInt()))
                .thenReturn(new DiscoverySuggestionsResponse(List.of(), List.of(), List.of(), List.of(), List.of()));

        String ip = "192.168.1.100";
        for (int i = 0; i < 20; i++) {
            assertDoesNotThrow(() -> discoveryService.getSuggestions("kitchen", ip));
        }

        assertThrows(RateLimitExceededException.class, () -> discoveryService.getSuggestions("kitchen", ip));
    }

    @Test
    @DisplayName("Suggestions for query shorter than 2 chars returns empty list immediately")
    void testSuggestionsShortQuery() {
        DiscoverySuggestionsResponse res = discoveryService.getSuggestions("a", "127.0.0.1");
        assertTrue(res.categories().isEmpty());
        assertTrue(res.cities().isEmpty());
        assertTrue(res.projects().isEmpty());
        verifyNoInteractions(discoveryRepository);
    }
}
