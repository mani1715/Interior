'use client';

import React from 'react';
import { Search, Bell } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';

export interface AppTopBarProps {
  title?: string;
  breadcrumbs?: React.ReactNode;
  onSearchChange?: (val: string) => void;
  user?: {
    name: string;
    avatarUrl?: string;
  };
  actions?: React.ReactNode;
}

export function AppTopBar({
  title,
  breadcrumbs,
  user = { name: 'Vikram Sharma' },
  actions,
}: AppTopBarProps) {
  return (
    <header className="sticky top-0 z-30 h-16 bg-[var(--surface)] border-b border-[var(--border)] px-4 sm:px-6 flex items-center justify-between gap-4">
      <div className="flex-1 flex items-center gap-4 min-w-0">
        {breadcrumbs ? (
          <div className="hidden sm:block truncate">{breadcrumbs}</div>
        ) : title ? (
          <h1 className="font-serif text-lg sm:text-xl font-semibold text-[var(--foreground)] truncate">
            {title}
          </h1>
        ) : null}
      </div>

      <div className="flex items-center gap-3">
        {/* Search quick button */}
        <button
          type="button"
          aria-label="Search"
          className="p-2 rounded-lg text-[var(--muted)] hover:text-[var(--foreground)] hover:bg-[var(--surface-raised)] transition-colors min-h-[40px] min-w-[40px] flex items-center justify-center"
        >
          <Search className="w-4 h-4" />
        </button>

        {/* Notifications */}
        <button
          type="button"
          aria-label="Notifications"
          className="relative p-2 rounded-lg text-[var(--muted)] hover:text-[var(--foreground)] hover:bg-[var(--surface-raised)] transition-colors min-h-[40px] min-w-[40px] flex items-center justify-center"
        >
          <Bell className="w-4 h-4" />
          <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-[var(--brand)]" />
        </button>

        {actions && <div className="flex items-center gap-2">{actions}</div>}

        <Avatar name={user.name} src={user.avatarUrl} size="sm" />
      </div>
    </header>
  );
}
