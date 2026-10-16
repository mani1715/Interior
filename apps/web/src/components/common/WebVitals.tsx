'use client';

import { useReportWebVitals } from 'next/web-vitals';

/**
 * Provider-neutral Web Vitals client performance instrumentation.
 * Collects Core Web Vitals (LCP, INP, CLS, FCP, TTFB) without transmitting
 * any personally identifiable attributes or telemetry to external providers.
 * Integration with future monitoring provider deferred to Phase 15.
 */
export function WebVitals() {
  useReportWebVitals((metric) => {
    // In local development, performance observations can be monitored
    if (process.env.NODE_ENV === 'development') {
      // Local development only: no external transmission
    }
  });

  return null;
}
