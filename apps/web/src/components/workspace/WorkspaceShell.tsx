'use client';

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import {
  Home,
  Layout,
  FolderKanban,
  Image,
  Sparkles,
  Users,
  Search,
  BarChart3,
  Bell,
  Building2,
  CreditCard,
  User,
  LogOut,
  MoreHorizontal,
  X,
  ChevronRight,
  ShieldAlert,
  ArrowRight,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/auth-context';
import { WorkspaceSummary } from '@/lib/workspace/types';
import { fetchWorkspaceSummary } from '@/lib/workspace/api';

interface WorkspaceShellProps {
  children: React.ReactNode;
}

interface NavItem {
  id: string;
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  roleRestricted?: string[]; // e.g. owner-only later
}

const PRIMARY_NAV_ITEMS: NavItem[] = [
  { id: 'home', label: 'Home', href: '/workspace', icon: Home },
  { id: 'portfolio', label: 'Portfolio', href: '/workspace/portfolio', icon: Layout },
  { id: 'projects', label: 'Projects', href: '/workspace/projects', icon: FolderKanban },
  { id: 'media', label: 'Media', href: '/workspace/media', icon: Image },
  { id: 'ai', label: 'AI Studio', href: '/workspace/ai', icon: Sparkles },
  { id: 'leads', label: 'Leads', href: '/workspace/leads', icon: Users },
  { id: 'seo', label: 'SEO Center', href: '/workspace/seo', icon: Search },
  { id: 'analytics', label: 'Analytics', href: '/workspace/analytics', icon: BarChart3 },
  { id: 'notifications', label: 'Notifications', href: '/workspace/notifications', icon: Bell },
];

const SECONDARY_NAV_ITEMS: NavItem[] = [
  { id: 'business', label: 'Business Profile', href: '/workspace/business', icon: Building2 },
  { id: 'billing', label: 'Billing & Plans', href: '/workspace/billing', icon: CreditCard },
  { id: 'account', label: 'Account Profile', href: '/account', icon: User },
];

// 5 bottom nav items for mobile (360-430px)
const MOBILE_BOTTOM_NAV = [
  { id: 'home', label: 'Home', href: '/workspace', icon: Home },
  { id: 'portfolio', label: 'Portfolio', href: '/workspace/portfolio', icon: Layout },
  { id: 'projects', label: 'Projects', href: '/workspace/projects', icon: FolderKanban },
  { id: 'ai', label: 'AI Studio', href: '/workspace/ai', icon: Sparkles },
];

export function WorkspaceShell({ children }: WorkspaceShellProps) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, isLoading: authLoading, isAuthenticated, logout } = useAuth();

  const [summary, setSummary] = useState<WorkspaceSummary | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [moreMenuOpen, setMoreMenuOpen] = useState<boolean>(false);
  const [selectedStudioId, setSelectedStudioId] = useState<string | undefined>(undefined);

  const bottomSheetRef = useRef<HTMLDivElement>(null);

  // Authentication check & Data fetching
  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push('/sign-in?returnUrl=/workspace');
      return;
    }

    if (!authLoading && user) {
      const isDesigner = user.roles.includes('DESIGNER') || user.roles.includes('DESIGNER_TEAM') || user.roles.includes('SUPER_ADMIN');
      if (!isDesigner) {
        setLoading(false);
        return;
      }

      setLoading(true);
      fetchWorkspaceSummary(selectedStudioId)
        .then((data) => {
          setSummary(data);
          setError(null);
        })
        .catch((err) => {
          setError(err.message || 'Failed to load workspace summary');
        })
        .finally(() => {
          setLoading(false);
        });
    }
  }, [authLoading, isAuthenticated, user, router, selectedStudioId]);

  // Handle ESC key for bottom sheet
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && moreMenuOpen) {
        setMoreMenuOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [moreMenuOpen]);

  if (authLoading || (loading && !summary && !error)) {
    return (
      <div className="min-h-screen bg-[var(--surface-sunken,#fbfaf8)] flex flex-col items-center justify-center p-4">
        <div className="w-10 h-10 border-4 border-bronze-200 border-t-bronze-600 rounded-full animate-spin mb-4" />
        <p className="text-sm text-charcoal-600 font-medium">Loading professional workspace...</p>
      </div>
    );
  }

  // Account Suspended Screen
  if (user && user.status === 'SUSPENDED') {
    return (
      <div className="min-h-screen bg-[var(--surface-sunken,#fbfaf8)] flex items-center justify-center p-6">
        <div className="max-w-md w-full bg-white border border-terracotta-200 rounded-2xl p-8 text-center shadow-sm">
          <div className="w-12 h-12 bg-terracotta-50 text-terracotta-700 rounded-full flex items-center justify-center mx-auto mb-4">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <h1 className="font-serif text-2xl text-charcoal-900 mb-2">Account Restricted</h1>
          <p className="text-sm text-charcoal-600 mb-6">
            Your professional account is currently suspended. Workspace access is restricted. Please contact platform support for resolution.
          </p>
          <button
            onClick={() => logout()}
            className="w-full py-2.5 px-4 rounded-xl bg-charcoal-900 text-white text-sm font-medium hover:bg-charcoal-800 transition-colors"
          >
            Sign Out
          </button>
        </div>
      </div>
    );
  }

  // Customer without onboarding guidance screen
  const isDesigner = user && (user.roles.includes('DESIGNER') || user.roles.includes('DESIGNER_TEAM') || user.roles.includes('SUPER_ADMIN'));
  if (user && !isDesigner) {
    return (
      <div className="min-h-screen bg-[var(--surface-sunken,#fbfaf8)] flex items-center justify-center p-6">
        <div className="max-w-lg w-full bg-white border border-sand-200 rounded-2xl p-8 text-center shadow-sm">
          <div className="w-12 h-12 bg-bronze-50 text-bronze-800 rounded-full flex items-center justify-center mx-auto mb-4">
            <Building2 className="w-6 h-6" />
          </div>
          <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block mb-1">
            Professional Access Required
          </span>
          <h1 className="font-serif text-2xl text-charcoal-900 mb-3">
            Register Your Professional Studio
          </h1>
          <p className="text-sm text-charcoal-600 mb-6 leading-relaxed">
            Your account ({user.email || user.displayName}) is currently registered as a baseline client account. To access the Professional Workspace, please complete the professional registration flow.
          </p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Link
              href="/onboarding/professional"
              className="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-bronze-700 text-white text-sm font-medium hover:bg-bronze-800 transition-colors"
            >
              <span>Complete Professional Onboarding</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
            <Link
              href="/account"
              className="inline-flex items-center justify-center px-4 py-2.5 rounded-xl border border-sand-300 text-charcoal-700 text-sm font-medium hover:bg-sand-50 transition-colors"
            >
              Account Profile
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const studio = summary?.studio;

  return (
    <div className="min-h-screen bg-[var(--surface-sunken,#fbfaf8)] flex flex-col md:flex-row">
      {/* ============================================================ */}
      {/* DESKTOP SIDEBAR (Visible at md / 768px+)                     */}
      {/* ============================================================ */}
      <aside
        className="hidden md:flex flex-col w-64 lg:w-72 bg-white border-r border-sand-200 sticky top-0 h-screen overflow-y-auto z-30"
        aria-label="Professional Workspace Navigation"
      >
        {/* Brand Header */}
        <div className="p-5 border-b border-sand-200">
          <Link href="/workspace" className="flex items-center gap-2.5 group">
            <div className="w-8 h-8 rounded-lg bg-bronze-700 text-white flex items-center justify-center font-serif font-bold text-base shadow-sm">
              E
            </div>
            <div className="flex flex-col">
              <span className="font-serif text-base font-semibold tracking-wider text-charcoal-900">
                ELÉGANCE
              </span>
              <span className="text-[9px] uppercase tracking-widest text-bronze-700 font-semibold -mt-1">
                Workspace
              </span>
            </div>
          </Link>
        </div>

        {/* Studio Tenancy Identity Card */}
        <div className="p-4 mx-3 my-3 bg-sand-50 border border-sand-200 rounded-xl">
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0">
              <h2 className="text-sm font-semibold text-charcoal-900 truncate">
                {studio?.name || 'My Studio'}
              </h2>
              <p className="text-[11px] text-charcoal-500 truncate mt-0.5">
                {studio?.professionalTitle || studio?.professionalType?.replace(/_/g, ' ') || 'Interior Professional'}
              </p>
            </div>
            <span className="flex-shrink-0 text-[10px] font-mono font-semibold px-2 py-0.5 rounded bg-sand-200 text-charcoal-800">
              {studio?.role || 'OWNER'}
            </span>
          </div>

          {/* Multiple studio selector if available */}
          {summary && summary.availableStudios && summary.availableStudios.length > 1 && (
            <div className="mt-3 pt-2.5 border-t border-sand-200">
              <label htmlFor="studio-select" className="text-[10px] font-medium text-charcoal-500 uppercase block mb-1">
                Switch Studio
              </label>
              <select
                id="studio-select"
                value={selectedStudioId || studio?.id || ''}
                onChange={(e) => setSelectedStudioId(e.target.value)}
                className="w-full text-xs bg-white border border-sand-300 rounded-lg px-2 py-1 text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-bronze-500"
              >
                {summary.availableStudios.map((s) => (
                  <option key={s.studioId} value={s.studioId}>
                    {s.studioName} ({s.role})
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        {/* Primary Navigation Links */}
        <nav className="flex-1 px-3 space-y-1" aria-label="Primary Workspace Modules">
          {PRIMARY_NAV_ITEMS.map((item) => {
            const isActive = pathname === item.href;
            const Icon = item.icon;
            return (
              <Link
                key={item.id}
                href={item.href}
                aria-current={isActive ? 'page' : undefined}
                className={`flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium transition-colors ${
                  isActive
                    ? 'bg-sand-200/90 text-charcoal-900 font-semibold shadow-2xs'
                    : 'text-charcoal-600 hover:bg-sand-100 hover:text-charcoal-900'
                }`}
              >
                <Icon className={`w-4 h-4 flex-shrink-0 ${isActive ? 'text-bronze-700' : 'text-charcoal-500'}`} />
                <span className="truncate">{item.label}</span>
              </Link>
            );
          })}
        </nav>

        {/* Secondary Navigation Group (Business Profile, Account, Sign Out) */}
        <div className="p-3 border-t border-sand-200 space-y-1">
          {SECONDARY_NAV_ITEMS.map((item) => {
            const isActive = pathname === item.href;
            const Icon = item.icon;
            return (
              <Link
                key={item.id}
                href={item.href}
                aria-current={isActive ? 'page' : undefined}
                className={`flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium transition-colors ${
                  isActive
                    ? 'bg-sand-200/90 text-charcoal-900 font-semibold'
                    : 'text-charcoal-600 hover:bg-sand-100 hover:text-charcoal-900'
                }`}
              >
                <Icon className={`w-4 h-4 flex-shrink-0 ${isActive ? 'text-bronze-700' : 'text-charcoal-500'}`} />
                <span className="truncate">{item.label}</span>
              </Link>
            );
          })}

          <button
            type="button"
            onClick={() => logout()}
            className="w-full flex items-center gap-3 px-3 py-2 rounded-xl text-xs font-medium text-terracotta-700 hover:bg-terracotta-50 transition-colors"
          >
            <LogOut className="w-4 h-4 flex-shrink-0" />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      {/* ============================================================ */}
      {/* MAIN CONTENT AREA & MOBILE SHELL                             */}
      {/* ============================================================ */}
      <div className="flex-1 flex flex-col min-w-0 pb-20 md:pb-8">
        {/* Mobile Top Bar (Visible only on < md) */}
        <header className="md:hidden sticky top-0 z-30 bg-white border-b border-sand-200 px-4 h-14 flex items-center justify-between">
          <Link href="/workspace" className="flex items-center gap-2 min-w-0">
            <div className="w-7 h-7 rounded-lg bg-bronze-700 text-white flex items-center justify-center font-serif font-bold text-xs shadow-2xs">
              E
            </div>
            <div className="min-w-0">
              <span className="text-xs font-semibold text-charcoal-900 truncate block">
                {studio?.name || 'Workspace'}
              </span>
              <span className="text-[10px] text-bronze-700 font-medium block -mt-0.5">
                {studio?.role || 'OWNER'}
              </span>
            </div>
          </Link>

          <Link
            href="/workspace/notifications"
            className="w-10 h-10 flex items-center justify-center rounded-xl text-charcoal-600 hover:bg-sand-100 transition-colors"
            aria-label="Notifications"
          >
            <Bell className="w-5 h-5" />
          </Link>
        </header>

        {/* Page Content */}
        <main className="flex-1" id="main-content" tabIndex={-1}>
          {children}
        </main>
      </div>

      {/* ============================================================ */}
      {/* MOBILE BOTTOM NAVIGATION (360px–430px thumb-friendly)        */}
      {/* ============================================================ */}
      <nav
        className="md:hidden fixed bottom-0 inset-x-0 z-40 bg-white border-t border-sand-200 px-2 py-1.5 flex items-center justify-around shadow-lg safe-area-bottom"
        aria-label="Mobile Workspace Navigation"
      >
        {MOBILE_BOTTOM_NAV.map((item) => {
          const isActive = pathname === item.href;
          const Icon = item.icon;
          return (
            <Link
              key={item.id}
              href={item.href}
              aria-current={isActive ? 'page' : undefined}
              className={`flex flex-col items-center justify-center min-w-[64px] min-h-[44px] rounded-xl px-2 py-1 text-[10px] transition-colors ${
                isActive ? 'text-bronze-800 font-semibold' : 'text-charcoal-500 hover:text-charcoal-900'
              }`}
            >
              <Icon className={`w-5 h-5 mb-0.5 ${isActive ? 'text-bronze-700' : 'text-charcoal-400'}`} />
              <span className="truncate">{item.label}</span>
            </Link>
          );
        })}

        {/* More Button */}
        <button
          type="button"
          onClick={() => setMoreMenuOpen(true)}
          className={`flex flex-col items-center justify-center min-w-[64px] min-h-[44px] rounded-xl px-2 py-1 text-[10px] transition-colors ${
            moreMenuOpen ? 'text-bronze-800 font-semibold' : 'text-charcoal-500 hover:text-charcoal-900'
          }`}
          aria-expanded={moreMenuOpen}
          aria-label="Open More modules menu"
        >
          <MoreHorizontal className="w-5 h-5 mb-0.5" />
          <span>More</span>
        </button>
      </nav>

      {/* ============================================================ */}
      {/* MOBILE BOTTOM SHEET MODAL (Exposes remaining modules)        */}
      {/* ============================================================ */}
      {moreMenuOpen && (
        <div
          className="md:hidden fixed inset-0 z-50 bg-charcoal-900/60 backdrop-blur-xs flex flex-col justify-end animate-fade-in"
          role="dialog"
          aria-modal="true"
          aria-labelledby="more-menu-title"
          onClick={() => setMoreMenuOpen(false)}
        >
          <div
            ref={bottomSheetRef}
            className="bg-white border-t border-sand-200 rounded-t-3xl p-5 max-h-[80vh] overflow-y-auto"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between pb-3 border-b border-sand-200 mb-4">
              <h2 id="more-menu-title" className="font-serif text-lg font-semibold text-charcoal-900">
                Workspace Modules
              </h2>
              <button
                type="button"
                onClick={() => setMoreMenuOpen(false)}
                className="w-8 h-8 rounded-full bg-sand-100 flex items-center justify-center text-charcoal-600 hover:bg-sand-200"
                aria-label="Close menu"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-1">
              {[
                { id: 'media', label: 'Media Library', href: '/workspace/media', icon: Image },
                { id: 'leads', label: 'Leads & Clients', href: '/workspace/leads', icon: Users },
                { id: 'seo', label: 'SEO Center', href: '/workspace/seo', icon: Search },
                { id: 'analytics', label: 'Analytics', href: '/workspace/analytics', icon: BarChart3 },
                { id: 'notifications', label: 'Notifications', href: '/workspace/notifications', icon: Bell },
                { id: 'business', label: 'Business Profile', href: '/workspace/business', icon: Building2 },
                { id: 'account', label: 'Account Profile', href: '/account', icon: User },
              ].map((item) => {
                const Icon = item.icon;
                return (
                  <Link
                    key={item.id}
                    href={item.href}
                    onClick={() => setMoreMenuOpen(false)}
                    className="flex items-center justify-between p-3 rounded-xl hover:bg-sand-50 active:bg-sand-100 transition-colors"
                  >
                    <div className="flex items-center gap-3">
                      <Icon className="w-4 h-4 text-charcoal-500" />
                      <span className="text-sm font-medium text-charcoal-800">{item.label}</span>
                    </div>
                    <ChevronRight className="w-4 h-4 text-charcoal-400" />
                  </Link>
                );
              })}

              <div className="pt-3 border-t border-sand-200 mt-2">
                <button
                  type="button"
                  onClick={() => {
                    setMoreMenuOpen(false);
                    logout();
                  }}
                  className="w-full flex items-center gap-3 p-3 rounded-xl text-sm font-medium text-terracotta-700 hover:bg-terracotta-50 transition-colors"
                >
                  <LogOut className="w-4 h-4" />
                  <span>Sign Out</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
