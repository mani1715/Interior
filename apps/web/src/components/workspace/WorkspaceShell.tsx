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
  ShieldCheck,
  MessageSquare,
  ArrowRight,
  Users2,
  ChevronsUpDown,
  Check,
  AlertTriangle,
  Plus,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/auth-context';
import { WorkspaceSummary } from '@/lib/workspace/types';
import { fetchWorkspaceSummary } from '@/lib/workspace/api';
import { useUnsavedChanges } from '@/lib/workspace/unsaved-changes-context';
import { CreateStudioModal } from './CreateStudioModal';

export function formatStudioRole(role?: string | null): string {
  if (!role) return 'Team Member';
  const r = role.toUpperCase();
  if (r === 'DESIGNER_ADMIN' || r === 'OWNER' || r === 'ADMIN') return 'Studio Admin';
  if (r === 'DESIGNER_MEMBER' || r === 'MEMBER') return 'Team Member';
  return role.replace(/_/g, ' ');
}

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
  { id: 'reviews', label: 'Reviews', href: '/workspace/reviews', icon: MessageSquare },
  { id: 'seo', label: 'SEO Center', href: '/workspace/seo', icon: Search },
  { id: 'analytics', label: 'Analytics', href: '/workspace/analytics', icon: BarChart3 },
  { id: 'notifications', label: 'Notifications', href: '/workspace/notifications', icon: Bell },
];

const SECONDARY_NAV_ITEMS: NavItem[] = [
  { id: 'team', label: 'Team', href: '/workspace/team', icon: Users2 },
  { id: 'verification', label: 'Studio Verification', href: '/workspace/verification', icon: ShieldCheck },
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
  const { user, isLoading: authLoading, isAuthenticated, switchStudio, logout } = useAuth();
  const { confirmDiscard, modalOpen, cancelDiscard, proceedDiscard } = useUnsavedChanges();

  const [summary, setSummary] = useState<WorkspaceSummary | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [moreMenuOpen, setMoreMenuOpen] = useState<boolean>(false);
  const [desktopSwitcherOpen, setDesktopSwitcherOpen] = useState<boolean>(false);
  const [mobileSwitcherOpen, setMobileSwitcherOpen] = useState<boolean>(false);
  const [createStudioModalOpen, setCreateStudioModalOpen] = useState<boolean>(false);
  const [announcement, setAnnouncement] = useState<string>('');
  const [selectedStudioId, setSelectedStudioId] = useState<string | undefined>(undefined);

  const bottomSheetRef = useRef<HTMLDivElement>(null);
  const desktopSwitcherRef = useRef<HTMLDivElement>(null);
  const mobileSwitcherRef = useRef<HTMLDivElement>(null);

  // Close desktop dropdown on outside click or ESC
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (desktopSwitcherRef.current && !desktopSwitcherRef.current.contains(e.target as Node)) {
        setDesktopSwitcherOpen(false);
      }
      if (mobileSwitcherRef.current && !mobileSwitcherRef.current.contains(e.target as Node)) {
        setMobileSwitcherOpen(false);
      }
    };
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setDesktopSwitcherOpen(false);
        setMobileSwitcherOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  // Authentication check & Data fetching
  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push('/sign-in?returnUrl=/workspace');
      return;
    }

    if (!authLoading && user) {
      const isProfessional =
        user.activeStudioId != null ||
        (user.studios && user.studios.length > 0) ||
        user.roles.includes('DESIGNER') ||
        user.roles.includes('DESIGNER_TEAM') ||
        user.roles.includes('SUPER_ADMIN');
      if (!isProfessional) {
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

  const handleSelectStudio = (targetStudioId: string, studioName: string) => {
    if (targetStudioId === (selectedStudioId || summary?.studio?.id || user?.activeStudioId)) {
      setDesktopSwitcherOpen(false);
      setMobileSwitcherOpen(false);
      return;
    }

    confirmDiscard(async () => {
      setSelectedStudioId(targetStudioId);
      setDesktopSwitcherOpen(false);
      setMobileSwitcherOpen(false);
      setAnnouncement(`Switched to studio ${studioName}`);

      try {
        if (switchStudio) {
          await switchStudio(targetStudioId);
        }
      } catch (err: any) {
        console.error('Failed to switch studio:', err);
      }
    });
  };

  const handleStudioCreated = async (newStudioId: string, newStudioName: string) => {
    setCreateStudioModalOpen(false);
    setSelectedStudioId(newStudioId);
    setAnnouncement(`Studio ${newStudioName} created successfully. Switched to workspace.`);
    try {
      if (switchStudio) {
        await switchStudio(newStudioId);
      }
      setLoading(true);
      const updated = await fetchWorkspaceSummary(newStudioId);
      setSummary(updated);
    } catch (err: any) {
      console.error('Failed to load updated workspace after studio creation:', err);
    } finally {
      setLoading(false);
    }
  };

  if (authLoading || (loading && !summary && !error)) {
    return (
      <div className="workspace-editorial min-h-screen bg-[var(--surface-sunken,#fbfaf8)] flex flex-col items-center justify-center p-4">
        <div className="w-10 h-10 border-4 border-bronze-200 border-t-bronze-600 rounded-full animate-spin mb-4" />
        <p className="text-sm text-charcoal-600 font-medium">Loading professional workspace...</p>
      </div>
    );
  }

  // Account Suspended Screen
  if (user && user.status === 'SUSPENDED') {
    return (
      <div className="workspace-editorial min-h-screen bg-[var(--surface-sunken,#fbfaf8)] flex items-center justify-center p-6">
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
  const isProfessional =
    user &&
    (user.activeStudioId != null ||
      (user.studios && user.studios.length > 0) ||
      user.roles.includes('DESIGNER') ||
      user.roles.includes('DESIGNER_TEAM') ||
      user.roles.includes('SUPER_ADMIN'));
  if (user && !isProfessional) {
    return (
      <div className="workspace-editorial min-h-screen bg-[var(--surface-sunken,#fbfaf8)] flex items-center justify-center p-6">
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
    <div className="workspace-editorial min-h-screen bg-[var(--surface-sunken,#fbfaf8)] flex flex-col md:flex-row">
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

        {/* Studio Tenancy Identity Card & Switcher */}
        <div ref={desktopSwitcherRef} className="relative p-4 mx-3 my-3 bg-sand-50 border border-sand-200 rounded-xl">
          {/* Accessible Screen-Reader Announcement Live Region */}
          <div aria-live="polite" aria-atomic="true" className="sr-only">
            {announcement}
          </div>

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

          {/* Studio selector with Create Another Studio action */}
          {summary && summary.availableStudios && summary.availableStudios.length > 1 ? (
            <div className="mt-3 pt-2.5 border-t border-sand-200">
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-[10px] font-semibold text-charcoal-500 uppercase tracking-wider">
                  Active Studio
                </span>
                <span className="text-[10px] text-bronze-700 font-medium">
                  {summary.availableStudios.length} studios
                </span>
              </div>

              {/* Accessible Custom Popover Trigger */}
              <button
                type="button"
                onClick={() => setDesktopSwitcherOpen((prev) => !prev)}
                aria-haspopup="listbox"
                aria-expanded={desktopSwitcherOpen}
                aria-label="Switch active studio"
                className="w-full flex items-center justify-between px-2.5 py-1.5 bg-white border border-sand-300 rounded-lg text-left text-xs font-medium text-charcoal-800 hover:border-bronze-500 focus:outline-none focus:ring-2 focus:ring-bronze-500 transition-colors"
              >
                <span className="truncate mr-2">
                  {studio?.name || 'Select Studio'}
                </span>
                <ChevronsUpDown className="w-3.5 h-3.5 text-charcoal-400 flex-shrink-0" />
              </button>

              {/* Custom Accessible Dropdown Listbox */}
              {desktopSwitcherOpen && (
                <div
                  role="listbox"
                  aria-label="Available studios"
                  className="absolute left-0 right-0 top-full mt-1.5 z-50 bg-white border border-sand-200 rounded-xl shadow-lg p-1.5 space-y-1 animate-fade-in"
                >
                  <div className="px-2 py-1 text-[10px] font-semibold text-charcoal-500 uppercase tracking-wider border-b border-sand-200 mb-1">
                    Your Studios
                  </div>
                  {summary.availableStudios.map((s) => {
                    const isCurrent = s.studioId === (selectedStudioId || studio?.id || user?.activeStudioId);
                    return (
                      <button
                        key={s.studioId}
                        role="option"
                        aria-selected={isCurrent}
                        type="button"
                        onClick={() => handleSelectStudio(s.studioId, s.studioName)}
                        className={`w-full flex items-center justify-between px-2.5 py-2 rounded-lg text-left text-xs transition-colors ${
                          isCurrent
                            ? 'bg-sand-100 text-charcoal-900 font-semibold'
                            : 'hover:bg-sand-50 text-charcoal-700'
                        }`}
                      >
                        <div className="min-w-0 pr-2">
                          <p className="truncate font-medium">{s.studioName}</p>
                          <p className="text-[10px] text-charcoal-500">{formatStudioRole(s.role)}</p>
                        </div>
                        {isCurrent && <Check className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />}
                      </button>
                    );
                  })}

                  {/* Create Another Studio Action */}
                  <button
                    type="button"
                    onClick={() => {
                      setDesktopSwitcherOpen(false);
                      setCreateStudioModalOpen(true);
                    }}
                    className="w-full flex items-center gap-2 px-2.5 py-2 mt-1 border-t border-sand-200 rounded-lg text-left text-xs font-medium text-bronze-800 hover:bg-sand-50 transition-colors"
                  >
                    <Plus className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />
                    <span>Create Another Studio</span>
                  </button>
                </div>
              )}

              {/* Fallback Native Select for standard form access & automated test compatibility */}
              <select
                id="studio-select"
                aria-label="Switch Studio Native"
                aria-hidden="true"
                value={selectedStudioId || studio?.id || ''}
                onChange={(e) => {
                  const target = summary.availableStudios.find((s) => s.studioId === e.target.value);
                  handleSelectStudio(e.target.value, target?.studioName || 'Studio');
                }}
                className="sr-only"
                tabIndex={-1}
              >
                {summary.availableStudios.map((s) => (
                  <option key={s.studioId} value={s.studioId}>
                    {s.studioName} ({s.role})
                  </option>
                ))}
              </select>
            </div>
          ) : (
            <div className="mt-3 pt-2.5 border-t border-sand-200">
              <button
                type="button"
                onClick={() => setCreateStudioModalOpen(true)}
                className="w-full flex items-center justify-center gap-1.5 py-1 px-2 text-[11px] font-medium text-bronze-800 hover:text-bronze-900 hover:bg-sand-100 rounded-lg transition-colors border border-dashed border-sand-300"
              >
                <Plus className="w-3.5 h-3.5 text-bronze-700" />
                <span>Create Another Studio</span>
              </button>
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
          <div ref={mobileSwitcherRef} className="relative flex items-center gap-2 min-w-0">
            <Link href="/workspace" className="flex items-center gap-2 min-w-0 flex-shrink-0">
              <div className="w-7 h-7 rounded-lg bg-bronze-700 text-white flex items-center justify-center font-serif font-bold text-xs shadow-2xs">
                E
              </div>
            </Link>

            {summary && summary.availableStudios && summary.availableStudios.length > 1 ? (
              <button
                type="button"
                onClick={() => setMobileSwitcherOpen((prev) => !prev)}
                aria-haspopup="listbox"
                aria-expanded={mobileSwitcherOpen}
                aria-label="Switch active studio"
                className="flex items-center gap-1.5 min-w-0 text-left py-1 px-1.5 rounded-lg hover:bg-sand-100 transition-colors"
              >
                <div className="min-w-0">
                  <span className="text-xs font-semibold text-charcoal-900 truncate block max-w-[150px]">
                    {studio?.name || 'Workspace'}
                  </span>
                  <span className="text-[10px] text-bronze-700 font-medium block -mt-0.5">
                    {formatStudioRole(studio?.role)}
                  </span>
                </div>
                <ChevronsUpDown className="w-3.5 h-3.5 text-charcoal-400 flex-shrink-0" />
              </button>
            ) : (
              <div className="min-w-0">
                <span className="text-xs font-semibold text-charcoal-900 truncate block max-w-[160px]">
                  {studio?.name || 'Workspace'}
                </span>
                <span className="text-[10px] text-bronze-700 font-medium block -mt-0.5">
                  {formatStudioRole(studio?.role)}
                </span>
              </div>
            )}

            {/* Mobile Studio Switcher Dropdown */}
            {mobileSwitcherOpen && summary && summary.availableStudios && (
              <div
                role="listbox"
                aria-label="Available studios mobile"
                className="absolute left-0 top-full mt-2 w-64 z-50 bg-white border border-sand-200 rounded-xl shadow-xl p-1.5 space-y-1 animate-fade-in"
              >
                <div className="px-2.5 py-1 text-[10px] font-semibold text-charcoal-500 uppercase tracking-wider border-b border-sand-200 mb-1">
                  Switch Active Studio
                </div>
                {summary.availableStudios.map((s) => {
                  const isCurrent = s.studioId === (selectedStudioId || studio?.id || user?.activeStudioId);
                  return (
                    <button
                      key={s.studioId}
                      role="option"
                      aria-selected={isCurrent}
                      type="button"
                      onClick={() => handleSelectStudio(s.studioId, s.studioName)}
                      className={`w-full flex items-center justify-between px-2.5 py-2 rounded-lg text-left text-xs transition-colors ${
                        isCurrent
                          ? 'bg-sand-100 text-charcoal-900 font-semibold'
                          : 'hover:bg-sand-50 text-charcoal-700'
                      }`}
                    >
                      <div className="min-w-0 pr-2">
                        <p className="truncate font-medium">{s.studioName}</p>
                        <p className="text-[10px] text-charcoal-500">{formatStudioRole(s.role)}</p>
                      </div>
                      {isCurrent && <Check className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />}
                    </button>
                  );
                })}

                {/* Create Another Studio Action */}
                <button
                  type="button"
                  onClick={() => {
                    setMobileSwitcherOpen(false);
                    setCreateStudioModalOpen(true);
                  }}
                  className="w-full flex items-center gap-2 px-2.5 py-2 mt-1 border-t border-sand-200 rounded-lg text-left text-xs font-medium text-bronze-800 hover:bg-sand-50 transition-colors"
                >
                  <Plus className="w-3.5 h-3.5 text-bronze-700 flex-shrink-0" />
                  <span>Create Another Studio</span>
                </button>
              </div>
            )}
          </div>

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

      {/* ============================================================ */}
      {/* UNSAVED CHANGES GUARD MODAL                                  */}
      {/* ============================================================ */}
      {modalOpen && (
        <div
          className="fixed inset-0 z-50 bg-charcoal-900/60 backdrop-blur-xs flex items-center justify-center p-4 animate-fade-in"
          role="dialog"
          aria-modal="true"
          aria-labelledby="unsaved-modal-title"
        >
          <div className="bg-white border border-sand-200 rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center gap-3 text-terracotta-700">
              <div className="w-10 h-10 rounded-full bg-terracotta-50 flex items-center justify-center flex-shrink-0">
                <AlertTriangle className="w-5 h-5 text-terracotta-600" />
              </div>
              <div>
                <h3 id="unsaved-modal-title" className="font-serif text-lg font-semibold text-charcoal-900">
                  Unsaved Changes
                </h3>
                <p className="text-xs text-charcoal-500">
                  You have unsaved changes in your current studio.
                </p>
              </div>
            </div>

            <p className="text-xs text-charcoal-600 leading-relaxed">
              Switching to another studio now will discard your unsaved progress. Are you sure you want to discard your changes and switch?
            </p>

            <div className="flex items-center justify-end gap-3 pt-3 border-t border-sand-200">
              <button
                type="button"
                onClick={cancelDiscard}
                className="px-4 py-2 text-xs font-medium text-charcoal-700 bg-sand-100 hover:bg-sand-200 rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={proceedDiscard}
                className="px-4 py-2 text-xs font-medium text-white bg-terracotta-700 hover:bg-terracotta-800 rounded-xl transition-colors shadow-2xs"
              >
                Discard & Switch Studio
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ============================================================ */}
      {/* CREATE ANOTHER STUDIO MODAL                                  */}
      {/* ============================================================ */}
      <CreateStudioModal
        isOpen={createStudioModalOpen}
        onClose={() => setCreateStudioModalOpen(false)}
        onSuccess={handleStudioCreated}
      />
    </div>
  );
}
