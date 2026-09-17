import React from 'react';
import Link from 'next/link';
import { Globe, Shield, Sparkles, Sliders, CheckCircle2, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { SectionHeader, SectionTitle, SectionEyebrow, SectionDescription } from '@/components/layout/Section';
import { VerifiedBadge } from '@/components/ui/Badge';

export function PortfolioSection() {
  const features = [
    {
      icon: Globe,
      title: 'Dedicated Studio Mini-Website',
      desc: 'Claim your studio link with your bio, awards, city coverage, team credentials, and client contact channels.',
    },
    {
      icon: Shield,
      title: 'Automatic Watermark Protection',
      desc: 'Your emblem and copyright hash are embedded into published project photography to protect your intellectual property.',
    },
    {
      icon: Sliders,
      title: 'Detailed Project Case Studies',
      desc: 'Publish comprehensive stories: floor plans, 3D renderings, material boards, budgets, and verified client testimonials.',
    },
    {
      icon: Sparkles,
      title: 'Client Collaboration Hub',
      desc: 'Share private concept boards with clients, capture room-by-room feedback, and align on finishes before procurement.',
    },
  ];

  return (
    <section className="w-full py-12 sm:py-16 md:py-24 bg-[var(--surface-alt)] border-b border-[var(--border)]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
          {/* Left Content */}
          <div className="lg:col-span-6 space-y-6">
            <SectionHeader className="mb-4">
              <div>
                <SectionEyebrow>02 / Portfolio Builder</SectionEyebrow>
                <SectionTitle>More than a profile. A digital home for your craft.</SectionTitle>
                <SectionDescription>
                  Social media compresses and buries your life’s work. Your studio deserves an authoritative portfolio where prospective clients can explore every detail of your projects.
                </SectionDescription>
              </div>
            </SectionHeader>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {features.map((f, idx) => {
                const Icon = f.icon;
                return (
                  <div key={idx} className="p-4 rounded-xl border border-[var(--border)] bg-[var(--surface)] space-y-2 shadow-sm">
                    <Icon className="w-5 h-5 text-[var(--brand)]" />
                    <h4 className="font-serif text-sm font-semibold text-[var(--foreground)]">
                      {f.title}
                    </h4>
                    <p className="text-xs text-[var(--muted)] leading-relaxed">
                      {f.desc}
                    </p>
                  </div>
                );
              })}
            </div>

            <div className="pt-2">
              <Link href="/register">
                <Button
                  variant="primary"
                  size="md"
                  rightIcon={<ArrowRight className="w-4 h-4" />}
                >
                  Start Building Your Portfolio
                </Button>
              </Link>
            </div>
          </div>

          {/* Right Visual Preview of Studio Portfolio Card */}
          <div className="lg:col-span-6">
            <div className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-5 sm:p-6 shadow-xl space-y-5">
              {/* Mock Studio Header */}
              <div className="flex items-center justify-between pb-4 border-b border-[var(--border)]">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-[var(--brand)] text-[var(--charcoal)] flex items-center justify-center font-serif font-bold text-xl shadow-sm">
                    E
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="font-serif text-base font-bold text-[var(--foreground)]">
                        Studio Elégance
                      </h4>
                      <VerifiedBadge label="Verified" />
                    </div>
                    <p className="text-xs text-[var(--muted)]">
                      Turnkey Architecture & Interiors • Guntur & Amaravati
                    </p>
                  </div>
                </div>
                <span className="hidden sm:inline-block px-2.5 py-1 rounded-md text-[10px] font-mono bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--muted)]">
                  elegance.interior/studio
                </span>
              </div>

              {/* Mini Project Strip */}
              <div className="grid grid-cols-3 gap-2">
                <div className="aspect-[4/3] rounded-lg bg-[var(--color-deep-charcoal)] border border-[var(--border)] flex items-center justify-center p-2 text-center text-[10px] text-[var(--color-warm-ivory)]">
                  TV Lounge
                </div>
                <div className="aspect-[4/3] rounded-lg bg-[#2E5D4B] border border-[var(--border)] flex items-center justify-center p-2 text-center text-[10px] text-[var(--color-warm-ivory)]">
                  Oak Kitchen
                </div>
                <div className="aspect-[4/3] rounded-lg bg-[#3E6D8C] border border-[var(--border)] flex items-center justify-center p-2 text-center text-[10px] text-[var(--color-warm-ivory)]">
                  Master Suite
                </div>
              </div>

              {/* Design Direction Note */}
              <div className="p-3.5 rounded-xl bg-[var(--surface-alt)] border border-[var(--border)] space-y-1">
                <p className="text-xs font-semibold text-[var(--foreground)] flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-[var(--brand)]" />
                  <span>Curated Aesthetic Directions</span>
                </p>
                <p className="text-[11px] text-[var(--muted)] leading-relaxed">
                  Tailor your presentation with distinct themes: Architectural, Minimalist, Warm Natural, Luxury, and Dark Cinematic.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
