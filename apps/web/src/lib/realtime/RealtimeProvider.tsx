'use client';

import React, { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react';
import { useAuth } from '@/lib/auth/auth-context';

export type RealtimeEventType =
  | 'NOTIFICATION_CREATED'
  | 'NOTIFICATION_READ'
  | 'NOTIFICATIONS_READ_ALL'
  | 'LEAD_CREATED'
  | 'LEAD_UPDATED'
  | 'AI_JOB_QUEUED'
  | 'AI_JOB_PROCESSING'
  | 'AI_JOB_COMPLETED'
  | 'AI_JOB_FAILED'
  | 'REVIEW_RECEIVED'
  | 'REVIEW_RESPONSE_UPDATED'
  | 'CLIENT_CONCEPT_APPROVED'
  | 'CLIENT_CONCEPT_CHANGES_REQUESTED'
  | 'VERIFICATION_STATUS_CHANGED'
  | 'SYSTEM_NOTICE'
  | 'RESYNC'
  | 'HEARTBEAT';

export interface RealtimeEventPayload {
  eventId: string;
  type: RealtimeEventType | string;
  occurredAt: string;
  recipientUserId?: string | null;
  studioId?: string | null;
  resourceType?: string | null;
  resourceId?: string | null;
  notificationId?: string | null;
  metadata?: Record<string, any> | null;
}

export type RealtimeConnectionState = 'connected' | 'connecting' | 'disconnected';

export type RealtimeEventHandler = (event: RealtimeEventPayload) => void;

interface RealtimeContextValue {
  connectionState: RealtimeConnectionState;
  lastEvent: RealtimeEventPayload | null;
  subscribe: (handler: RealtimeEventHandler, eventTypes?: (RealtimeEventType | string)[]) => () => void;
  reconnect: () => void;
}

const RealtimeContext = createContext<RealtimeContextValue | null>(null);

export function RealtimeProvider({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  const [connectionState, setConnectionState] = useState<RealtimeConnectionState>('disconnected');
  const [lastEvent, setLastEvent] = useState<RealtimeEventPayload | null>(null);

  const eventSourceRef = useRef<EventSource | null>(null);
  const subscribersRef = useRef<Map<RealtimeEventHandler, (RealtimeEventType | string)[] | undefined>>(new Map());
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const reconnectAttemptsRef = useRef<number>(0);
  const lastEventIdRef = useRef<string | null>(null);

  const notifySubscribers = useCallback((event: RealtimeEventPayload) => {
    subscribersRef.current.forEach((filterTypes, handler) => {
      try {
        if (!filterTypes || filterTypes.length === 0 || filterTypes.includes(event.type)) {
          handler(event);
        }
      } catch (err) {
        console.error('Error invoking realtime event handler:', err);
      }
    });
  }, []);

  const connect = useCallback(() => {
    if (typeof window === 'undefined') return;
    if (!isAuthenticated) {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      setConnectionState('disconnected');
      return;
    }

    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }

    setConnectionState('connecting');

    try {
      const es = new EventSource('/api/v1/events/stream', { withCredentials: true });
      eventSourceRef.current = es;

      es.onopen = () => {
        setConnectionState('connected');
        reconnectAttemptsRef.current = 0;
      };

      es.onmessage = (e: MessageEvent) => {
        try {
          if (!e.data) return;
          const data: RealtimeEventPayload = JSON.parse(e.data);
          if (data.eventId) {
            lastEventIdRef.current = data.eventId;
          }
          if (data.type !== 'HEARTBEAT') {
            setLastEvent(data);
          }
          notifySubscribers(data);
        } catch (err) {
          console.warn('Received non-JSON realtime event or parse failure:', err);
        }
      };

      es.onerror = () => {
        setConnectionState('disconnected');
        if (eventSourceRef.current) {
          eventSourceRef.current.close();
          eventSourceRef.current = null;
        }

        // Exponential backoff reconnect: 2s, 4s, 8s, up to 30s
        const backoffMs = Math.min(30000, 2000 * Math.pow(1.5, reconnectAttemptsRef.current));
        reconnectAttemptsRef.current += 1;

        if (reconnectTimeoutRef.current) {
          clearTimeout(reconnectTimeoutRef.current);
        }
        reconnectTimeoutRef.current = setTimeout(() => {
          if (isAuthenticated) {
            connect();
          }
        }, backoffMs);
      };
    } catch (err) {
      console.error('Failed to instantiate EventSource:', err);
      setConnectionState('disconnected');
    }
  }, [isAuthenticated, notifySubscribers]);

  const reconnect = useCallback(() => {
    reconnectAttemptsRef.current = 0;
    connect();
  }, [connect]);

  // Setup connection when authenticated
  useEffect(() => {
    if (isAuthenticated) {
      connect();
    } else {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      setConnectionState('disconnected');
    }

    return () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
    };
  }, [isAuthenticated, connect]);

  // Resync / Reconnect on page visibility change
  useEffect(() => {
    if (typeof window === 'undefined') return;

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && isAuthenticated) {
        // If disconnected or closed, reconnect immediately and trigger a RESYNC notice
        if (!eventSourceRef.current || eventSourceRef.current.readyState === EventSource.CLOSED) {
          connect();
        }
        notifySubscribers({
          eventId: 'local-resync',
          type: 'RESYNC',
          occurredAt: new Date().toISOString(),
          resourceType: 'WINDOW_FOCUS',
        });
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [isAuthenticated, connect, notifySubscribers]);

  const subscribe = useCallback(
    (handler: RealtimeEventHandler, eventTypes?: (RealtimeEventType | string)[]) => {
      subscribersRef.current.set(handler, eventTypes);
      return () => {
        subscribersRef.current.delete(handler);
      };
    },
    []
  );

  return (
    <RealtimeContext.Provider value={{ connectionState, lastEvent, subscribe, reconnect }}>
      {children}
    </RealtimeContext.Provider>
  );
}

export function useRealtime(): RealtimeContextValue {
  const context = useContext(RealtimeContext);
  if (!context) {
    // Provide safe no-op fallback if used outside RealtimeProvider
    return {
      connectionState: 'disconnected',
      lastEvent: null,
      subscribe: () => () => {},
      reconnect: () => {},
    };
  }
  return context;
}

export function useRealtimeSubscription(
  eventTypes: (RealtimeEventType | string)[] | undefined,
  handler: RealtimeEventHandler
) {
  const { subscribe } = useRealtime();
  const handlerRef = useRef<RealtimeEventHandler>(handler);
  handlerRef.current = handler;

  useEffect(() => {
    const unsubscribe = subscribe((event) => {
      handlerRef.current(event);
    }, eventTypes);
    return () => {
      unsubscribe();
    };
  }, [subscribe, JSON.stringify(eventTypes)]);
}
