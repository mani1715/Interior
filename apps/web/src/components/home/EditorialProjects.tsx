'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { ArrowUpRight } from 'lucide-react';
import { fetchDiscoveryProjects, type DiscoveryProjectCard } from '@/lib/discovery/api';
import styles from './EditorialHome.module.css';
export function EditorialProjects() {
  const [projects, setProjects] = useState<DiscoveryProjectCard[]>([]);
  const [status, setStatus] = useState<'loading'|'ready'|'error'>('loading');
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let cancelled = false;
    fetchDiscoveryProjects({ limit: 3 }).then(response => {
      if (!cancelled) { setProjects(response.projects || []); setStatus('ready'); }
    }).catch(() => { if (!cancelled) setStatus('error'); });
    return () => { cancelled = true; };
  }, [retry]);
  return <section className={styles.section} aria-labelledby="project-discovery-title">
    <div className={styles.headingRow}><div><span className={styles.kicker}>02 / FROM THE COMMUNITY</span><h2 id="project-discovery-title">Good design.<br /><em>Real places.</em></h2></div><div><p>Explore public projects and meet the professionals behind the details.</p><Link className={styles.textLink} href="/projects">Explore all projects <ArrowUpRight size={18} /></Link></div></div>
    {status === 'loading' ? <div className={styles.empty} role="status">Finding public projects…</div> : status === 'error' ? <div className={styles.empty} role="status"><h3>Projects are taking a moment.</h3><p>We couldn’t load public projects. Please try again.</p><button onClick={() => { setStatus('loading'); setRetry(retry + 1); }}>Try again <ArrowUpRight size={16} /></button></div> : projects.length === 0 ? <div className={styles.empty}><h3>A space for work worth discovering.</h3><p>Published projects will appear here as professionals share their work.</p><Link href="/professionals" className={styles.textLink}>Explore professionals <ArrowUpRight size={18} /></Link></div> : <div className={styles.projectGrid}>{projects.map((project, index) => <article key={project.id}><Link href={`/projects/${project.slug}?studio=${encodeURIComponent(project.studioSlug)}`} data-motion-image={index % 2 === 0 ? "right" : "left"} className={styles.projectImage}>{project.coverImageUrl ? <img src={project.coverImageUrl} alt={project.title} loading="lazy" width={1200} height={900} /> : <span>Project image unavailable</span>}{project.isAiConceptCover && <span className={styles.aiBadge}>AI Concept Visualization</span>}</Link><span className={styles.kicker}>{project.categoryName}{project.city ? ` / ${project.city}` : ''}</span><h3><Link href={`/projects/${project.slug}?studio=${encodeURIComponent(project.studioSlug)}`}>{project.title}</Link></h3><Link className={styles.attribution} href={`/professionals/${project.studioSlug}`}>{project.studioName} <ArrowUpRight size={14} /></Link></article>)}</div>}
  </section>;
}


