'use client';

import React, { useState, useMemo } from 'react';
import { Search, MapPin } from 'lucide-react';
import { ProfessionalCard } from './ProfessionalCard';
import { getProfessionals } from '@/lib/discovery/queries';
import { PROFESSIONAL_TYPES, LOCATIONS } from '@/lib/discovery/demo-data';

export function ProfessionalsDiscoveryClient() {
  const [q, setQ] = useState('');
  const [selectedType, setSelectedType] = useState('all');
  const [selectedLocation, setSelectedLocation] = useState('all');

  const professionals = useMemo(() => {
    return getProfessionals({
      q: q.trim() || undefined,
      professionalType: selectedType !== 'all' ? selectedType : undefined,
      location: selectedLocation !== 'all' ? selectedLocation : undefined,
    });
  }, [q, selectedType, selectedLocation]);

  return (
    <div className="w-full space-y-6 sm:space-y-8">
      {/* Search & Location Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-12 gap-3">
        <div className="sm:col-span-8 relative">
          <Search className="w-4 h-4 text-[var(--brand)] absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search by studio name, designer, or specialty..."
            aria-label="Search professionals"
            className="w-full py-3 pl-11 pr-4 rounded-xl border border-[var(--border)] bg-[var(--surface)] text-sm text-[var(--foreground)] placeholder:text-[var(--muted)] focus:outline-none focus:border-[var(--brand)] min-h-[44px]"
          />
        </div>

        <div className="sm:col-span-4 relative">
          <MapPin className="w-4 h-4 text-[var(--brand)] absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none" />
          <select
            value={selectedLocation}
            onChange={(e) => setSelectedLocation(e.target.value)}
            aria-label="Filter by location"
            className="w-full py-3 pl-11 pr-8 rounded-xl border border-[var(--border)] bg-[var(--surface)] text-sm text-[var(--foreground)] focus:outline-none focus:border-[var(--brand)] min-h-[44px]"
          >
            <option value="all">All Locations</option>
            {LOCATIONS.map((loc) => (
              <option key={loc.slug} value={loc.slug}>
                {loc.name}, {loc.state}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Trade Type Filter Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none -mx-4 px-4 sm:mx-0 sm:px-0">
        <button
          type="button"
          onClick={() => setSelectedType('all')}
          className={`px-3.5 py-2 rounded-full text-xs font-medium whitespace-nowrap transition-all min-h-[38px] border ${
            selectedType === 'all'
              ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold shadow-sm'
              : 'bg-[var(--surface)] text-[var(--foreground)] border-[var(--border)] hover:border-[var(--border-strong)]'
          }`}
        >
          All Professionals
        </button>
        {PROFESSIONAL_TYPES.map((pt) => (
          <button
            key={pt.slug}
            type="button"
            onClick={() => setSelectedType(pt.slug)}
            className={`px-3.5 py-2 rounded-full text-xs font-medium whitespace-nowrap transition-all min-h-[38px] border ${
              selectedType === pt.slug
                ? 'bg-[var(--brand)] text-[var(--charcoal)] border-[var(--brand)] font-semibold shadow-sm'
                : 'bg-[var(--surface)] text-[var(--foreground)] border-[var(--border)] hover:border-[var(--border-strong)]'
            }`}
          >
            {pt.name}
          </button>
        ))}
      </div>

      {/* Results Header */}
      <div className="text-xs sm:text-sm text-[var(--muted)] font-medium">
        Showing <span className="font-semibold text-[var(--foreground)]">{professionals.length}</span> {professionals.length === 1 ? 'studio' : 'studios'}
      </div>

      {/* Professional Cards Grid */}
      {professionals.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {professionals.map((prof) => (
            <ProfessionalCard key={prof.id} professional={prof} />
          ))}
        </div>
      ) : (
        <div className="p-8 sm:p-12 text-center rounded-2xl border border-dashed border-[var(--border)] bg-[var(--surface)] space-y-3">
          <p className="font-serif text-base font-semibold text-[var(--foreground)]">
            No professionals found matching your search
          </p>
          <p className="text-xs text-[var(--muted)]">
            Try resetting your search query or selecting a different trade or city.
          </p>
        </div>
      )}
    </div>
  );
}
