'use client';
import { useId, useRef, useState } from 'react';
import type { NavItem } from '@/lib/portfolio/template-contract';
import styles from './ModernTemplate.module.css';
export function ModernHeader({mainId,name, links, cta}: {mainId:string;name:string; links:NavItem[]; cta?:{href:string;label:string}}) {
  const [open,setOpen]=useState(false); const button=useRef<HTMLButtonElement>(null); const id=useId();
  return <header className={styles.header} onKeyDown={e=>{if(e.key==='Escape'&&open){setOpen(false);button.current?.focus();}}}>
    <a className={styles.mark} href={`#${mainId}`}><span aria-hidden="true">/</span>{name}</a>
    {(links.length>0||cta)&&<button ref={button} className={styles.menu} type="button" aria-expanded={open} aria-controls={id} onClick={()=>setOpen(!open)}>{open?'Close':'Index'} <span aria-hidden="true">{open?'×':'+'}</span></button>}
    {(links.length>0||cta)&&<nav id={id} className={styles.nav} data-open={open} aria-label="Portfolio"><span className={styles.navLabel}>Explore</span>{links.map(x=><a key={x.sectionId} href={x.anchor} onClick={()=>setOpen(false)}>{x.label}</a>)}{cta&&<a className={styles.navCta} href={cta.href} onClick={()=>setOpen(false)}>{cta.label}<span aria-hidden="true">↗</span></a>}</nav>}
  </header>;
}
