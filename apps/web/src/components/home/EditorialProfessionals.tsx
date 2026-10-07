'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { ArrowUpRight } from 'lucide-react';
import { fetchDiscoveryProfessionals, type DiscoveryProfessionalCard } from '@/lib/discovery/api';
import styles from './EditorialHome.module.css';

export function EditorialProfessionals() {
  const [professionals, setProfessionals] = useState<DiscoveryProfessionalCard[]>([]);

  useEffect(() => {
    let cancelled = false;
    fetchDiscoveryProfessionals({ limit: 3 })
      .then(res => {
        if (!cancelled) {
          setProfessionals(res.professionals || []);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setProfessionals([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <section id="professional-discovery" className={styles.professionalsSection} aria-labelledby="professionals-title">
      <div className={styles.headingRow}>
        <div>
          <span className={styles.kicker}>03 / MEET THE MAKERS</span>
          <h2 id="professionals-title">Find the people<br /><em>for your space.</em></h2>
        </div>
        <div className={styles.professionalHeaderCta}>
          <p>Connect with interior designers, architects, and custom cabinetry makers who understand your vision.</p>
          <Link className={styles.textLink} href="/professionals">
            Find a professional <ArrowUpRight size={18} />
          </Link>
        </div>
      </div>

      {professionals.length > 0 ? (
        <div className={styles.professionalGrid}>
          {professionals.map((prof, index) => (
            <article key={prof.id} className={styles.professionalCard}>
              <Link
                href={`/professionals/${prof.slug}`}
                className={styles.professionalCardImage}
                data-motion-image={index % 2 === 0 ? 'left' : 'right'}
              >
                {prof.sampleProjectCoverUrls?.[0] ? (
                  <img src={prof.sampleProjectCoverUrls[0]} alt={prof.name} loading="lazy" width={800} height={600} />
                ) : (
                  <div className={styles.professionalPlaceholder}>{prof.name.slice(0, 1)}</div>
                )}
              </Link>
              <div className={styles.professionalCardBody}>
                <span className={styles.kicker}>
                  {prof.professionalTypeLabel}{prof.city ? ` / ${prof.city}` : ''}
                </span>
                <h3>
                  <Link href={`/professionals/${prof.slug}`}>{prof.name}</Link>
                </h3>
                {prof.specialties && prof.specialties.length > 0 && (
                  <p className={styles.specialties}>{prof.specialties.slice(0, 2).join(' · ')}</p>
                )}
                <Link className={styles.attribution} href={`/professionals/${prof.slug}`}>
                  View profile & projects <ArrowUpRight size={14} />
                </Link>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <div className={styles.professionalsFallback}>
          <div data-motion-image="left" className={styles.professionalPhoto}>
            <Image
              src="/images/generated/hall-cabinetry-v2.png"
              alt="Detailed walnut and bronze glass cabinetry with burgundy cupboards — AI Concept Visualization"
              fill
              sizes="(max-width: 1023px) 100vw, 50vw"
            />
            <span className={styles.aiBadge}>AI Concept Visualization</span>
          </div>
          <div className={styles.professionalCopy}>
            <p>The best spaces begin with the right conversation. Discover designers, architects, and cabinetry craftspeople whose work speaks to you.</p>
            <ul>
              <li>Interior designers & design studios</li>
              <li>Architects & architecture firms</li>
              <li>Custom cabinetry & modular wardrobe makers</li>
              <li>Turnkey interior contractors</li>
            </ul>
            <Link className={styles.primaryLink} href="/professionals">
              Find a professional <ArrowUpRight size={18} />
            </Link>
          </div>
        </div>
      )}
    </section>
  );
}
