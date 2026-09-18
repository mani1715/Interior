import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import Link from 'next/link';
import { MapPin, Briefcase, Award, CheckCircle2, ArrowRight } from 'lucide-react';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { Footer } from '@/components/home/Footer';
import { Breadcrumb } from '@/components/ui/Breadcrumb';
import { ProjectCard } from '@/components/discovery/ProjectCard';
import { Button } from '@/components/ui/Button';
import { getProfessionalBySlug, getProjectsByProfessional } from '@/lib/discovery/queries';

interface PageProps {
  params: Promise<{
    professionalSlug: string;
  }>;
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { professionalSlug } = await params;
  const professional = getProfessionalBySlug(professionalSlug);

  if (!professional) {
    return {
      title: 'Professional Not Found | Elégance',
    };
  }

  const title = `${professional.studioName} | ${professional.professionalTypeLabel} in ${professional.locationName}`;
  const description = professional.bio;

  return {
    title,
    description,
    alternates: {
      canonical: `/professionals/${professional.slug}`,
    },
    openGraph: {
      title,
      description,
      url: `/professionals/${professional.slug}`,
      siteName: 'Elégance Interior Platform',
      locale: 'en_IN',
      type: 'profile',
    },
  };
}

export default async function ProfessionalDetailPage({ params }: PageProps) {
  const { professionalSlug } = await params;
  const professional = getProfessionalBySlug(professionalSlug);

  if (!professional) {
    notFound();
  }

  const projects = getProjectsByProfessional(professional.slug);

  return (
    <div className="min-h-screen bg-[var(--background)] text-[var(--foreground)] flex flex-col selection:bg-[var(--brand)] selection:text-[var(--charcoal)]">
      <PublicHeader currentPath="/professionals" />

      <main id="main-content" className="flex-1 w-full py-6 sm:py-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8 sm:space-y-12">
          {/* Breadcrumbs */}
          <Breadcrumb
            items={[
              { label: 'Home', href: '/' },
              { label: 'Professionals', href: '/professionals' },
              { label: professional.studioName, href: `/professionals/${professional.slug}`, current: true },
            ]}
          />

          {/* Studio Profile Header Card */}
          <div className="p-6 sm:p-8 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-md">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
              <div className="flex items-start sm:items-center gap-4 sm:gap-6">
                <div
                  className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl flex items-center justify-center font-serif font-bold text-3xl text-[var(--charcoal)] shadow-sm flex-shrink-0"
                  style={{ backgroundColor: professional.avatarColor }}
                >
                  {professional.avatarChar}
                </div>
                <div>
                  <div className="flex items-center gap-2 flex-wrap mb-1">
                    <span className="px-2.5 py-0.5 rounded text-[11px] font-semibold bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--foreground)]">
                      {professional.professionalTypeLabel}
                    </span>
                    {professional.startingBudgetLabel && (
                      <span className="text-xs text-[var(--muted)] font-mono">
                        From {professional.startingBudgetLabel}
                      </span>
                    )}
                  </div>

                  {/* Single H1 */}
                  <h1 className="font-serif text-2xl sm:text-3xl lg:text-4xl font-semibold tracking-tight text-[var(--foreground)]">
                    {professional.studioName}
                  </h1>

                  <p className="text-xs sm:text-sm text-[var(--muted)] mt-1 flex items-center gap-1.5 flex-wrap">
                    <span>Principal: <strong className="text-[var(--foreground)]">{professional.name}</strong></span>
                    <span>•</span>
                    <span className="flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5 text-[var(--brand)]" />
                      {professional.locationName}
                    </span>
                  </p>
                </div>
              </div>

              {/* Service Areas */}
              <div className="text-xs text-[var(--muted)] flex flex-col sm:items-end gap-1.5 pt-4 md:pt-0 border-t md:border-t-0 border-[var(--border)]">
                <span className="font-semibold uppercase tracking-wider text-[10px] text-[var(--brand)]">
                  Serving Cities
                </span>
                <div className="flex items-center gap-1.5 flex-wrap">
                  {professional.serviceAreas.map((area, idx) => (
                    <span
                      key={idx}
                      className="px-2 py-0.5 rounded bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--foreground)] font-medium text-[11px]"
                    >
                      {area}
                    </span>
                  ))}
                </div>
              </div>
            </div>

            {/* Bio */}
            <div className="mt-6 pt-6 border-t border-[var(--border)]">
              <h2 className="font-serif text-base font-semibold text-[var(--foreground)] mb-2">
                Studio Philosophy & Overview
              </h2>
              <p className="text-xs sm:text-sm text-[var(--muted)] leading-relaxed max-w-4xl">
                {professional.bio}
              </p>
            </div>

            {/* Specialties & Services */}
            <div className="mt-6 pt-6 border-t border-[var(--border)] grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <h3 className="text-xs font-semibold uppercase tracking-wider text-[var(--brand)] mb-2">
                  Specialties & Craft
                </h3>
                <div className="flex flex-wrap gap-1.5">
                  {professional.specialties.map((spec, idx) => (
                    <span
                      key={idx}
                      className="px-2.5 py-1 rounded-xl text-xs font-medium bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--foreground)]"
                    >
                      {spec}
                    </span>
                  ))}
                </div>
              </div>

              <div>
                <h3 className="text-xs font-semibold uppercase tracking-wider text-[var(--brand)] mb-2">
                  Trade Services Offered
                </h3>
                <div className="flex flex-wrap gap-1.5">
                  {professional.services.map((svc, idx) => (
                    <span
                      key={idx}
                      className="px-2.5 py-1 rounded-xl text-xs font-medium bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--foreground)]"
                    >
                      {svc}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Published Projects Section */}
          <div className="space-y-6">
            <div className="flex items-center justify-between pb-3 border-b border-[var(--border)]">
              <div>
                <h2 className="font-serif text-xl sm:text-2xl font-semibold text-[var(--foreground)]">
                  Published Projects
                </h2>
                <p className="text-xs text-[var(--muted)]">
                  Explore turnkey spatial work designed and delivered by {professional.studioName}.
                </p>
              </div>
              <span className="text-xs font-semibold text-[var(--muted)]">
                {projects.length} {projects.length === 1 ? 'Project' : 'Projects'}
              </span>
            </div>

            {projects.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {projects.map((project) => (
                  <ProjectCard key={project.id} project={project} />
                ))}
              </div>
            ) : (
              <div className="p-8 text-center rounded-2xl border border-dashed border-[var(--border)] bg-[var(--surface)] text-xs text-[var(--muted)]">
                No public projects currently listed for this studio.
              </div>
            )}
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
}
