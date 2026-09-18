'use client';

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { apiFetch, resetCsrfToken } from '../api-client';

export interface StudioSummary {
  studioId: string;
  studioName: string;
  studioSlug: string;
  role: string;
}

export interface AuthUser {
  id: string;
  displayName: string;
  email: string | null;
  status: string;
  roles: string[];
  permissions: string[];
  activeStudioId: string | null;
  activeStudioRole: string | null;
  studios: StudioSummary[];
  assurance: string;
}

interface AuthResponse {
  authenticated: boolean;
  id?: string;
  displayName?: string;
  email?: string;
  status?: string;
  roles?: string[];
  permissions?: string[];
  activeStudioId?: string;
  activeStudioRole?: string;
  studios?: StudioSummary[];
  assurance?: string;
}

export interface AuthContextType {
  user: AuthUser | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  refreshUser: () => Promise<void>;
  loginDevPersona: (persona: string) => Promise<void>;
  logout: () => Promise<void>;
  revokeAllSessions: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const refreshUser = useCallback(async () => {
    try {
      const data = await apiFetch<AuthResponse>('/auth/me');
      if (data && data.authenticated && data.id) {
        setUser({
          id: data.id,
          displayName: data.displayName || 'User',
          email: data.email || null,
          status: data.status || 'ACTIVE',
          roles: data.roles || ['CUSTOMER'],
          permissions: data.permissions || [],
          activeStudioId: data.activeStudioId || null,
          activeStudioRole: data.activeStudioRole || null,
          studios: data.studios || [],
          assurance: data.assurance || 'PASSWORD',
        });
      } else {
        setUser(null);
      }
    } catch {
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  const loginDevPersona = async (persona: string) => {
    setIsLoading(true);
    try {
      const data = await apiFetch<AuthResponse>('/auth/dev-login', {
        method: 'POST',
        body: JSON.stringify({ persona }),
      });
      if (data && data.id) {
        setUser({
          id: data.id,
          displayName: data.displayName || 'User',
          email: data.email || null,
          status: data.status || 'ACTIVE',
          roles: data.roles || ['CUSTOMER'],
          permissions: data.permissions || [],
          activeStudioId: data.activeStudioId || null,
          activeStudioRole: data.activeStudioRole || null,
          studios: data.studios || [],
          assurance: data.assurance || 'PASSWORD',
        });
      }
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    setIsLoading(true);
    try {
      await apiFetch('/auth/logout', { method: 'POST' });
    } catch {
      // Ignore network failures on logout
    } finally {
      resetCsrfToken();
      setUser(null);
      setIsLoading(false);
    }
  };

  const revokeAllSessions = async () => {
    setIsLoading(true);
    try {
      await apiFetch('/auth/revoke-all', { method: 'POST' });
    } catch {
      // Ignore network failures
    } finally {
      resetCsrfToken();
      setUser(null);
      setIsLoading(false);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        isAuthenticated: !!user,
        refreshUser,
        loginDevPersona,
        logout,
        revokeAllSessions,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

const defaultAuthContext: AuthContextType = {
  user: null,
  isLoading: false,
  isAuthenticated: false,
  refreshUser: async () => {},
  loginDevPersona: async () => {},
  logout: async () => {},
  revokeAllSessions: async () => {},
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  return context || defaultAuthContext;
}
