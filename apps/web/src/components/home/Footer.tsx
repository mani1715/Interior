import React from 'react';
import Link from 'next/link';

export function Footer() {
  const footerSections = [
    {
      title: 'Discover',
      links: [
        { label: 'All Projects', href: '#project-inspiration' },
        { label: 'TV Units & Consoles', href: '#project-inspiration' },
        { label: 'Modular Kitchens', href: '#project-inspiration' },
        { label: 'Master Bedroom Suites', href: '#project-inspiration' },
        { label: 'Pooja Mandir Units', href: '#project-inspiration' },
      ],
    },
    {
      title: 'For Professionals',
      links: [
        { label: 'Create Portfolio', href: '#portfolio-builder' },
        { label: 'AI Spatial Visualizer', href: '#ai-visualizer' },
        { label: 'Watermark Protection', href: '#portfolio-builder' },
        { label: 'Studio Showcase', href: '#discover-projects' },
      ],
    },
    {
      title: 'Product',
      links: [
        { label: 'How It Works', href: '#how-it-works' },
        { label: 'Google Discoverability', href: '#seo-discoverability' },
        { label: 'Transformation Craft', href: '#transformation-experience' },
        { label: 'Design System (Internal)', href: '/design-system' },
      ],
    },
    {
      title: 'Standards & Legal',
      links: [
        { label: 'Privacy Policy (Planned)', href: undefined },
        { label: 'Terms of Service (Planned)', href: undefined },
        { label: 'AI Disclaimer Notice', href: '#ai-visualizer' },
        { label: 'Copyright & Protection', href: '#portfolio-builder' },
      ],
    },
  ];

  return (
    <footer className="w-full bg-[var(--surface)] border-t border-[var(--border)] pt-12 pb-16 sm:pt-16 sm:pb-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 md:grid-cols-5 gap-8 mb-12">
          {/* Brand Info */}
          <div className="col-span-2 md:col-span-1 space-y-3">
            <Link href="/" className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-[var(--brand)] text-[var(--charcoal)] flex items-center justify-center font-serif font-bold text-base shadow-sm">
                E
              </div>
              <span className="font-serif text-base font-semibold tracking-wider text-[var(--foreground)]">
                ELÉGANCE
              </span>
            </Link>
            <p className="text-xs text-[var(--muted)] leading-relaxed">
              The portfolio, discovery, and AI visualization platform engineered for interior designers, studios, and architects across India.
            </p>
          </div>

          {/* Nav Groups */}
          {footerSections.map((group, idx) => (
            <div key={idx} className="space-y-3">
              <h5 className="font-serif text-xs font-semibold uppercase tracking-wider text-[var(--foreground)]">
                {group.title}
              </h5>
              <ul className="space-y-2">
                {group.links.map((link, lIdx) => (
                  <li key={lIdx}>
                    {link.href ? (
                      <Link
                        href={link.href}
                        className="text-xs text-[var(--muted)] hover:text-[var(--brand)] transition-colors"
                      >
                        {link.label}
                      </Link>
                    ) : (
                      <span className="text-xs text-[var(--muted)] opacity-70 cursor-default">
                        {link.label}
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* Bottom Bar */}
        <div className="pt-8 border-t border-[var(--border)] flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-[var(--muted)]">
          <p>© 2026 Elégance Interior Platform. All architectural rights reserved.</p>
          <p className="font-mono text-[11px]">
            Mobile-First • Locked Palette • Tenant Isolation Architecture
          </p>
        </div>
      </div>
    </footer>
  );
}
