'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Menu, X, Sparkles, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/Button';

export interface NavLinkItem {
  label: string;
  href: string;
}

const defaultNavLinks: NavLinkItem[] = [
  { label: 'Projects', href: '/projects' },
  { label: 'Professionals', href: '/professionals' },
  { label: 'Categories', href: '/categories/tv-units' },
  { label: 'AI Visualizer', href: '/#ai-visualizer' },
];

export interface PublicHeaderProps {
  navLinks?: NavLinkItem[];
  currentPath?: string;
  onSignInClick?: () => void;
  onGetStartedClick?: () => void;
}

export function PublicHeader({
  navLinks = defaultNavLinks,
  currentPath = '/',
  onSignInClick,
  onGetStartedClick,
}: PublicHeaderProps) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <header className="sticky top-0 z-40 w-full bg-[var(--surface)] border-b border-[var(--border)] transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Logo */}
        <Link href="/" className="flex items-center gap-2.5 group">
          <div className="w-8 h-8 rounded-lg bg-[var(--brand)] text-[var(--charcoal)] flex items-center justify-center font-serif font-bold text-base shadow-sm group-hover:scale-105 transition-transform">
            E
          </div>
          <div className="flex flex-col">
            <span className="font-serif text-base font-semibold tracking-wider text-[var(--foreground)]">
              ELÉGANCE
            </span>
            <span className="text-[9px] uppercase tracking-widest text-[var(--brand)] font-medium -mt-1">
              Interior AI
            </span>
          </div>
        </Link>

        {/* Desktop Navigation */}
        <nav className="hidden md:flex items-center gap-6" aria-label="Main Navigation">
          {navLinks.map((link) => {
            const isActive = currentPath === link.href;
            return (
              <Link
                key={link.href}
                href={link.href}
                className={`text-xs font-medium uppercase tracking-wider transition-colors hover:text-[var(--brand)] py-2 ${
                  isActive ? 'text-[var(--brand)] font-semibold' : 'text-[var(--muted)]'
                }`}
              >
                {link.label}
              </Link>
            );
          })}
        </nav>

        {/* Desktop Actions */}
        <div className="hidden md:flex items-center gap-3">
          <Link href="#portfolio-builder">
            <Button variant="ghost" size="sm" onClick={onSignInClick}>
              Sign In
            </Button>
          </Link>
          <Link href="#portfolio-builder">
            <Button
              variant="primary"
              size="sm"
              onClick={onGetStartedClick}
              rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
            >
              Get Started
            </Button>
          </Link>
        </div>

        {/* Mobile Hamburger Toggle */}
        <div className="flex items-center md:hidden">
          <button
            type="button"
            onClick={() => setMobileMenuOpen((prev) => !prev)}
            aria-label={mobileMenuOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={mobileMenuOpen}
            className="p-2 rounded-lg text-[var(--foreground)] hover:bg-[var(--surface-raised)] transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center"
          >
            {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="md:hidden fixed inset-x-0 top-16 bottom-0 z-40 bg-[var(--surface)] border-b border-[var(--border)] overflow-y-auto p-5 animate-fade-in flex flex-col justify-between">
          <nav className="flex flex-col gap-3" aria-label="Mobile Navigation">
            {navLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                onClick={() => setMobileMenuOpen(false)}
                className="px-4 py-3 rounded-xl text-base font-medium text-[var(--foreground)] hover:bg-[var(--surface-raised)] active:bg-[var(--surface-raised)] transition-colors flex items-center justify-between"
              >
                <span>{link.label}</span>
                <ArrowRight className="w-4 h-4 text-[var(--muted)]" />
              </Link>
            ))}
            <div className="pt-2">
              <Link
                href="#ai-visualizer"
                onClick={() => setMobileMenuOpen(false)}
                className="px-4 py-3 rounded-xl bg-[var(--brand-muted)] border border-[var(--brand)]/20 text-[var(--brand)] text-base font-medium flex items-center gap-2.5"
              >
                <Sparkles className="w-5 h-5 flex-shrink-0" />
                <span>Try AI Visualizer</span>
              </Link>
            </div>
          </nav>

          <div className="pt-6 border-t border-[var(--border)] flex flex-col gap-3 mb-6">
            <Link href="#portfolio-builder" onClick={() => setMobileMenuOpen(false)} className="w-full">
              <Button
                variant="outline"
                size="lg"
                className="w-full justify-center"
                onClick={onSignInClick}
              >
                Sign In
              </Button>
            </Link>
            <Link href="#portfolio-builder" onClick={() => setMobileMenuOpen(false)} className="w-full">
              <Button
                variant="primary"
                size="lg"
                className="w-full justify-center"
                onClick={onGetStartedClick}
              >
                Get Started Free
              </Button>
            </Link>
          </div>
        </div>
      )}
    </header>
  );
}
