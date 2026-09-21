'use client';

import { useId, useRef, useState } from 'react';
import type { NavItem } from '@/lib/portfolio/template-contract';
import styles from './BasicTemplate.module.css';

export function BasicHeader({ name, links, sticky, cta }: {
  name: string; links: NavItem[]; sticky: boolean; cta?: { href: string; label: string };
}) {
  const [open, setOpen] = useState(false);
  const button = useRef<HTMLButtonElement>(null);
  const id = useId();
  return <header className={`${styles.header} ${sticky ? styles.sticky : ''}`} onKeyDown={event => {
    if (event.key === 'Escape' && open) { setOpen(false); button.current?.focus(); }
  }}>
    <div className={styles.headerRow}>
      <span className={styles.brand}>{name}</span>
      {(links.length > 0 || cta) && <button ref={button} type="button" className={styles.menuButton}
        aria-expanded={open} aria-controls={id} onClick={() => setOpen(!open)}>{open ? 'Close menu −' : 'Menu +'}</button>}
    </div>
    {(links.length > 0 || cta) && <nav id={id} aria-label="Portfolio" className={styles.navigation} data-open={open}>
      {links.map(link => <a key={link.sectionId} href={link.anchor} onClick={() => setOpen(false)}>{link.label}</a>)}
      {cta && <a href={cta.href} className={styles.button} onClick={() => setOpen(false)}>{cta.label} <span aria-hidden="true">↗</span></a>}
    </nav>}
  </header>;
}
