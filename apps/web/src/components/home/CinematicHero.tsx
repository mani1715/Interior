'use client';

import { useEffect, useRef, type CSSProperties } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { ArrowDown, ArrowUpRight } from 'lucide-react';
import { STORY_SCENES, STORY_DISTANCE, activeScene, clamp, sceneMotion } from './story-scenes';
import styles from './CinematicHero.module.css';

export function CinematicHero() {
  const root = useRef<HTMLElement>(null);
  useEffect(() => {
    const element = root.current;
    if (!element || !window.matchMedia || !window.IntersectionObserver) return;
    const motion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const desktop = window.matchMedia('(min-width: 768px) and (min-height: 600px)');
    const scenes = Array.from(element.querySelectorAll<HTMLElement>('[data-scene]'));
    const nav = Array.from(element.querySelectorAll<HTMLElement>('[data-scene-link]'));
    const images = Array.from(element.querySelectorAll<HTMLImageElement>('[data-scene] img'));
    let frame = 0;
    let visible = false;
    const reset = () => {
      delete element.dataset.cinematic;
      scenes.forEach(scene => { scene.removeAttribute('style'); scene.removeAttribute('inert'); scene.removeAttribute('aria-hidden'); });
      nav.forEach(link => link.removeAttribute('aria-current'));
      element.style.removeProperty('--journey');
      images.forEach((image, index) => { image.setAttribute('loading', index === 0 ? 'eager' : 'lazy'); });
    };
    const draw = () => {
      frame = 0;
      if (motion.matches || !desktop.matches) { reset(); return; }
      element.dataset.cinematic = 'true';
      const box = element.getBoundingClientRect();
      const progress = clamp((76 - box.top) / Math.max(1, box.height - window.innerHeight + 76));
      const current = activeScene(progress);
      // Warm only the current and next room instead of downloading all five at high priority.
      images.forEach((image, index) => {
        if (index <= current + 1 && image.loading !== 'eager') image.setAttribute('loading', 'eager');
      });
      scenes.forEach((scene, index) => {
        const state = sceneMotion(progress, index);
        scene.style.setProperty('--visibility', String(state.textOpacity));
        scene.style.setProperty('--pan', `${state.x}%`);
        scene.style.setProperty('--turn', `${state.rotation}deg`);
        scene.style.setProperty('--zoom', String(state.scale));
        scene.style.setProperty('--reveal', `${state.reveal}%`);
        scene.style.setProperty('--depth', `${state.depth}px`);
        scene.style.setProperty('--room-visible', state.visible ? '1' : '0');
        scene.style.setProperty('--foreground-pan', `${state.foreground}px`);
        scene.style.setProperty('--drift', `${clamp(state.local) * -5}px`);
        scene.style.setProperty('--text-y', `${(1 - state.textOpacity) * 14}px`);
        scene.inert = index !== current;
        scene.setAttribute('aria-hidden', String(index !== current));
      });
      nav.forEach((link, index) => {
        if (index === current) link.setAttribute('aria-current', 'step'); else link.removeAttribute('aria-current');
      });
      element.style.setProperty('--journey', String(progress));
    };
    const schedule = () => { if (!frame && visible) frame = requestAnimationFrame(draw); };
    const observer = new IntersectionObserver(entries => {
      visible = entries[0].isIntersecting;
      if (visible) schedule();
    }, { rootMargin: '100px' });
    observer.observe(element);
    const change = () => { reset(); draw(); };
    window.addEventListener('scroll', schedule, { passive: true });
    window.addEventListener('resize', schedule);
    motion.addEventListener('change', change);
    desktop.addEventListener('change', change);
    draw();
    return () => {
      cancelAnimationFrame(frame); observer.disconnect(); reset();
      window.removeEventListener('scroll', schedule); window.removeEventListener('resize', schedule);
      motion.removeEventListener('change', change); desktop.removeEventListener('change', change);
    };
  }, []);

  function goToScene(event: React.MouseEvent<HTMLAnchorElement>, index: number) {
    const element = root.current;
    if (element?.dataset.cinematic !== 'true') return;
    event.preventDefault();
    const start = element.getBoundingClientRect().top + window.scrollY - 76;
    const distance = element.offsetHeight - window.innerHeight + 76;
    // Land inside the scene's settled interval, beyond fractional-pixel reveal boundaries.
    const destination = index === 0 ? 0 : (index + 0.06) / STORY_DISTANCE;
    window.scrollTo({ top: start + distance * destination, behavior: 'instant' });
  }

  return <section ref={root} className={styles.journey} aria-label="A journey through crafted interiors">
    <a className={styles.skip} href="#explore-interiors">Skip interior story</a>
    <div className={styles.stage}>
      <div className={styles.topline}><span>INTERIORS, WITH INTENTION</span><span>DESIGNED FOR EVERYDAY LIVING</span></div>
      <div className={styles.scenes}>
        {STORY_SCENES.map((scene, index) => <article key={scene.key} id={`story-${scene.key}`} data-scene className={styles.scene}>
          <div className={styles.copy}>
            <span className={styles.sceneCount} aria-label={`Scene ${index + 1} of ${STORY_SCENES.length}`}>0{index + 1}<span> / {String(STORY_SCENES.length).padStart(2, '0')}</span></span>
            <span className={styles.eyebrow}>{scene.detail}</span>
            {index === 0 ? <h1>{scene.title}</h1> : <h2>{scene.title}</h2>}
            <p>{scene.body}</p>
            <Link className={styles.cta} href={scene.href}>{scene.cta}<ArrowUpRight size={19} aria-hidden="true" /></Link>
            <span className={styles.category}>{scene.category}</span>
          </div>
          <figure className={styles.visual} style={{ '--focal': scene.focal, '--mobile-focal': scene.mobileFocal } as CSSProperties}>
            <div className={styles.room}>
              <Image src={scene.image} alt={scene.alt} fill sizes="(max-width: 1023px) 94vw, (min-width: 1800px) 1120px, 64vw" loading={index === 0 ? 'eager' : 'lazy'} fetchPriority={index === 0 ? 'high' : 'auto'} className={styles.photo} />
              <div className={styles.foreground} aria-hidden="true" />
            </div>
            <figcaption><span>{scene.material}</span><span>AI Concept Visualization</span></figcaption>
          </figure>
        </article>)}
      </div>
      <div className={styles.bottomline}>
        <span className={styles.progress} aria-hidden="true" />
        <nav aria-label="Interior story scenes" className={styles.navigation}>
          {STORY_SCENES.map((scene, index) => <a key={scene.key} data-scene-link href={`#story-${scene.key}`} onClick={event => goToScene(event, index)}><span>0{index + 1}</span>{scene.category}</a>)}
        </nav>
        <a className={styles.scroll} href="#explore-interiors">SCROLL TO DISCOVER <ArrowDown size={16} aria-hidden="true" /></a>
      </div>
    </div>
  </section>;
}
