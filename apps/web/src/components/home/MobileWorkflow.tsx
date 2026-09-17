import React from 'react';
import { Camera, Sparkles, Smartphone, Save, Share2, ArrowRight } from 'lucide-react';
import { SectionHeader, SectionTitle, SectionEyebrow, SectionDescription } from '@/components/layout/Section';

export function MobileWorkflow() {
  const steps = [
    {
      icon: Camera,
      title: '1. Snap On-Site Photo',
      desc: 'Take a quick smartphone photograph during your client walkthrough of raw masonry or room dimensions.',
    },
    {
      icon: Sparkles,
      title: '2. Generate Instant Ideas',
      desc: 'Add a prompt like "Warm oak with bronze handles" and see multiple design directions rendered in seconds.',
    },
    {
      icon: Smartphone,
      title: '3. Align with Client on the Spot',
      desc: 'Review concepts directly on your mobile screen with the homeowner while standing in the physical room.',
    },
    {
      icon: Save,
      title: '4. Save & Publish Case Study',
      desc: 'Document the finalized project back at your desk. High-resolution photos are watermarked and indexed automatically.',
    },
  ];

  return (
    <section className="w-full py-12 sm:py-16 md:py-24 bg-[var(--background)] border-b border-[var(--border)]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeader>
          <div>
            <SectionEyebrow>11 / Mobile-First Utility</SectionEyebrow>
            <SectionTitle>Designed for the Field, Not Just the Desk</SectionTitle>
            <SectionDescription>
              Interior work happens on construction sites, in modular factories, and in client living rooms. Every capability is optimized for fast, touch-first mobile execution.
            </SectionDescription>
          </div>
        </SectionHeader>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {steps.map((item, idx) => {
            const Icon = item.icon;
            return (
              <div
                key={idx}
                className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-sm space-y-3 hover:border-[var(--brand)] transition-colors"
              >
                <div className="w-10 h-10 rounded-xl bg-[var(--brand-muted)] border border-[var(--brand)]/20 text-[var(--brand)] flex items-center justify-center">
                  <Icon className="w-5 h-5" />
                </div>
                <h4 className="font-serif text-base font-semibold text-[var(--foreground)]">
                  {item.title}
                </h4>
                <p className="text-xs sm:text-sm text-[var(--muted)] leading-relaxed">
                  {item.desc}
                </p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
