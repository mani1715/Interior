import { PortfolioMotion } from '../PortfolioMotion';
import { visualPalette } from '../visual-palette';
import { useId } from 'react';
import type { PortfolioTemplateProps } from '@/lib/portfolio/template-contract';
import { BasicHeader } from './BasicHeader';
import { BasicSection } from './BasicSections';
import { safeUrl, text, usable } from './presentation';
import styles from './BasicTemplate.module.css';

export function BasicTemplate(props: PortfolioTemplateProps) {
  const mainId = `basic-main-${useId()}`;
  const { navigationSettings: navigation } = props;
  const candidates = [...props.visibleSections].sort((a, b) => a.displayOrder - b.displayOrder).filter(s => usable(s, props));
  const taken = new Set<string>();
  const planned = candidates.map((section, index) => {
    const nav = navigation.navItems.find(n => n.sectionId === section.sectionId);
    const preferred = nav?.anchor.replace(/^#/, '') || (section.sectionType === 'CONTACT' ? 'contact' : section.sectionType.toLowerCase().replace(/_/g, '-'));
    const safe = /^[a-z][a-z0-9-]*$/i.test(preferred) ? preferred : `section-${index}`;
    const id = taken.has(safe) ? `${safe}-${index}` : safe;
    taken.add(id);
    return { section, id };
  });
  // Resolve CTA dependencies without leaving navigation targets for empty/invalid sections.
  let rendered = planned;
  for (let pass = 0; pass <= planned.length; pass++) {
    const targets = new Set(rendered.map(s => s.id));
    const next = rendered.filter(({ section, id }) => section.sectionType !== 'CTA' ||
      (section.content.buttonLink !== `#${id}` && safeUrl(section.content.buttonLink, targets)));
    if (next.length === rendered.length) break;
    rendered = next;
  }
  const anchors = new Set(rendered.map(s => s.id));
  const links = navigation.navItems.flatMap(link => {
    const target = rendered.find(s => s.section.sectionId === link.sectionId);
    return target ? [{ ...link, anchor: `#${target.id}` }] : [];
  });
  const ctaHref = navigation.showPrimaryCta ? safeUrl(navigation.primaryCtaAnchor, anchors) : undefined;
  const hero = rendered.find(s => s.section.sectionType === 'HERO');
  const name = text(props.studioName) || 'Portfolio';
  return <div className={styles.root} style={{...visualPalette(props, ['#FAF8F5', '#1F1F1F', '#B88A5A'])}} data-font={props.fontPairing || 'SYSTEM_SANS'} data-mobile={props.isMobilePreview || undefined}>
    <a href={`#${mainId}`} className={styles.skip}>Skip to portfolio content</a>
    <BasicHeader name={name} links={links} sticky={navigation.sticky} cta={ctaHref ? { href: ctaHref, label: navigation.primaryCtaLabel } : undefined} />
    <main id={mainId} tabIndex={-1}>
      {!hero && <div className={styles.section}><h1>{name}</h1></div>}
      {rendered.map(({ section, id }) => <BasicSection key={section.sectionId} section={section} portfolio={props} id={id} anchors={anchors} heroIsHeading={section === hero?.section} />)}
    </main><PortfolioMotion mainId={mainId}/>
    <footer className={styles.footer}><strong>{name}</strong><span>{[props.studioCity, props.studioState].filter(Boolean).join(', ')}</span></footer>
  </div>;
}
