import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { RealtimeProvider, useRealtime, useRealtimeSubscription, RealtimeEventPayload } from '@/lib/realtime/RealtimeProvider';
import { AuthContext, AuthContextType } from '@/lib/auth/auth-context';

// Mock EventSource
class MockEventSource {
  static instances: MockEventSource[] = [];
  url: string;
  config: any;
  readyState: number = 0;
  onopen: (() => void) | null = null;
  onmessage: ((e: MessageEvent) => void) | null = null;
  onerror: ((e: any) => void) | null = null;

  constructor(url: string, config?: any) {
    this.url = url;
    this.config = config;
    MockEventSource.instances.push(this);
    setTimeout(() => {
      this.readyState = 1;
      if (this.onopen) this.onopen();
    }, 10);
  }

  close() {
    this.readyState = 2;
  }

  simulateMessage(data: RealtimeEventPayload) {
    if (this.onmessage) {
      this.onmessage(new MessageEvent('message', { data: JSON.stringify(data) }));
    }
  }

  simulateError() {
    this.readyState = 2;
    if (this.onerror) {
      this.onerror(new Event('error'));
    }
  }
}

const mockAuthValue: AuthContextType = {
  user: {
    id: 'user-123',
    displayName: 'Aarav Designer',
    email: 'aarav@example.com',
    roles: ['DESIGNER'],
    status: 'ACTIVE',
    permissions: [],
    activeStudioId: null,
    activeStudioRole: null,
    studios: [],
    assurance: 'PASSWORD',
  },
  isAuthenticated: true,
  isLoading: false,
  refreshUser: vi.fn(),
  loginDevPersona: vi.fn(),
  logout: vi.fn(),
  revokeAllSessions: vi.fn(),
};

describe('Realtime SSE Client & Subscription Hooks', () => {
  beforeEach(() => {
    MockEventSource.instances = [];
    vi.stubGlobal('EventSource', MockEventSource);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('connects to SSE stream when user is authenticated', async () => {
    function TestConsumer() {
      const { connectionState } = useRealtime();
      return <div data-testid="status">{connectionState}</div>;
    }

    render(
      <AuthContext.Provider value={mockAuthValue}>
        <RealtimeProvider>
          <TestConsumer />
        </RealtimeProvider>
      </AuthContext.Provider>
    );

    expect(MockEventSource.instances.length).toBe(1);
    expect(MockEventSource.instances[0].url).toBe('/api/v1/events/stream');

    await act(async () => {
      await new Promise((r) => setTimeout(r, 20));
    });

    expect(screen.getByTestId('status').textContent).toBe('connected');
  });

  it('receives filtered events in useRealtimeSubscription without reload', async () => {
    const receivedEvents: RealtimeEventPayload[] = [];

    function SubscriptionConsumer() {
      useRealtimeSubscription(['NOTIFICATION_CREATED', 'AI_JOB_COMPLETED'], (e) => {
        receivedEvents.push(e);
      });
      return <div data-testid="consumer">Subscribed</div>;
    }

    render(
      <AuthContext.Provider value={mockAuthValue}>
        <RealtimeProvider>
          <SubscriptionConsumer />
        </RealtimeProvider>
      </AuthContext.Provider>
    );

    await act(async () => {
      await new Promise((r) => setTimeout(r, 20));
    });

    const es = MockEventSource.instances[0];

    // 1. Dispatch matching event
    act(() => {
      es.simulateMessage({
        eventId: 'evt-1',
        type: 'NOTIFICATION_CREATED',
        occurredAt: '2026-10-04T09:40:00Z',
        recipientUserId: 'user-123',
      });
    });

    expect(receivedEvents.length).toBe(1);
    expect(receivedEvents[0].eventId).toBe('evt-1');

    // 2. Dispatch non-matching event (should be ignored by filter)
    act(() => {
      es.simulateMessage({
        eventId: 'evt-2',
        type: 'LEAD_CREATED',
        occurredAt: '2026-10-04T09:40:01Z',
      });
    });

    expect(receivedEvents.length).toBe(1);

    // 3. Dispatch second matching event
    act(() => {
      es.simulateMessage({
        eventId: 'evt-3',
        type: 'AI_JOB_COMPLETED',
        occurredAt: '2026-10-04T09:40:02Z',
        resourceId: 'job-999',
      });
    });

    expect(receivedEvents.length).toBe(2);
    expect(receivedEvents[1].resourceId).toBe('job-999');
  });

  it('ignores heartbeat messages in lastEvent state', async () => {
    function HeartbeatConsumer() {
      const { lastEvent } = useRealtime();
      return <div data-testid="last">{lastEvent ? lastEvent.type : 'none'}</div>;
    }

    render(
      <AuthContext.Provider value={mockAuthValue}>
        <RealtimeProvider>
          <HeartbeatConsumer />
        </RealtimeProvider>
      </AuthContext.Provider>
    );

    await act(async () => {
      await new Promise((r) => setTimeout(r, 20));
    });

    const es = MockEventSource.instances[0];

    act(() => {
      es.simulateMessage({
        eventId: 'hb-1',
        type: 'HEARTBEAT',
        occurredAt: '2026-10-04T09:40:00Z',
      });
    });

    expect(screen.getByTestId('last').textContent).toBe('none');
  });

  it('gracefully handles connection termination and reconnect on error', async () => {
    function ReconnectConsumer() {
      const { connectionState } = useRealtime();
      return <div data-testid="state">{connectionState}</div>;
    }

    render(
      <AuthContext.Provider value={mockAuthValue}>
        <RealtimeProvider>
          <ReconnectConsumer />
        </RealtimeProvider>
      </AuthContext.Provider>
    );

    await act(async () => {
      await new Promise((r) => setTimeout(r, 20));
    });

    expect(screen.getByTestId('state').textContent).toBe('connected');

    act(() => {
      MockEventSource.instances[0].simulateError();
    });

    expect(screen.getByTestId('state').textContent).toBe('disconnected');
  });
});
