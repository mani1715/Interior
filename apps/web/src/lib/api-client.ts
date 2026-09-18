import { env } from './env';

export interface ErrorEnvelope {
  code: string;
  message: string;
  requestId: string;
  fieldErrors?: Array<{ field: string; message: string }>;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public envelope: ErrorEnvelope
  ) {
    super(envelope.message);
    this.name = 'ApiError';
  }
}

let cachedCsrfToken: string | null = null;

export async function getCsrfToken(): Promise<string> {
  if (typeof document !== 'undefined') {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
    if (match && match[1]) {
      return decodeURIComponent(match[1]);
    }
  }

  if (cachedCsrfToken) {
    return cachedCsrfToken;
  }

  try {
    const res = await fetch(`${env.apiBaseUrl}/auth/csrf`, {
      method: 'GET',
      credentials: 'include',
      headers: { 'Cache-Control': 'no-cache' },
    });
    if (res.ok) {
      const data = await res.json();
      cachedCsrfToken = data.csrfToken;
      return data.csrfToken;
    }
  } catch {
    // If backend is unavailable, return empty
  }
  return '';
}

export function resetCsrfToken() {
  cachedCsrfToken = null;
}

export function setCachedCsrfToken(token: string) {
  cachedCsrfToken = token;
}

/**
 * Standardized API client fetching backend REST endpoints (/api/v1).
 * Credentials (cookies) are automatically included for secure session auth.
 * Automatically forwards/extracts CSRF tokens and request IDs.
 */
export async function apiFetch<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const url = endpoint.startsWith('http') ? endpoint : `${env.apiBaseUrl}${endpoint}`;
  
  const headers = new Headers(options.headers);
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const method = (options.method || 'GET').toUpperCase();
  if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method) && !headers.has('X-CSRF-Token')) {
    const csrfToken = await getCsrfToken();
    if (csrfToken) {
      headers.set('X-CSRF-Token', csrfToken);
    }
  }

  // Include credentials for session cookie authentication across ports/hosts
  const config: RequestInit = {
    ...options,
    headers,
    credentials: 'include',
  };

  const response = await fetch(url, config);

  if (!response.ok) {
    let envelope: ErrorEnvelope;
    try {
      envelope = await response.json();
    } catch {
      envelope = {
        code: 'HTTP_ERROR',
        message: `HTTP error ${response.status}`,
        requestId: response.headers.get('X-Request-Id') || 'unknown',
      };
    }
    throw new ApiError(response.status, envelope);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}
