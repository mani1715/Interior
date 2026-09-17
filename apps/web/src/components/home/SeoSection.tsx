import React from 'react';
import { Search, Globe, FileCode2, Share2, ArrowRight } from 'lucide-react';
import { SectionHeader, SectionTitle, SectionEyebrow, SectionDescription } from '@/components/layout/Section';

export function SeoSection() {
  const seoPillars = [
    {
      icon: Globe,
      title: 'Clean, Semantic URLs',
      desc: 'Human-readable paths like /projects/tv-unit-guntur-walnut that search engines easily index.',
    },
    {
      icon: FileCode2,
      title: 'Built-in Schema.org Metadata',
      desc: 'Automated JSON-LD structured data for projects, creator profiles, and local service areas.',
    },
    {
      icon: Search,
      title: 'Local Context Indexing',
      desc: 'Structured city and neighborhood tagging ensures homeowners in your area find your projects.',
    },
    {
      icon: Share2,
      title: 'Fast Server-Side Rendering',
      desc: 'Instant HTML delivery with sub-second LCP so search engine bots index 100% of your photography.',
    },
  ];

  return (
    <section className="w-full py-12 sm:py-16 md:py-24 bg-[var(--surface-alt)] border-b border-[var(--border)]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
          {/* Left Text */}
          <div className="lg:col-span-6 space-y-6">
            <SectionHeader className="mb-4">
              <div>
                <SectionEyebrow>06 / Organic Discovery</SectionEyebrow>
                <SectionTitle>Built to be discoverable on Google.</SectionTitle>
                <SectionDescription>
                  Social algorithms bury your best work after 24 hours. On Elégance, every project you publish becomes an enduring, search-optimized web page designed to attract local clients for years to come.
                </SectionDescription>
              </div>
            </SectionHeader>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {seoPillars.map((p, idx) => {
                const Icon = p.icon;
                return (
                  <div key={idx} className="p-4 rounded-xl border border-[var(--border)] bg-[var(--surface)] space-y-2 shadow-sm">
                    <Icon className="w-5 h-5 text-[var(--brand)]" />
                    <h4 className="font-serif text-sm font-semibold text-[var(--foreground)]">
                      {p.title}
                    </h4>
                    <p className="text-xs text-[var(--muted)] leading-relaxed">
                      {p.desc}
                    </p>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Right Visual: Realistic Search Result Snippet Preview */}
          <div className="lg:col-span-6">
            <div className="p-5 sm:p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-xl space-y-5">
              <span className="text-[11px] font-semibold uppercase tracking-widest text-[var(--muted)] block">
                Google Search Result Simulation
              </span>

              {/* Mock Google Card */}
              <div className="p-4 rounded-xl border border-[var(--border)] bg-[var(--background)] space-y-2">
                <div className="flex items-center gap-2 text-xs text-[var(--muted)]">
                  <div className="w-4 h-4 rounded-full bg-[var(--brand)] flex items-center justify-center text-[8px] font-bold text-[var(--charcoal)]">
                    E
                  </div>
                  <span className="font-mono text-[11px] truncate">
                    elegance.interior › projects › modern-tv-unit-guntur
                  </span>
                </div>

                <h4 className="text-sm sm:text-base font-medium text-[var(--info)] hover:underline cursor-pointer">
                  Modern TV Unit Design in Guntur | Studio Elégance
                </h4>

                <p className="text-xs text-[var(--muted)] leading-relaxed">
                  Explore this turnkey 75-inch TV lounge with bookmatched Italian Statuario marble, 12mm CNC fluted walnut paneling, and warm 3000K indirect cove lighting in Guntur, AP.
                </p>

                <div className="pt-2 flex items-center gap-2 flex-wrap text-[10px] text-[var(--muted)]">
                  <span className="px-2 py-0.5 rounded bg-[var(--surface-alt)] border border-[var(--border)]">
                    City: Guntur
                  </span>
                  <span className="px-2 py-0.5 rounded bg-[var(--surface-alt)] border border-[var(--border)]">
                    Material: Statuario & Walnut
                  </span>
                  <span className="px-2 py-0.5 rounded bg-[var(--surface-alt)] border border-[var(--border)]">
                    Status: Verified Execution
                  </span>
                </div>
              </div>

              {/* Conversion Flow Path */}
              <div className="p-3.5 rounded-xl bg-[var(--surface-alt)] border border-[var(--border)] flex items-center justify-between text-xs text-[var(--muted)] flex-wrap gap-2">
                <span className="font-semibold text-[var(--foreground)]">Discovery Loop:</span>
                <div className="flex items-center gap-1.5 font-medium text-[11px]">
                  <span>Project</span>
                  <ArrowRight className="w-3 h-3 text-[var(--brand)]" />
                  <span>Indexable Page</span>
                  <ArrowRight className="w-3 h-3 text-[var(--brand)]" />
                  <span>Homeowner</span>
                  <ArrowRight className="w-3 h-3 text-[var(--brand)]" />
                  <span className="text-[var(--brand)] font-bold">Enquiry</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
