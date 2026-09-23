'use client';

import React, { useState, useEffect, useRef, useId } from 'react';
import { Search, X, Sparkles, Building2, MapPin, Palette, FolderGit2 } from 'lucide-react';
import { fetchDiscoverySuggestions, SuggestionItem } from '@/lib/discovery/api';

export interface DiscoverySearchBarProps {
  initialQuery?: string;
  onSearch: (q: string) => void;
  placeholder?: string;
  suggestions?: string[];
}

const DEFAULT_SUGGESTIONS = [
  'TV unit',
  'Modular kitchen',
  'Bengaluru',
  'Mumbai',
  'Hyderabad',
  'Scandinavian',
  'Minimalist',
];

export function DiscoverySearchBar({
  initialQuery = '',
  onSearch,
  placeholder = 'Search TV units, wardrobes, modular kitchens, styles...',
  suggestions = DEFAULT_SUGGESTIONS,
}: DiscoverySearchBarProps) {
  const [query, setQuery] = useState(initialQuery);
  const [liveSuggestions, setLiveSuggestions] = useState<SuggestionItem[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listboxId = useId();

  useEffect(() => {
    setQuery(initialQuery);
  }, [initialQuery]);

  // Debounced live suggestions fetching
  useEffect(() => {
    if (!query || query.trim().length < 2) {
      setLiveSuggestions([]);
      setIsOpen(false);
      setActiveIndex(-1);
      return;
    }

    const timer = setTimeout(async () => {
      try {
        const res = await fetchDiscoverySuggestions(query);
        const combined: SuggestionItem[] = [
          ...res.categories,
          ...res.styles,
          ...res.cities,
          ...res.studios,
          ...res.projects,
        ];
        setLiveSuggestions(combined);
        setIsOpen(combined.length > 0);
        setActiveIndex(-1);
      } catch {
        // Silently ignore suggestion fetch errors
        setLiveSuggestions([]);
        setIsOpen(false);
      }
    }, 250);

    return () => clearTimeout(timer);
  }, [query]);

  // Dismiss dropdown on click outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
        setActiveIndex(-1);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (activeIndex >= 0 && activeIndex < liveSuggestions.length) {
      handleSelectSuggestion(liveSuggestions[activeIndex]);
    } else {
      setIsOpen(false);
      onSearch(query.trim());
    }
  };

  const handleClear = () => {
    setQuery('');
    setLiveSuggestions([]);
    setIsOpen(false);
    setActiveIndex(-1);
    onSearch('');
    inputRef.current?.focus();
  };

  const handleSelectSuggestion = (item: SuggestionItem) => {
    setQuery(item.text);
    setIsOpen(false);
    setActiveIndex(-1);
    onSearch(item.text);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!isOpen || liveSuggestions.length === 0) {
      if (e.key === 'Escape') {
        setIsOpen(false);
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setActiveIndex((prev) => (prev < liveSuggestions.length - 1 ? prev + 1 : 0));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setActiveIndex((prev) => (prev > 0 ? prev - 1 : liveSuggestions.length - 1));
        break;
      case 'Enter':
        if (activeIndex >= 0 && activeIndex < liveSuggestions.length) {
          e.preventDefault();
          handleSelectSuggestion(liveSuggestions[activeIndex]);
        }
        break;
      case 'Escape':
        e.preventDefault();
        setIsOpen(false);
        setActiveIndex(-1);
        break;
    }
  };

  const renderIcon = (type: SuggestionItem['type']) => {
    switch (type) {
      case 'category':
        return <FolderGit2 className="w-3.5 h-3.5 text-[var(--brand)]" aria-hidden="true" />;
      case 'style':
        return <Palette className="w-3.5 h-3.5 text-[var(--brand)]" aria-hidden="true" />;
      case 'city':
        return <MapPin className="w-3.5 h-3.5 text-[var(--brand)]" aria-hidden="true" />;
      case 'studio':
        return <Building2 className="w-3.5 h-3.5 text-[var(--brand)]" aria-hidden="true" />;
      default:
        return <Sparkles className="w-3.5 h-3.5 text-[var(--brand)]" aria-hidden="true" />;
    }
  };

  return (
    <div ref={containerRef} className="w-full max-w-3xl mx-auto relative">
      <form onSubmit={handleSubmit} className="relative w-full">
        <div className="relative flex items-center rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-md focus-within:border-[var(--brand)] focus-within:ring-2 focus-within:ring-[var(--brand)]/20 transition-all">
          <div className="pl-4 text-[var(--muted)] pointer-events-none">
            <Search className="w-5 h-5 text-[var(--brand)]" aria-hidden="true" />
          </div>

          <input
            ref={inputRef}
            type="text"
            role="combobox"
            aria-autocomplete="list"
            aria-expanded={isOpen}
            aria-controls={isOpen ? listboxId : undefined}
            aria-activedescendant={
              isOpen && activeIndex >= 0 ? `${listboxId}-opt-${activeIndex}` : undefined
            }
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => {
              if (liveSuggestions.length > 0) setIsOpen(true);
            }}
            placeholder={placeholder}
            aria-label="Search interior projects"
            className="w-full py-3.5 sm:py-4 pl-3 pr-24 bg-transparent text-sm sm:text-base text-[var(--foreground)] placeholder:text-[var(--muted)] focus:outline-none min-h-[44px]"
          />

          <div className="absolute right-2 flex items-center gap-1.5">
            {query && (
              <button
                type="button"
                onClick={handleClear}
                aria-label="Clear search input"
                className="p-2 rounded-lg text-[var(--muted)] hover:text-[var(--foreground)] hover:bg-[var(--surface-alt)] transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center"
              >
                <X className="w-4 h-4" />
              </button>
            )}

            <button
              type="submit"
              className="px-4 py-2 rounded-xl bg-[var(--brand)] text-[var(--charcoal)] font-semibold text-xs sm:text-sm hover:brightness-105 active:scale-95 transition-all shadow-sm min-h-[44px] flex items-center justify-center"
            >
              Search
            </button>
          </div>
        </div>
      </form>

      {/* Autocomplete Dropdown List */}
      {isOpen && liveSuggestions.length > 0 && (
        <ul
          id={listboxId}
          role="listbox"
          aria-label="Search suggestions"
          className="absolute z-50 top-full left-0 right-0 mt-2 bg-[var(--surface)] border border-[var(--border)] rounded-2xl shadow-xl overflow-hidden max-h-80 overflow-y-auto"
        >
          {liveSuggestions.map((item, idx) => {
            const isSelected = idx === activeIndex;
            return (
              <li
                key={`${item.type}-${item.slug}-${idx}`}
                id={`${listboxId}-opt-${idx}`}
                role="option"
                aria-selected={isSelected}
                onClick={() => handleSelectSuggestion(item)}
                onMouseEnter={() => setActiveIndex(idx)}
                className={`px-4 py-3 cursor-pointer flex items-center justify-between text-xs sm:text-sm transition-colors min-h-[44px] border-b border-[var(--border)]/50 last:border-b-0 ${
                  isSelected
                    ? 'bg-[var(--surface-alt)] text-[var(--brand)] font-medium'
                    : 'text-[var(--foreground)] hover:bg-[var(--surface-alt)]'
                }`}
              >
                <div className="flex items-center gap-2.5 truncate">
                  {renderIcon(item.type)}
                  <span className="truncate">{item.text}</span>
                </div>
                <span className="text-[10px] uppercase font-mono tracking-wider text-[var(--muted)] px-2 py-0.5 rounded bg-[var(--surface-alt)] flex-shrink-0 ml-2">
                  {item.type}
                </span>
              </li>
            );
          })}
        </ul>
      )}

      {/* Suggested Search Chips */}
      {!isOpen && suggestions.length > 0 && (
        <div className="flex items-center gap-1.5 flex-wrap mt-3 px-1 text-xs text-[var(--muted)]">
          <span className="text-[11px] font-medium mr-1">Popular:</span>
          {suggestions.map((term, idx) => (
            <button
              key={idx}
              type="button"
              onClick={() => {
                setQuery(term);
                onSearch(term);
              }}
              className="px-3 py-1.5 rounded-full text-[11px] font-medium bg-[var(--surface)] border border-[var(--border)] text-[var(--muted)] hover:text-[var(--brand)] hover:border-[var(--brand)] transition-colors min-h-[44px] flex items-center"
            >
              {term}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
