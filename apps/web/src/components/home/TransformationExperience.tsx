'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import { ChevronLeft, ChevronRight, Layers, CheckCircle2 } from 'lucide-react';
import { transformationStages } from './mockHomeData';
import { Button } from '@/components/ui/Button';
import { SectionHeader, SectionTitle, SectionEyebrow, SectionDescription } from '@/components/layout/Section';

export function TransformationExperience() {
  const [activeStageIndex, setActiveStageIndex] = useState(0);
  const current = transformationStages[activeStageIndex];

  const handlePrev = () => {
    setActiveStageIndex((prev) => (prev > 0 ? prev - 1 : transformationStages.length - 1));
  };

  const handleNext = () => {
    setActiveStageIndex((prev) => (prev < transformationStages.length - 1 ? prev + 1 : 0));
  };

  return (
    <section
      id="transformation-experience"
      aria-label="Interior Transformation Architectural Experience"
      className="w-full py-12 sm:py-16 md:py-24 bg-[var(--background)] border-b border-[var(--border)] overflow-hidden"
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeader>
          <div>
            <SectionEyebrow>Signature Experience</SectionEyebrow>
            <SectionTitle>From Raw Shell to Built Reality</SectionTitle>
            <SectionDescription>
              Step inside the craft of interior architecture. Experience how structure, materials, joinery, and lighting come together to create a living space.
            </SectionDescription>
          </div>
          <div className="hidden sm:flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handlePrev}
              aria-label="Previous construction stage"
              leftIcon={<ChevronLeft className="w-4 h-4" />}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleNext}
              aria-label="Next construction stage"
              rightIcon={<ChevronRight className="w-4 h-4" />}
            >
              Next
            </Button>
          </div>
        </SectionHeader>

        {/* Step Progression Bar (Scrollable on mobile) */}
        <div
          role="tablist"
          aria-label="Transformation Stages"
          className="flex items-center gap-1.5 overflow-x-auto pb-3 mb-6 sm:mb-8 scrollbar-none"
        >
          {transformationStages.map((stage, idx) => {
            const isActive = idx === activeStageIndex;
            return (
              <button
                key={stage.id}
                role="tab"
                aria-selected={isActive}
                onClick={() => setActiveStageIndex(idx)}
                className={`flex-shrink-0 px-3 py-2 rounded-xl text-xs font-medium transition-all min-h-[44px] flex items-center gap-2 border ${
                  isActive
                    ? 'bg-[var(--surface-raised)] border-[var(--brand)] text-[var(--brand)] shadow-sm font-semibold'
                    : 'bg-[var(--surface)] border-[var(--border)] text-[var(--muted)] hover:text-[var(--foreground)]'
                }`}
              >
                <span
                  className={`w-5 h-5 rounded-full text-[10px] flex items-center justify-center font-mono ${
                    isActive
                      ? 'bg-[var(--brand)] text-[var(--charcoal)] font-bold'
                      : 'bg-[var(--surface-alt)] text-[var(--muted)]'
                  }`}
                >
                  {stage.id}
                </span>
                <span className="whitespace-nowrap">{stage.name.split('. ')[1]}</span>
              </button>
            );
          })}
        </div>

        {/* Stage Presentation Canvas */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 lg:gap-8 items-center">
          {/* Visual Display */}
          <div className="lg:col-span-8 relative rounded-2xl overflow-hidden border border-[var(--border)] bg-[var(--surface)] shadow-xl aspect-[16/10] sm:aspect-[16/10]">
            <Image
              src={current.svgContent}
              alt={`Stage ${current.id}: ${current.title}`}
              fill
              className="object-cover transition-opacity duration-300"
              sizes="(max-width: 1024px) 100vw, 65vw"
            />

            {/* Stage Indicator Badge */}
            <div className="absolute top-3 left-3 bg-black/60 backdrop-blur-sm px-3 py-1 rounded-lg border border-white/10 text-white text-xs font-medium flex items-center gap-2">
              <Layers className="w-3.5 h-3.5 text-[var(--brand)]" />
              <span>
                Stage {current.id} of {transformationStages.length}
              </span>
            </div>

            {/* Mobile Prev/Next Overlay Buttons */}
            <div className="flex sm:hidden absolute bottom-3 right-3 gap-2">
              <button
                type="button"
                onClick={handlePrev}
                aria-label="Previous stage"
                className="w-10 h-10 rounded-full bg-black/60 backdrop-blur-sm text-white flex items-center justify-center border border-white/20 active:scale-95"
              >
                <ChevronLeft className="w-5 h-5" />
              </button>
              <button
                type="button"
                onClick={handleNext}
                aria-label="Next stage"
                className="w-10 h-10 rounded-full bg-[var(--brand)] text-[var(--charcoal)] flex items-center justify-center font-bold active:scale-95 shadow-lg"
              >
                <ChevronRight className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Technical Specs & Storytelling Column */}
          <div className="lg:col-span-4 flex flex-col justify-between space-y-4 sm:space-y-6">
            <div className="p-5 sm:p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-sm">
              <span className="text-[11px] font-semibold uppercase tracking-widest text-[var(--brand)] block mb-1">
                {current.name}
              </span>
              <h3 className="font-serif text-xl sm:text-2xl font-semibold text-[var(--foreground)] mb-2">
                {current.title}
              </h3>
              <p className="text-xs sm:text-sm text-[var(--muted)] leading-relaxed mb-4">
                {current.fullDesc}
              </p>

              <div className="pt-4 border-t border-[var(--border)] space-y-2">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-[var(--foreground)]">
                  Execution Specifications:
                </p>
                <ul className="space-y-1.5">
                  {current.specs.map((spec, sIdx) => (
                    <li key={sIdx} className="flex items-center gap-2 text-xs text-[var(--muted)]">
                      <CheckCircle2 className="w-3.5 h-3.5 text-[var(--brand)] flex-shrink-0" />
                      <span>{spec}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            {/* Semantic Accessible Fallback for Screen Readers */}
            <div className="sr-only">
              <h4>All 7 Construction Stages:</h4>
              <ol>
                {transformationStages.map((s) => (
                  <li key={s.id}>
                    <strong>
                      Stage {s.id}: {s.title}
                    </strong>{' '}
                    — {s.fullDesc}
                  </li>
                ))}
              </ol>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
