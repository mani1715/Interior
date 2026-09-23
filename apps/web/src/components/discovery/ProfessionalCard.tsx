import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { MapPin, ArrowRight, Briefcase } from 'lucide-react';
import { Professional } from '@/lib/discovery/types';

export interface ProfessionalCardProps {
  professional: Professional;
}

export function ProfessionalCard({ professional }: ProfessionalCardProps) {
  const sampleCovers = professional.sampleProjectCoverUrls || [];
  const publishedCount = professional.projectCount ?? sampleCovers.length;

  return (
    <article className="group flex flex-col rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-5 sm:p-6 shadow-sm hover:border-[var(--brand)] hover:shadow-md transition-all duration-300">
      {/* Header with Avatar & Studio Meta */}
      <div className="flex items-start justify-between gap-4 mb-4">
        <div className="flex items-center gap-3">
          <div
            className="w-12 h-12 rounded-xl flex items-center justify-center font-serif font-bold text-xl text-[var(--charcoal)] shadow-sm flex-shrink-0"
            style={{ backgroundColor: professional.avatarColor }}
          >
            {professional.avatarChar}
          </div>
          <div>
            <h3 className="font-serif text-lg font-semibold text-[var(--foreground)] group-hover:text-[var(--brand)] transition-colors leading-tight">
              <Link href={`/professionals/${professional.slug}`}>
                {professional.studioName}
              </Link>
            </h3>
            <p className="text-xs text-[var(--muted)]">{professional.name}</p>
            <div className="flex items-center gap-2 mt-1">
              <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--muted)]">
                {professional.professionalTypeLabel}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Bio excerpt */}
      <p className="text-xs sm:text-sm text-[var(--muted)] line-clamp-2 mb-4 leading-relaxed">
        {professional.bio}
      </p>

      {/* Location & Starting Budget */}
      <div className="flex items-center justify-between text-xs text-[var(--muted)] mb-4 pt-3 border-t border-[var(--border)]">
        <div className="flex items-center gap-1">
          <MapPin className="w-3.5 h-3.5 text-[var(--brand)] flex-shrink-0" />
          <span className="truncate">{professional.locationName}</span>
        </div>
        {professional.startingBudgetLabel && (
          <div className="text-[11px] font-mono">
            Starts from <span className="font-semibold text-[var(--foreground)]">{professional.startingBudgetLabel}</span>
          </div>
        )}
      </div>

      {/* Specialties */}
      <div className="flex flex-wrap gap-1.5 mb-5">
        {professional.specialties.map((spec, idx) => (
          <span
            key={idx}
            className="px-2 py-0.5 rounded text-[10px] font-medium bg-[var(--surface-alt)] text-[var(--muted)] border border-[var(--border)]"
          >
            {spec}
          </span>
        ))}
      </div>

      {/* Project Thumbnails Preview (real cover assets only) */}
      {sampleCovers.length > 0 && (
        <div className="grid grid-cols-3 gap-2 mb-5">
          {sampleCovers.map((coverUrl, idx) => (
            <div
              key={idx}
              className="relative aspect-[4/3] rounded-lg overflow-hidden border border-[var(--border)] bg-[var(--surface-alt)] group/thumb"
            >
              <Image
                src={coverUrl}
                alt={`${professional.name} sample work ${idx + 1}`}
                fill
                sizes="(max-width: 640px) 30vw, 15vw"
                className="object-cover group-hover/thumb:scale-105 transition-transform duration-300"
              />
            </div>
          ))}
        </div>
      )}

      {/* Footer CTA */}
      <div className="pt-3 border-t border-[var(--border)] mt-auto flex items-center justify-between">
        <span className="text-xs text-[var(--muted)] flex items-center gap-1">
          <Briefcase className="w-3.5 h-3.5 text-[var(--brand)]" />
          <span>{publishedCount} Published Projects</span>
        </span>

        <Link
          href={`/professionals/${professional.slug}`}
          className="text-xs font-semibold text-[var(--brand)] flex items-center gap-1 group-hover:translate-x-1 transition-transform min-h-[44px] inline-flex items-center"
        >
          <span>View Studio Profile</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      </div>
    </article>
  );
}
