/* eslint-disable @next/next/no-img-element -- Supplied remote media has no Next image loader contract yet. */
import type { PortfolioTemplateProps } from '@/lib/portfolio/template-contract';
import type { PreviewSectionDto } from '@/lib/portfolio/types';
import { contactLinks, items, mediaUrl, safeUrl, text } from './presentation';
import styles from './BasicTemplate.module.css';

export function BasicSection({ section, portfolio: p, id, anchors, heroIsHeading }: {
  section: PreviewSectionDto; portfolio: PortfolioTemplateProps; id: string; anchors: Set<string>; heroIsHeading: boolean;
}) {
  const c = section.content || {};
  const title = text(c.sectionHeadline);
  const Heading = heroIsHeading ? 'h1' : 'h2';
  const location = [p.studioCity, p.studioState].filter(Boolean).join(', ');
  const label = (fallback: string) => <h2>{title || fallback}</h2>;
  const paragraph = (value: unknown) => text(value) ? <p>{text(value)}</p> : null;
  const action = (value: unknown, caption: unknown) => {
    const href = safeUrl(value, anchors);
    return href && text(caption) ? <a className={styles.button} href={href}>{text(caption)} <span aria-hidden="true">↗</span></a> : null;
  };
  let body;
  switch (section.sectionType) {
    case 'HERO': body = <>
      <p className={styles.eyebrow}>{text(c.badgeText) || p.professionalTitle || p.professionalType?.replace(/_/g, ' ')}</p>
      <Heading>{text(c.headlineOverride) || p.headline || p.studioName}</Heading>
      <div className={styles.heroBottom}><div>{paragraph(text(c.subheadlineOverride) || p.subheadline)}{location && <p className={styles.location}>{location}</p>}</div>
        {action(c.ctaLink || p.navigationSettings.primaryCtaAnchor, c.ctaText || (p.navigationSettings.showPrimaryCta ? p.navigationSettings.primaryCtaLabel : ''))}</div>
    </>; break;
    case 'ABOUT': body = <div className={styles.editorial}><div>{label('About')} {p.yearsOfExperience != null && <p className={styles.experience}><strong>{p.yearsOfExperience}</strong> years of experience</p>}</div><div>
      {paragraph(text(c.narrativeOverride) || p.bio)}{paragraph(text(c.philosophyOverride) || p.designPhilosophy)}
      {text(c.quote) && <blockquote>{text(c.quote)}</blockquote>}
      {p.canonicalSpecialties.length > 0 && <ul className={styles.tags} aria-label="Specialties">{p.canonicalSpecialties.map(s => <li key={s.specialtyCode}>{s.specialtyName}</li>)}</ul>}
    </div></div>; break;
    case 'SERVICES': body = <>{label('Services')}{paragraph(c.sectionDescription)}<ul className={styles.grid}>{p.canonicalServices.filter(s => text(s.serviceName)).map((s, i) => <li key={s.serviceCode} className={styles.card}><span className={styles.number} aria-hidden="true">{String(i + 1).padStart(2, '0')}</span><h3>{s.serviceName}</h3></li>)}</ul></>; break;
    case 'DESIGN_PROCESS': body = <>{label('Our process')}<ol className={styles.grid}>{items(c.steps).filter(s => text(s.title) || text(s.desc)).map((s, i) => <li key={i} className={styles.card}><span className={styles.number}>{text(s.step) || String(i + 1).padStart(2, '0')}</span>{text(s.title) && <h3>{text(s.title)}</h3>}{paragraph(s.desc)}</li>)}</ol></>; break;
    case 'TESTIMONIALS': body = <>{label('In their words')}<div className={styles.grid}>{items(c.items).filter(s => text(s.quote) && text(s.clientName)).map((s, i) => <figure key={i} className={styles.card}><blockquote>“{text(s.quote)}”</blockquote><figcaption>{text(s.clientName)}{text(s.projectLocation) && ` · ${text(s.projectLocation)}`}</figcaption></figure>)}</div></>; break;
    case 'TEAM': body = <>{label('People')}<div className={styles.grid}>{items(c.members).filter(s => text(s.name)).map((s, i) => <article key={i} className={styles.card}><h3>{text(s.name)}</h3>{paragraph(s.role)}{paragraph(s.bio)}</article>)}</div></>; break;
    case 'AWARDS': body = <>{label('Recognition')}<ul className={styles.grid}>{items(c.items).filter(s => text(s.title)).map((s, i) => <li key={i} className={styles.card}><h3>{text(s.title)}</h3>{paragraph(s.issuer)}{(typeof s.year === 'number' || typeof s.year === 'string') && <p>{String(s.year)}</p>}</li>)}</ul></>; break;
    case 'PRESS': body = <>{label('Press')}<div className={styles.grid}>{items(c.articles).filter(s => text(s.title)).map((s, i) => <article key={i} className={styles.card}>{paragraph(s.publication)}<h3>{text(s.title)}</h3>{action(mediaUrl(s.url), 'Read article')}</article>)}</div></>; break;
    case 'SERVICE_AREAS': body = <div className={styles.editorial}><div>{label('Where we work')}{paragraph(c.coverageNote)}</div><ul className={styles.areaList}>{p.canonicalServiceAreas.filter(s => text(s.cityName) || text(s.locality)).map((s, i) => <li key={i}>{[s.locality, s.cityName].filter(Boolean).join(', ')}</li>)}</ul></div>; break;
    case 'FAQ': body = <div className={styles.editorial}>{label('Good to know')}<div>{items(c.items).filter(s => text(s.question) && text(s.answer)).map((s, i) => <details key={i} className={styles.disclosure}><summary>{text(s.question)}</summary><p>{text(s.answer)}</p></details>)}</div></div>; break;
    case 'CONTACT': body = <div className={styles.editorial}><div>{label('Let’s talk')}{paragraph(c.contactIntro)}{location && <p>{location}</p>}</div><div className={styles.contactList}>{contactLinks(p.publicContacts).map((link, i) => <a key={i} href={link.href}>{link.label}<span aria-hidden="true">↗</span></a>)}</div></div>; break;
    case 'CTA': body = <div className={styles.cta}><div><h2>{text(c.headline)}</h2>{paragraph(c.description)}</div>{action(c.buttonLink, c.buttonText)}</div>; break;
    case 'CUSTOM_NOTE': body = <div className={styles.reading}><h2>{text(c.title) || 'A note from the practice'}</h2>{paragraph(c.body)}</div>; break;
    case 'VIDEO': body = <>{label(text(c.title) || 'Watch')}<a className={styles.button} href={mediaUrl(c.videoUrl)}>Watch {text(c.title) || 'video'} <span aria-hidden="true">↗</span></a>{paragraph(c.caption)}</>; break;
    case 'BEFORE_AFTER': case 'BEFORE_AI_REALITY': {
      const ai = section.sectionType === 'BEFORE_AI_REALITY';
      body = <>{label(ai ? 'Concept to reality' : 'Before & after')}{items(c.pairs).filter(pair => ai ? mediaUrl(pair.conceptImageUrl) && mediaUrl(pair.realityImageUrl) : mediaUrl(pair.beforeImageUrl) && mediaUrl(pair.afterImageUrl)).map((pair, i) => <article key={i} className={styles.comparison}>
        {text(pair.title) && <h3>{text(pair.title)}</h3>}<div className={styles.grid}>{(ai
          ? [{ src: pair.conceptImageUrl, label: 'AI Concept Visualization' }, { src: pair.realityImageUrl, label: 'Real Result' }]
          : [{ src: pair.beforeImageUrl, label: 'Before' }, { src: pair.afterImageUrl, label: 'After' }]).map(media => <figure key={media.label}><img src={mediaUrl(media.src)} alt={`${media.label}${text(pair.title) ? ` — ${text(pair.title)}` : ''}`} width={960} height={720} loading="lazy" referrerPolicy="no-referrer" /><figcaption>{media.label}</figcaption></figure>)}</div>{paragraph(pair.caption)}</article>)}</>; break;
    }
    default: return null;
  }
  return <section id={id} className={`${styles.section} ${section.sectionType === 'HERO' ? styles.hero : ''}`} data-section-type={section.sectionType}>{body}</section>;
}
