import type { AuthUser } from './auth-context';

/**
 * Sanitizes a redirect URL to prevent open redirect vulnerabilities.
 * Only allows safe, relative internal application paths.
 */
export function sanitizeRedirectUrl(url?: string | null, defaultUrl: string = '/account'): string {
  if (!url || typeof url !== 'string') {
    return defaultUrl;
  }

  const trimmed = url.trim();

  // Reject CRLF header injection
  if (trimmed.includes('\r') || trimmed.includes('\n')) {
    return defaultUrl;
  }

  // Reject empty, protocol-relative, backslash, or windows network share paths
  if (!trimmed.startsWith('/') || trimmed.startsWith('//') || trimmed.startsWith('/\\') || trimmed.startsWith('\\')) {
    return defaultUrl;
  }

  // Disallow javascript:, data:, backslash, or encoded slashes/backslashes
  const lower = trimmed.toLowerCase();
  if (
    lower.includes('javascript:') ||
    lower.includes('data:') ||
    lower.includes('\\') ||
    lower.includes('%2f') ||
    lower.includes('%5c')
  ) {
    return defaultUrl;
  }

  return trimmed;
}

/**
 * Deterministically resolves post-authentication routing based on user global role,
 * studio membership, onboarding status, and sanitized returnUrl.
 */
export function resolveAuthDestination(user?: AuthUser | null, returnUrl?: string | null): string {
  // If user provided a specific non-default internal destination, respect it!
  if (returnUrl) {
    const sanitized = sanitizeRedirectUrl(returnUrl, '');
    if (sanitized && sanitized !== '/' && sanitized !== '/account' && sanitized !== '/sign-in') {
      return sanitized;
    }
  }

  if (!user) {
    return '/';
  }

  // 1. Platform Admin / Super Admin explicit admin entry
  if (user.roles && (user.roles.includes('SUPER_ADMIN') || user.roles.includes('ADMIN'))) {
    return '/admin';
  }

  // 2. Professional with studio membership -> professional workspace
  if (user.activeStudioId || (user.studios && user.studios.length > 0)) {
    return '/workspace';
  }

  // 3. User with DESIGNER role but incomplete onboarding (no studio created yet)
  if (user.roles && user.roles.includes('DESIGNER')) {
    return '/onboarding/professional';
  }

  // 4. Customer / default home account experience
  return '/account';
}
