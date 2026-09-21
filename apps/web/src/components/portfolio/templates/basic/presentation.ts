import type { CSSProperties } from 'react';
import type { PortfolioTemplateProps } from '@/lib/portfolio/template-contract';
import type { PreviewContactDto, PreviewSectionDto } from '@/lib/portfolio/types';

export const text = (value: unknown): string => typeof value === 'string' ? value.trim() : '';
export const items = (value: unknown): Record<string, unknown>[] => Array.isArray(value)
  ? value.filter((item): item is Record<string, unknown> => !!item && typeof item === 'object' && !Array.isArray(item)) : [];

// Presentation defense in depth: never turn arbitrary section text into executable URLs.
export function safeUrl(value: unknown, anchors?: Set<string>): string | undefined {
  const url = text(value);
  if (!url || /[\s\u0000-\u001f\u007f]/.test(url)) return;
  if (url.startsWith('#')) return anchors?.has(url.slice(1)) ? url : undefined;
  if (/^mailto:[^@?]+@[^@?]+\.[^@?]+$/i.test(url) || /^tel:\+?[\d()-]+$/i.test(url)) return url;
  try {
    const parsed = new URL(url);
    if (parsed.protocol === 'https:' && !parsed.username && !parsed.password) return parsed.href;
  } catch { /* Missing or malformed optional links are omitted. */ }
}

export function mediaUrl(value: unknown): string | undefined {
  const url = safeUrl(value);
  return url?.startsWith('https:') ? url : undefined;
}

export function contactLinks(contacts: PreviewContactDto[]) {
  return contacts.flatMap(({ kind, contactValue }) => {
    const value = text(contactValue);
    let href: string | undefined;
    let label = value;
    if (kind === 'EMAIL' && /^[^\s@?]+@[^\s@?]+\.[^\s@?]+$/.test(value)) {
      href = safeUrl(`mailto:${value}`); label = `Email ${value}`;
    } else if ((kind === 'PHONE' || kind === 'WHATSAPP') && /^\+?[\d ()-]{7,24}$/.test(value)) {
      const number = value.replace(/[ ()-]/g, '');
      href = kind === 'PHONE' ? safeUrl(`tel:${number}`) : `https://wa.me/${number.replace(/^\+/, '')}`;
      label = `${kind === 'PHONE' ? 'Call' : 'WhatsApp'} ${value}`;
    } else if (kind === 'WEBSITE' || kind === 'INSTAGRAM') {
      href = mediaUrl(value); label = kind === 'INSTAGRAM' ? 'Instagram' : value;
    }
    return href ? [{ href, label }] : [];
  });
}

export function usable(section: PreviewSectionDto, props: PortfolioTemplateProps): boolean {
  const c = section.content || {};
  switch (section.sectionType) {
    case 'HERO': return true;
    case 'ABOUT': return !!(text(c.narrativeOverride) || text(props.bio) || text(c.philosophyOverride) || text(props.designPhilosophy) || props.yearsOfExperience != null || props.canonicalSpecialties.length);
    case 'SERVICES': return props.canonicalServices.some(s => text(s.serviceName) !== '');
    // No project DTO/media resolution contract exists yet. IDs alone cannot become project cards.
    case 'FEATURED_PROJECTS': case 'PROJECT_GRID': return false;
    case 'BEFORE_AFTER': return items(c.pairs).some(p => mediaUrl(p.beforeImageUrl) && mediaUrl(p.afterImageUrl));
    case 'BEFORE_AI_REALITY': return items(c.pairs).some(p => mediaUrl(p.conceptImageUrl) && mediaUrl(p.realityImageUrl));
    case 'DESIGN_PROCESS': return items(c.steps).some(p => text(p.title) || text(p.desc));
    case 'TESTIMONIALS': return items(c.items).some(p => text(p.quote) && text(p.clientName));
    case 'TEAM': return items(c.members).some(p => text(p.name));
    case 'AWARDS': return items(c.items).some(p => text(p.title));
    case 'PRESS': return items(c.articles).some(p => text(p.title));
    case 'SERVICE_AREAS': return props.canonicalServiceAreas.some(p => text(p.cityName) || text(p.locality));
    case 'FAQ': return items(c.items).some(p => text(p.question) && text(p.answer));
    case 'CONTACT': return contactLinks(props.publicContacts).length > 0;
    case 'CTA': return !!(text(c.headline) && text(c.buttonText) && text(c.buttonLink));
    case 'VIDEO': return !!mediaUrl(c.videoUrl);
    case 'CUSTOM_NOTE': return !!text(c.body);
    default: return false;
  }
}

const hex = (value: unknown, fallback: string) => /^#[0-9a-f]{6}$/i.test(text(value)) ? text(value) : fallback;
function luminance(color: string) {
  const rgb = [1, 3, 5].map(i => parseInt(color.slice(i, i + 2), 16) / 255)
    .map(n => n <= .04045 ? n / 12.92 : ((n + .055) / 1.055) ** 2.4);
  return rgb[0] * .2126 + rgb[1] * .7152 + rgb[2] * .0722;
}
export function contrast(a: string, b: string) {
  const x = luminance(a), y = luminance(b);
  return (Math.max(x, y) + .05) / (Math.min(x, y) + .05);
}
export function themeStyle(props: PortfolioTemplateProps): CSSProperties {
  const surface = hex(props.secondaryColor, '#FAF8F5');
  const requestedInk = hex(props.primaryColor, '#1F1F1F');
  const ink = contrast(requestedInk, surface) >= 4.5 ? requestedInk
    : contrast('#000000', surface) >= 4.5 ? '#000000' : '#FFFFFF';
  const accent = hex(props.accentColor, '#B88A5A');
  const accentInk = contrast('#1F1F1F', accent) >= 4.5 ? '#1F1F1F'
    : contrast('#000000', accent) >= 4.5 ? '#000000' : '#FFFFFF';
  return { '--basic-surface': surface, '--basic-ink': ink, '--basic-accent': accent, '--basic-accent-ink': accentInk } as CSSProperties;
}
