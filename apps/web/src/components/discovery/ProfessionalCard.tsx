import Link from 'next/link';
import { ArrowUpRight } from 'lucide-react';
import { Professional } from '@/lib/discovery/types';
import styles from './EditorialDiscovery.module.css';
export interface ProfessionalCardProps { professional: Professional }
export function ProfessionalCard({ professional }: ProfessionalCardProps) {
  return <article className={styles.professional}>
    <span className={styles.label}>{professional.professionalTypeLabel}</span>
    <h3><Link href={`/professionals/${professional.slug}`}>{professional.studioName}</Link></h3>
    {professional.locationName && <p className={styles.location}>{professional.locationName}</p>}
    {professional.bio && <p>{professional.bio}</p>}
    {!!professional.sampleProjectCoverUrls?.length && <div className={styles.samples}>{professional.sampleProjectCoverUrls.slice(0,3).map((src,i) => <img key={src} src={src} alt={`${professional.studioName} published work ${i+1}`} width={400} height={300} loading="lazy"/>)}</div>}
    <div className={styles.specialties}>{professional.specialties.map(s => <span key={s}>{s}</span>)}</div>
    <div className={styles.attribution}><span>{professional.projectCount ?? 0} Published Projects</span><Link href={`/professionals/${professional.slug}`}>View Studio Profile <ArrowUpRight size={16}/></Link></div>
  </article>;
}
