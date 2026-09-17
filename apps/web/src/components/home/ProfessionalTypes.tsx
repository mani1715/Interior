import React from 'react';
import { SectionHeader, SectionTitle, SectionEyebrow, SectionDescription } from '@/components/layout/Section';
import { professionalTypes } from './mockHomeData';

export function ProfessionalTypes() {
  return (
    <section className="w-full py-12 sm:py-16 md:py-24 bg-[var(--background)] border-b border-[var(--border)]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeader>
          <div>
            <SectionEyebrow>09 / Built for Practitioners</SectionEyebrow>
            <SectionTitle>Tailored for the Architecture & Interior Trade</SectionTitle>
            <SectionDescription>
              We don’t build for generic creators or influencers. Elégance is engineered specifically for spatial practitioners who design, build, and deliver real physical environments.
            </SectionDescription>
          </div>
        </SectionHeader>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {professionalTypes.map((item, idx) => (
            <div
              key={idx}
              className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-sm space-y-3 hover:border-[var(--brand)] transition-colors"
            >
              <span className="text-[10px] font-semibold uppercase tracking-widest text-[var(--brand)] block">
                {item.tag}
              </span>
              <h4 className="font-serif text-lg font-semibold text-[var(--foreground)]">
                {item.title}
              </h4>
              <p className="text-xs sm:text-sm text-[var(--muted)] leading-relaxed">
                {item.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
