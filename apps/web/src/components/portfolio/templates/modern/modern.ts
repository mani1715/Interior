import type { CSSProperties } from 'react';
import type { PortfolioTemplateProps } from '@/lib/portfolio/template-contract';
import type { PreviewContactDto, PreviewSectionDto } from '@/lib/portfolio/types';

export const text = (v: unknown) => typeof v === 'string' ? v.trim() : '';
export const objects = (v: unknown): Record<string, unknown>[] => Array.isArray(v) ? v.filter(x => !!x && typeof x === 'object' && !Array.isArray(x)) as Record<string, unknown>[] : [];
export function safeUrl(v: unknown, anchors?: Set<string>) {
  const url = text(v); if (!url || /[\s\u0000-\u001f\u007f]/.test(url)) return;
  if (url.startsWith('#')) return anchors?.has(url.slice(1)) ? url : undefined;
  if (/^mailto:[^@?]+@[^@?]+\.[^@?]+$/i.test(url) || /^tel:\+?[\d()-]+$/i.test(url)) return url;
  try { const parsed = new URL(url); return parsed.protocol === 'https:' && !parsed.username && !parsed.password ? parsed.href : undefined; } catch { return; }
}
export function mediaUrl(v: unknown) { const url = safeUrl(v); return url?.startsWith('https:') ? url : undefined; }
export function contacts(list: PreviewContactDto[]) {
  return list.flatMap(({ kind, contactValue }) => {
    const value = text(contactValue); let href: string | undefined; let label = value;
    if (kind === 'EMAIL' && /^[^\s@?]+@[^\s@?]+\.[^\s@?]+$/.test(value)) { href = `mailto:${value}`; label = value; }
    else if ((kind === 'PHONE' || kind === 'WHATSAPP') && /^\+?[\d ()-]{7,24}$/.test(value)) { const n = value.replace(/[ ()-]/g, ''); href = kind === 'PHONE' ? `tel:${n}` : `https://wa.me/${n.replace(/^\+/, '')}`; label = value; }
    else if (kind === 'WEBSITE' || kind === 'INSTAGRAM') { href = mediaUrl(value); label = kind === 'INSTAGRAM' ? 'Instagram' : value; }
    return href ? [{ href, label }] : [];
  });
}
export function usable(section: PreviewSectionDto, p: PortfolioTemplateProps) {
  const c = section.content || {};
  switch (section.sectionType) {
    case 'HERO': return true;
    case 'ABOUT': return !!(text(c.narrativeOverride) || text(p.bio) || text(c.philosophyOverride) || text(p.designPhilosophy) || p.yearsOfExperience != null);
    case 'SERVICES': return p.canonicalServices.some(x => text(x.serviceName));
    case 'FEATURED_PROJECTS': case 'PROJECT_GRID': return false;
    case 'BEFORE_AFTER': return objects(c.pairs).some(x => mediaUrl(x.beforeImageUrl) && mediaUrl(x.afterImageUrl));
    case 'BEFORE_AI_REALITY': return objects(c.pairs).some(x => mediaUrl(x.conceptImageUrl) && mediaUrl(x.realityImageUrl));
    case 'DESIGN_PROCESS': return objects(c.steps).some(x => text(x.title) || text(x.desc));
    case 'TESTIMONIALS': return objects(c.items).some(x => text(x.quote) && text(x.clientName));
    case 'TEAM': return objects(c.members).some(x => text(x.name));
    case 'AWARDS': return objects(c.items).some(x => text(x.title));
    case 'PRESS': return objects(c.articles).some(x => text(x.title));
    case 'SERVICE_AREAS': return p.canonicalServiceAreas.some(x => text(x.cityName) || text(x.locality));
    case 'FAQ': return objects(c.items).some(x => text(x.question) && text(x.answer));
    case 'CONTACT': return contacts(p.publicContacts).length > 0;
    case 'CTA': return !!(text(c.headline) && text(c.buttonText) && text(c.buttonLink));
    case 'VIDEO': return !!mediaUrl(c.videoUrl);
    case 'CUSTOM_NOTE': return !!text(c.body);
    default: return false;
  }
}
const hex = (v: unknown, fallback: string) => /^#[0-9a-f]{6}$/i.test(text(v)) ? text(v) : fallback;
function luminance(color: string) { const rgb = [1,3,5].map(i => parseInt(color.slice(i,i+2),16)/255).map(n => n <= .04045 ? n/12.92 : ((n+.055)/1.055)**2.4); return rgb[0]*.2126+rgb[1]*.7152+rgb[2]*.0722; }
function contrast(a: string, b: string) { const x=luminance(a), y=luminance(b); return (Math.max(x,y)+.05)/(Math.min(x,y)+.05); }
export function themeStyle(p: PortfolioTemplateProps): CSSProperties {
  const surface = hex(p.secondaryColor, '#F6F7F5'); const ink = hex(p.primaryColor, '#182126'); const accent = hex(p.accentColor, '#537A8A');
  return { '--modern-surface': surface, '--modern-ink': contrast(ink,surface) >= 4.5 ? ink : '#182126', '--modern-accent': accent, '--modern-accent-ink': contrast('#182126',accent) >= 4.5 ? '#182126' : '#FFFFFF' } as CSSProperties;
}
