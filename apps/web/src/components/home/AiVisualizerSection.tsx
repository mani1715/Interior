import React from 'react';
import Image from 'next/image';
import { Sparkles, Camera, Sliders, Palette, Layers, Info } from 'lucide-react';
import { SectionHeader, SectionTitle, SectionEyebrow, SectionDescription } from '@/components/layout/Section';
import { AIConceptBadge } from '@/components/ui/Badge';

export function AiVisualizerSection() {
  const referenceTypes = [
    {
      title: 'Colour Reference',
      desc: 'Pinpoint specific shade palettes like Sage Neutral or Deep Charcoal to guide the ambient tone.',
      icon: Palette,
    },
    {
      title: 'Material Reference',
      desc: 'Guide the diffusion with fluted walnut, Statuario marble, or microcement textures.',
      icon: Layers,
    },
    {
      title: 'Handle & Hardware',
      desc: 'Specify brass profiles, knurled knobs, or seamless push-to-open shadow grooves.',
      icon: Sliders,
    },
    {
      title: 'Design Style',
      desc: 'Anchor spatial layout to Japandi, Neo-Classical, or Contemporary Architectural aesthetics.',
      icon: Sparkles,
    },
  ];

  return (
    <section id="ai-visualizer" className="w-full py-12 sm:py-16 md:py-24 bg-[var(--surface-alt)] border-b border-[var(--border)]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeader>
          <div>
            <SectionEyebrow>04 / AI Spatial Diffusion</SectionEyebrow>
            <SectionTitle>Visualize Client Ideas on Real Site Photos</SectionTitle>
            <SectionDescription>
              Don’t start from a blank canvas. Snap a photo of an unfinished room, add your instructions and reference swatches, and generate realistic spatial concepts to align with your client.
            </SectionDescription>
          </div>
        </SectionHeader>

        {/* The 4-Step Formula Strip */}
        <div className="p-4 sm:p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-md mb-10 sm:mb-12">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-center text-center sm:text-left">
            <div className="p-3 rounded-xl bg-[var(--surface-alt)] border border-[var(--border)]">
              <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--brand)] block mb-1">
                Step 1: Input
              </span>
              <p className="text-xs font-semibold text-[var(--foreground)]">Real Site Photo</p>
              <p className="text-[11px] text-[var(--muted)]">Raw masonry or shell</p>
            </div>

            <div className="p-3 rounded-xl bg-[var(--surface-alt)] border border-[var(--border)]">
              <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--brand)] block mb-1">
                Step 2: Direction
              </span>
              <p className="text-xs font-semibold text-[var(--foreground)]">Text Prompt</p>
              <p className="text-[11px] text-[var(--muted)]">“Add walnut fluting & warm cove”</p>
            </div>

            <div className="p-3 rounded-xl bg-[var(--surface-alt)] border border-[var(--border)]">
              <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--brand)] block mb-1">
                Step 3: Precision
              </span>
              <p className="text-xs font-semibold text-[var(--foreground)]">Reference Swatches</p>
              <p className="text-[11px] text-[var(--muted)]">Colour, Material & Handle</p>
            </div>

            <div className="p-3 rounded-xl bg-[var(--brand-muted)] border border-[var(--brand)]/30">
              <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--brand)] block mb-1">
                Step 4: Output
              </span>
              <p className="text-xs font-semibold text-[var(--brand)]">AI Concept Study</p>
              <p className="text-[11px] text-[var(--muted)]">Tagged with disclaimer</p>
            </div>
          </div>

          {/* Prompt Example Box */}
          <div className="mt-4 p-3 rounded-xl bg-[var(--surface-alt)] border border-[var(--border)] flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
            <div className="flex items-center gap-2">
              <span className="px-2 py-0.5 rounded bg-[var(--surface)] text-[10px] font-mono text-[var(--brand)] font-bold border border-[var(--border)]">
                PROMPT
              </span>
              <span className="text-[var(--foreground)] italic">
                “Add alternating fluted walnut and white matte shutters. Use champagne brass handles. Keep existing structural columns unchanged.”
              </span>
            </div>
          </div>
        </div>

        {/* Reference Image Differentiator Grid */}
        <div className="space-y-4 mb-10 sm:mb-12">
          <div className="max-w-xl">
            <h4 className="font-serif text-lg font-semibold text-[var(--foreground)]">
              Reference Images: Directing the AI with Precision
            </h4>
            <p className="text-xs sm:text-sm text-[var(--muted)] mt-1 leading-relaxed">
              Words alone cannot capture the nuance of smoked oak versus natural teak. Reference images guide the rendering engine to explore the specific aesthetics your client envisions.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {referenceTypes.map((item, idx) => {
              const Icon = item.icon;
              return (
                <div key={idx} className="p-4 rounded-xl border border-[var(--border)] bg-[var(--surface)] space-y-2 shadow-sm">
                  <div className="w-8 h-8 rounded-lg bg-[var(--surface-alt)] flex items-center justify-center text-[var(--brand)]">
                    <Icon className="w-4 h-4" />
                  </div>
                  <h5 className="font-serif text-sm font-semibold text-[var(--foreground)]">
                    {item.title}
                  </h5>
                  <p className="text-xs text-[var(--muted)] leading-relaxed">
                    {item.desc}
                  </p>
                </div>
              );
            })}
          </div>
        </div>

        {/* Mandatory AI Disclosure Alert */}
        <div className="p-4 rounded-xl border border-[var(--border)] bg-[var(--surface)] flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <AIConceptBadge />
            <div className="text-xs text-[var(--muted)]">
              <span className="font-semibold text-[var(--foreground)]">Legal & Architectural Disclosure:</span>{' '}
              Colours and materials shown in AI concepts are visual simulations and may differ from the final physical manufactured result.
            </div>
          </div>
          <div className="flex items-center gap-1.5 text-xs text-[var(--muted)] flex-shrink-0">
            <Info className="w-4 h-4 text-[var(--brand)]" />
            <span>Standard on all renders</span>
          </div>
        </div>
      </div>
    </section>
  );
}
