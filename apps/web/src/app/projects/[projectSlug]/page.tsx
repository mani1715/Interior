import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import Link from 'next/link';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { Footer } from '@/components/home/Footer';
import { ProjectDetailActions } from '@/components/discovery/ProjectDetailActions';
import { ProjectSpaceShowcase } from '@/components/discovery/ProjectSpaceShowcase';
import { fetchDiscoveryProjects, mapDiscoveryCardToProject } from '@/lib/discovery/api';
import { fetchPublicProject } from '@/lib/seo/api';
import { buildProjectMetadata } from '@/lib/seo/metadata';
import { SafeJsonLd, buildProjectJsonLd, buildBreadcrumbJsonLd } from '@/lib/seo/structured-data';
import { PublicTelemetryTracker } from '@/components/analytics/PublicTelemetryTracker';
import { normalizeProjectSpaceShowcase } from '@/lib/projects/showcase-normalizer';
import styles from './ProjectStory.module.css';

interface PageProps {
  params: Promise<{ projectSlug: string }>;
  searchParams?: Promise<{ studio?: string }>;
}

async function loadProject(props: PageProps) {
  const { projectSlug } = await props.params;
  const query = await props.searchParams;
  let studioSlug = query?.studio;
  if (!studioSlug) {
    const result = await fetchDiscoveryProjects({
      q: projectSlug.replace(/-/g, ' '),
      limit: 100,
    }).catch(() => null);
    studioSlug = result?.projects.find((project) => project.slug === projectSlug)?.studioSlug;
  }
  return studioSlug ? fetchPublicProject(studioSlug, projectSlug) : null;
}

export async function generateMetadata(props: PageProps): Promise<Metadata> {
  const project = await loadProject(props);
  return project
    ? buildProjectMetadata(project)
    : { title: 'Project unavailable | Elégance', robots: { index: false, follow: false } };
}

export default async function ProjectDetailPage(props: PageProps) {
  const project = await loadProject(props);
  if (!project) notFound();

  const origin = process.env.NEXT_PUBLIC_APP_URL || 'https://interior.com';
  const showcaseData = normalizeProjectSpaceShowcase(project);
  const cover = showcaseData.project.coverPhoto;

  const actionProject = mapDiscoveryCardToProject({
    id: project.id,
    slug: project.slug,
    title: project.title,
    categoryCode: project.categoryCode,
    categoryName: project.categoryCode.replace(/_/g, ' '),
    styleCodes: project.styleCodes,
    styleNames: project.styleCodes.map((s) => s.replace(/_/g, ' ')),
    city: project.city || undefined,
    state: project.state || undefined,
    propertyType: project.propertyType || undefined,
    projectScope: project.projectScope || undefined,
    shortDescription: project.shortDescription || undefined,
    coverImageUrl: cover?.largeUrl || cover?.mediumUrl,
    isAiConceptCover: cover?.isAiConcept || false,
    studioId: project.studio.studioId,
    studioSlug: project.studio.slug,
    studioName: project.studio.name,
    professionalType: '',
    professionalTypeLabel: '',
    completionYear: project.completionYear,
  });

  return (
    <>
      <PublicHeader currentPath="/projects" />
      <main id="main-content" className={styles.story}>
        <PublicTelemetryTracker
          eventType="PUBLIC_PROJECT_VIEW"
          entityType="PROJECT"
          entitySlug={project.slug}
        />
        <SafeJsonLd data={buildProjectJsonLd(project, origin)} />
        <SafeJsonLd
          data={buildBreadcrumbJsonLd(
            [
              { name: 'Home', url: '/' },
              { name: 'Projects', url: '/projects' },
              { name: project.title, url: project.canonicalUrl },
            ],
            origin
          )}
        />
        <nav aria-label="Breadcrumb">
          <Link href="/">Home</Link>
          <span>/</span>
          <Link href="/projects">Projects</Link>
          <span>/</span>
          <span>{project.title}</span>
        </nav>
        <header>
          <span className={styles.kicker}>
            {project.categoryCode.replace(/_/g, ' ')}
            {project.city ? ' / ' + project.city : ''}
          </span>
          <h1>{project.title}</h1>
          <Link href={`/professionals/${project.studio.slug}`}>
            By {project.studio.name} ↗
          </Link>
        </header>

        {cover && (
          <figure className={styles.hero}>
            <img
              src={cover.largeUrl || cover.mediumUrl}
              alt={cover.altText || project.title}
              width={cover.width || 1600}
              height={cover.height || 1000}
              fetchPriority="high"
              style={{
                objectPosition: `${cover.focalX * 100}% ${cover.focalY * 100}%`,
              }}
            />
            {cover.isAiConcept && <figcaption>AI Concept Visualization</figcaption>}
          </figure>
        )}

        <div className={styles.introduction}>
          <div>
            <span className={styles.kicker}>THE PROJECT</span>
            <h2>A closer look.</h2>
            <p>
              {project.fullDescription ||
                project.shortDescription ||
                'The studio has not added a project story yet.'}
            </p>
          </div>
          <aside>
            <h2>Project Specifications</h2>
            <dl>
              {[
                ['Category', project.categoryCode.replace(/_/g, ' ')],
                ['Location', [project.city, project.state].filter(Boolean).join(', ')],
                ['Style', project.styleCodes.join(' / ').replace(/_/g, ' ')],
                ['Completed', project.completionYear?.toString()],
                ['Area', project.areaSqFt ? project.areaSqFt + ' sq ft' : null],
                ['Scope', project.projectScope],
              ]
                .filter(([, value]) => value)
                .map(([label, value]) => (
                  <div key={label}>
                    <dt>{label}</dt>
                    <dd>{value}</dd>
                  </div>
                ))}
            </dl>
          </aside>
        </div>

        {/* Dynamic Public Room Showcase & Full-Screen Viewer */}
        <ProjectSpaceShowcase data={showcaseData} actionProject={actionProject} />

        {/* Server-rendered fallback for non-JS / search engines */}
        <noscript>
          <div className={styles.gallery}>
            {showcaseData.allPhotos.filter((item) => item.id !== cover?.id).map((item) => (
              <figure key={item.id}>
                <img
                  src={item.largeUrl || item.mediumUrl}
                  alt={item.altText || project.title}
                  width={item.width || 1200}
                  height={item.height || 900}
                  loading="lazy"
                />
                {(item.caption || item.isAiConcept) && (
                  <figcaption>
                    {item.isAiConcept ? 'AI Concept Visualization — ' : ''}
                    {item.caption}
                  </figcaption>
                )}
              </figure>
            ))}
          </div>
        </noscript>

        <section className={styles.enquire}>
          <span className={styles.kicker}>START A CONVERSATION</span>
          <h2>See something that feels like you?</h2>
          <p>Discuss a similar brief with {project.studio.name}.</p>
          <ProjectDetailActions project={actionProject} />
        </section>
      </main>
      <Footer />
    </>
  );
}
