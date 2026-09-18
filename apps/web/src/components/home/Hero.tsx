import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { ArrowRight, Sparkles, ShieldCheck, Compass } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { AIConceptBadge } from '@/components/ui/Badge';

export function Hero() {
  return (
    <section className="relative w-full pt-8 pb-12 sm:pt-14 sm:pb-20 overflow-hidden bg-[var(--background)]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
          {/* Left Editorial Copy (Single H1) */}
          <div className="lg:col-span-7 flex flex-col items-start text-left">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[var(--surface-alt)] border border-[var(--border)] mb-4 sm:mb-6">
              <span className="w-2 h-2 rounded-full bg-[var(--brand)] animate-pulse" />
              <span className="text-[11px] sm:text-xs font-semibold uppercase tracking-widest text-[var(--foreground)]">
                The Digital Home for Interior Professionals
              </span>
            </div>

            <h1 className="font-serif text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-semibold tracking-tight text-[var(--foreground)] leading-[1.12] mb-4 sm:mb-6">
              Turn beautiful interiors into a business people can discover.
            </h1>

            <p className="text-sm sm:text-base md:text-lg text-[var(--muted)] leading-relaxed max-w-2xl mb-6 sm:mb-8">
              Build your professional portfolio. Built to be discoverable on Google with structured project pages. Visualize ideas on real site photos with AI. Turn client admiration into direct project inquiries.
            </p>

            {/* CTAs */}
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 w-full sm:w-auto mb-6 sm:mb-8">
              <Link href="#portfolio-builder" className="w-full sm:w-auto">
                <Button
                  variant="primary"
                  size="lg"
                  className="w-full justify-center text-sm"
                  rightIcon={<ArrowRight className="w-4 h-4" />}
                >
                  Create Your Portfolio
                </Button>
              </Link>
              <Link href="/projects" className="w-full sm:w-auto">
                <Button
                  variant="outline"
                  size="lg"
                  className="w-full justify-center text-sm"
                  leftIcon={<Compass className="w-4 h-4 text-[var(--brand)]" />}
                >
                  Explore Interior Projects
                </Button>
              </Link>
            </div>

            {/* Trust Statement */}
            <div className="flex items-center gap-2.5 text-xs text-[var(--muted)] pt-2 border-t border-[var(--border)] w-full sm:w-auto">
              <ShieldCheck className="w-4 h-4 text-[var(--brand)] flex-shrink-0" />
              <span>Built for interior professionals and homeowners across India&apos;s growing design communities.</span>
            </div>
          </div>

          {/* Right Visual Composition */}
          <div className="lg:col-span-5 relative w-full">
            <div className="relative rounded-2xl overflow-hidden border border-[var(--border)] bg-[var(--surface)] shadow-xl aspect-[4/3] sm:aspect-[16/11]">
              {/* Primary Architectural Showcase Visual (LCP Candidate) */}
              <Image
                src={`data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 700 500" width="700" height="500">
                  <defs>
                    <linearGradient id="heroBg" x1="0" y1="0" x2="0" y2="100%">
                      <stop offset="0%" stop-color="%231f1d1b"/>
                      <stop offset="100%" stop-color="%23141312"/>
                    </linearGradient>
                    <linearGradient id="warmLight" x1="50%" y1="0%" x2="50%" y2="100%">
                      <stop offset="0%" stop-color="%23ffd699" stop-opacity="0.85"/>
                      <stop offset="100%" stop-color="%23ffd699" stop-opacity="0"/>
                    </linearGradient>
                  </defs>
                  <rect width="700" height="500" fill="url(%23heroBg)"/>
                  <!-- Cove lighting cone -->
                  <polygon points="180,40 100,340 260,340" fill="url(%23warmLight)" opacity="0.3"/>
                  <polygon points="520,40 440,340 600,340" fill="url(%23warmLight)" opacity="0.3"/>
                  <!-- Statuario Wall -->
                  <rect x="140" y="70" width="420" height="280" fill="%23e8e4df" rx="6"/>
                  <path d="M 160 100 Q 300 210 380 160 T 520 280" fill="none" stroke="%23b8b0a5" stroke-width="3" opacity="0.75"/>
                  <!-- Fluted Walnut Sides -->
                  <rect x="70" y="70" width="70" height="280" fill="%235c3b24"/>
                  <rect x="560" y="70" width="70" height="280" fill="%235c3b24"/>
                  <!-- OLED Screen -->
                  <rect x="200" y="110" width="300" height="170" fill="%230a0a0a" stroke="%232b2b2b" stroke-width="3" rx="4"/>
                  <circle cx="350" cy="195" r="28" fill="%23B88A5A" opacity="0.4"/>
                  <!-- Floating console with brass line -->
                  <rect x="70" y="350" width="560" height="60" fill="%23482d1b" rx="4"/>
                  <line x1="70" y1="352" x2="630" y2="352" stroke="%23B88A5A" stroke-width="3"/>
                </svg>`}
                alt="Contemporary TV Lounge Architecture by Studio Elégance"
                fill
                priority
                className="object-cover"
                sizes="(max-width: 1024px) 100vw, 45vw"
              />

              {/* Floating Overlays */}
              <div className="absolute top-3 left-3">
                <AIConceptBadge />
              </div>

              <div className="absolute bottom-3 inset-x-3 p-3 rounded-xl bg-[var(--surface)]/90 backdrop-blur-md border border-[var(--border)] flex items-center justify-between text-xs">
                <div>
                  <p className="font-medium text-[var(--foreground)] truncate">Modern TV Unit & Lounge</p>
                  <p className="text-[10px] text-[var(--muted)]">Guntur, Andhra Pradesh • Studio Elégance</p>
                </div>
                <div className="flex items-center gap-1 text-[var(--brand)] font-semibold text-[11px]">
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>AI Concept Preview</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
