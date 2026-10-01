import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, cleanup, render } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { PortfolioMotion, portfolioDepth } from '../portfolio/templates/PortfolioMotion';
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

  it('observes visible text continuously and removes enhancement when reduced motion changes', () => {
    let reduced = false;
    let onChange: () => void = () => {};
    let onEntry: (entries: {isIntersecting:boolean;target:Element}[]) => void = () => {};
    const disconnect = vi.fn(), unobserve = vi.fn(), observe = vi.fn();
    vi.stubGlobal('matchMedia', () => ({get matches(){return reduced;}, addEventListener: (_:string, f:()=>void) => {onChange=f;},removeEventListener:vi.fn()}));
    vi.stubGlobal('IntersectionObserver', class {
      constructor(f: typeof onEntry){onEntry=f;}
      observe=observe; disconnect=disconnect; unobserve=unobserve;
    });
    const {container,unmount}=render(<><main id="qa"><section><h2>Visible content</h2></section></main><PortfolioMotion mainId="qa"/></>);
    const section=container.querySelector('h2')!;
    expect(observe).toHaveBeenCalledWith(section);
    expect(section.hasAttribute('data-portfolio-motion')).toBe(true);
    act(()=>onEntry([{isIntersecting:true,target:section}]));
    expect(section.getAttribute('data-portfolio-motion')).toBe('text');
    expect(unobserve).not.toHaveBeenCalled();
    act(()=>{reduced=true;onChange();});
    expect(section.hasAttribute('data-portfolio-motion')).toBe(false);
    unmount();expect(disconnect).toHaveBeenCalled();
  });

  it('uses opposite photo entrances, gentler compact movement and bounded reversible depth', () => {
    expect(portfolioDepth(0,-1,false).x).toBeLessThan(0);
    expect(portfolioDepth(0,1,false).x).toBeGreaterThan(0);
    expect(Math.abs(portfolioDepth(0,1,true).x)).toBeLessThan(Math.abs(portfolioDepth(0,1,false).x));
    Object.values(portfolioDepth(.5,1,false)).forEach(value => expect(Math.abs(value)).toBe(0));
    expect(portfolioDepth(-1,1,false)).toEqual(portfolioDepth(0,1,false));
    expect(portfolioDepth(2,1,false)).toEqual(portfolioDepth(1,1,false));
  });

  it('moves comparison pairs as one plane and leaves disclosure content static', () => {
    vi.stubGlobal('matchMedia',()=>({matches:false,addEventListener:vi.fn(),removeEventListener:vi.fn()}));
    vi.stubGlobal('IntersectionObserver',class {observe(){} disconnect(){}});
    const {container}=render(<><main id="qa-pair"><section data-section-type="BEFORE_AFTER"><article><figure><img alt="Before"/></figure><figure><img alt="After"/></figure></article></section><details><summary>Question</summary><p>Answer</p></details></main><PortfolioMotion mainId="qa-pair"/></>);
    expect(container.querySelector('article')?.getAttribute('data-portfolio-motion')).toBe('media');
    expect(container.querySelectorAll('figure[data-portfolio-motion]')).toHaveLength(0);
    expect(container.querySelector('details p')?.hasAttribute('data-portfolio-motion')).toBe(false);
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

