import React from 'react';
import { SectionHeader, SectionTitle, SectionEyebrow, SectionDescription } from '@/components/layout/Section';
import { howItWorksSteps } from './mockHomeData';

export function HowItWorks() {
  return (
    <section id="how-it-works" className="w-full py-12 sm:py-16 md:py-24 bg-[var(--surface-alt)] border-b border-[var(--border)]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeader>
          <div>
            <SectionEyebrow>08 / Professional Workflow</SectionEyebrow>
            <SectionTitle>How It Works for Studios & Designers</SectionTitle>
            <SectionDescription>
              From project photography to automated search indexation and client enquiry capture in six straightforward steps.
            </SectionDescription>
          </div>
        </SectionHeader>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {howItWorksSteps.map((item, idx) => (
            <div
              key={idx}
              className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-sm space-y-3 relative group hover:border-[var(--brand)] transition-colors"
            >
              <span className="font-mono text-xs font-bold text-[var(--brand)] px-2.5 py-1 rounded-md bg-[var(--brand-muted)] inline-block">
                STEP {item.step}
              </span>
              <h4 className="font-serif text-lg font-semibold text-[var(--foreground)]">
                {item.title}
              </h4>
              <p className="text-xs sm:text-sm text-[var(--muted)] leading-relaxed">
                {item.desc}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
