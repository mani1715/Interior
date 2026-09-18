'use client';

import React, { useState, useEffect } from 'react';
import { Search, X } from 'lucide-react';

export interface DiscoverySearchBarProps {
  initialQuery?: string;
  onSearch: (q: string) => void;
  placeholder?: string;
  suggestions?: string[];
}

const DEFAULT_SUGGESTIONS = [
  'TV unit',
  'Modular kitchen',
  'Guntur',
  'Amaravati',
  'Hyderabad',
  '3BHK',
  'Luxury',
];

export function DiscoverySearchBar({
  initialQuery = '',
  onSearch,
  placeholder = 'Search TV units, wardrobes, modular kitchens, styles...',
  suggestions = DEFAULT_SUGGESTIONS,
}: DiscoverySearchBarProps) {
  const [query, setQuery] = useState(initialQuery);

  useEffect(() => {
    setQuery(initialQuery);
  }, [initialQuery]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch(query.trim());
  };

  const handleClear = () => {
    setQuery('');
    onSearch('');
  };

  const handleSuggestionClick = (term: string) => {
    setQuery(term);
    onSearch(term);
  };

  return (
    <div className="w-full max-w-3xl mx-auto">
      <form onSubmit={handleSubmit} className="relative w-full">
        <div className="relative flex items-center rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-md focus-within:border-[var(--brand)] focus-within:ring-2 focus-within:ring-[var(--brand)]/20 transition-all">
          <div className="pl-4 text-[var(--muted)]">
            <Search className="w-5 h-5 text-[var(--brand)]" />
          </div>

          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={placeholder}
            aria-label="Search interior projects"
            className="w-full py-3.5 sm:py-4 pl-3 pr-24 bg-transparent text-sm sm:text-base text-[var(--foreground)] placeholder:text-[var(--muted)] focus:outline-none"
          />

          <div className="absolute right-2 flex items-center gap-1.5">
            {query && (
              <button
                type="button"
                onClick={handleClear}
                aria-label="Clear search input"
                className="p-1.5 rounded-lg text-[var(--muted)] hover:text-[var(--foreground)] hover:bg-[var(--surface-alt)] transition-colors min-h-[36px] min-w-[36px] flex items-center justify-center"
              >
                <X className="w-4 h-4" />
              </button>
            )}

            <button
              type="submit"
              className="px-4 py-2 rounded-xl bg-[var(--brand)] text-[var(--charcoal)] font-semibold text-xs sm:text-sm hover:brightness-105 active:scale-95 transition-all shadow-sm min-h-[38px]"
            >
              Search
            </button>
          </div>
        </div>
      </form>

      {/* Suggested Search Chips */}
      {suggestions.length > 0 && (
        <div className="flex items-center gap-1.5 flex-wrap mt-3 px-1 text-xs text-[var(--muted)]">
          <span className="text-[11px] font-medium mr-1">Popular:</span>
          {suggestions.map((term, idx) => (
            <button
              key={idx}
              type="button"
              onClick={() => handleSuggestionClick(term)}
              className="px-2.5 py-1 rounded-full text-[11px] font-medium bg-[var(--surface)] border border-[var(--border)] text-[var(--muted)] hover:text-[var(--brand)] hover:border-[var(--brand)] transition-colors min-h-[28px]"
            >
              {term}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
