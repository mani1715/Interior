'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { MapPin, Bookmark, ArrowRight, Sparkles } from 'lucide-react';
import { Project } from '@/lib/discovery/types';

export interface ProjectCardProps {
  project: Project;
  priority?: boolean;
}

export function ProjectCard({ project, priority = false }: ProjectCardProps) {
  const [isSaved, setIsSaved] = useState(false);

  const toggleSave = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsSaved((prev) => !prev);
  };

  return (
    <article className="group flex flex-col rounded-2xl border border-[var(--border)] bg-[var(--surface)] overflow-hidden shadow-sm hover:border-[var(--brand)] hover:shadow-md transition-all duration-300">
      {/* Visual Image Container */}
      <div className="relative aspect-[16/10] w-full overflow-hidden bg-[var(--surface-alt)]">
        <Link href={`/projects/${project.slug}`} tabIndex={-1} aria-hidden="true">
          <Image
            src={project.coverImage}
            alt={project.title}
            fill
            priority={priority}
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
            className="object-cover group-hover:scale-105 transition-transform duration-500 ease-out"
          />
        </Link>

        {/* Category Badge */}
        <div className="absolute top-3 left-3 px-2.5 py-1 rounded-md bg-black/60 backdrop-blur-sm text-white text-[10px] font-semibold uppercase tracking-wider">
          {project.categoryName}
        </div>

        {/* AI Concept Marker if project uses AI concept visualization */}
        {(project.isAiConceptCover || project.aiImage) && (
          <div
            className="absolute top-3 left-24 px-2.5 py-1 rounded bg-[var(--brand)] text-[var(--charcoal)] text-[9px] font-bold uppercase tracking-wider flex items-center gap-1 shadow-sm"
            aria-label="AI Concept Visualization"
          >
            <Sparkles className="w-3 h-3" aria-hidden="true" />
            <span>✦ AI Concept Visualization</span>
          </div>
        )}

        {/* Local Demo Save Action */}
        <button
          type="button"
          onClick={toggleSave}
          aria-label={isSaved ? 'Remove from saved inspiration' : 'Save to inspiration (Demo)'}
          title={isSaved ? 'Saved (Demo)' : 'Save to inspiration (Demo)'}
          className={`absolute top-3 right-3 p-2 rounded-full backdrop-blur-md transition-all min-h-[44px] min-w-[44px] flex items-center justify-center ${
            isSaved
              ? 'bg-[var(--brand)] text-[var(--charcoal)]'
              : 'bg-black/50 text-white/90 hover:bg-black/70'
          }`}
        >
          <Bookmark className="w-3.5 h-3.5" fill={isSaved ? 'currentColor' : 'none'} />
        </button>

        {/* Budget Overlay Badge */}
        {project.budgetLabel && (
          <div className="absolute bottom-3 right-3 px-2 py-0.5 rounded bg-black/60 backdrop-blur-sm text-white/90 text-[10px] font-mono">
            {project.budgetLabel}
          </div>
        )}
      </div>

      {/* Card Content */}
      <div className="p-4 sm:p-5 flex-1 flex flex-col justify-between">
        <div>
          {/* Title linking to Project Detail */}
          <h3 className="font-serif text-base sm:text-lg font-semibold text-[var(--foreground)] mb-1.5 leading-snug group-hover:text-[var(--brand)] transition-colors line-clamp-2">
            <Link href={`/projects/${project.slug}`} className="focus:outline-none focus:underline">
              {project.title}
            </Link>
          </h3>

          {/* Location & Style Meta */}
          <div className="flex items-center gap-2 text-xs text-[var(--muted)] mb-3 flex-wrap">
            <span className="flex items-center gap-1">
              <MapPin className="w-3.5 h-3.5 text-[var(--brand)] flex-shrink-0" />
              <span className="truncate">{project.locationName}</span>
            </span>
            <span className="text-[var(--border-strong)]">•</span>
            <span>{project.styleName}</span>
            <span className="text-[var(--border-strong)]">•</span>
            <span>{project.propertyTypeName}</span>
          </div>

          {/* Materials Preview Chips */}
          <div className="flex flex-wrap gap-1 mb-4">
            {project.materials.slice(0, 3).map((m, idx) => (
              <span
                key={idx}
                className="px-2 py-0.5 rounded text-[10px] font-medium bg-[var(--surface-alt)] text-[var(--muted)] border border-[var(--border)]"
              >
                {m}
              </span>
            ))}
            {project.materials.length > 3 && (
              <span className="px-1.5 py-0.5 rounded text-[10px] text-[var(--muted)]">
                +{project.materials.length - 3}
              </span>
            )}
          </div>
        </div>

        {/* Studio Attribution Footer */}
        <div className="pt-3 border-t border-[var(--border)] flex items-center justify-between text-xs">
          <Link
            href={`/professionals/${project.professionalSlug}`}
            className="flex items-center gap-2 hover:opacity-80 transition-opacity truncate max-w-[70%]"
          >
            <div className="w-6 h-6 rounded-md bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--brand)] flex items-center justify-center font-serif font-bold text-xs flex-shrink-0">
              {project.studioName.charAt(0)}
            </div>
            <div className="truncate">
              <p className="font-medium text-[var(--foreground)] truncate">{project.studioName}</p>
              <p className="text-[10px] text-[var(--muted)] truncate">{project.professionalTypeName}</p>
            </div>
          </Link>

          <Link
            href={`/projects/${project.slug}`}
            className="text-[var(--brand)] font-semibold flex items-center gap-1 group-hover:translate-x-1 transition-transform pl-2"
          >
            <span>View</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>
    </article>
  );
}
