'use client';

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { Bell, Check, ExternalLink, Filter } from 'lucide-react';
import { useAuth } from '@/lib/auth/auth-context';

import { useRealtimeSubscription } from '@/lib/realtime/RealtimeProvider';
import {
  fetchNotifications,
  fetchUnreadNotificationCount,
  markNotificationAsRead,
  markAllNotificationsAsRead,
} from '@/lib/notifications/api';

export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message: string;
  actionUrl?: string | null;
  readAt?: string | null;
  createdAt: string;
}

export function NotificationBell() {
  const { isAuthenticated } = useAuth();
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [isOpen, setIsOpen] = useState<boolean>(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const popoverRef = useRef<HTMLDivElement>(null);

  const fetchCount = async () => {
    if (!isAuthenticated) return;
    try {
      const count = await fetchUnreadNotificationCount();
      setUnreadCount(count);
    } catch {
      // Silently fail if offline or unauthenticated
    }
  };

  // Initial fetch on auth
  useEffect(() => {
    if (isAuthenticated) {
      fetchCount();
    }
  }, [isAuthenticated]);

  // Real-time event handling: update badge & list without polling
  useRealtimeSubscription(
    ['NOTIFICATION_CREATED', 'NOTIFICATION_READ', 'NOTIFICATIONS_READ_ALL', 'RESYNC'],
    (event) => {
      if (event.type === 'NOTIFICATION_CREATED') {
        setUnreadCount((prev) => prev + 1);
        if (isOpen) {
          loadNotifications();
        }
      } else if (event.type === 'NOTIFICATIONS_READ_ALL') {
        setUnreadCount(0);
        setNotifications((prev) =>
          prev.map((n) => ({ ...n, readAt: n.readAt || new Date().toISOString() }))
        );
      } else if (event.type === 'NOTIFICATION_READ') {
        if (event.notificationId) {
          setNotifications((prev) =>
            prev.map((n) =>
              n.id === event.notificationId
                ? { ...n, readAt: n.readAt || new Date().toISOString() }
                : n
            )
          );
        }
        fetchCount();
      } else if (event.type === 'RESYNC') {
        fetchCount();
        if (isOpen) {
          loadNotifications();
        }
      }
    }
  );

  const loadNotifications = async () => {
    setLoading(true);
    try {
      const data = await fetchNotifications(8, 0);
      setNotifications(data.notifications || []);
      setUnreadCount(data.unreadCount || 0);
    } catch {
      // Ignore
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = () => {
    const nextState = !isOpen;
    setIsOpen(nextState);
    if (nextState) {
      loadNotifications();
    }
  };

  const handleMarkAsRead = async (id: string, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    try {
      await markNotificationAsRead(id);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, readAt: new Date().toISOString() } : n));
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch {
      // Ignore
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllNotificationsAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, readAt: new Date().toISOString() })));
      setUnreadCount(0);
    } catch {
      // Ignore
    }
  };

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (popoverRef.current && !popoverRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  if (!isAuthenticated) return null;

  return (
    <div className="relative" ref={popoverRef}>
      <button
        type="button"
        onClick={handleToggle}
        aria-label="Notifications"
        aria-expanded={isOpen}
        className="relative p-2 rounded-xl text-charcoal-600 hover:text-charcoal-900 hover:bg-sand-100 transition-colors flex items-center justify-center min-w-[38px] min-h-[38px]"
      >
        <Bell className="w-4 h-4" />
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-bronze-700 text-[9px] font-bold text-white shadow-xs">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 sm:w-96 rounded-2xl bg-white border border-sand-200 shadow-xl z-50 overflow-hidden animate-fade-in">
          <div className="flex items-center justify-between px-4 py-3 border-b border-sand-200 bg-sand-50/60">
            <div className="flex items-center gap-2">
              <span className="font-serif text-sm font-semibold text-charcoal-900">Notifications</span>
              {unreadCount > 0 && (
                <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-bronze-100 text-bronze-800">
                  {unreadCount} unread
                </span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={handleMarkAllRead}
                className="text-[11px] font-medium text-bronze-700 hover:text-bronze-900 transition-colors"
              >
                Mark all read
              </button>
            )}
          </div>

          <div className="max-h-80 overflow-y-auto divide-y divide-sand-100">
            {loading ? (
              <div className="p-6 text-center text-xs text-charcoal-500">
                Loading notifications...
              </div>
            ) : notifications.length === 0 ? (
              <div className="p-6 text-center text-xs text-charcoal-500">
                No notifications right now.
              </div>
            ) : (
              notifications.map((n) => {
                const isUnread = !n.readAt;
                return (
                  <div
                    key={n.id}
                    className={`p-3.5 transition-colors text-xs flex items-start gap-3 ${
                      isUnread ? 'bg-sand-50/70 hover:bg-sand-100/70' : 'hover:bg-sand-50/40'
                    }`}
                  >
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-1 mb-1">
                        <span className={`font-semibold truncate ${isUnread ? 'text-charcoal-900' : 'text-charcoal-700'}`}>
                          {n.title}
                        </span>
                        <span className="text-[10px] text-charcoal-400 shrink-0">
                          {new Date(n.createdAt).toLocaleDateString([], { month: 'short', day: 'numeric' })}
                        </span>
                      </div>
                      <p className="text-charcoal-600 line-clamp-2 leading-relaxed">
                        {n.message}
                      </p>
                      {n.actionUrl && (
                        <Link
                          href={n.actionUrl}
                          onClick={() => setIsOpen(false)}
                          className="inline-flex items-center gap-1 text-[11px] font-medium text-bronze-700 hover:text-bronze-900 mt-2"
                        >
                          <span>View Details</span>
                          <ExternalLink className="w-3 h-3" />
                        </Link>
                      )}
                    </div>
                    {isUnread && (
                      <button
                        type="button"
                        onClick={(e) => handleMarkAsRead(n.id, e)}
                        title="Mark as read"
                        className="p-1 rounded text-charcoal-400 hover:text-charcoal-700 hover:bg-sand-200/50"
                      >
                        <Check className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                );
              })
            )}
          </div>

          <div className="p-2 border-t border-sand-200 bg-sand-50/40 text-center">
            <Link
              href="/workspace/notifications"
              onClick={() => setIsOpen(false)}
              className="text-xs font-medium text-charcoal-700 hover:text-charcoal-900"
            >
              See all notifications →
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
