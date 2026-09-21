import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import Link from 'next/link';
import { MapPin, Phone, Mail, ArrowRight } from 'lucide-react';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { Footer } from '@/components/home/Footer';
import { Breadcrumb } from '@/components/ui/Breadcrumb';
import { ProjectCard } from '@/components/discovery/ProjectCard';
import { getProfessionalBySlug, getProjectsByProfessional } from '@/lib/discovery/queries';
import { fetchPublicStudio } from '@/lib/seo/api';
import { buildStudioMetadata } from '@/lib/seo/metadata';
import { buildBreadcrumbJsonLd, buildStudioJsonLd, SafeJsonLd } from '@/lib/seo/structured-data';
import { TEMPLATE_REGISTRY } from '@/lib/portfolio/template-registry';
import { PortfolioTemplateKey } from '@/lib/portfolio/types';
import { normalizePortfolioProps } from '@/lib/portfolio/normalize-props';

interface PageProps {
  params: Promise<{
    professionalSlug: string;
  }>;
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { professionalSlug } = await params;
  const liveStudio = await fetchPublicStudio(professionalSlug);

  if (liveStudio) {
    return buildStudioMetadata(liveStudio);
  }

  const professional = getProfessionalBySlug(professionalSlug);
  if (!professional) {
    return {
      title: 'Professional Not Found | Interior',
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
      siteName: 'Interior Platform',
      locale: 'en_IN',
      type: 'profile',
    },
  };
}

export default async function ProfessionalDetailPage({ params }: PageProps) {
  const { professionalSlug } = await params;
  const baseUrl = process.env.NEXT_PUBLIC_APP_URL || 'https://interior.com';

  const liveStudio = await fetchPublicStudio(professionalSlug);

  if (liveStudio) {
    const breadcrumbData = buildBreadcrumbJsonLd(
      [
        { name: 'Home', url: '/' },
        { name: 'Professionals', url: '/professionals' },
        { name: liveStudio.name, url: `/professionals/${liveStudio.slug}` },
      ],
      baseUrl
    );
    const studioJsonLd = buildStudioJsonLd(liveStudio, baseUrl);

    // If portfolio template is configured and ready, render template
    if (liveStudio.portfolio) {
      const templateKey = (liveStudio.portfolio.templateKey as PortfolioTemplateKey) || 'BASIC';
      const TemplateComp = TEMPLATE_REGISTRY[templateKey]?.component || TEMPLATE_REGISTRY.BASIC.component;

      const visibleSections = liveStudio.portfolio.sections.map((s) => {
        let parsed = {};
        try {
          parsed = s.content ? JSON.parse(s.content) : {};
        } catch {
          parsed = {};
        }
        return {
          sectionId: `sec-${s.sortOrder}`,
          sectionType: s.sectionType as any,
          displayOrder: s.sortOrder,
          isVisible: true,
          content: parsed,
        };
      });

      const portfolioProps = normalizePortfolioProps({
        portfolioId: liveStudio.portfolio.templateKey || 'portfolio-main',
        studioId: liveStudio.id,
        studioName: liveStudio.name,
        studioSlug: liveStudio.slug,
        professionalType: liveStudio.professionalType || undefined,
        professionalTitle: liveStudio.professionalTitle || undefined,
        studioCity: liveStudio.city || undefined,
        studioState: liveStudio.state || undefined,
        templateKey,
        headline: liveStudio.portfolio.headline,
        subheadline: liveStudio.portfolio.subheadline || liveStudio.tagline,
        bio: liveStudio.portfolio.bio || liveStudio.tagline,
        designPhilosophy: liveStudio.portfolio.designPhilosophy,
        yearsOfExperience: liveStudio.portfolio.yearsOfExperience,
        primaryColor: liveStudio.portfolio.primaryColor || undefined,
        secondaryColor: liveStudio.portfolio.secondaryColor || undefined,
        accentColor: liveStudio.portfolio.accentColor || undefined,
        fontPairing: (liveStudio.portfolio.fontPairing as any) || undefined,
        publicContacts: liveStudio.contacts.map((c) => ({
          channelType: c.channelType as any,
          contactValue: c.contactValue,
          isPublic: true,
          displayOrder: c.sortOrder,
        })),
        canonicalServices: liveStudio.services.map((s) => ({ serviceCode: s, displayName: s })),
        canonicalSpecialties: liveStudio.specialties.map((s) => ({ specialtyCode: s, displayName: s })),
        canonicalServiceAreas: liveStudio.serviceAreas.map((a) => ({ city: a, state: liveStudio.state || '' })),
        visibleSections,
        portfolioProjects: liveStudio.projects.map((p) => ({
          projectId: p.id,
          title: p.title,
          slug: p.slug,
          categoryCode: p.categoryCode as any,
          categoryDisplayName: p.categoryCode,
          city: p.city || liveStudio.city,
          completionYear: p.completionYear,
          coverImageUrl: p.coverImageUrl,
          featured: false,
          displayOrder: 0,
        })),
      } as any);

      return (
        <div>
          <SafeJsonLd data={breadcrumbData} />
          <SafeJsonLd data={studioJsonLd} />
          <TemplateComp {...portfolioProps} />
        </div>
      );
    }

    // Studio Profile Layout without portfolio
    return (
      <div className="min-h-screen bg-[#FAF8F5] text-[#1F1F1F] flex flex-col selection:bg-[#B88A5A]/20 selection:text-[#1F1F1F]">
        <SafeJsonLd data={breadcrumbData} />
        <SafeJsonLd data={studioJsonLd} />
        <PublicHeader currentPath="/professionals" />

        <main id="main-content" className="flex-1 w-full py-6 sm:py-10">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8 sm:space-y-12">
            <Breadcrumb
              items={[
                { label: 'Home', href: '/' },
                { label: 'Professionals', href: '/professionals' },
                { label: liveStudio.name, href: `/professionals/${liveStudio.slug}`, current: true },
              ]}
            />

            {/* Profile Overview Card */}
            <div className="p-6 sm:p-8 rounded-2xl border border-sand-200 bg-white shadow-sm">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
                <div>
                  <div className="flex items-center gap-2 flex-wrap mb-1">
                    <span className="px-2.5 py-0.5 rounded text-[11px] font-semibold bg-sand-100 border border-sand-200 text-charcoal-800">
                      {liveStudio.professionalTitle || liveStudio.professionalType || 'Interior Studio'}
                    </span>
                    {liveStudio.city && (
                      <span className="text-xs text-charcoal-500 flex items-center gap-1">
                        <MapPin className="w-3.5 h-3.5 text-bronze-600" />
                        {liveStudio.city}, {liveStudio.state || liveStudio.country}
                      </span>
                    )}
                  </div>
                  <h1 className="font-serif text-2xl sm:text-4xl font-semibold text-charcoal-900 tracking-tight">
                    {liveStudio.name}
                  </h1>
                  {liveStudio.tagline && (
                    <p className="text-xs sm:text-sm text-charcoal-600 mt-1 max-w-2xl">
                      {liveStudio.tagline}
                    </p>
                  )}
                </div>

                {liveStudio.contacts.length > 0 && (
                  <div className="flex flex-wrap items-center gap-3 pt-4 md:pt-0 border-t md:border-t-0 border-sand-200">
                    {liveStudio.contacts.map((contact, idx) => (
                      <div
                        key={idx}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-sand-200 bg-sand-50 text-xs text-charcoal-700 font-mono"
                      >
                        {contact.channelType === 'PHONE' ? (
                          <Phone className="w-3.5 h-3.5 text-bronze-600" />
                        ) : (
                          <Mail className="w-3.5 h-3.5 text-bronze-600" />
                        )}
                        <span>{contact.contactValue}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Specialties & Services */}
              {(liveStudio.specialties.length > 0 || liveStudio.services.length > 0) && (
                <div className="mt-6 pt-6 border-t border-sand-200 grid grid-cols-1 md:grid-cols-2 gap-6">
                  {liveStudio.specialties.length > 0 && (
                    <div>
                      <h3 className="text-xs font-semibold uppercase tracking-wider text-bronze-700 mb-2">
                        Design Specialties
                      </h3>
                      <div className="flex flex-wrap gap-1.5">
                        {liveStudio.specialties.map((spec, idx) => (
                          <span
                            key={idx}
                            className="px-2.5 py-1 rounded-xl text-xs font-medium bg-sand-50 border border-sand-200 text-charcoal-800"
                          >
                            {spec}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {liveStudio.services.length > 0 && (
                    <div>
                      <h3 className="text-xs font-semibold uppercase tracking-wider text-bronze-700 mb-2">
                        Services Provided
                      </h3>
                      <div className="flex flex-wrap gap-1.5">
                        {liveStudio.services.map((svc, idx) => (
                          <span
                            key={idx}
                            className="px-2.5 py-1 rounded-xl text-xs font-medium bg-sand-50 border border-sand-200 text-charcoal-800"
                          >
                            {svc}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Public Projects Grid */}
            <div className="space-y-6">
              <div className="flex items-center justify-between pb-3 border-b border-sand-200">
                <div>
                  <h2 className="font-serif text-xl sm:text-2xl font-semibold text-charcoal-900">
                    Published Projects
                  </h2>
                  <p className="text-xs text-charcoal-500">
                    Explore turnkey spatial work designed and delivered by {liveStudio.name}.
                  </p>
                </div>
                <span className="text-xs font-semibold text-charcoal-500">
                  {liveStudio.projects.length} {liveStudio.projects.length === 1 ? 'Project' : 'Projects'}
                </span>
              </div>

              {liveStudio.projects.length > 0 ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                  {liveStudio.projects.map((p) => (
                    <Link
                      key={p.id}
                      href={`/projects/${p.slug}`}
                      className="group block rounded-2xl border border-sand-200 bg-white overflow-hidden shadow-sm hover:shadow-md transition-all"
                    >
                      <div className="aspect-[4/3] bg-sand-100 relative overflow-hidden">
                        {p.coverImageUrl ? (
                          <img
                            src={p.coverImageUrl}
                            alt={p.title}
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-charcoal-400 text-xs font-mono">
                            No Cover Image
                          </div>
                        )}
                      </div>
                      <div className="p-4">
                        <div className="text-[11px] font-semibold text-bronze-700 uppercase tracking-wider mb-1">
                          {p.categoryCode.replace(/_/g, ' ')}
                        </div>
                        <h3 className="font-serif text-base text-charcoal-900 font-semibold group-hover:text-bronze-700 transition-colors">
                          {p.title}
                        </h3>
                        <div className="text-xs text-charcoal-500 mt-1 flex items-center justify-between">
                          <span>{p.city || liveStudio.city}</span>
                          {p.completionYear && <span>{p.completionYear}</span>}
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              ) : (
                <div className="p-8 text-center rounded-2xl border border-dashed border-sand-200 bg-white text-xs text-charcoal-500">
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

  // Fallback to local discovery mock if not found in live API
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
