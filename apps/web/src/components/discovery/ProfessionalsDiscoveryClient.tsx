'use client';

import React, { useState, useMemo, useEffect } from 'react';
import { Search, MapPin } from 'lucide-react';
import { ProfessionalCard } from './ProfessionalCard';
import { getProfessionals } from '@/lib/discovery/queries';
import { PROFESSIONAL_TYPES, LOCATIONS } from '@/lib/discovery/demo-data';
import { fetchDiscoveryProfessionals, mapDiscoveryCardToProfessional } from '@/lib/discovery/api';
import { Professional } from '@/lib/discovery/types';

export interface ProfessionalsDiscoveryClientProps {
  initialProfessionals?: Professional[];
  initialTotal?: number;
}

export function ProfessionalsDiscoveryClient({
  initialProfessionals,
  initialTotal,
}: ProfessionalsDiscoveryClientProps = {}) {
  const [q, setQ] = useState('');
  const [selectedType, setSelectedType] = useState('all');
  const [selectedLocation, setSelectedLocation] = useState('all');

  const [liveProfessionals, setLiveProfessionals] = useState<Professional[] | null>(
    initialProfessionals || null
  );
  const [liveTotal, setLiveTotal] = useState<number | null>(initialTotal ?? null);
  const [usingLive, setUsingLive] = useState<boolean>(Boolean(initialProfessionals));

  useEffect(() => {
    let isCancelled = false;
    async function loadData() {
      try {
        const res = await fetchDiscoveryProfessionals({
          q: q.trim() || undefined,
          professionalType: selectedType !== 'all' ? selectedType : undefined,
          city: selectedLocation !== 'all' ? selectedLocation : undefined,
          limit: 18,
          offset: 0,
        });
        if (!isCancelled && res?.professionals) {
          const mapped = res.professionals.map(mapDiscoveryCardToProfessional);
          setLiveProfessionals(mapped);
          setLiveTotal(res.totalProfessionals);
          setUsingLive(true);
        }
      } catch {
        if (!isCancelled) {
          setUsingLive(false);
        }
      }
    }
    loadData();
    return () => {
      isCancelled = true;
    };
  }, [q, selectedType, selectedLocation]);

  const fallbackProfessionals = useMemo(() => {
    return getProfessionals({
      q: q.trim() || undefined,
      professionalType: selectedType !== 'all' ? (selectedType as any) : undefined,
      location: selectedLocation !== 'all' ? selectedLocation : undefined,
    });
  }, [q, selectedType, selectedLocation]);

  const professionals = usingLive && liveProfessionals ? liveProfessionals : fallbackProfessionals;

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
          className={`px-3.5 py-2 rounded-full text-xs font-medium whitespace-nowrap transition-all min-h-[44px] border ${
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
            className={`px-3.5 py-2 rounded-full text-xs font-medium whitespace-nowrap transition-all min-h-[44px] border ${
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
      <div className="flex items-center justify-between text-xs text-[var(--muted)] border-b border-[var(--border)] pb-3">
        <span>
          Showing <strong className="text-[var(--foreground)] font-semibold">{professionals.length}</strong> verified practitioners
        </span>
      </div>

      {/* Professionals Grid */}
      {professionals.length === 0 ? (
        <div className="py-16 text-center rounded-2xl border border-dashed border-[var(--border-strong)] bg-[var(--surface)] p-8">
          <p className="font-serif text-lg font-semibold text-[var(--foreground)] mb-1">
            No matching professionals found
          </p>
          <p className="text-xs text-[var(--muted)] max-w-sm mx-auto mb-4">
            Try adjusting your search query, location filter, or trade specialty.
          </p>
          <button
            type="button"
            onClick={() => {
              setQ('');
              setSelectedType('all');
              setSelectedLocation('all');
            }}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-[var(--surface-alt)] text-[var(--foreground)] border border-[var(--border)] hover:border-[var(--brand)] transition-colors min-h-[44px]"
          >
            Clear Filters
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {professionals.map((prof) => (
            <ProfessionalCard key={prof.id} professional={prof} />
          ))}
        </div>
      )}
    </div>
  );
}
