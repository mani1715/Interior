'use client';
import { useEffect, useRef, type ReactNode } from 'react';
import styles from './HomeMotion.module.css';

export function ScrollEntrances({ children }: { children: ReactNode }) {
  const root = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const el = root.current;
    if (!el || !window.matchMedia || !window.IntersectionObserver) return;
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
    const targets = new Set<HTMLElement>();
    const observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        const node = entry.target as HTMLElement;
        if (entry.isIntersecting) node.dataset.entered = 'true';
        else if (entry.boundingClientRect.top > window.innerHeight) delete node.dataset.entered;
      });
    }, { threshold: .08, rootMargin: '0px 0px -30px 0px' });
    const register = () => {
      if (reduced.matches) return;
      el.dataset.entrances = 'true';
      el.querySelectorAll<HTMLElement>('h2, h3, p, [data-motion-image]').forEach(node => {
        if (targets.has(node)) return;
        targets.add(node);
        node.dataset.enter = node.dataset.motionImage || 'up';
        if (node.getBoundingClientRect().top < window.innerHeight - 30) node.dataset.entered = 'true';
        observer.observe(node);
      });
      targets.forEach(node => { if (!el.contains(node)) { observer.unobserve(node); targets.delete(node); } });
    };
    const reset = () => {
      observer.disconnect(); delete el.dataset.entrances;
      targets.forEach(node => { delete node.dataset.enter; delete node.dataset.entered; });
      targets.clear();
    };
    const change = () => { reset(); register(); };
    // Register project results that arrive after the server-rendered content.
    const mutations = new MutationObserver(register);
    mutations.observe(el, { childList: true, subtree: true });
    reduced.addEventListener('change', change);
    register();
    return () => { mutations.disconnect(); reduced.removeEventListener('change', change); reset(); };
  }, []);
  return <div ref={root} className={styles.content}>{children}</div>;
}
