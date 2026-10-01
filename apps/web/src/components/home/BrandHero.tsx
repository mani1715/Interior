'use client';

import { useEffect, useRef } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { ArrowDown, ArrowUpRight } from 'lucide-react';
import styles from './BrandHero.module.css';

const clamp = (n: number) => Math.min(1, Math.max(0, n));
const ease = (n: number) => { const t = clamp(n); return t * t * (3 - 2 * t); };

// One continuous camera path; the foreground studies share the room's perspective.
export function brandMotion(progress: number) {
  const p = clamp(progress);
  return {
    approach: ease(p / .35),
    openingTravel: clamp(p / .28),
    craftTravel: clamp((p - .29) / .45),
    destinationTravel: clamp((p - .76) / .24),
    craft: ease((p - .29) / .13) * (1 - ease((p - .65) / .09)),
    release: ease((p - .76) / .2),
    opening: 1 - ease((p - .12) / .16),
    living: ease((p - .4) / .28),
    chapter: p < .28 ? 0 : p < .76 ? 1 : 2,
  };
}

export function BrandHero() {
  const root = useRef<HTMLElement>(null);
  useEffect(() => {
    const el = root.current;
    if (!el || !window.matchMedia || !window.IntersectionObserver) return;
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
    const wide = window.matchMedia('(min-width: 768px) and (min-height: 600px)');
    const chapters = Array.from(el.querySelectorAll<HTMLElement>('[data-brand-copy]'));
    let frame = 0;
    let visible = true;
    const reset = () => {
      delete el.dataset.motion;
      el.removeAttribute('style');
      chapters.forEach(node => { node.inert = false; node.removeAttribute('aria-hidden'); });
    };
    const draw = () => {
      frame = 0;
      if (reduced.matches || !wide.matches) { reset(); return; }
      el.dataset.motion = 'true';
      const rect = el.getBoundingClientRect();
      const p = clamp((76 - rect.top) / Math.max(1, rect.height - window.innerHeight + 76));
      const state = brandMotion(p);
      Object.entries(state).forEach(([name, value]) => el.style.setProperty(`--${name}`, String(value)));
      el.style.setProperty('--progress', String(p));
      chapters.forEach((node, index) => { node.inert = index !== state.chapter; node.setAttribute('aria-hidden', String(index !== state.chapter)); });
    };
    const schedule = () => { if (visible && !frame) frame = requestAnimationFrame(draw); };
    const observer = new IntersectionObserver(entries => { visible = entries[0].isIntersecting; if (visible) schedule(); }, { rootMargin: '100px' });
    observer.observe(el);
    const change = () => { reset(); draw(); };
    window.addEventListener('scroll', schedule, { passive: true });
    window.addEventListener('resize', schedule);
    reduced.addEventListener('change', change);
    wide.addEventListener('change', change);
    draw();
    return () => {
      cancelAnimationFrame(frame); observer.disconnect(); reset();
      window.removeEventListener('scroll', schedule); window.removeEventListener('resize', schedule);
      reduced.removeEventListener('change', change); wide.removeEventListener('change', change);
    };
  }, []);

  return <section ref={root} className={styles.hero} aria-label="The Elégance design experience">
    <a className={styles.skip} href="#explore-interiors">Skip cinematic introduction</a>
    <div className={styles.stage}>
      <div className={styles.world} aria-hidden="true">
        <div className={styles.room}>
          <Image src="/images/approved/hall.png" alt="" fill sizes="100vw" loading="eager" fetchPriority="high" />
          <div className={styles.living}><Image src="/images/approved/tv-unit.png" alt="" fill sizes="100vw" /></div>
        </div>
        <div className={styles.shade} />
        <div className={`${styles.study} ${styles.leftStudy}`}><Image src="/images/approved/kitchen.png" alt="" fill sizes="(max-width: 767px) 1px, 34vw" /><span>01 / MATERIAL & FORM</span></div>
        <div className={`${styles.study} ${styles.rightStudy}`}><Image src="/images/approved/feature.png" alt="" fill sizes="(max-width: 767px) 1px, 30vw" /><span>02 / LIGHT & DETAIL</span></div>
        <div className={styles.portal} />
      </div>
      <div className={styles.topline}><span>SPACES FOR A LIFE WELL LIVED</span><span>ELÉGANCE / INDIA</span></div>
      <div data-brand-copy className={`${styles.copy} ${styles.opening}`}>
        <span className={styles.eyebrow}>A MORE CONSIDERED WAY TO LIVE</span>
        <h1>Beautiful spaces.<br /><em>Human stories.</em></h1>
        <p>Discover interiors you love.<br />Meet the people who can make them yours.</p>
        <div data-motion-photo className={styles.mobilePhoto}><Image src="/images/approved/hall.png" alt="Warm timber and illuminated cabinetry in an imagined interior" fill sizes="(max-width: 767px) 100vw, 1px" /></div><Link className={styles.button} href="/projects">Discover the possibilities <ArrowUpRight size={18} /></Link>
      </div>
      <div data-brand-copy className={`${styles.copy} ${styles.craft}`}>
        <span className={styles.eyebrow}>THE DIFFERENCE IS IN THE DETAILS</span>
        <h2>Made with care.<br /><em>Made for living.</em></h2>
        <p>Warm materials. Thoughtful storage.<br />Everyday spaces, beautifully resolved.</p>
        <Link className={styles.textLink} href="/interior-journey">Take the interior journey <ArrowUpRight size={18} /></Link>
      </div>
      <div data-brand-copy className={`${styles.copy} ${styles.destination}`}>
        <span className={styles.eyebrow}>FROM INSPIRATION TO YOUR INTERIOR</span>
        <h2>See the potential.<br /><em>Find your people.</em></h2>
        <p>Explore real projects, connect with studios,<br />and visualize a space that feels like you.</p>
        <div className={styles.links}><Link className={styles.button} href="/professionals">Find your professional <ArrowUpRight size={18} /></Link><Link className={styles.textLink} href="#ai-visualizer">Visualize your ideas <ArrowUpRight size={18} /></Link></div>
      </div>
      <div className={styles.bottomline}><a href="#explore-interiors">SCROLL TO EXPLORE <ArrowDown size={15} /></a><span className={styles.track} aria-hidden="true" /><span>IMAGINED SPACES · AI CONCEPT VISUALIZATION</span></div>
    </div>
  </section>;
}



