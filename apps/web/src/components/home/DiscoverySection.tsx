import React from 'react';
import Link from 'next/link';
import { Search, MapPin, UserCheck, ArrowUpRight } from 'lucide-react';
import { SectionHeader, SectionTitle, SectionEyebrow, SectionDescription } from '@/components/layout/Section';
import { FilterChip } from '@/components/ui/Filter';

export function DiscoverySection() {
  const popularSearches = [
    'TV Units in Guntur',
    'Modern Wardrobes',
    'Modular Kitchens in Amaravati',
    'Pooja Units in Vijayawada',
    'Luxury 3BHK Interiors',
  ];

  const discoverySteps = [
    {
      num: '01',
      title: 'Real Project Discovery',
      desc: 'Homeowners search for specific spaces, materials, and city locations rather than vague directory listings.',
    },
    {
      num: '02',
      title: 'Explore Case Study',
      desc: 'They see authentic photos, site context, materials used, and the designer or studio that executed it.',
    },
    {
      num: '03',
      title: 'Contact the Creator',
      desc: 'Direct WhatsApp and structured enquiry links connect the interested homeowner directly to your studio.',
    },
  ];

  return (
    <section id="discover-projects" className="w-full py-12 sm:py-16 md:py-24 bg-[var(--background)] border-b border-[var(--border)]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeader>
          <div>
            <SectionEyebrow>03 / Project Discovery</SectionEyebrow>
            <SectionTitle>How Homeowners Find You</SectionTitle>
            <SectionDescription>
              Clients don’t search for generic agencies. They search for specific design inspiration: a TV unit in Guntur, an open kitchen in Amaravati, or a pooja mandir in Vijayawada.
            </SectionDescription>
          </div>
        </SectionHeader>

        {/* Mock Search Bar Interaction */}
        <div className="max-w-2xl mx-auto mb-10 sm:mb-12">
          <div className="p-2 sm:p-3 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-md flex items-center gap-3">
            <Search className="w-5 h-5 text-[var(--brand)] ml-2 flex-shrink-0" />
            <span className="text-xs sm:text-sm text-[var(--foreground)] font-medium flex-1">
              TV Units in Guntur, Andhra Pradesh
            </span>
            <span className="px-3 py-1.5 rounded-xl bg-[var(--brand)] text-[var(--charcoal)] font-semibold text-xs whitespace-nowrap">
              Search Projects
            </span>
          </div>

          <div className="flex items-center gap-1.5 flex-wrap justify-center mt-3">
            <span className="text-[11px] text-[var(--muted)] mr-1">Trending:</span>
            {popularSearches.map((term, idx) => (
              <span
                key={idx}
                className="px-2.5 py-1 rounded-full text-[11px] font-medium bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--muted)]"
              >
                {term}
              </span>
            ))}
          </div>
        </div>

        {/* 3-Step Discovery Journey */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {discoverySteps.map((step, idx) => (
            <div
              key={idx}
              className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-sm hover:border-[var(--brand)] transition-colors relative space-y-3"
            >
              <span className="font-serif text-3xl font-bold text-[var(--brand)] opacity-80 block">
                {step.num}
              </span>
              <h4 className="font-serif text-lg font-semibold text-[var(--foreground)]">
                {step.title}
              </h4>
              <p className="text-xs sm:text-sm text-[var(--muted)] leading-relaxed">
                {step.desc}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
