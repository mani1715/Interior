import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import Link from 'next/link';
import Image from 'next/image';
import { MapPin, Calendar, Clock, Sparkles, Building2, Layers, CheckCircle2, ArrowRight } from 'lucide-react';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { Footer } from '@/components/home/Footer';
import { Breadcrumb } from '@/components/ui/Breadcrumb';
import { ProjectDetailActions } from '@/components/discovery/ProjectDetailActions';
import { ProjectCard } from '@/components/discovery/ProjectCard';
import { BeforeAfterSlider } from '@/components/media/BeforeAfterSlider';
import { BeforeAiReality } from '@/components/media/BeforeAiReality';
import { AIConceptBadge } from '@/components/ui/Badge';
import { getProjectBySlug, getRelatedProjects, getProfessionalBySlug } from '@/lib/discovery/queries';

interface PageProps {
  params: Promise<{
    projectSlug: string;
  }>;
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { projectSlug } = await params;
  const project = getProjectBySlug(projectSlug);

  if (!project) {
    return {
      title: 'Project Not Found | Elégance',
    };
  }

  const title = `${project.title} in ${project.locationName} | ${project.studioName}`;
  const description = project.description;

  return {
    title,
    description,
    alternates: {
      canonical: `/projects/${project.slug}`,
    },
    openGraph: {
      title,
      description,
      url: `/projects/${project.slug}`,
      siteName: 'Elégance Interior Platform',
      locale: 'en_IN',
      type: 'article',
    },
  };
}

export default async function ProjectDetailPage({ params }: PageProps) {
  const { projectSlug } = await params;
  const project = getProjectBySlug(projectSlug);

  if (!project) {
    notFound();
  }

  const professional = getProfessionalBySlug(project.professionalSlug);
  const relatedProjects = getRelatedProjects(project, 3);

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
        name: 'Projects',
        item: '/projects',
      },
      {
        '@type': 'ListItem',
        position: 3,
        name: project.title,
        item: `/projects/${project.slug}`,
      },
    ],
  };

  return (
    <div className="min-h-screen bg-[var(--background)] text-[var(--foreground)] flex flex-col selection:bg-[var(--brand)] selection:text-[var(--charcoal)] pb-16 sm:pb-0">
      {/* Truthful Structured Data */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbJsonLd) }}
      />

      <PublicHeader currentPath="/projects" />

      <main id="main-content" className="flex-1 w-full py-6 sm:py-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8 sm:space-y-12">
          {/* Breadcrumb Navigation */}
          <Breadcrumb
            items={[
              { label: 'Home', href: '/' },
              { label: 'Projects', href: '/projects' },
              { label: project.categoryName, href: `/categories/${project.category}` },
              { label: project.title, href: `/projects/${project.slug}`, current: true },
            ]}
          />

          {/* Project Header (Single H1) */}
          <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-6 pb-6 border-b border-[var(--border)]">
            <div className="space-y-3 max-w-3xl">
              <div className="flex items-center gap-2 flex-wrap">
                <Link
                  href={`/categories/${project.category}`}
                  className="px-2.5 py-1 rounded-md text-[11px] font-semibold uppercase tracking-wider bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--foreground)] hover:border-[var(--brand)] transition-colors"
                >
                  {project.categoryName}
                </Link>
                <span className="text-xs text-[var(--muted)]">•</span>
                <span className="text-xs font-medium text-[var(--muted)]">{project.styleName} Style</span>
                <span className="text-xs text-[var(--muted)]">•</span>
                <span className="text-xs font-medium text-[var(--muted)]">{project.propertyTypeName}</span>
              </div>

              <h1 className="font-serif text-2xl sm:text-4xl md:text-5xl font-semibold tracking-tight text-[var(--foreground)] leading-tight">
                {project.title}
              </h1>

              {/* Location & Studio Attribution Bar */}
              <div className="flex items-center gap-4 text-xs sm:text-sm text-[var(--muted)] flex-wrap pt-1">
                <div className="flex items-center gap-1.5">
                  <MapPin className="w-4 h-4 text-[var(--brand)]" />
                  <Link
                    href={`/locations/${project.location}`}
                    className="hover:text-[var(--brand)] transition-colors font-medium text-[var(--foreground)]"
                  >
                    {project.locationName}
                  </Link>
                </div>

                <span>•</span>

                <div>
                  Designed & Executed by{' '}
                  <Link
                    href={`/professionals/${project.professionalSlug}`}
                    className="font-semibold text-[var(--brand)] hover:underline"
                  >
                    {project.studioName}
                  </Link>
                </div>

                {project.budgetLabel && (
                  <>
                    <span>•</span>
                    <span className="font-mono text-[var(--foreground)] font-medium">
                      Budget: {project.budgetLabel}
                    </span>
                  </>
                )}
              </div>
            </div>

            {/* Interactive Top Actions: "I Want Something Similar" & Share */}
            <ProjectDetailActions project={project} />
          </div>

          {/* Primary Hero Architectural Showcase Image */}
          <div className="relative aspect-[16/9] sm:aspect-[21/9] w-full rounded-2xl overflow-hidden border border-[var(--border)] bg-[var(--surface-alt)] shadow-lg">
            <Image
              src={project.coverImage}
              alt={project.title}
              fill
              priority
              sizes="100vw"
              className="object-cover"
            />
          </div>

          {/* 2-Column Overview & Technical Specs */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12">
            {/* Story & Scope (Left 7 cols) */}
            <div className="lg:col-span-7 space-y-6">
              <div>
                <h2 className="font-serif text-xl sm:text-2xl font-semibold text-[var(--foreground)] mb-3">
                  Project Story & Overview
                </h2>
                <p className="text-sm sm:text-base text-[var(--muted)] leading-relaxed">
                  {project.description}
                </p>
              </div>

              <div>
                <h3 className="font-serif text-base sm:text-lg font-semibold text-[var(--foreground)] mb-2">
                  Scope of Execution
                </h3>
                <p className="text-xs sm:text-sm text-[var(--muted)] leading-relaxed">
                  {project.scope}
                </p>
              </div>

              {/* Materials Used */}
              <div>
                <h3 className="font-serif text-base sm:text-lg font-semibold text-[var(--foreground)] mb-3">
                  Key Architectural Materials
                </h3>
                <div className="flex flex-wrap gap-2">
                  {project.materials.map((mat, idx) => (
                    <span
                      key={idx}
                      className="px-3 py-1.5 rounded-xl text-xs font-medium bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--foreground)]"
                    >
                      {mat}
                    </span>
                  ))}
                </div>
              </div>
            </div>

            {/* Technical Specifications Card (Right 5 cols) */}
            <div className="lg:col-span-5">
              <div className="p-6 rounded-2xl border border-[var(--border)] bg-[var(--surface)] shadow-sm space-y-4">
                <h3 className="font-serif text-base font-semibold uppercase tracking-wider text-[var(--foreground)] pb-3 border-b border-[var(--border)]">
                  Project Specifications
                </h3>

                <dl className="space-y-3 text-xs sm:text-sm">
                  <div className="flex items-center justify-between">
                    <dt className="text-[var(--muted)]">Location</dt>
                    <dd className="font-medium text-[var(--foreground)]">{project.locationName}</dd>
                  </div>
                  <div className="flex items-center justify-between">
                    <dt className="text-[var(--muted)]">Property Type</dt>
                    <dd className="font-medium text-[var(--foreground)]">{project.propertyTypeName}</dd>
                  </div>
                  <div className="flex items-center justify-between">
                    <dt className="text-[var(--muted)]">Design Style</dt>
                    <dd className="font-medium text-[var(--foreground)]">{project.styleName}</dd>
                  </div>
                  <div className="flex items-center justify-between">
                    <dt className="text-[var(--muted)]">Completion Year</dt>
                    <dd className="font-medium text-[var(--foreground)]">{project.completionYear}</dd>
                  </div>
                  <div className="flex items-center justify-between">
                    <dt className="text-[var(--muted)]">Execution Duration</dt>
                    <dd className="font-medium text-[var(--foreground)]">{project.duration}</dd>
                  </div>
                  <div className="flex items-center justify-between">
                    <dt className="text-[var(--muted)]">Budget Range</dt>
                    <dd className="font-medium font-mono text-[var(--brand)]">{project.budgetLabel}</dd>
                  </div>
                </dl>

                {/* Professional attribution mini badge */}
                <div className="pt-4 border-t border-[var(--border)] flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-[var(--surface-alt)] border border-[var(--border)] text-[var(--brand)] flex items-center justify-center font-serif font-bold text-base flex-shrink-0">
                    {project.studioName.charAt(0)}
                  </div>
                  <div className="truncate">
                    <p className="text-xs font-semibold text-[var(--foreground)] truncate">{project.studioName}</p>
                    <Link
                      href={`/professionals/${project.professionalSlug}`}
                      className="text-[11px] text-[var(--brand)] hover:underline"
                    >
                      View Studio Portfolio →
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Architectural Photography Gallery */}
          {project.gallery.length > 0 && (
            <div className="space-y-4 pt-6 border-t border-[var(--border)]">
              <div>
                <h2 className="font-serif text-xl sm:text-2xl font-semibold text-[var(--foreground)] mb-1">
                  Project Gallery
                </h2>
                <p className="text-xs sm:text-sm text-[var(--muted)]">
                  High-resolution details, elevations, and ambient evening photography.
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {project.gallery.map((item, idx) => (
                  <figure
                    key={idx}
                    className="flex flex-col rounded-2xl overflow-hidden border border-[var(--border)] bg-[var(--surface)] shadow-sm"
                  >
                    <div className="relative aspect-[16/10] w-full overflow-hidden bg-[var(--surface-alt)]">
                      <Image
                        src={item.url}
                        alt={item.alt}
                        fill
                        sizes="(max-width: 768px) 100vw, 50vw"
                        className="object-cover"
                      />
                    </div>
                    {item.caption && (
                      <figcaption className="p-3 text-xs text-[var(--muted)] bg-[var(--surface-alt)] border-t border-[var(--border)]">
                        {item.caption}
                      </figcaption>
                    )}
                  </figure>
                ))}
              </div>
            </div>
          )}

          {/* Conditional Before/After Slider or Before->AI->Reality */}
          {project.beforeImage && project.realityImage && !project.aiImage && (
            <div className="space-y-4 pt-6 border-t border-[var(--border)]">
              <div>
                <h2 className="font-serif text-xl sm:text-2xl font-semibold text-[var(--foreground)] mb-1">
                  Before & After Transformation
                </h2>
                <p className="text-xs sm:text-sm text-[var(--muted)]">
                  Drag the slider to compare raw site masonry with the handcrafted completed interior.
                </p>
              </div>
              <div className="max-w-4xl mx-auto">
                <BeforeAfterSlider
                  beforeImage={project.beforeImage}
                  afterImage={project.realityImage}
                  beforeLabel="Site Condition"
                  afterLabel="Built Reality"
                />
              </div>
            </div>
          )}

          {/* 3-Stage Narrative: Site -> AI Concept -> Reality (When project has AI study data) */}
          {project.beforeImage && project.aiImage && project.realityImage && (
            <div className="space-y-4 pt-6 border-t border-[var(--border)]">
              <div>
                <h2 className="font-serif text-xl sm:text-2xl font-semibold text-[var(--foreground)] mb-1">
                  Architectural Journey: Site → AI Concept → Built Reality
                </h2>
                <p className="text-xs sm:text-sm text-[var(--muted)]">
                  How client alignment began from a raw photograph to an AI concept study and finished execution.
                </p>
              </div>

              <BeforeAiReality
                title=""
                subtitle=""
                beforeImage={project.beforeImage}
                aiImage={project.aiImage}
                realityImage={project.realityImage}
                beforeDescription="Raw masonry with conduit drops and laser datum points."
                aiDescription="Client-aligned AI concept visualization exploring materials and warm cove illumination."
                realityDescription="Final completed living space matching client-approved specifications."
              />

              {/* Mandatory AI Concept Disclaimer */}
              <div className="p-3.5 rounded-xl border border-[var(--border)] bg-[var(--surface)] flex items-center gap-3 text-xs text-[var(--muted)]">
                <AIConceptBadge />
                <span>
                  AI visualizations are conceptual. Colours, materials, dimensions and construction details may differ from the final result. Confirm final specifications and buildability before execution.
                </span>
              </div>
            </div>
          )}

          {/* Professional Studio Attribution Section */}
          {professional && (
            <div className="p-6 sm:p-8 rounded-2xl border border-[var(--border)] bg-[var(--surface-alt)] shadow-sm space-y-4">
              <span className="text-[11px] font-semibold uppercase tracking-widest text-[var(--brand)] block">
                About the Studio
              </span>

              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <div
                    className="w-14 h-14 rounded-2xl flex items-center justify-center font-serif font-bold text-2xl text-[var(--charcoal)] shadow-sm flex-shrink-0"
                    style={{ backgroundColor: professional.avatarColor }}
                  >
                    {professional.avatarChar}
                  </div>
                  <div>
                    <h3 className="font-serif text-xl font-bold text-[var(--foreground)]">
                      {professional.studioName}
                    </h3>
                    <p className="text-xs text-[var(--muted)]">
                      {professional.professionalTypeLabel} • {professional.locationName}
                    </p>
                  </div>
                </div>

                <Link
                  href={`/professionals/${professional.slug}`}
                  className="inline-flex items-center gap-1.5 text-xs font-semibold text-[var(--brand)] hover:underline"
                >
                  <span>Explore All {professional.studioName} Projects</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>

              <p className="text-xs sm:text-sm text-[var(--muted)] leading-relaxed max-w-3xl">
                {professional.bio}
              </p>
            </div>
          )}

          {/* Related Projects Section */}
          {relatedProjects.length > 0 && (
            <div className="space-y-6 pt-6 border-t border-[var(--border)]">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="font-serif text-xl sm:text-2xl font-semibold text-[var(--foreground)]">
                    Similar Interior Projects
                  </h2>
                  <p className="text-xs text-[var(--muted)]">
                    Discover projects sharing similar design aesthetics or location context.
                  </p>
                </div>
                <Link
                  href={`/categories/${project.category}`}
                  className="text-xs font-semibold text-[var(--brand)] hover:underline"
                >
                  View All {project.categoryName} →
                </Link>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {relatedProjects.map((rel) => (
                  <ProjectCard key={rel.id} project={rel} />
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
