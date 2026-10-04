'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  Bell,
  ArrowLeft,
  Check,
  CheckCheck,
  ExternalLink,
  Filter,
  Inbox,
  ShieldCheck,
  Sparkles,
  Users,
  MessageSquare,
  Building2,
  Settings,
} from 'lucide-react';
import { useAuth } from '@/lib/auth/auth-context';
import { useRealtimeSubscription } from '@/lib/realtime/RealtimeProvider';

interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message: string;
  actionUrl?: string;
  readAt?: string | null;
  createdAt: string;
}

export default function NotificationsWorkspacePage() {
  const { isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [filter, setFilter] = useState<'all' | 'unread'>('all');
  const [loading, setLoading] = useState<boolean>(true);

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/v1/notifications?limit=50', { credentials: 'include' });
      if (res.ok) {
        const data = await res.json();
        setNotifications(data.notifications || []);
        setUnreadCount(data.unreadCount || 0);
      }
    } catch {
      // Ignore
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      loadNotifications();
    }
  }, [isAuthenticated]);

  // Reactive updates via SSE
  useRealtimeSubscription(
    ['NOTIFICATION_CREATED', 'NOTIFICATION_READ', 'NOTIFICATIONS_READ_ALL', 'RESYNC'],
    (event) => {
      if (event.type === 'NOTIFICATION_CREATED' || event.type === 'RESYNC') {
        loadNotifications();
      } else if (event.type === 'NOTIFICATIONS_READ_ALL') {
        setNotifications((prev) => prev.map((n) => ({ ...n, readAt: new Date().toISOString() })));
        setUnreadCount(0);
      } else if (event.type === 'NOTIFICATION_READ' && event.notificationId) {
        setNotifications((prev) =>
          prev.map((n) => (n.id === event.notificationId ? { ...n, readAt: new Date().toISOString() } : n))
        );
        setUnreadCount((prev) => Math.max(0, prev - 1));
      }
    }
  );

  const handleMarkAsRead = async (id: string) => {
    try {
      await fetch(`/api/v1/notifications/${id}/read`, {
        method: 'POST',
        credentials: 'include',
      });
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, readAt: new Date().toISOString() } : n));
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch {
      // Ignore
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await fetch('/api/v1/notifications/read-all', {
        method: 'POST',
        credentials: 'include',
      });
      setNotifications(prev => prev.map(n => ({ ...n, readAt: new Date().toISOString() })));
      setUnreadCount(0);
    } catch {
      // Ignore
    }
  };

  const filteredNotifications = notifications.filter(n => {
    if (filter === 'unread') return !n.readAt;
    return true;
  });

  const getIconForType = (type: string) => {
    switch (type) {
      case 'NEW_LEAD':
      case 'LEAD_FOLLOW_UP':
        return <Users className="w-5 h-5 text-bronze-700" />;
      case 'NEW_REVIEW':
      case 'REVIEW_RESPONSE':
        return <MessageSquare className="w-5 h-5 text-sand-800" />;
      case 'AI_GENERATION_COMPLETE':
      case 'AI_GENERATION_FAILED':
      case 'CLIENT_APPROVED_CONCEPT':
      case 'CLIENT_REQUESTED_CHANGES':
        return <Sparkles className="w-5 h-5 text-bronze-600" />;
      case 'VERIFICATION_UPDATE':
        return <ShieldCheck className="w-5 h-5 text-forest-600" />;
      default:
        return <Building2 className="w-5 h-5 text-charcoal-600" />;
    }
  };

  return (
    <div className="p-4 sm:p-6 lg:p-10 max-w-4xl mx-auto space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <Link
            href="/workspace"
            className="inline-flex items-center gap-1.5 text-xs font-medium text-charcoal-500 hover:text-charcoal-900 mb-3 transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Workspace Home</span>
          </Link>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center">
              <Bell className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
                Inbox & Activity
              </span>
              <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
                Notification Center
              </h1>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {unreadCount > 0 && (
            <button
              onClick={handleMarkAllRead}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white border border-sand-300 rounded-xl text-xs font-medium text-charcoal-700 hover:bg-sand-50 transition-colors"
            >
              <CheckCheck className="w-4 h-4 text-bronze-700" />
              <span>Mark all as read</span>
            </button>
          )}
          <Link
            href="/account#preferences"
            className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-white border border-sand-300 rounded-xl text-xs font-medium text-charcoal-700 hover:bg-sand-50 transition-colors"
          >
            <Settings className="w-4 h-4 text-charcoal-500" />
            <span>Preferences</span>
          </Link>
        </div>
      </div>

      <div className="flex items-center gap-2 border-b border-sand-200 pb-3">
        <button
          onClick={() => setFilter('all')}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
            filter === 'all'
              ? 'bg-charcoal-900 text-white'
              : 'text-charcoal-600 hover:bg-sand-100'
          }`}
        >
          All Activity ({notifications.length})
        </button>
        <button
          onClick={() => setFilter('unread')}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
            filter === 'unread'
              ? 'bg-charcoal-900 text-white'
              : 'text-charcoal-600 hover:bg-sand-100'
          }`}
        >
          Unread ({unreadCount})
        </button>
      </div>

      <div className="bg-white border border-sand-200 rounded-2xl shadow-sm overflow-hidden divide-y divide-sand-100">
        {loading ? (
          <div className="p-12 text-center text-charcoal-500 text-sm">
            Loading notifications...
          </div>
        ) : filteredNotifications.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <div className="w-12 h-12 rounded-full bg-sand-100 text-charcoal-400 flex items-center justify-center mx-auto">
              <Inbox className="w-6 h-6" />
            </div>
            <h3 className="font-serif text-base text-charcoal-900 font-semibold">
              {filter === 'unread' ? 'No unread notifications' : 'No notifications yet'}
            </h3>
            <p className="text-xs text-charcoal-500 max-w-sm mx-auto">
              {filter === 'unread'
                ? 'All caught up! New lead inquiries, review submissions, and AI generation notices will appear here.'
                : 'When clients submit inquiries or reviews, or when AI concepts complete, updates will appear in your center.'}
            </p>
          </div>
        ) : (
          filteredNotifications.map((notif) => {
            const isUnread = !notif.readAt;
            return (
              <div
                key={notif.id}
                className={`p-4 sm:p-5 flex items-start gap-4 transition-colors ${
                  isUnread ? 'bg-sand-50/60 hover:bg-sand-100/50' : 'hover:bg-sand-50/30'
                }`}
              >
                <div className="w-9 h-9 rounded-xl bg-white border border-sand-200 flex items-center justify-center shrink-0 mt-0.5 shadow-2xs">
                  {getIconForType(notif.type)}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <h3 className={`text-sm font-semibold truncate ${isUnread ? 'text-charcoal-900' : 'text-charcoal-700'}`}>
                      {notif.title}
                    </h3>
                    <span className="text-[11px] text-charcoal-400 shrink-0">
                      {new Date(notif.createdAt).toLocaleDateString([], {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </span>
                  </div>

                  <p className="text-xs text-charcoal-600 mt-1 leading-relaxed">
                    {notif.message}
                  </p>

                  <div className="mt-3 flex items-center gap-3">
                    {notif.actionUrl && (
                      <Link
                        href={notif.actionUrl}
                        className="inline-flex items-center gap-1 text-xs font-medium text-bronze-700 hover:text-bronze-900 transition-colors"
                      >
                        <span>View Details</span>
                        <ExternalLink className="w-3 h-3" />
                      </Link>
                    )}

                    {isUnread && (
                      <button
                        onClick={() => handleMarkAsRead(notif.id)}
                        className="inline-flex items-center gap-1 text-xs font-medium text-charcoal-500 hover:text-charcoal-800 transition-colors"
                      >
                        <Check className="w-3.5 h-3.5" />
                        <span>Mark as read</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
