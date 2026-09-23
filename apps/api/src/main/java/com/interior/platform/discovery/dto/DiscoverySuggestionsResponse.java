package com.interior.platform.discovery.dto;

import java.util.List;

public record DiscoverySuggestionsResponse(
        List<SuggestionItem> categories,
        List<SuggestionItem> cities,
        List<SuggestionItem> styles,
        List<SuggestionItem> studios,
        List<SuggestionItem> projects
) {
    public record SuggestionItem(
            String text,
            String type,
            String slug,
            String meta
    ) {}
}
