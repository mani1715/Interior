import { PortfolioMotion } from '../PortfolioMotion';
import { visualPalette } from '../visual-palette';
import { useId } from 'react';
import type { PortfolioTemplateProps } from '@/lib/portfolio/template-contract';
import { ModernHeader } from './ModernHeader';
import { ModernSection } from './ModernSections';
import { safeUrl, text, usable } from './modern';
import styles from './ModernTemplate.module.css';
export function ModernTemplate(p: PortfolioTemplateProps) {
  const mainId=`modern-main-${useId()}`; const candidates=[...p.visibleSections].sort((a,b)=>a.displayOrder-b.displayOrder).filter(s=>usable(s,p)); const used=new Set<string>();
  const planned=candidates.map((section,i)=>{const nav=p.navigationSettings.navItems.find(x=>x.sectionId===section.sectionId); const raw=nav?.anchor.replace(/^#/,'')||section.sectionType.toLowerCase().replace(/_/g,'-'); const base=/^[a-z][a-z0-9-]*$/i.test(raw)?raw:`section-${i}`; const id=used.has(base)?`${base}-${i}`:base; used.add(id); return {section,id};});
  const anchors=new Set(planned.map(x=>x.id)); const rendered=planned.filter(x=>x.section.sectionType!=='CTA'||safeUrl(x.section.content.buttonLink,anchors)); const links=p.navigationSettings.navItems.flatMap(n=>{const target=rendered.find(x=>x.section.sectionId===n.sectionId);return target?[{...n,anchor:`#${target.id}`}]:[]}); const cta=p.navigationSettings.showPrimaryCta?safeUrl(p.navigationSettings.primaryCtaAnchor,anchors):undefined; const hero=rendered.find(x=>x.section.sectionType==='HERO'); const name=text(p.studioName)||'Portfolio';
  return <div className={styles.root} style={{...visualPalette(p, ['#F6F7F5', '#182126', '#537A8A'])}} data-font={p.fontPairing||'SYSTEM_SANS'} data-mobile={p.isMobilePreview||undefined}><a className={styles.skip} href={`#${mainId}`}>Skip to portfolio content</a><ModernHeader mainId={mainId} name={name} links={links} cta={cta?{href:cta,label:p.navigationSettings.primaryCtaLabel}:undefined}/><main id={mainId} tabIndex={-1}>{!hero&&<section className={styles.section}><h1>{name}</h1></section>}{rendered.map(x=><ModernSection key={x.section.sectionId} section={x.section} portfolio={p} id={x.id} anchors={anchors} isHero={x===hero}/>)}</main><PortfolioMotion mainId={mainId}/><footer className={styles.footer}><div><strong><span aria-hidden="true">/</span>{name}</strong><p>{[p.studioCity,p.studioState].filter(Boolean).join(', ')}</p></div></footer></div>;
}
