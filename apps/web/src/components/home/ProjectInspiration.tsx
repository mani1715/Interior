'use client';

import React, { useState } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { Eye, MapPin, ArrowRight } from 'lucide-react';
import { SectionHeader, SectionTitle, SectionEyebrow, SectionDescription } from '@/components/layout/Section';
import { FilterChip } from '@/components/ui/Filter';
import { Button } from '@/components/ui/Button';
import { inspirationProjects } from './mockHomeData';

export function ProjectInspiration() {
  const [activeCategory, setActiveCategory] = useState('All');
  const categories = ['All', 'TV Unit', 'Modular Kitchen', 'Bedroom', 'Pooja Unit', 'Wardrobe', 'Living Room'];

  const filteredProjects =
    activeCategory === 'All'
      ? inspirationProjects
      : inspirationProjects.filter((p) => p.category === activeCategory);

  return (
    <section id="project-inspiration" className="w-full py-12 sm:py-16 md:py-24 bg-[var(--background)] border-b border-[var(--border)]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeader>
          <div>
            <SectionEyebrow>07 / Inspiration Gallery</SectionEyebrow>
            <SectionTitle>Curated Interior Work Across India</SectionTitle>
            <SectionDescription>
              Explore real spatial transformations designed and built by verified architects, interior studios, and cabinetry professionals.
            </SectionDescription>
          </div>
        </SectionHeader>

        {/* Filter Pills */}
        <div className="flex items-center gap-2 overflow-x-auto pb-4 mb-6 sm:mb-8 scrollbar-none">
          {categories.map((cat) => (
            <FilterChip
              key={cat}
              label={cat}
              isSelected={activeCategory === cat}
              onToggle={() => setActiveCategory(cat)}
            />
          ))}
        </div>

        {/* Project Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredProjects.map((project) => (
            <article
              key={project.id}
              className="flex flex-col rounded-2xl border border-[var(--border)] bg-[var(--surface)] overflow-hidden shadow-sm hover:border-[var(--brand)] hover:shadow-md transition-all group"
            >
              {/* Image with 16/10 aspect ratio */}
              <div className="relative aspect-[16/10] w-full overflow-hidden bg-[var(--surface-alt)]">
                <Image
                  src={project.image}
                  alt={project.title}
                  fill
                  sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                  className="object-cover group-hover:scale-105 transition-transform duration-500"
                />

                <div className="absolute top-3 left-3 px-2.5 py-1 rounded-md bg-black/60 backdrop-blur-sm text-white text-[10px] font-semibold uppercase tracking-wider">
                  {project.category}
                </div>

                <div className="absolute top-3 right-3 px-2 py-0.5 rounded bg-black/60 backdrop-blur-sm text-white/90 text-[10px] flex items-center gap-1 font-mono">
                  <Eye className="w-3 h-3" />
                  <span>{project.viewCount}</span>
                </div>
              </div>

              {/* Card Meta Body */}
              <div className="p-4 sm:p-5 flex-1 flex flex-col justify-between">
                <div>
                  <h4 className="font-serif text-base font-semibold text-[var(--foreground)] mb-1.5 group-hover:text-[var(--brand)] transition-colors">
                    {project.title}
                  </h4>
                  <div className="flex items-center gap-1.5 text-xs text-[var(--muted)] mb-3">
                    <MapPin className="w-3.5 h-3.5 text-[var(--brand)] flex-shrink-0" />
                    <span className="truncate">{project.location}</span>
                  </div>

                  {/* Material & Feature Tags */}
                  <div className="flex flex-wrap gap-1.5 mb-4">
                    {project.tags.map((tag, tIdx) => (
                      <span
                        key={tIdx}
                        className="px-2 py-0.5 rounded text-[10px] font-medium bg-[var(--surface-alt)] text-[var(--muted)] border border-[var(--border)]"
                      >
                        {tag}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Footer Creator Link */}
                <div className="pt-3 border-t border-[var(--border)] flex items-center justify-between text-xs">
                  <div className="truncate">
                    <p className="font-medium text-[var(--foreground)] truncate">{project.studioName}</p>
                    <p className="text-[10px] text-[var(--muted)]">{project.designerName}</p>
                  </div>
                  <span className="text-[var(--brand)] font-semibold flex items-center gap-1 group-hover:translate-x-1 transition-transform">
                    <span>View</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </span>
                </div>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
