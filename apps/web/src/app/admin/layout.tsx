'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Users,
  Building2,
  ShieldCheck,
  MessageSquare,
  FileText,
  ShieldAlert,
  ArrowLeft,
  ExternalLink,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/auth-context';

interface AdminLayoutProps {
  children: React.ReactNode;
}

interface AdminNavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
}

const ADMIN_NAV_ITEMS: AdminNavItem[] = [
  { label: 'Overview', href: '/admin', icon: LayoutDashboard },
  { label: 'Users', href: '/admin/users', icon: Users },
  { label: 'Studios', href: '/admin/studios', icon: Building2 },
  { label: 'Verification', href: '/admin/verification', icon: ShieldCheck },
  { label: 'Reviews', href: '/admin/reviews', icon: MessageSquare },
  { label: 'Audit Logs', href: '/admin/audit', icon: FileText },
];

export default function AdminLayout({ children }: AdminLayoutProps) {
  const { user, isLoading } = useAuth();
  const pathname = usePathname();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-sand-50 flex items-center justify-center p-6 text-charcoal-700">
        <div className="flex items-center gap-3">
          <div className="w-5 h-5 border-2 border-bronze-600 border-t-transparent rounded-full animate-spin" />
          <span className="text-sm font-medium">Verifying administrator session...</span>
        </div>
      </div>
    );
  }

  const isPlatformAdmin =
    Boolean(user?.roles?.includes('ADMIN') || user?.roles?.includes('SUPER_ADMIN'));

  if (!user || !isPlatformAdmin) {
    return (
      <main className="min-h-screen bg-sand-50 flex items-center justify-center p-6">
        <div className="max-w-md w-full bg-white border border-sand-300 rounded-xl p-8 shadow-sm text-center">
          <div className="w-12 h-12 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-4">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <h1 className="text-xl font-serif text-charcoal-900 mb-2">
            Administrator Access Required
          </h1>
          <p className="text-sm text-charcoal-600 mb-6 leading-relaxed">
            This internal console is strictly restricted to platform administrators. Studio owners and public customers do not have access to administrative operations.
          </p>
          <div className="flex flex-col gap-2">
            <Link
              href="/workspace"
              className="inline-flex items-center justify-center px-4 py-2.5 rounded-lg bg-charcoal-900 text-white text-sm font-medium hover:bg-charcoal-800 transition-colors"
            >
              Return to Workspace
            </Link>
            <Link
              href="/"
              className="inline-flex items-center justify-center px-4 py-2 text-sm text-charcoal-600 hover:text-charcoal-900"
            >
              Go to Homepage
            </Link>
          </div>
        </div>
      </main>
    );
  }

  return (
    <div className="min-h-screen bg-sand-50 flex flex-col md:flex-row">
      {/* Sidebar Navigation */}
      <aside
        className="w-full md:w-64 bg-white border-b md:border-b-0 md:border-r border-sand-200 flex flex-col shrink-0"
        aria-label="Platform Admin Navigation"
      >
        <div className="p-4 md:p-6 border-b border-sand-100 flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-bronze-700 bg-bronze-50 px-2 py-0.5 rounded">
                Admin Console
              </span>
            </div>
            <h2 className="text-base font-serif text-charcoal-900 mt-1">Platform Operations</h2>
          </div>
        </div>

        <nav className="p-3 md:p-4 space-y-1 flex-1 overflow-y-auto" aria-label="Admin Navigation Links">
          {ADMIN_NAV_ITEMS.map((item) => {
            const isActive =
              item.href === '/admin' ? pathname === '/admin' : pathname?.startsWith(item.href);
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-charcoal-900 text-white'
                    : 'text-charcoal-700 hover:bg-sand-100 hover:text-charcoal-900'
                }`}
                aria-current={isActive ? 'page' : undefined}
              >
                <Icon className={`w-4 h-4 ${isActive ? 'text-white' : 'text-charcoal-500'}`} />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>

        <div className="p-4 border-t border-sand-100 text-xs text-charcoal-500 flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="font-medium text-charcoal-700 truncate">{user.displayName}</span>
            <span className="text-[10px] bg-sand-100 text-charcoal-600 px-1.5 py-0.5 rounded font-mono">
              {user.roles.includes('SUPER_ADMIN') ? 'SUPER_ADMIN' : 'ADMIN'}
            </span>
          </div>
          <Link
            href="/workspace"
            className="inline-flex items-center gap-1.5 text-xs text-bronze-700 hover:text-bronze-800 font-medium"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Studio Workspace</span>
          </Link>
        </div>
      </aside>

      {/* Main Content Area */}
      <main id="admin-main" className="flex-1 min-w-0 p-4 sm:p-6 lg:p-8 overflow-y-auto">
        <div className="max-w-7xl mx-auto">{children}</div>
      </main>
    </div>
  );
}
