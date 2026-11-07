import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from '../auth-context';
import { apiFetch, resetCsrfToken } from '../../api-client';

vi.mock('../../api-client', () => ({
  apiFetch: vi.fn(), resetCsrfToken: vi.fn(),
  setStoredActiveStudioId: vi.fn(), setActiveStudioIdHeader: vi.fn(),
}));

function Login() {
  const auth = useAuth();
  return <button disabled={auth.isLoading} onClick={() => auth.loginDevPersona('designer_owner')}>{auth.user?.displayName || 'Login'}</button>;
}

describe('development session transition', () => {
  it('invalidates the anonymous CSRF token when a new authenticated session starts', async () => {
    vi.mocked(apiFetch).mockResolvedValueOnce({ authenticated: false })
      .mockResolvedValueOnce({ authenticated: true, id: 'owner', displayName: 'Owner' });
    render(<AuthProvider><Login /></AuthProvider>);
    await waitFor(() => expect((screen.getByRole('button') as HTMLButtonElement).disabled).toBe(false));
    expect(resetCsrfToken).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button'));
    await screen.findByRole('button', { name: 'Owner' });
    expect(resetCsrfToken).toHaveBeenCalledTimes(1);
  });
});
