'use client';

import { useEffect } from 'react';

/** Presentation-only enhancement. SSR content is visible before and without JS. */
export function PortfolioMotion({ mainId }: { mainId: string }) {
  useEffect(() => {
    const main = document.getElementById(mainId);
    if (!main || !window.matchMedia || !window.IntersectionObserver) return;
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)');
    let observer: IntersectionObserver | undefined;
    const targets = Array.from(main.querySelectorAll('section, figure'));
    const reset = () => {
      observer?.disconnect();
      targets.forEach(target => target.removeAttribute('data-entered'));
    };
    const start = () => {
      reset();
      if (preference.matches) return;
      observer = new IntersectionObserver(entries => {
        entries.forEach(entry => {
          if (!entry.isIntersecting) return;
          entry.target.setAttribute('data-entered', 'true');
          observer?.unobserve(entry.target);
        });
      }, { threshold: 0, rootMargin: '0px 0px -24px 0px' });
      targets.forEach(target => observer?.observe(target));
    };
    start();
    preference.addEventListener('change', start);
    return () => { reset(); preference.removeEventListener('change', start); };
  }, [mainId]);
  return null;
}
