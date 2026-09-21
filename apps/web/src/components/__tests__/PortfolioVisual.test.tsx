import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { PortfolioMotion } from '../portfolio/templates/PortfolioMotion';
import { contrast, visualPalette } from '../portfolio/templates/visual-palette';
import { TEMPLATE_REGISTRY } from '@/lib/portfolio/template-registry';
import { basicFixture } from './BasicTemplate.fixture';

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

describe('progressive portfolio presentation', () => {
  it('never hides SSR content, including when browser APIs are absent', () => {
    vi.stubGlobal('IntersectionObserver', undefined);
    for (const entry of Object.values(TEMPLATE_REGISTRY)) {
      const Component = entry.component;
      const html = renderToString(<Component {...basicFixture({templateKey:entry.key})}/>);
      expect(html).toContain('Considered spaces. Everyday living.');
      expect(html).not.toContain('data-entered');
      expect(html.match(/<h1[ >]/g)).toHaveLength(1);
    }
  });

  it('observes once, disconnects, and immediately removes enhancement when reduced motion changes', () => {
    let reduced = false;
    let onChange: () => void = () => {};
    let onEntry: (entries: {isIntersecting:boolean;target:Element}[]) => void = () => {};
    const disconnect = vi.fn(), unobserve = vi.fn(), observe = vi.fn();
    vi.stubGlobal('matchMedia', () => ({get matches(){return reduced;}, addEventListener: (_:string, f:()=>void) => {onChange=f;},removeEventListener:vi.fn()}));
    vi.stubGlobal('IntersectionObserver', class {
      constructor(f: typeof onEntry){onEntry=f;}
      observe=observe; disconnect=disconnect; unobserve=unobserve;
    });
    const {container,unmount}=render(<><main id="qa"><section>Visible content</section></main><PortfolioMotion mainId="qa"/></>);
    const section=container.querySelector('section')!;
    expect(observe).toHaveBeenCalledWith(section);
    expect(section.hasAttribute('data-entered')).toBe(false);
    act(()=>onEntry([{isIntersecting:true,target:section}]));
    expect(section.getAttribute('data-entered')).toBe('true');
    expect(unobserve).toHaveBeenCalledWith(section);
    act(()=>{reduced=true;onChange();});
    expect(section.hasAttribute('data-entered')).toBe(false);
    unmount();expect(disconnect).toHaveBeenCalled();
  });

  it('does not observe at all for reduced-motion users', () => {
    const constructor=vi.fn();
    vi.stubGlobal('matchMedia',()=>({matches:true,addEventListener:vi.fn(),removeEventListener:vi.fn()}));
    vi.stubGlobal('IntersectionObserver',class {constructor(){constructor();}});
    const {container}=render(<><main id="qa"><section>Visible</section></main><PortfolioMotion mainId="qa"/></>);
    expect(constructor).not.toHaveBeenCalled();expect(container.textContent).toBe('Visible');
  });

  it('preserves brand surfaces and ensures text and action contrast even with conflicting colors', () => {
    for(const surface of ['#000000','#FFFFFF','#888888','#2E5D4B','#FAF8F5']) {
      const style=visualPalette(basicFixture({secondaryColor:surface,primaryColor:surface,accentColor:surface}),['#FFFFFF','#111111','#B88A5A']) as Record<string,string>;
      expect(style['--portfolio-surface']).toBe(surface);
      expect(contrast(style['--portfolio-ink'],surface)).toBeGreaterThanOrEqual(4.5);
      expect(contrast(style['--portfolio-accent-text'],surface)).toBeGreaterThanOrEqual(4.5);
      expect(contrast(style['--portfolio-on-accent'],surface)).toBeGreaterThanOrEqual(4.5);
    }
  });

  it('renders supplied comparisons instead of labels alone in the later templates', () => {
    for(const key of ['ARCHITECTURAL','WARM_NATURAL','DARK_CINEMATIC'] as const) {
      const Component=TEMPLATE_REGISTRY[key].component;
      const {container,unmount}=render(<Component {...basicFixture({templateKey:key,visibleSections:[{sectionId:'comparison',sectionType:'BEFORE_AI_REALITY',displayOrder:0,schemaVersion:1,content:{pairs:[{conceptImageUrl:'https://example.com/concept.jpg',realityImageUrl:'https://example.com/real.jpg'}]}}]})}/>);
      expect(container.querySelectorAll('img')).toHaveLength(2);
      expect(container.textContent).toContain('AI Concept Visualization');
      expect(container.textContent).toContain('Real Result');
      for(const anchor of container.querySelectorAll('a[href^="#"]')) expect(document.getElementById(anchor.getAttribute('href')!.slice(1))).toBeTruthy();
      unmount();
    }
  });
});
