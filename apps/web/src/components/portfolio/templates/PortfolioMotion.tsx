'use client';

import { useEffect } from 'react';

export function portfolioDepth(progress: number, direction: number, compact: boolean, strength = 1) {
  const p = Math.min(1, Math.max(0, progress));
  const entry = Math.min(1, p / .3);
  const smooth = entry * entry * (3 - 2 * entry);
  const amount = (compact ? .32 : 1) * strength;
  return { x: (1 - smooth) * direction * 44 * amount, y: (18 - p * 36) * amount,
    turn: (1 - smooth) * direction * -2 * amount, depth: (1 - smooth) * -20 * amount,
    text: (1 - smooth) * 22 * amount };
}

/** Native scroll enhancement only: immutable content and fully visible SSR fallback. */
export function PortfolioMotion({ mainId }: { mainId: string }) {
  useEffect(() => {
    const main = document.getElementById(mainId);
    if (!main || !window.matchMedia || !window.IntersectionObserver) return;
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)');
    const active = new Set<HTMLElement>();
    const targets = new Map<HTMLElement, {media:boolean;direction:number}>();
    let observer: IntersectionObserver | undefined;
    let frame = 0;
    const variables = ['--portfolio-x','--portfolio-y','--portfolio-turn','--portfolio-depth','--portfolio-text'];
    const strength = mainId.startsWith('cinematic') ? 1.15 : mainId.startsWith('architectural') ? .65 : mainId.startsWith('luxury') ? .8 : mainId.startsWith('warm') ? .85 : 1;
    const draw = () => {
      frame = 0;
      if (preference.matches) return;
      const compact = main.clientWidth < 768;
      active.forEach(node => {
        const rect = node.getBoundingClientRect();
        // Offset geometry from its untransformed layout to avoid transform feedback.
        const previousY = Number.parseFloat(node.style.getPropertyValue('--portfolio-y')) || 0;
        const previousText = Number.parseFloat(node.style.getPropertyValue('--portfolio-text')) || 0;
        const media = targets.get(node)!.media;
        const top = rect.top - (media ? previousY : previousText);
        const progress = (window.innerHeight - top) / (window.innerHeight + rect.height);
        const motion = portfolioDepth(progress, targets.get(node)!.direction, compact, strength);
        node.style.setProperty('--portfolio-x', `${motion.x}px`);
        node.style.setProperty('--portfolio-y', `${motion.y}px`);
        node.style.setProperty('--portfolio-turn', `${motion.turn}deg`);
        node.style.setProperty('--portfolio-depth', `${motion.depth}px`);
        node.style.setProperty('--portfolio-text', `${motion.text}px`);
      });
    };
    const schedule = () => { if (!frame && active.size) frame = requestAnimationFrame(draw); };
    const reset = () => {
      cancelAnimationFrame(frame); frame = 0; observer?.disconnect(); active.clear();
      targets.forEach((_,node) => { delete node.dataset.portfolioMotion; variables.forEach(name=>node.style.removeProperty(name)); });
      targets.clear();
    };
    const start = () => {
      reset();
      if (preference.matches) return;
      let index = 0;
      main.querySelectorAll<HTMLElement>('figure').forEach(figure => {
        if (!figure.querySelector('img')) return;
        if (figure.dataset.cinematicControlled === 'true' || figure.closest('[data-cinematic-controlled="true"]')) return;
        const section = figure.closest('[data-section-type]');
        const comparison = section?.getAttribute('data-section-type')?.startsWith('BEFORE_') || figure.closest('[data-comparisons]');
        const target = comparison ? figure.closest<HTMLElement>('article') || figure.parentElement! : figure;
        if (!targets.has(target)) targets.set(target,{media:true,direction:index++ % 2 ? 1 : -1});
      });
      main.querySelectorAll<HTMLElement>('h1,h2,h3,p,blockquote').forEach(node => {
        if (node.closest('details,figure') || node.dataset.cinematicControlled === 'true' || node.closest('[data-cinematic-controlled="true"]') || [...targets.keys()].some(parent=>parent.contains(node))) return;
        targets.set(node,{media:false,direction:0});
      });
      observer = new IntersectionObserver(entries => {
        entries.forEach(entry => {
          const node = entry.target as HTMLElement;
          if (entry.isIntersecting) active.add(node); else active.delete(node);
        });
        schedule();
      },{rootMargin:'100px 0px',threshold:0});
      targets.forEach((state,node) => { node.dataset.portfolioMotion = state.media ? 'media' : 'text'; observer!.observe(node); });
    };
    start();
    const resize = window.ResizeObserver ? new ResizeObserver(schedule) : undefined;
    resize?.observe(main);
    const changes = new MutationObserver(start);
    changes.observe(main, {childList:true,subtree:true});
    // Capture also handles scrolling inside the workspace preview container.
    window.addEventListener('scroll',schedule,{passive:true,capture:true});
    window.addEventListener('resize',schedule);
    preference.addEventListener('change',start);
    return () => { reset(); resize?.disconnect(); changes.disconnect(); window.removeEventListener('scroll',schedule,true); window.removeEventListener('resize',schedule); preference.removeEventListener('change',start); };
  },[mainId]);
  return null;
}

