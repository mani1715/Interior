'use client';

import React from 'react';
import { BottomSheet } from '@/components/overlay/BottomSheet';
import { Button } from '@/components/ui/Button';
import {
  CATEGORIES,
  LOCATIONS,
  STYLES,
  BUDGET_RANGES,
  PROPERTY_TYPES,
  PROFESSIONAL_TYPES,
} from '@/lib/discovery/demo-data';
import { FilterParams } from '@/lib/discovery/types';

export interface ProjectFilterSheetProps {
  isOpen: boolean;
  onClose: () => void;
  filters: FilterParams;
  onFilterChange: (key: keyof FilterParams, value: string | undefined) => void;
  onClearAll: () => void;
  totalResults: number;
}

export function ProjectFilterSheet({
  isOpen,
  onClose,
  filters,
  onFilterChange,
  onClearAll,
  totalResults,
}: ProjectFilterSheetProps) {
  // Count active filter dimensions
  const activeCount = [
    filters.category,
    filters.location,
    filters.style,
    filters.budget,
    filters.propertyType,
    filters.professionalType,
  ].filter(Boolean).length;

  return (
    <BottomSheet
      isOpen={isOpen}
      onClose={onClose}
      title="Filter Projects"
      footer={
        <div className="flex items-center gap-3 w-full">
          <Button
            variant="outline"
            size="lg"
            onClick={() => {
              onClearAll();
            }}
            disabled={activeCount === 0}
            className="flex-1 justify-center min-h-[44px]"
          >
            Clear All
          </Button>
          <Button
            variant="primary"
            size="lg"
            onClick={onClose}
            className="flex-1 justify-center min-h-[44px]"
          >
            Show {totalResults} {totalResults === 1 ? 'Project' : 'Projects'}
          </Button>
        </div>
      }
    >
      <div className="space-y-6 pb-4">
        {/* Location */}
        <div>
          <label className="block text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] mb-2.5">
            Location
          </label>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => onFilterChange('location', undefined)}
              className={`px-3 py-2 rounded-xl text-xs font-medium transition-colors min-h-[44px] min-w-[44px] border ${
                !filters.location
                  ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold'
                  : 'bg-[var(--surface-alt)] text-[var(--foreground)] border-[var(--border)]'
              }`}
            >
              All Locations
            </button>
            {LOCATIONS.map((loc) => (
              <button
                key={loc.slug}
                type="button"
                onClick={() => onFilterChange('location', filters.location === loc.slug ? undefined : loc.slug)}
                className={`px-3 py-2 rounded-xl text-xs font-medium transition-colors min-h-[44px] min-w-[44px] border ${
                  filters.location === loc.slug
                    ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold'
                    : 'bg-[var(--surface-alt)] text-[var(--foreground)] border-[var(--border)]'
                }`}
              >
                {loc.name}
              </button>
            ))}
          </div>
        </div>

        {/* Category */}
        <div>
          <label className="block text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] mb-2.5">
            Space & Category
          </label>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => onFilterChange('category', undefined)}
              className={`px-3 py-2 rounded-xl text-xs font-medium transition-colors min-h-[44px] min-w-[44px] border ${
                !filters.category
                  ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold'
                  : 'bg-[var(--surface-alt)] text-[var(--foreground)] border-[var(--border)]'
              }`}
            >
              All Categories
            </button>
            {CATEGORIES.map((cat) => (
              <button
                key={cat.slug}
                type="button"
                onClick={() => onFilterChange('category', filters.category === cat.slug ? undefined : cat.slug)}
                className={`px-3 py-2 rounded-xl text-xs font-medium transition-colors min-h-[44px] min-w-[44px] border ${
                  filters.category === cat.slug
                    ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold'
                    : 'bg-[var(--surface-alt)] text-[var(--foreground)] border-[var(--border)]'
                }`}
              >
                {cat.name}
              </button>
            ))}
          </div>
        </div>

        {/* Style */}
        <div>
          <label className="block text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] mb-2.5">
            Design Style
          </label>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => onFilterChange('style', undefined)}
              className={`px-3 py-2 rounded-xl text-xs font-medium transition-colors min-h-[44px] min-w-[44px] border ${
                !filters.style
                  ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold'
                  : 'bg-[var(--surface-alt)] text-[var(--foreground)] border-[var(--border)]'
              }`}
            >
              All Styles
            </button>
            {STYLES.map((st) => (
              <button
                key={st.slug}
                type="button"
                onClick={() => onFilterChange('style', filters.style === st.slug ? undefined : st.slug)}
                className={`px-3 py-2 rounded-xl text-xs font-medium transition-colors min-h-[44px] min-w-[44px] border ${
                  filters.style === st.slug
                    ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold'
                    : 'bg-[var(--surface-alt)] text-[var(--foreground)] border-[var(--border)]'
                }`}
              >
                {st.name}
              </button>
            ))}
          </div>
        </div>

        {/* Budget */}
        <div>
          <label className="block text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] mb-2.5">
            Budget Range
          </label>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => onFilterChange('budget', undefined)}
              className={`px-3 py-2 rounded-xl text-xs font-medium transition-colors min-h-[44px] min-w-[44px] border ${
                !filters.budget
                  ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold'
                  : 'bg-[var(--surface-alt)] text-[var(--foreground)] border-[var(--border)]'
              }`}
            >
              All Budgets
            </button>
            {BUDGET_RANGES.map((b) => (
              <button
                key={b.slug}
                type="button"
                onClick={() => onFilterChange('budget', filters.budget === b.slug ? undefined : b.slug)}
                className={`px-3 py-2 rounded-xl text-xs font-medium transition-colors min-h-[44px] min-w-[44px] border ${
                  filters.budget === b.slug
                    ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold'
                    : 'bg-[var(--surface-alt)] text-[var(--foreground)] border-[var(--border)]'
                }`}
              >
                {b.label}
              </button>
            ))}
          </div>
        </div>

        {/* Property Type */}
        <div>
          <label className="block text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] mb-2.5">
            Property Type
          </label>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => onFilterChange('propertyType', undefined)}
              className={`px-3 py-2 rounded-xl text-xs font-medium transition-colors min-h-[44px] min-w-[44px] border ${
                !filters.propertyType
                  ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold'
                  : 'bg-[var(--surface-alt)] text-[var(--foreground)] border-[var(--border)]'
              }`}
            >
              All Types
            </button>
            {PROPERTY_TYPES.map((pt) => (
              <button
                key={pt.slug}
                type="button"
                onClick={() => onFilterChange('propertyType', filters.propertyType === pt.slug ? undefined : pt.slug)}
                className={`px-3 py-2 rounded-xl text-xs font-medium transition-colors min-h-[44px] min-w-[44px] border ${
                  filters.propertyType === pt.slug
                    ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold'
                    : 'bg-[var(--surface-alt)] text-[var(--foreground)] border-[var(--border)]'
                }`}
              >
                {pt.name}
              </button>
            ))}
          </div>
        </div>

        {/* Professional Type */}
        <div>
          <label className="block text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] mb-2.5">
            Professional Type
          </label>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => onFilterChange('professionalType', undefined)}
              className={`px-3 py-2 rounded-xl text-xs font-medium transition-colors min-h-[44px] min-w-[44px] border ${
                !filters.professionalType
                  ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold'
                  : 'bg-[var(--surface-alt)] text-[var(--foreground)] border-[var(--border)]'
              }`}
            >
              All Professionals
            </button>
            {PROFESSIONAL_TYPES.map((p) => (
              <button
                key={p.slug}
                type="button"
                onClick={() => onFilterChange('professionalType', filters.professionalType === p.slug ? undefined : p.slug)}
                className={`px-3 py-2 rounded-xl text-xs font-medium transition-colors min-h-[44px] min-w-[44px] border ${
                  filters.professionalType === p.slug
                    ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold'
                    : 'bg-[var(--surface-alt)] text-[var(--foreground)] border-[var(--border)]'
                }`}
              >
                {p.name}
              </button>
            ))}
          </div>
        </div>
      </div>
    </BottomSheet>
  );
}
