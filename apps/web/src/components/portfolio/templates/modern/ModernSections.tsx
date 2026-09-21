/* eslint-disable @next/next/no-img-element -- media loader is not part of the portfolio contract. */
import type { PortfolioTemplateProps } from '@/lib/portfolio/template-contract';
import type { PreviewSectionDto } from '@/lib/portfolio/types';
import { contacts, mediaUrl, objects, safeUrl, text } from './modern';
import styles from './ModernTemplate.module.css';
export function ModernSection({section:p, portfolio:x, id, anchors, isHero}: {section:PreviewSectionDto;portfolio:PortfolioTemplateProps;id:string;anchors:Set<string>;isHero:boolean}) {
  const c=p.content||{}; const title=text(c.sectionHeadline); const location=[x.studioCity,x.studioState].filter(Boolean).join(', '); const H=isHero?'h1':'h2';
  const heading=(fallback:string)=><H>{title||fallback}</H>; const para=(v:unknown)=>text(v)?<p>{text(v)}</p>:null;
  const action=(v:unknown,label:unknown)=>{const href=safeUrl(v,anchors); return href&&text(label)?<a className={styles.action} href={href}>{text(label)} <span aria-hidden="true">↗</span></a>:null;};
  let body;
  switch(p.sectionType){
    case 'HERO': body=<><p className={styles.kicker}>/ {text(c.badgeText)||x.professionalTitle||x.professionalType?.replace(/_/g,' ')}</p><H>{text(c.headlineOverride)||x.headline||x.studioName}</H><div className={styles.heroMeta}>{para(text(c.subheadlineOverride)||x.subheadline)}{location&&<p className={styles.meta}>{location}</p>}{action(c.ctaLink||x.navigationSettings.primaryCtaAnchor,c.ctaText||x.navigationSettings.primaryCtaLabel)}</div></> ;break;
    case 'ABOUT': body=<div className={styles.split}><div><span className={styles.index}>01 / PROFILE</span>{heading('About the practice')}</div><div>{para(text(c.narrativeOverride)||x.bio)}{para(text(c.philosophyOverride)||x.designPhilosophy)}{x.yearsOfExperience!=null&&<p className={styles.bigMeta}>{String(x.yearsOfExperience).padStart(2,'0')} <small>years in practice</small></p>}</div></div>;break;
    case 'SERVICES': body=<><span className={styles.index}>02 / CAPABILITIES</span>{heading('What we do')}<ol className={styles.serviceRows}>{x.canonicalServices.filter(s=>text(s.serviceName)).map((s,i)=><li key={s.serviceCode}><span>{String(i+1).padStart(2,'0')}</span><h3>{s.serviceName}</h3><b aria-hidden="true">↗</b></li>)}</ol></>;break;
    case 'DESIGN_PROCESS': body=<><span className={styles.index}>03 / METHOD</span>{heading('A clear way forward')}<ol className={styles.process}>{objects(c.steps).filter(s=>text(s.title)||text(s.desc)).map((s,i)=><li key={i}><span>{text(s.step)||String(i+1).padStart(2,'0')}</span><div><h3>{text(s.title)}</h3>{para(s.desc)}</div></li>)}</ol></>;break;
    case 'FEATURED_PROJECTS': case 'PROJECT_GRID': return null;
    case 'TESTIMONIALS': body=<><span className={styles.index}>04 / WORDS</span>{heading('In their words')}<div className={styles.quoteGrid}>{objects(c.items).filter(s=>text(s.quote)&&text(s.clientName)).map((s,i)=><figure key={i}><blockquote>“{text(s.quote)}”</blockquote><figcaption>{text(s.clientName)}{text(s.projectLocation)&&` · ${text(s.projectLocation)}`}</figcaption></figure>)}</div></>;break;
    case 'TEAM': body=<><span className={styles.index}>05 / PEOPLE</span>{heading('The people behind the work')}<div className={styles.cards}>{objects(c.members).filter(s=>text(s.name)).map((s,i)=><article key={i}><h3>{text(s.name)}</h3>{para(s.role)}{para(s.bio)}</article>)}</div></>;break;
    case 'AWARDS': body=<><span className={styles.index}>06 / RECOGNITION</span>{heading('Selected recognition')}<div className={styles.rows}>{objects(c.items).filter(s=>text(s.title)).map((s,i)=><div key={i}><span>{String(s.year||'—')}</span><h3>{text(s.title)}</h3><p>{text(s.issuer)}</p></div>)}</div></>;break;
    case 'PRESS': body=<><span className={styles.index}>07 / JOURNAL</span>{heading('In print')}<div className={styles.rows}>{objects(c.articles).filter(s=>text(s.title)).map((s,i)=><div key={i}><span>{text(s.publication)}</span><h3>{text(s.title)}</h3>{action(s.url,'Read')}</div>)}</div></>;break;
    case 'SERVICE_AREAS': body=<div className={styles.split}><div>{heading('Places we work')}</div><ul className={styles.places}>{x.canonicalServiceAreas.filter(s=>text(s.cityName)||text(s.locality)).map((s,i)=><li key={i}>{[s.locality,s.cityName].filter(Boolean).join(', ')}</li>)}</ul></div>;break;
    case 'FAQ': body=<div className={styles.split}><div>{heading('Questions, answered')}</div><div>{objects(c.items).filter(s=>text(s.question)&&text(s.answer)).map((s,i)=><details key={i} className={styles.faq}><summary>{text(s.question)}</summary><p>{text(s.answer)}</p></details>)}</div></div>;break;
    case 'CONTACT': body=<div className={styles.split}><div><span className={styles.index}>08 / START HERE</span>{heading('Let’s make room for what matters')}{para(c.contactIntro)}{location&&<p className={styles.meta}>{location}</p>}</div><div className={styles.contactLinks}>{contacts(x.publicContacts).map((a,i)=><a key={i} href={a.href}>{a.label}<span aria-hidden="true">↗</span></a>)}</div></div>;break;
    case 'CTA': body=<div className={styles.cta}><div><span className={styles.index}>NEXT / ENQUIRE</span><h2>{text(c.headline)}</h2>{para(c.description)}</div>{action(c.buttonLink,c.buttonText)}</div>;break;
    case 'CUSTOM_NOTE': body=<div className={styles.note}><span className={styles.index}>NOTE</span><h2>{text(c.title)||'A note from the practice'}</h2>{para(c.body)}</div>;break;
    case 'VIDEO': body=<><span className={styles.index}>FILM</span>{heading(text(c.title)||'Watch the practice')}<a className={styles.action} href={mediaUrl(c.videoUrl)}>Open film ↗</a>{para(c.caption)}</>;break;
    case 'BEFORE_AFTER': case 'BEFORE_AI_REALITY': {
      const ai = p.sectionType === 'BEFORE_AI_REALITY';
      const pairs = objects(c.pairs).filter(s => ai ? mediaUrl(s.conceptImageUrl) && mediaUrl(s.realityImageUrl) : mediaUrl(s.beforeImageUrl) && mediaUrl(s.afterImageUrl));
      body = <>
        <span className={styles.index}>TRANSFORMATION</span>{heading(ai ? 'Concept to reality' : 'Before / after')}
        <div className={styles.mediaGrid}>{pairs.map((item, i) => {
          const media = ai
            ? [{ src: item.conceptImageUrl, label: 'AI Concept Visualization' }, { src: item.realityImageUrl, label: 'Real Result' }]
            : [{ src: item.beforeImageUrl, label: 'Before' }, { src: item.afterImageUrl, label: 'After' }];
          return <article key={i}>{text(item.title) && <h3>{text(item.title)}</h3>}<div>{media.map(asset => <figure key={asset.label}><img src={mediaUrl(asset.src)} alt={`${asset.label}${text(item.title) ? ` — ${text(item.title)}` : ''}`} width={960} height={720} loading="lazy" /><figcaption>{asset.label}</figcaption></figure>)}</div></article>;
        })}</div>
      </>;
      break;
    }
    default:return null;
  }
  return <section id={id} className={`${styles.section} ${isHero?styles.hero:''}`} data-section-type={p.sectionType}>{body}</section>;
}
