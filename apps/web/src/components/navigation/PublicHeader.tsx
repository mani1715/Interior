'use client';
import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { ArrowUpRight, Menu, X } from 'lucide-react';
import { useAuth } from '@/lib/auth/auth-context';
import styles from './PublicHeader.module.css';
export interface NavLinkItem { label: string; href: string }
export interface PublicHeaderProps { navLinks?: NavLinkItem[]; currentPath?: string; onSignInClick?: () => void; onGetStartedClick?: () => void }
const defaultNavLinks = [{ label: 'Explore projects', href: '/projects' }, { label: 'Find professionals', href: '/professionals' }, { label: 'AI Visualizer', href: '/#ai-visualizer' }];
export function PublicHeader({ navLinks = defaultNavLinks, currentPath = '/', onSignInClick, onGetStartedClick }: PublicHeaderProps) {
  const [open, setOpen] = useState(false);
  const toggle = useRef<HTMLButtonElement>(null);
  const panel = useRef<HTMLDivElement>(null);
  const { user, isAuthenticated, logout } = useAuth();
  const professional = user && (user.roles.includes('DESIGNER') || user.roles.includes('DESIGNER_TEAM'));
  useEffect(() => {
    if (!open) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    panel.current?.querySelector<HTMLElement>('a,button')?.focus();
    const close = () => { setOpen(false); toggle.current?.focus(); };
    const keydown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') { event.preventDefault(); close(); }
      if (event.key === 'Tab') {
        const nodes = Array.from(panel.current?.querySelectorAll<HTMLElement>('a[href],button') || []);
        const first = nodes[0], last = nodes[nodes.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
        if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
      }
    };
    const media = window.matchMedia?.('(min-width: 1100px)');
    const resize = () => { if (media?.matches) close(); };
    media?.addEventListener('change', resize);
    document.addEventListener('keydown', keydown);
    return () => { document.body.style.overflow = previous; document.removeEventListener('keydown', keydown); media?.removeEventListener('change', resize); };
  }, [open]);
  const close = () => { setOpen(false); toggle.current?.focus(); };
  const actions = <>
    {isAuthenticated && user ? <>
      {professional ? <Link href="/workspace" onClick={close}>Workspace</Link> : user.studios.length === 0 ? <Link href="/onboarding/professional" onClick={close}>Register as Professional</Link> : null}
      <Link href="/account" onClick={close}><span>{user.displayName}</span><small>{user.roles.includes('DESIGNER') ? 'DESIGNER' : user.roles[0] || 'CUSTOMER'}</small></Link>
      <button onClick={() => { void logout(); close(); }}>Sign Out</button>
    </> : <>
      <Link href="/sign-in" onClick={() => { close(); onSignInClick?.(); }}>Sign In</Link>
      <Link className={styles.join} href="/sign-up" onClick={() => { close(); onGetStartedClick?.(); }}>For professionals <ArrowUpRight size={16} aria-hidden="true" /></Link>
    </>}
  </>;
  return <header className={styles.header}>
    <a className={styles.skip} href="#main-content">Skip to content</a>
    <div className={styles.bar}>
      <Link href="/" className={styles.brand} aria-label="Elégance home">elégance<span>INTERIORS. PEOPLE. POSSIBILITIES.</span></Link>
      <nav className={styles.desktop} aria-label="Main Navigation">{navLinks.map(link => <Link key={link.href} href={link.href} aria-current={currentPath === link.href ? 'page' : undefined}>{link.label}</Link>)}</nav>
      <div className={styles.actions}>{actions}</div>
      <button ref={toggle} className={styles.toggle} aria-label={open ? 'Close menu' : 'Open menu'} aria-expanded={open} aria-controls="public-menu" onClick={() => setOpen(!open)}>{open ? <X size={22} /> : <Menu size={22} />}</button>
    </div>
    {open && <div ref={panel} id="public-menu" className={styles.panel} role="dialog" aria-modal="true" aria-label="Navigation menu">
      <button className={styles.close} onClick={close} aria-label="Close navigation"><X size={22} /></button>
      <span className={styles.menuLabel}>EXPLORE ELÉGANCE</span>
      <nav aria-label="Mobile Navigation">{navLinks.map((link, index) => <Link key={link.href} href={link.href} onClick={close}><small>0{index + 1}</small>{link.label}<ArrowUpRight size={20} /></Link>)}</nav>
      <div className={styles.mobileActions}>{actions}</div>
    </div>}
  </header>;
}
