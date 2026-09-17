'use client';

import React from 'react';
import Link from 'next/link';
import {
  LayoutDashboard,
  FolderKanban,
  Sparkles,
  Users,
  Settings,
  HelpCircle,
  LogOut,
  ChevronRight,
} from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';

export interface SidebarItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: string | number;
}

const defaultNavItems: SidebarItem[] = [
  { label: 'Dashboard', href: '/app/dashboard', icon: LayoutDashboard },
  { label: 'Projects', href: '/app/projects', icon: FolderKanban, badge: '12' },
  { label: 'AI Studio', href: '/app/ai-studio', icon: Sparkles },
  { label: 'Clients & Leads', href: '/app/clients', icon: Users, badge: '3' },
  { label: 'Settings', href: '/app/settings', icon: Settings },
];

export interface DesktopSidebarProps {
  currentPath?: string;
  items?: SidebarItem[];
  user?: {
    name: string;
    email: string;
    avatarUrl?: string;
    role?: string;
  };
  onSignOut?: () => void;
  className?: string;
}

export function DesktopSidebar({
  currentPath = '/app/dashboard',
  items = defaultNavItems,
  user = {
    name: 'Vikram Sharma',
    email: 'vikram@studiodesign.in',
    role: 'Lead Architect',
  },
  onSignOut,
  className = '',
}: DesktopSidebarProps) {
  return (
    <aside
      className={`hidden md:flex flex-col w-64 h-screen sticky top-0 bg-[var(--surface)] border-r border-[var(--border)] p-4 justify-between select-none ${className}`}
      aria-label="App Sidebar"
    >
      <div className="space-y-6">
        {/* Brand Header */}
        <Link href="/app/dashboard" className="flex items-center gap-2.5 px-2">
          <div className="w-8 h-8 rounded-lg bg-[var(--brand)] text-[var(--charcoal)] flex items-center justify-center font-serif font-bold text-base shadow-sm">
            E
          </div>
          <div className="flex flex-col">
            <span className="font-serif text-base font-semibold tracking-wider text-[var(--foreground)]">
              ELÉGANCE
            </span>
            <span className="text-[9px] uppercase tracking-widest text-[var(--brand)] font-medium -mt-1">
              Studio Workspace
            </span>
          </div>
        </Link>

        {/* Nav Links */}
        <nav className="space-y-1" aria-label="Sidebar Menu">
          {items.map((item) => {
            const Icon = item.icon;
            const isActive = currentPath === item.href;

            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-medium transition-colors ${
                  isActive
                    ? 'bg-[var(--brand-muted)] text-[var(--brand)] font-semibold'
                    : 'text-[var(--muted)] hover:text-[var(--foreground)] hover:bg-[var(--surface-raised)]'
                }`}
              >
                <div className="flex items-center gap-3">
                  <Icon className={`w-4 h-4 ${isActive ? 'stroke-[2.2]' : 'stroke-[1.6]'}`} />
                  <span>{item.label}</span>
                </div>
                {item.badge && (
                  <span
                    className={`px-1.5 py-0.5 rounded-full text-[10px] font-mono ${
                      isActive
                        ? 'bg-[var(--brand)] text-[var(--charcoal)] font-bold'
                        : 'bg-[var(--surface-raised)] text-[var(--muted)] border border-[var(--border)]'
                    }`}
                  >
                    {item.badge}
                  </span>
                )}
              </Link>
            );
          })}
        </nav>
      </div>

      {/* Footer Profile & Logout */}
      <div className="space-y-3 pt-4 border-t border-[var(--border)]">
        <Link
          href="/app/profile"
          className="flex items-center gap-3 p-2 rounded-xl hover:bg-[var(--surface-raised)] transition-colors group"
        >
          <Avatar name={user.name} src={user.avatarUrl} size="sm" />
          <div className="flex-1 truncate">
            <p className="text-xs font-medium text-[var(--foreground)] truncate">{user.name}</p>
            <p className="text-[10px] text-[var(--muted)] truncate">{user.role}</p>
          </div>
          <ChevronRight className="w-3.5 h-3.5 text-[var(--muted)] group-hover:text-[var(--foreground)] transition-colors" />
        </Link>

        <div className="flex items-center justify-between px-2 pt-1 text-[11px] text-[var(--muted)]">
          <Link href="/help" className="flex items-center gap-1.5 hover:text-[var(--foreground)] transition-colors">
            <HelpCircle className="w-3.5 h-3.5" />
            <span>Help</span>
          </Link>
          <button
            type="button"
            onClick={onSignOut}
            className="flex items-center gap-1.5 hover:text-[var(--danger)] transition-colors"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>Sign Out</span>
          </button>
        </div>
      </div>
    </aside>
  );
}
