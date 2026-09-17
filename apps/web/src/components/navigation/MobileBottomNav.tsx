'use client';

import React from 'react';
import Link from 'next/link';
import { Home, FolderKanban, Plus, Sparkles, MoreHorizontal } from 'lucide-react';

export interface MobileBottomNavProps {
  currentPath?: string;
  onCreateClick?: () => void;
}

export function MobileBottomNav({
  currentPath = '/',
  onCreateClick,
}: MobileBottomNavProps) {
  const navItems = [
    { label: 'Home', href: '/', icon: Home },
    { label: 'Projects', href: '/projects', icon: FolderKanban },
    { label: 'Create', href: '#create', icon: Plus, isAction: true },
    { label: 'AI Studio', href: '/ai-studio', icon: Sparkles },
    { label: 'More', href: '/more', icon: MoreHorizontal },
  ];

  return (
    <nav
      aria-label="Mobile Navigation Bar"
      className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[var(--surface)] border-t border-[var(--border)] pb-safe transition-colors"
    >
      <div className="grid grid-cols-5 h-16 max-w-lg mx-auto">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentPath === item.href;

          if (item.isAction) {
            return (
              <div key={item.label} className="flex items-center justify-center">
                <button
                  type="button"
                  onClick={onCreateClick}
                  aria-label="Create new project or design"
                  className="w-11 h-11 rounded-full bg-[var(--brand)] text-[var(--charcoal)] flex items-center justify-center shadow-lg -mt-4 active:scale-95 transition-transform"
                >
                  <Icon className="w-5 h-5 stroke-[2.5]" />
                </button>
              </div>
            );
          }

          return (
            <Link
              key={item.label}
              href={item.href}
              className={`flex flex-col items-center justify-center py-1 transition-colors min-h-[44px] ${
                isActive
                  ? 'text-[var(--brand)] font-semibold'
                  : 'text-[var(--muted)] hover:text-[var(--foreground)]'
              }`}
            >
              <Icon className={`w-5 h-5 mb-0.5 ${isActive ? 'stroke-[2.2]' : 'stroke-[1.6]'}`} />
              <span className="text-[10px] tracking-tight">{item.label}</span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
