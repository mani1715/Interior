'use client';

import Image from 'next/image';
import Link from 'next/link';
import { ArrowUpRight } from 'lucide-react';
import styles from './BrandHero.module.css';

// Backward compatibility helper
export function brandMotion(progress: number) {
  const p = Math.min(1, Math.max(0, progress));
  return {
    approach: 1,
    openingTravel: p,
    craftTravel: p,
    destinationTravel: p,
    craft: 0,
    release: p >= 0.5 ? 1 : 0,
    opening: p < 0.5 ? 1 : 0,
    living: 1,
    chapter: p < 0.28 ? 0 : p < 0.76 ? 1 : 2,
  };
}

export function BrandHero() {
  return (
    <section className={styles.hero} aria-label="The Elégance design experience">
      <div className={styles.splitContainer}>
        <div className={styles.copyPanel}>
          <span className={styles.eyebrow}>A MORE CONSIDERED WAY TO LIVE</span>
          <h1 className={styles.title}>
            Find your kind of space.<br />
            <em>Meet its creators.</em>
          </h1>
          <p className={styles.description}>
            Explore interior projects, room by room. Discover the professionals behind them and start a conversation.
          </p>
          <div className={styles.actions}>
            <Link className={styles.button} href="/projects">
              Explore projects <ArrowUpRight size={18} aria-hidden="true" />
            </Link>
            <Link className={styles.secondaryButton} href="/professionals">
              Find a professional <ArrowUpRight size={18} aria-hidden="true" />
            </Link>
          </div>
          <div className={styles.metaRow}>
            <span className={styles.metaLabel}>CURATED RESIDENTIAL SPACES</span>
            <span className={styles.metaDot} aria-hidden="true">·</span>
            <span className={styles.metaLabel}>ARCHITECTURAL JOINERY</span>
          </div>
        </div>

        <div className={styles.imagePlate}>
          <Image
            src="/images/approved/hero-architectural-walnut.jpg"
            alt="Bespoke fluted walnut joinery and illuminated architectural shelving in a residential interior"
            fill
            priority
            quality={90}
            sizes="(max-width: 1023px) 100vw, 58vw"
            className={styles.heroImage}
          />
          <div className={styles.imagePlateOverlay} aria-hidden="true" />
          <div className={styles.imageBadge}>
            <span>BESPOKE JOINERY & ARCHITECTURAL INTERIORS</span>
          </div>
        </div>
      </div>
    </section>
  );
}
