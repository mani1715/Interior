'use client';

import { useEffect, useRef } from 'react';
import { trackPublicEvent } from '@/lib/analytics/api';
import { AnalyticsEventType } from '@/lib/analytics/types';

interface PublicTelemetryTrackerProps {
  eventType: AnalyticsEventType;
  entityType?: 'STUDIO' | 'PROJECT';
  entitySlug: string;
  metadata?: Record<string, unknown>;
}

function getSessionToken(): string {
  if (typeof window === 'undefined') return '';
  try {
    let session = window.sessionStorage.getItem('interior_telemetry_session');
    if (!session) {
      session = 'sess_' + Math.random().toString(36).substring(2, 15) + Date.now().toString(36);
      window.sessionStorage.setItem('interior_telemetry_session', session);
    }
    return session;
  } catch {
    return '';
  }
}

function resolveDeviceClass(): 'MOBILE' | 'TABLET' | 'DESKTOP' | 'UNKNOWN' {
  if (typeof window === 'undefined') return 'UNKNOWN';
  const width = window.innerWidth;
  if (width < 768) return 'MOBILE';
  if (width < 1024) return 'TABLET';
  return 'DESKTOP';
}

export function PublicTelemetryTracker({
  eventType,
  entityType,
  entitySlug,
  metadata,
}: PublicTelemetryTrackerProps) {
  const trackedRef = useRef(false);

  useEffect(() => {
    if (trackedRef.current) return;
    trackedRef.current = true;

    const sessionHash = getSessionToken();
    const referrer = typeof document !== 'undefined' ? document.referrer : undefined;
    const deviceClass = resolveDeviceClass();

    try {
      const promise = trackPublicEvent({
        eventType,
        entityType,
        entitySlug,
        sessionHash: sessionHash || undefined,
        referrer: referrer || undefined,
        deviceClass,
        metadata,
      });
      if (promise && typeof promise.catch === 'function') {
        promise.catch(() => {
          // Swallowed safely
        });
      }
    } catch {
      // Swallowed safely
    }
  }, [eventType, entityType, entitySlug, metadata]);

  return null;
}
