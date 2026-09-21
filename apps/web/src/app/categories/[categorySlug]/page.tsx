import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import Link from 'next/link';
import { ArrowRight, Sparkles } from 'lucide-react';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { Footer } from '@/components/home/Footer';
import { Breadcrumb } from '@/components/ui/Breadcrumb';
import { ProjectCard } from '@/components/discovery/ProjectCard';
import { getCategoryBySlug, getProjects } from '@/lib/discovery/queries';
import { SafeJsonLd } from '@/lib/seo/structured-data';

interface PageProps {
  params: Promise<{
    categorySlug: string;
  }>;
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { categorySlug } = await params;
  const category = getCategoryBySlug(categorySlug);

  if (!category) {
    return {
      title: 'Category Not Found | Elégance',
    };
  }

  return {
    title: `${category.name} Design Projects & Ideas | Elégance`,
    description: category.heroDescription,
    alternates: {
      canonical: `/categories/${category.slug}`,
    },
    openGraph: {
      title: `${category.name} Design Projects & Ideas | Elégance`,
      description: category.heroDescription,
      url: `/categories/${category.slug}`,
      siteName: 'Elégance Interior Platform',
      locale: 'en_IN',
      type: 'website',
    },
  };
}

export default async function CategoryLandingPage({ params }: PageProps) {
  const { categorySlug } = await params;
  const category = getCategoryBySlug(categorySlug);

  if (!category) {
    notFound();
  }

  const { projects, total } = getProjects({ category: category.slug });

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
        name: 'Categories',
        item: '/projects',
      },
      {
        '@type': 'ListItem',
        position: 3,
        name: category.name,
        item: `/categories/${category.slug}`,
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
              { label: category.name, href: `/categories/${category.slug}`, current: true },
            ]}
          />

          {/* Category Editorial Hero */}
          <div className="p-6 sm:p-10 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-sm space-y-4">
            <span className="text-[11px] sm:text-xs font-semibold uppercase tracking-widest text-[var(--brand)] block">
              Space & Category Architecture
            </span>

            {/* Single H1 */}
            <h1 className="font-serif text-2xl sm:text-4xl lg:text-5xl font-semibold tracking-tight text-[var(--foreground)]">
              {category.name} Design Projects
            </h1>

            <p className="text-xs sm:text-base text-[var(--muted)] max-w-3xl leading-relaxed">
              {category.heroDescription}
            </p>

            {/* Popular Styles in Category */}
            {category.popularStyles.length > 0 && (
              <div className="pt-3 border-t border-[var(--border)] flex items-center gap-2 flex-wrap text-xs text-[var(--muted)]">
                <span className="font-medium text-[var(--foreground)]">Popular Design Styles:</span>
                {category.popularStyles.map((st, idx) => (
                  <span
                    key={idx}
                    className="px-2.5 py-1 rounded-full bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--foreground)] font-medium text-[11px]"
                  >
                    {st}
                  </span>
                ))}
              </div>
            )}
          </div>

          {/* Projects in Category */}
          <div className="space-y-6">
            <div className="flex items-center justify-between pb-3 border-b border-[var(--border)]">
              <div>
                <h2 className="font-serif text-xl sm:text-2xl font-semibold text-[var(--foreground)]">
                  {category.name} Inspiration
                </h2>
                <p className="text-xs text-[var(--muted)]">
                  Authentic work executed by regional interior studios and cabinetry artisans.
                </p>
              </div>

              <Link
                href={`/projects?category=${category.slug}`}
                className="text-xs font-semibold text-[var(--brand)] hover:underline flex items-center gap-1"
              >
                <span>Filter All {total}</span>
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
                No projects currently cataloged in this category.
              </div>
            )}
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
}
