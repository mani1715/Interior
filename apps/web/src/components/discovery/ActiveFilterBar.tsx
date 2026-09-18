'use client';

import React from 'react';
import { X, RotateCcw } from 'lucide-react';
import { FilterParams } from '@/lib/discovery/types';
import {
  CATEGORIES,
  LOCATIONS,
  STYLES,
  BUDGET_RANGES,
  PROPERTY_TYPES,
  PROFESSIONAL_TYPES,
} from '@/lib/discovery/demo-data';

export interface ActiveFilterBarProps {
  filters: FilterParams;
  totalResults: number;
  onRemoveFilter: (key: keyof FilterParams) => void;
  onClearAll: () => void;
}

export function ActiveFilterBar({
  filters,
  totalResults,
  onRemoveFilter,
  onClearAll,
}: ActiveFilterBarProps) {
  const activeChips: { key: keyof FilterParams; label: string; value: string }[] = [];

  if (filters.q) {
    activeChips.push({ key: 'q', label: 'Search', value: `“${filters.q}”` });
  }
  if (filters.category) {
    const found = CATEGORIES.find((c) => c.slug === filters.category);
    activeChips.push({ key: 'category', label: 'Category', value: found?.name || filters.category });
  }
  if (filters.location) {
    const found = LOCATIONS.find((l) => l.slug === filters.location);
    activeChips.push({ key: 'location', label: 'Location', value: found?.name || filters.location });
  }
  if (filters.style) {
    const found = STYLES.find((s) => s.slug === filters.style);
    activeChips.push({ key: 'style', label: 'Style', value: found?.name || filters.style });
  }
  if (filters.budget) {
    const found = BUDGET_RANGES.find((b) => b.slug === filters.budget);
    activeChips.push({ key: 'budget', label: 'Budget', value: found?.label || filters.budget });
  }
  if (filters.propertyType) {
    const found = PROPERTY_TYPES.find((p) => p.slug === filters.propertyType);
    activeChips.push({ key: 'propertyType', label: 'Property', value: found?.name || filters.propertyType });
  }
  if (filters.professionalType) {
    const found = PROFESSIONAL_TYPES.find((p) => p.slug === filters.professionalType);
    activeChips.push({ key: 'professionalType', label: 'Trade', value: found?.name || filters.professionalType });
  }

  return (
    <div className="w-full flex flex-wrap items-center justify-between gap-3 py-3">
      {/* Screen Reader Result Count Announcement */}
      <div aria-live="polite" aria-atomic="true" className="sr-only">
        {totalResults} {totalResults === 1 ? 'project' : 'projects'} found.
      </div>

      {/* Visible Results Count */}
      <div className="text-xs sm:text-sm text-[var(--muted)] font-medium">
        Showing <span className="font-semibold text-[var(--foreground)]">{totalResults}</span> {totalResults === 1 ? 'project' : 'projects'}
      </div>

      {/* Active Filter Chips */}
      {activeChips.length > 0 && (
        <div className="flex items-center gap-1.5 flex-wrap">
          {activeChips.map((chip) => (
            <span
              key={chip.key}
              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs bg-[var(--surface)] border border-[var(--border)] text-[var(--foreground)] shadow-sm"
            >
              <span className="text-[var(--muted)]">{chip.label}:</span>
              <span className="font-medium">{chip.value}</span>
              <button
                type="button"
                onClick={() => onRemoveFilter(chip.key)}
                aria-label={`Remove filter ${chip.label}: ${chip.value}`}
                className="ml-1 p-0.5 rounded-full hover:bg-[var(--surface-alt)] text-[var(--muted)] hover:text-[var(--foreground)] transition-colors min-h-[20px] min-w-[20px] flex items-center justify-center"
              >
                <X className="w-3 h-3" />
              </button>
            </span>
          ))}

          <button
            type="button"
            onClick={onClearAll}
            className="text-xs text-[var(--brand)] hover:underline ml-1 font-medium flex items-center gap-1 min-h-[32px] px-2"
          >
            <RotateCcw className="w-3 h-3" />
            <span>Clear All</span>
          </button>
        </div>
      )}
    </div>
  );
}
