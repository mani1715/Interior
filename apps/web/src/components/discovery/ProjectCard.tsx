'use client';
import Link from 'next/link';
import { ArrowUpRight } from 'lucide-react';
import { Project } from '@/lib/discovery/types';
import { SaveToCollectionButton } from '@/components/collections/SaveToCollectionButton';
import styles from './EditorialDiscovery.module.css';
export interface ProjectCardProps { project: Project; priority?: boolean }
export function ProjectCard({ project, priority = false }: ProjectCardProps) {
  const href = `/projects/${project.slug}?studio=${encodeURIComponent(project.professionalSlug)}`;
  return <article className={styles.project}>
    <div className={styles.image}><Link href={href} tabIndex={-1} aria-hidden="true">{project.coverImage ? <img src={project.coverImage} alt="" width={1200} height={900} loading={priority ? 'eager' : 'lazy'} /> : <span>Image not supplied</span>}</Link>
      {(project.isAiConceptCover || project.aiImage) && <span aria-label="AI Concept Visualization" className={styles.ai}>✦ AI Concept Visualization</span>}
      <div className={styles.save}><SaveToCollectionButton projectId={project.id} className="min-h-[44px] min-w-[44px]" /></div>
    </div>
    <div className={styles.meta}>{project.categoryName}{project.locationName && <span>{project.locationName}</span>}</div>
    <h3><Link href={href}>{project.title}</Link></h3>
    <div className={styles.attribution}><Link href={`/professionals/${project.professionalSlug}`}>{project.studioName}</Link><Link href={href} aria-label={`View ${project.title}`}><ArrowUpRight size={20}/></Link></div>
  </article>;
}
