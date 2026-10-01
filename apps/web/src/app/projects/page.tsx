import type { Metadata } from 'next';
import { Suspense } from 'react';
import Image from 'next/image';
import styles from './DiscoveryPage.module.css';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { Footer } from '@/components/home/Footer';
import { Breadcrumb } from '@/components/ui/Breadcrumb';
import { ProjectsDiscoveryClient } from '@/components/discovery/ProjectsDiscoveryClient';
import { FilterParams, Project } from '@/lib/discovery/types';
import { CATEGORIES, LOCATIONS } from '@/lib/discovery/demo-data';
import { fetchDiscoveryProjects, mapDiscoveryCardToProject } from '@/lib/discovery/api';

interface PageProps {
  searchParams: Promise<{
    q?: string;
    category?: string;
    location?: string;
    style?: string;
    budget?: string;
    propertyType?: string;
    professionalType?: string;
    sort?: 'recommended' | 'newest' | 'most_viewed';
  }>;
}

export async function generateMetadata({ searchParams }: PageProps): Promise<Metadata> {
  const params = await searchParams;
  const category = CATEGORIES.find((c) => c.slug === params.category);
  const location = LOCATIONS.find((l) => l.slug === params.location);

  let title = 'Discover Interior Projects | Elégance';
  let description =
    'Explore curated interior design and architecture projects across India. Find real TV units, modular kitchens, wardrobes, and living rooms built by regional studios.';

  if (category && location) {
    title = `${category.name} in ${location.name} | Interior Projects | Elégance`;
    description = `Explore real ${category.name.toLowerCase()} designed and executed by interior studios and architects in ${location.name}, ${location.state}.`;
  } else if (category) {
    title = `${category.name} Design Projects | Elégance`;
    description = `Explore bespoke ${category.name.toLowerCase()} projects, material palettes, and architectural craftsmanship across India.`;
  } else if (location) {
    title = `Interior Design Projects in ${location.name} | Elégance`;
    description = `Discover verified interior and architecture projects completed across ${location.name}, ${location.state}.`;
  }

  const isSearchQuery = Boolean(
    params.q || params.sort || params.propertyType || params.budget || params.style
  );

  return {
    title,
    description,
    robots: isSearchQuery ? { index: false, follow: true } : { index: true, follow: true },
    alternates: {
      canonical: '/projects',
    },
    openGraph: {
      title,
      description,
      url: '/projects',
      siteName: 'Elégance Interior Platform',
      locale: 'en_IN',
      type: 'website',
    },
  };
}

export default async function ProjectsPage({ searchParams }: PageProps) {
  const resolvedParams = await searchParams;

  const initialFilters: FilterParams = {
    q: resolvedParams.q,
    category: resolvedParams.category,
    location: resolvedParams.location,
    style: resolvedParams.style,
    budget: resolvedParams.budget,
    propertyType: resolvedParams.propertyType,
    professionalType: resolvedParams.professionalType,
    sort: resolvedParams.sort || 'recommended',
  };

  let initialProjects: Project[] | undefined;
  let initialTotal: number | undefined;

  try {
    const res = await fetchDiscoveryProjects({
      q: resolvedParams.q,
      category: resolvedParams.category,
      city: resolvedParams.location,
      style: resolvedParams.style,
      sort: resolvedParams.sort,
      limit: 12,
      offset: 0,
    });
    if (res?.projects) {
      initialProjects = res.projects.map(mapDiscoveryCardToProject);
      initialTotal = res.totalProjects;
    }
  } catch {
    // Fallback gracefully if backend is offline during static build/test
  }

  return (
    <div className="public-editorial min-h-screen bg-[var(--background)] text-[var(--foreground)] flex flex-col selection:bg-[var(--brand)] selection:text-[var(--charcoal)]">
      <PublicHeader currentPath="/projects" />

      <main id="main-content" className={styles.page}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          {/* Breadcrumb Navigation */}
          <div className="mb-4 sm:mb-6">
            <Breadcrumb
              items={[
                { label: 'Home', href: '/' },
                { label: 'Projects', href: '/projects', current: true },
              ]}
            />
          </div>

          <section className={styles.hero} aria-labelledby="discovery-title">
            <div className={styles.intro}><span>THE PROJECT LIBRARY</span><h1 id="discovery-title">Good design.<br /><em>Find your starting point.</em></h1><p>Explore spaces, discover the details, and meet the professionals behind the work.</p><a href="#project-search">Explore the library <span aria-hidden="true">↘</span></a></div>
            <figure className={styles.image}><Image src="/images/approved/tv-unit.png" alt="Warm timber and stone living-room concept" fill sizes="(max-width: 767px) 100vw, 55vw" loading="eager" /><figcaption>SPACES TO INSPIRE · AI CONCEPT VISUALIZATION</figcaption></figure>
          </section>
          {/* Interactive Client Discovery Island with URL State */}
          <Suspense fallback={<div className="py-12 text-center text-xs text-[var(--muted)]">Loading projects...</div>}>
            <ProjectsDiscoveryClient
              initialFilters={initialFilters}
              initialProjects={initialProjects}
              initialTotal={initialTotal}
            />
          </Suspense>
        </div>
      </main>

      <Footer />
    </div>
  );
}

