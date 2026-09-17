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

/**
 * Standardized API client fetching backend REST endpoints (/api/v1).
 * Credentials (cookies) are automatically included for same-origin session auth.
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

  // Include credentials for session cookie authentication
  const config: RequestInit = {
    ...options,
    headers,
    credentials: 'same-origin',
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
