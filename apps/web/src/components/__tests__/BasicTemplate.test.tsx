import React from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { readFileSync, readdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { BasicTemplate } from '../portfolio/templates/basic/BasicTemplate';
import { basicFixture, basicSections } from './BasicTemplate.fixture';
import { contrast, safeUrl, themeStyle } from '../portfolio/templates/basic/presentation';
import type { PreviewSectionDto } from '@/lib/portfolio/types';

afterEach(() => { cleanup(); vi.restoreAllMocks(); });
const section = (type: PreviewSectionDto['sectionType'], content: Record<string, unknown>): PreviewSectionDto => ({ sectionId: type, sectionType: type, displayOrder: 1, schemaVersion: 1, content });

describe('BASIC production presentation', () => {
  it('renders semantic landmarks and one meaningful headline without auth or API calls', () => {
    const network = vi.spyOn(globalThis, 'fetch');
    render(<BasicTemplate {...basicFixture()} />);
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    expect(screen.getByRole('heading', { name: 'Considered spaces. Everyday living.' })).toBeTruthy();
    expect(screen.getByRole('banner')).toBeTruthy();
    expect(screen.getByRole('main')).toBeTruthy();
    expect(screen.getByRole('contentinfo')).toBeTruthy();
    expect(network).not.toHaveBeenCalled();
  });
  it('preserves engine ordering without mutating the input', () => {
    const source = [basicSections[2], basicSections[0], basicSections[1]];
    const snapshot = JSON.stringify(source);
    const { container } = render(<BasicTemplate {...basicFixture({ visibleSections: source })} />);
    expect([...container.querySelectorAll('section')].map(s => s.dataset.sectionType)).toEqual(['HERO', 'ABOUT', 'SERVICES']);
    expect(JSON.stringify(source)).toBe(snapshot);
  });
  it('omits disabled/absent sections and drops navigation to empty sections', () => {
    const { container } = render(<BasicTemplate {...basicFixture({ visibleSections: [basicSections[0], basicSections[3]] })} />);
    expect(container.querySelector('#about')).toBeNull();
    expect(container.querySelector('a[href="#projects"]')).toBeNull();
    expect(container.querySelector('a[href="#contact"]')).toBeNull();
  });
  it('renders safely with zero optional content and no hero', () => {
    const { container } = render(<BasicTemplate {...basicFixture({ visibleSections: [], publicContacts: [], canonicalServices: [], canonicalSpecialties: [], canonicalServiceAreas: [] })} />);
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Form & Field');
    expect(container.querySelectorAll('img,section')).toHaveLength(0);
    expect(container.textContent).not.toMatch(/undefined|TODO|Verified|Award Winning/);
  });
  it('hides unresolved projects, missing media, empty testimonials, awards and socials', () => {
    const sections = [section('FEATURED_PROJECTS', { projectIds: ['unresolved-real-id'] }), section('PROJECT_GRID', {}), section('TESTIMONIALS', { items: [] }), section('AWARDS', { items: [] }), section('BEFORE_AFTER', {}), section('BEFORE_AI_REALITY', {}), section('VIDEO', {})];
    const { container } = render(<BasicTemplate {...basicFixture({ visibleSections: sections, publicContacts: [] })} />);
    expect(container.querySelectorAll('section,img')).toHaveLength(0);
    expect(container.textContent).not.toContain('Projects In Curation');
  });
  it('renders only supplied contact actions and rejects malformed contact targets', () => {
    render(<BasicTemplate {...basicFixture({ publicContacts: [
      { kind: 'EMAIL', contactValue: 'public@example.com' }, { kind: 'PHONE', contactValue: '+91 12345 67890' },
      { kind: 'INSTAGRAM', contactValue: 'javascript:alert(1)' }, { kind: 'EMAIL', contactValue: 'bad?bcc=private@example.com' },
    ] })} />);
    expect(screen.getByRole('link', { name: /Email public/ }).getAttribute('href')).toBe('mailto:public@example.com');
    expect(screen.getByRole('link', { name: /Call/ }).getAttribute('href')).toBe('tel:+911234567890');
    expect(screen.queryByText(/private@example.com/)).toBeNull();
    expect(screen.queryByRole('link', { name: 'Instagram' })).toBeNull();
  });
  it('opens the mobile menu, closes on Escape and restores focus', () => {
    render(<BasicTemplate {...basicFixture()} />);
    const menu = screen.getByRole('button', { name: 'Menu +' });
    fireEvent.click(menu);
    expect(menu.getAttribute('aria-expanded')).toBe('true');
    fireEvent.keyDown(menu, { key: 'Escape' });
    expect(menu.getAttribute('aria-expanded')).toBe('false');
    expect(document.activeElement).toBe(menu);
    fireEvent.click(menu);
    fireEvent.click(screen.getByRole('link', { name: 'About' }));
    expect(menu.getAttribute('aria-expanded')).toBe('false');
  });
  it('supports long real content and many supplied services without truncating text', () => {
    const long = 'A carefully considered professional practice '.repeat(30);
    render(<BasicTemplate {...basicFixture({ studioName: long, headline: long, bio: long, canonicalServices: Array.from({ length: 15 }, (_, i) => ({ serviceCode: String(i), serviceName: `Service ${i}` })) })} />);
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe(long);
    expect(screen.getByRole('heading', { name: 'Service 14' })).toBeTruthy();
  });
  it('honors canonical section field names for supplied process, FAQ and testimonials', () => {
    render(<BasicTemplate {...basicFixture({ visibleSections: [
      section('DESIGN_PROCESS', { steps: [{ step: 'A', title: 'First step', desc: 'A supplied description' }] }),
      section('FAQ', { items: [{ question: 'A supplied question?', answer: 'A supplied answer.' }] }),
      section('TESTIMONIALS', { items: [{ quote: 'A supplied quote.', clientName: 'QA client' }] }),
    ] })} />);
    expect(screen.getByText('A supplied description')).toBeTruthy();
    expect(screen.getByText('A supplied question?').tagName).toBe('SUMMARY');
    expect(screen.getByText('“A supplied quote.”')).toBeTruthy();
  });
  it('labels AI concept and real result explicitly without inventing a before image', () => {
    const { container } = render(<BasicTemplate {...basicFixture({ visibleSections: [section('BEFORE_AI_REALITY', { pairs: [{ conceptImageUrl: 'https://example.com/concept.jpg', realityImageUrl: 'https://example.com/result.jpg', title: 'Supplied comparison' }] })] })} />);
    expect(screen.getByText('AI Concept Visualization')).toBeTruthy();
    expect(screen.getByText('Real Result')).toBeTruthy();
    expect(container.querySelectorAll('img')).toHaveLength(2);
  });
  it('rejects executable URLs, credentials, missing anchors and unsafe media', () => {
    for (const url of ['javascript:alert(1)', 'data:text/html,test', 'file:///test', '//example.com', 'https://user:secret@example.com', '#missing']) expect(safeUrl(url)).toBeUndefined();
    expect(safeUrl('#about', new Set(['about']))).toBe('#about');
    const { container } = render(<BasicTemplate {...basicFixture({ visibleSections: [section('CTA', { headline: 'Unsafe', buttonText: 'Go', buttonLink: 'javascript:alert(1)' }), section('VIDEO', { videoUrl: 'data:video/test' })] })} />);
    expect(container.querySelectorAll('section')).toHaveLength(0);
  });
  it('keeps IDs unique when repeatable sections occur and all navigation targets exist', () => {
    const { container } = render(<BasicTemplate {...basicFixture({ visibleSections: [basicSections[0], basicSections[7], section('CUSTOM_NOTE', { body: 'First' }), { ...section('CUSTOM_NOTE', { body: 'Second' }), sectionId: 'second-note' }] })} />);
    const ids = [...container.querySelectorAll('[id]')].map(n => n.id);
    expect(new Set(ids).size).toBe(ids.length);
    for (const link of container.querySelectorAll('a[href^="#"]')) expect(ids).toContain(link.getAttribute('href')!.slice(1));
  });
  it('selects accessible text colors even for poor custom colors and rejects CSS injection', () => {
    for (const color of ['#FFFFFF', '#000000', '#777777', '#888888', '#B88A5A', '#00FF00']) {
      const style = themeStyle(basicFixture({ primaryColor: color, secondaryColor: color, accentColor: color })) as Record<string, string>;
      expect(contrast(style['--basic-ink'], style['--basic-surface'])).toBeGreaterThanOrEqual(4.5);
      expect(contrast(style['--basic-accent-ink'], style['--basic-accent'])).toBeGreaterThanOrEqual(4.5);
    }
    expect(JSON.stringify(themeStyle(basicFixture({ primaryColor: 'red; background:url(evil)' })))).not.toContain('evil');
  });
  it('server renders without browser globals or business data dependencies', () => {
    expect(renderToString(<BasicTemplate {...basicFixture()} />)).toContain('Considered spaces');
    const directory = resolve('src/components/portfolio/templates/basic');
    for (const file of readdirSync(directory).filter(f => /\.[tj]sx?$/.test(f))) {
      const source = readFileSync(resolve(directory, file), 'utf8');
      expect(source).not.toMatch(/apiFetch|AuthContext|fetch\(|localStorage|sessionStorage|dangerouslySetInnerHTML|\/lib\/auth/);
    }
    const css = readFileSync(resolve(directory, 'BasicTemplate.module.css'), 'utf8');
    expect(css).toContain('prefers-reduced-motion: reduce');
    expect(css).toContain(':focus-visible');
  });
  it('renders the remaining supplied canonical content without inventing claims', () => {
    const { container } = render(<BasicTemplate {...basicFixture({ visibleSections: [
      basicSections[0], basicSections[7],
      section('TEAM', { members: [{ name: 'Supplied team member', role: 'Designer', bio: 'Supplied biography.' }] }),
      section('AWARDS', { items: [{ title: 'Supplied distinction', issuer: 'Supplied issuer', year: 2025 }] }),
      section('PRESS', { articles: [{ title: 'Supplied article', publication: 'Supplied publication', url: 'https://example.com/article' }] }),
      section('VIDEO', { title: 'Supplied film', videoUrl: 'https://example.com/film' }),
      section('BEFORE_AFTER', { pairs: [{ beforeImageUrl: 'https://example.com/before.jpg', afterImageUrl: 'https://example.com/after.jpg', title: 'Supplied work' }] }),
      section('CTA', { headline: 'Discuss your space', description: 'Supplied introduction.', buttonText: 'Contact the practice', buttonLink: '#contact' }),
      section('CUSTOM_NOTE', { title: 'Material approach', body: 'A supplied editorial note.' }),
    ] })} />);
    expect(screen.getByRole('heading', { name: 'Supplied team member' })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Supplied distinction' })).toBeTruthy();
    expect(screen.getByRole('link', { name: /Read article/ }).getAttribute('href')).toBe('https://example.com/article');
    expect(screen.getByRole('link', { name: /Watch Supplied film/ })).toBeTruthy();
    expect(screen.getByText('Before')).toBeTruthy();
    expect(screen.getByText('After')).toBeTruthy();
    expect(screen.getByRole('link', { name: /Contact the practice/ }).getAttribute('href')).toBe('#contact');
    expect(container.querySelectorAll('h1')).toHaveLength(1);
  });
  it('ignores unknown future sections and malformed optional collection entries', () => {
    const future = { ...section('CUSTOM_NOTE', {}), sectionType: 'FUTURE' } as unknown as PreviewSectionDto;
    const { container } = render(<BasicTemplate {...basicFixture({ visibleSections: [future, section('TESTIMONIALS', { items: [null, 1, 'text', { quote: 'Incomplete' }] }), section('FAQ', { items: 'invalid' })] })} />);
    expect(container.querySelectorAll('section')).toHaveLength(0);
  });
});
