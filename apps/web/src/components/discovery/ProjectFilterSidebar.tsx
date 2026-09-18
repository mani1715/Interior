'use client';

import React from 'react';
import { RotateCcw } from 'lucide-react';
import {
  CATEGORIES,
  LOCATIONS,
  STYLES,
  BUDGET_RANGES,
  PROPERTY_TYPES,
  PROFESSIONAL_TYPES,
} from '@/lib/discovery/demo-data';
import { FilterParams } from '@/lib/discovery/types';

export interface ProjectFilterSidebarProps {
  filters: FilterParams;
  onFilterChange: (key: keyof FilterParams, value: string | undefined) => void;
  onClearAll: () => void;
}

export function ProjectFilterSidebar({
  filters,
  onFilterChange,
  onClearAll,
}: ProjectFilterSidebarProps) {
  const activeCount = [
    filters.category,
    filters.location,
    filters.style,
    filters.budget,
    filters.propertyType,
    filters.professionalType,
  ].filter(Boolean).length;

  return (
    <aside className="w-full space-y-6" aria-label="Desktop Filters">
      {/* Sidebar Header */}
      <div className="flex items-center justify-between pb-3 border-b border-[var(--border)]">
        <div className="flex items-center gap-2">
          <h3 className="font-serif text-sm font-semibold uppercase tracking-wider text-[var(--foreground)]">
            Filters
          </h3>
          {activeCount > 0 && (
            <span className="w-5 h-5 rounded-full bg-[var(--brand)] text-[var(--charcoal)] text-[10px] font-bold flex items-center justify-center">
              {activeCount}
            </span>
          )}
        </div>

        {activeCount > 0 && (
          <button
            type="button"
            onClick={onClearAll}
            className="text-xs text-[var(--muted)] hover:text-[var(--brand)] flex items-center gap-1 transition-colors"
          >
            <RotateCcw className="w-3 h-3" />
            <span>Reset</span>
          </button>
        )}
      </div>

      {/* Location Filter */}
      <div className="space-y-2">
        <label className="text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] block">
          Location
        </label>
        <select
          value={filters.location || ''}
          onChange={(e) => onFilterChange('location', e.target.value || undefined)}
          className="w-full py-2 px-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] text-xs text-[var(--foreground)] focus:outline-none focus:border-[var(--brand)]"
          aria-label="Filter by location"
        >
          <option value="">All Locations</option>
          {LOCATIONS.map((loc) => (
            <option key={loc.slug} value={loc.slug}>
              {loc.name}, {loc.state}
            </option>
          ))}
        </select>
      </div>

      {/* Category Filter */}
      <div className="space-y-2">
        <label className="text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] block">
          Space & Category
        </label>
        <select
          value={filters.category || ''}
          onChange={(e) => onFilterChange('category', e.target.value || undefined)}
          className="w-full py-2 px-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] text-xs text-[var(--foreground)] focus:outline-none focus:border-[var(--brand)]"
          aria-label="Filter by category"
        >
          <option value="">All Categories</option>
          {CATEGORIES.map((cat) => (
            <option key={cat.slug} value={cat.slug}>
              {cat.name}
            </option>
          ))}
        </select>
      </div>

      {/* Style Filter */}
      <div className="space-y-2">
        <label className="text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] block">
          Design Style
        </label>
        <select
          value={filters.style || ''}
          onChange={(e) => onFilterChange('style', e.target.value || undefined)}
          className="w-full py-2 px-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] text-xs text-[var(--foreground)] focus:outline-none focus:border-[var(--brand)]"
          aria-label="Filter by design style"
        >
          <option value="">All Styles</option>
          {STYLES.map((st) => (
            <option key={st.slug} value={st.slug}>
              {st.name}
            </option>
          ))}
        </select>
      </div>

      {/* Budget Range Filter */}
      <div className="space-y-2">
        <label className="text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] block">
          Budget Range
        </label>
        <select
          value={filters.budget || ''}
          onChange={(e) => onFilterChange('budget', e.target.value || undefined)}
          className="w-full py-2 px-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] text-xs text-[var(--foreground)] focus:outline-none focus:border-[var(--brand)]"
          aria-label="Filter by budget"
        >
          <option value="">All Budgets</option>
          {BUDGET_RANGES.map((b) => (
            <option key={b.slug} value={b.slug}>
              {b.label}
            </option>
          ))}
        </select>
      </div>

      {/* Property Type Filter */}
      <div className="space-y-2">
        <label className="text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] block">
          Property Type
        </label>
        <select
          value={filters.propertyType || ''}
          onChange={(e) => onFilterChange('propertyType', e.target.value || undefined)}
          className="w-full py-2 px-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] text-xs text-[var(--foreground)] focus:outline-none focus:border-[var(--brand)]"
          aria-label="Filter by property type"
        >
          <option value="">All Property Types</option>
          {PROPERTY_TYPES.map((pt) => (
            <option key={pt.slug} value={pt.slug}>
              {pt.name}
            </option>
          ))}
        </select>
      </div>

      {/* Professional Type Filter */}
      <div className="space-y-2">
        <label className="text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] block">
          Professional Type
        </label>
        <select
          value={filters.professionalType || ''}
          onChange={(e) => onFilterChange('professionalType', e.target.value || undefined)}
          className="w-full py-2 px-3 rounded-xl border border-[var(--border)] bg-[var(--surface)] text-xs text-[var(--foreground)] focus:outline-none focus:border-[var(--brand)]"
          aria-label="Filter by professional trade"
        >
          <option value="">All Professional Types</option>
          {PROFESSIONAL_TYPES.map((p) => (
            <option key={p.slug} value={p.slug}>
              {p.name}
            </option>
          ))}
        </select>
      </div>
    </aside>
  );
}
