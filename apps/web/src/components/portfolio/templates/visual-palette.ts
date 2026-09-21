import type { CSSProperties } from 'react';
import type { PortfolioTemplateProps } from '@/lib/portfolio/template-contract';

export function luminance(hex: string) {
  const rgb = [1, 3, 5].map(i => parseInt(hex.slice(i, i + 2), 16) / 255)
    .map(v => v <= .04045 ? v / 12.92 : ((v + .055) / 1.055) ** 2.4);
  return rgb[0] * .2126 + rgb[1] * .7152 + rgb[2] * .0722;
}
export function contrast(a: string, b: string) {
  const x = luminance(a), y = luminance(b);
  return (Math.max(x, y) + .05) / (Math.min(x, y) + .05);
}
function readable(candidate: string, surface: string) {
  if (contrast(candidate, surface) >= 4.5) return candidate;
  return contrast('#FFFFFF', surface) > contrast('#111111', surface) ? '#FFFFFF' : '#111111';
}
const color = (value: unknown, fallback: string) => typeof value === 'string' && /^#[0-9a-f]{6}$/i.test(value) ? value : fallback;

/** Retain configured surfaces/accent; derive accessible text independently. */
export function visualPalette(p: PortfolioTemplateProps, defaults: [string, string, string]): CSSProperties {
  const surface = color(p.secondaryColor, defaults[0]);
  const ink = readable(color(p.primaryColor, defaults[1]), surface);
  const accent = color(p.accentColor, defaults[2]);
  return {
    '--portfolio-surface': surface, '--portfolio-ink': ink,
    '--portfolio-accent': accent, '--portfolio-accent-text': readable(accent, surface),
    '--portfolio-on-accent': readable(ink, accent),
  } as CSSProperties;
}
