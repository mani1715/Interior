import type { Metadata } from 'next';
import Link from 'next/link';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { CinematicHero } from '@/components/home/CinematicHero';
import { Footer } from '@/components/home/Footer';
import styles from '@/components/home/EditorialHome.module.css';

export const metadata: Metadata = {
  title: 'The Interior Journey — Elégance',
  description: 'Explore five considered interior concepts, from custom wardrobes to kitchens and living spaces.',
  alternates: { canonical: '/interior-journey' },
};

export default function InteriorJourneyPage() {
  return <><PublicHeader /><main id="main-content"><CinematicHero />
    <section id="explore-interiors" className={styles.finalCta}><span className={styles.kicker}>INSPIRATION IS JUST THE BEGINNING</span><h2>Make room<br /><em>for your own story.</em></h2><div><Link className={styles.primaryLink} href="/projects">Discover real projects ↗</Link><Link className={styles.textLink} href="/professionals">Find your professional ↗</Link></div><p>These five images are AI concept visualizations.</p></section>
  </main><Footer /></>;
}
