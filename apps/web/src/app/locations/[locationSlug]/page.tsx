import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import Link from 'next/link';
import { MapPin, ArrowRight, Building } from 'lucide-react';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { Footer } from '@/components/home/Footer';
import { Breadcrumb } from '@/components/ui/Breadcrumb';
import { ProjectCard } from '@/components/discovery/ProjectCard';
import { ProfessionalCard } from '@/components/discovery/ProfessionalCard';
import { getLocationBySlug, getProjects, getProfessionals } from '@/lib/discovery/queries';
import {
  fetchDiscoveryProjects,
  fetchDiscoveryProfessionals,
  mapDiscoveryCardToProject,
  mapDiscoveryCardToProfessional,
} from '@/lib/discovery/api';
import { SafeJsonLd } from '@/lib/seo/structured-data';

interface PageProps {
  params: Promise<{
    locationSlug: string;
  }>;
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { locationSlug } = await params;
  const location = getLocationBySlug(locationSlug);

  if (!location) {
    return {
      title: 'Location Not Found | Elégance',
    };
  }

  return {
    title: `Interior Design & Architecture in ${location.name} | Elégance`,
    description: `Discover interior designers, architecture studios, and completed turnkey projects in ${location.name}, ${location.state}.`,
    alternates: {
      canonical: `/locations/${location.slug}`,
    },
    openGraph: {
      title: `Interior Design & Architecture in ${location.name} | Elégance`,
      description: `Discover interior designers, architecture studios, and completed turnkey projects in ${location.name}, ${location.state}.`,
      url: `/locations/${location.slug}`,
      siteName: 'Elégance Interior Platform',
      locale: 'en_IN',
      type: 'website',
    },
  };
}

export default async function LocationLandingPage({ params }: PageProps) {
  const { locationSlug } = await params;
  const location = getLocationBySlug(locationSlug);

  if (!location) {
    notFound();
  }

  const fallbackProj = getProjects({ location: location.slug });
  let projects = fallbackProj.projects;
  let total = fallbackProj.total;
  let professionals = getProfessionals({ location: location.slug });

  try {
    const [projRes, profRes] = await Promise.all([
      fetchDiscoveryProjects({ city: location.name, limit: 12 }),
      fetchDiscoveryProfessionals({ city: location.name, limit: 12 }),
    ]);
    if (projRes?.projects && projRes.projects.length > 0) {
      projects = projRes.projects.map(mapDiscoveryCardToProject);
      total = projRes.totalProjects;
    }
    if (profRes?.professionals && profRes.professionals.length > 0) {
      professionals = profRes.professionals.map(mapDiscoveryCardToProfessional);
    }
  } catch {
    // Graceful fallback to static demo data
  }

  // Structured Data (BreadcrumbList)
  const breadcrumbJsonLd = {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: [
      {
        '@type': 'ListItem',
        position: 1,
        name: 'Home',
        item: '/',
      },
      {
        '@type': 'ListItem',
        position: 2,
        name: 'Locations',
        item: '/projects',
      },
      {
        '@type': 'ListItem',
        position: 3,
        name: location.name,
        item: `/locations/${location.slug}`,
      },
    ],
  };

  return (
    <div className="min-h-screen bg-[var(--background)] text-[var(--foreground)] flex flex-col selection:bg-[var(--brand)] selection:text-[var(--charcoal)]">
      <SafeJsonLd data={breadcrumbJsonLd} />

      <PublicHeader currentPath="/projects" />

      <main id="main-content" className="flex-1 w-full py-6 sm:py-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8 sm:space-y-12">
          {/* Breadcrumbs */}
          <Breadcrumb
            items={[
              { label: 'Home', href: '/' },
              { label: 'Projects', href: '/projects' },
              { label: `${location.name}, ${location.state}`, href: `/locations/${location.slug}`, current: true },
            ]}
          />

          {/* Location Editorial Hero */}
          <div className="p-6 sm:p-10 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-sm space-y-4">
            <div className="flex items-center gap-2">
              <MapPin className="w-4 h-4 text-[var(--brand)]" />
              <span className="text-[11px] sm:text-xs font-semibold uppercase tracking-widest text-[var(--brand)]">
                Regional Design Hub • {location.state}
              </span>
            </div>

            {/* Single H1 */}
            <h1 className="font-serif text-2xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-[var(--foreground)]">
              Interior Design & Architecture in {location.name}
            </h1>

            <p className="text-xs sm:text-base text-[var(--muted)] max-w-3xl leading-relaxed">
              {location.description}
            </p>

            {/* Popular Categories in this Location */}
            {location.popularCategories.length > 0 && (
              <div className="pt-3 border-t border-[var(--border)] flex items-center gap-2 flex-wrap text-xs text-[var(--muted)]">
                <span className="font-medium text-[var(--foreground)]">Popular Spaces in {location.name}:</span>
                {location.popularCategories.map((cat, idx) => (
                  <span
                    key={idx}
                    className="px-2.5 py-1 rounded-full bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--foreground)] font-medium text-[11px]"
                  >
                    {cat}
                  </span>
                ))}
              </div>
            )}
          </div>

          {/* Local Projects */}
          <div className="space-y-6">
            <div className="flex items-center justify-between pb-3 border-b border-[var(--border)]">
              <div>
                <h2 className="font-serif text-xl sm:text-2xl font-semibold text-[var(--foreground)]">
                  Projects in {location.name}
                </h2>
                <p className="text-xs text-[var(--muted)]">
                  Real residential and commercial interiors executed in {location.name}.
                </p>
              </div>

              <Link
                href={`/projects?location=${location.slug}`}
                className="text-xs font-semibold text-[var(--brand)] hover:underline flex items-center gap-1"
              >
                <span>Browse All ({total})</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>

            {projects.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {projects.map((project) => (
                  <ProjectCard key={project.id} project={project} />
                ))}
              </div>
            ) : (
              <div className="p-8 text-center rounded-2xl border border-dashed border-[var(--border)] bg-[var(--surface)] text-xs text-[var(--muted)]">
                No projects currently published in {location.name}.
              </div>
            )}
          </div>

          {/* Studios Serving This City */}
          {professionals.length > 0 && (
            <div className="space-y-6 pt-6 border-t border-[var(--border)]">
              <div>
                <h2 className="font-serif text-xl sm:text-2xl font-semibold text-[var(--foreground)]">
                  Studios Serving {location.name}
                </h2>
                <p className="text-xs text-[var(--muted)]">
                  Practitioners with active presence and execution teams in {location.name}.
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {professionals.map((prof) => (
                  <ProfessionalCard key={prof.id} professional={prof} />
                ))}
              </div>
            </div>
          )}
        </div>
      </main>

      <Footer />
    </div>
  );
}
