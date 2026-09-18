'use client';

import React, { useState, useMemo, useEffect, useTransition } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { SlidersHorizontal, ArrowUpDown } from 'lucide-react';
import { FilterParams } from '@/lib/discovery/types';
import { CATEGORIES, SORT_OPTIONS } from '@/lib/discovery/demo-data';
import { getProjects } from '@/lib/discovery/queries';
import { ProjectCard } from './ProjectCard';
import { DiscoverySearchBar } from './DiscoverySearchBar';
import { ProjectFilterSheet } from './ProjectFilterSheet';
import { ProjectFilterSidebar } from './ProjectFilterSidebar';
import { ActiveFilterBar } from './ActiveFilterBar';
import { Button } from '@/components/ui/Button';

export interface ProjectsDiscoveryClientProps {
  initialFilters: FilterParams;
}

export function ProjectsDiscoveryClient({ initialFilters }: ProjectsDiscoveryClientProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [isPending, startTransition] = useTransition();

  const [filters, setFilters] = useState<FilterParams>(initialFilters);
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);
  const [page, setPage] = useState(1);

  // Sync state if URL searchParams change
  useEffect(() => {
    if (Array.from(searchParams.keys()).length === 0) return;
    setFilters({
      q: searchParams.get('q') || undefined,
      category: searchParams.get('category') || undefined,
      location: searchParams.get('location') || undefined,
      style: (searchParams.get('style') as any) || undefined,
      budget: (searchParams.get('budget') as any) || undefined,
      propertyType: (searchParams.get('propertyType') as any) || undefined,
      professionalType: (searchParams.get('professionalType') as any) || undefined,
      sort: (searchParams.get('sort') as any) || 'recommended',
    });
    setPage(1);
  }, [searchParams]);

  // Update URL search parameters when filters change
  const updateUrl = (newFilters: FilterParams) => {
    const params = new URLSearchParams();
    if (newFilters.q) params.set('q', newFilters.q);
    if (newFilters.category) params.set('category', newFilters.category);
    if (newFilters.location) params.set('location', newFilters.location);
    if (newFilters.style) params.set('style', newFilters.style);
    if (newFilters.budget) params.set('budget', newFilters.budget);
    if (newFilters.propertyType) params.set('propertyType', newFilters.propertyType);
    if (newFilters.professionalType) params.set('professionalType', newFilters.professionalType);
    if (newFilters.sort && newFilters.sort !== 'recommended') params.set('sort', newFilters.sort);

    const queryString = params.toString();
    startTransition(() => {
      router.replace(`/projects${queryString ? `?${queryString}` : ''}`, { scroll: false });
    });
  };

  const handleFilterChange = (key: keyof FilterParams, value: string | undefined) => {
    const updated = { ...filters, [key]: value };
    setFilters(updated);
    setPage(1);
    updateUrl(updated);
  };

  const handleClearAll = () => {
    const cleared: FilterParams = { sort: filters.sort };
    setFilters(cleared);
    setPage(1);
    updateUrl(cleared);
  };

  const handleRemoveFilter = (key: keyof FilterParams) => {
    handleFilterChange(key, undefined);
  };

  const handleSearch = (query: string) => {
    handleFilterChange('q', query.length > 0 ? query : undefined);
  };

  const handleSortChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const sortVal = e.target.value as any;
    handleFilterChange('sort', sortVal);
  };

  // Compute filtered projects using the centralized query layer
  const { projects, total, hasMore } = useMemo(() => {
    return getProjects({ ...filters, page });
  }, [filters, page]);

  const activeFilterCount = [
    filters.category,
    filters.location,
    filters.style,
    filters.budget,
    filters.propertyType,
    filters.professionalType,
  ].filter(Boolean).length;

  return (
    <div className="w-full space-y-6 sm:space-y-8">
      {/* 1. Prominent Search Bar */}
      <DiscoverySearchBar
        initialQuery={filters.q || ''}
        onSearch={handleSearch}
        placeholder="Search TV units, modular kitchens, styles, or cities..."
      />

      {/* 2. Quick Horizontal Category Chips Bar */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none -mx-4 px-4 sm:mx-0 sm:px-0">
        <button
          type="button"
          onClick={() => handleFilterChange('category', undefined)}
          className={`px-3.5 py-2 rounded-full text-xs font-medium whitespace-nowrap transition-all min-h-[38px] border ${
            !filters.category
              ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold shadow-sm'
              : 'bg-[var(--surface)] text-[var(--foreground)] border-[var(--border)] hover:border-[var(--border-strong)]'
          }`}
        >
          All Categories
        </button>
        {CATEGORIES.slice(0, 8).map((cat) => (
          <button
            key={cat.slug}
            type="button"
            onClick={() => handleFilterChange('category', filters.category === cat.slug ? undefined : cat.slug)}
            className={`px-3.5 py-2 rounded-full text-xs font-medium whitespace-nowrap transition-all min-h-[38px] border ${
              filters.category === cat.slug
                ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold shadow-sm'
                : 'bg-[var(--surface)] text-[var(--foreground)] border-[var(--border)] hover:border-[var(--border-strong)]'
            }`}
          >
            {cat.name}
          </button>
        ))}
      </div>

      {/* 3. Mobile Control Bar: Filter Trigger + Sort Select */}
      <div className="flex items-center justify-between gap-3 lg:hidden pt-2 border-t border-[var(--border)]">
        <button
          type="button"
          onClick={() => setMobileFilterOpen(true)}
          className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl border border-[var(--border)] bg-[var(--surface)] text-xs font-semibold text-[var(--foreground)] shadow-sm min-h-[44px] hover:border-[var(--brand)] transition-colors"
        >
          <SlidersHorizontal className="w-4 h-4 text-[var(--brand)]" />
          <span>Filters</span>
          {activeFilterCount > 0 && (
            <span className="w-5 h-5 rounded-full bg-[var(--brand)] text-[var(--charcoal)] text-[10px] font-bold flex items-center justify-center">
              {activeFilterCount}
            </span>
          )}
        </button>

        <div className="relative flex-1">
          <select
            value={filters.sort || 'recommended'}
            onChange={handleSortChange}
            aria-label="Sort projects"
            className="w-full appearance-none px-3.5 py-2.5 pr-8 rounded-xl border border-[var(--border)] bg-[var(--surface)] text-xs font-medium text-[var(--foreground)] shadow-sm min-h-[44px] focus:outline-none focus:border-[var(--brand)]"
          >
            {SORT_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                Sort: {opt.label}
              </option>
            ))}
          </select>
          <ArrowUpDown className="w-3.5 h-3.5 text-[var(--muted)] absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none" />
        </div>
      </div>

      {/* 4. Active Filters Bar with Result Count and Live Announcer */}
      <ActiveFilterBar
        filters={filters}
        totalResults={total}
        onRemoveFilter={handleRemoveFilter}
        onClearAll={handleClearAll}
      />

      {/* 5. Main Grid Layout (Desktop Sidebar + Projects Grid) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Desktop Sidebar (hidden on mobile) */}
        <div className="hidden lg:block lg:col-span-3 sticky top-24 p-5 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-sm">
          {/* Desktop Sort */}
          <div className="mb-6 pb-4 border-b border-[var(--border)]">
            <label className="text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] block mb-2">
              Sort By
            </label>
            <select
              value={filters.sort || 'recommended'}
              onChange={handleSortChange}
              aria-label="Sort projects desktop"
              className="w-full py-2 px-3 rounded-xl border border-[var(--border)] bg-[var(--background)] text-xs text-[var(--foreground)] focus:outline-none focus:border-[var(--brand)]"
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <ProjectFilterSidebar
            filters={filters}
            onFilterChange={handleFilterChange}
            onClearAll={handleClearAll}
          />
        </div>

        {/* Project Results Area */}
        <div className="lg:col-span-9">
          {projects.length > 0 ? (
            <div className="space-y-8">
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
                {projects.map((project, idx) => (
                  <ProjectCard
                    key={project.id}
                    project={project}
                    priority={idx < 4}
                  />
                ))}
              </div>

              {/* Load More Pagination Foundation */}
              {hasMore && (
                <div className="pt-6 text-center">
                  <Button
                    variant="outline"
                    size="lg"
                    onClick={() => setPage((p) => p + 1)}
                    className="min-h-[44px] px-8"
                  >
                    Load More Projects
                  </Button>
                </div>
              )}
            </div>
          ) : (
            /* Empty State */
            <div className="p-8 sm:p-12 text-center rounded-2xl border border-dashed border-[var(--border)] bg-[var(--surface)] space-y-4">
              <div className="w-12 h-12 rounded-full bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--brand)] flex items-center justify-center mx-auto shadow-sm">
                <SlidersHorizontal className="w-6 h-6" />
              </div>
              <div className="space-y-1">
                <h4 className="font-serif text-lg font-semibold text-[var(--foreground)]">
                  No projects match these filters
                </h4>
                <p className="text-xs sm:text-sm text-[var(--muted)] max-w-md mx-auto leading-relaxed">
                  Try broadening your location, clearing specific style filters, or exploring all available categories.
                </p>
              </div>
              <div className="pt-2 flex flex-wrap items-center justify-center gap-3">
                <Button variant="primary" size="md" onClick={handleClearAll}>
                  Clear All Filters
                </Button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Mobile Filter Sheet */}
      <ProjectFilterSheet
        isOpen={mobileFilterOpen}
        onClose={() => setMobileFilterOpen(false)}
        filters={filters}
        onFilterChange={handleFilterChange}
        onClearAll={handleClearAll}
        totalResults={total}
      />
    </div>
  );
}
