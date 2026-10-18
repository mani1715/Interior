'use client';

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { apiFetch, resetCsrfToken, setStoredActiveStudioId, setActiveStudioIdHeader } from '../api-client';

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
  switchStudio?: (studioId: string) => Promise<void>;
  loginDevPersona: (persona: string) => Promise<AuthUser | null | void>;
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
        const userStudios = data.studios || [];
        const validStudioId = data.activeStudioId || (userStudios.length > 0 ? userStudios[0].studioId : null);
        setUser({
          id: data.id,
          displayName: data.displayName || 'User',
          email: data.email || null,
          status: data.status || 'ACTIVE',
          roles: data.roles || ['CUSTOMER'],
          permissions: data.permissions || [],
          activeStudioId: validStudioId,
          activeStudioRole: data.activeStudioRole || (userStudios.length > 0 ? userStudios[0].role : null),
          studios: userStudios,
          assurance: data.assurance || 'PASSWORD',
        });
        if (validStudioId) {
          setStoredActiveStudioId(validStudioId);
          setActiveStudioIdHeader(validStudioId);
        } else {
          setStoredActiveStudioId(null);
          setActiveStudioIdHeader(null);
        }
      } else {
        setUser(null);
        setStoredActiveStudioId(null);
        setActiveStudioIdHeader(null);
      }
    } catch {
      setUser(null);
      setStoredActiveStudioId(null);
      setActiveStudioIdHeader(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const switchStudio = useCallback(async (studioId: string) => {
    if (!studioId) return;
    setStoredActiveStudioId(studioId);
    setActiveStudioIdHeader(studioId);

    try {
      const data = await apiFetch<AuthResponse>('/auth/me', {
        headers: {
          'X-Studio-Id': studioId,
        },
      });

      if (data && data.authenticated && data.id) {
        setUser({
          id: data.id,
          displayName: data.displayName || 'User',
          email: data.email || null,
          status: data.status || 'ACTIVE',
          roles: data.roles || ['CUSTOMER'],
          permissions: data.permissions || [],
          activeStudioId: data.activeStudioId || studioId,
          activeStudioRole: data.activeStudioRole || null,
          studios: data.studios || [],
          assurance: data.assurance || 'PASSWORD',
        });
      }
    } finally {
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('studio-switched', { detail: { studioId } }));
      }
    }
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  const loginDevPersona = async (persona: string): Promise<AuthUser | null> => {
    setIsLoading(true);
    try {
      const data = await apiFetch<AuthResponse>('/auth/dev-login', {
        method: 'POST',
        body: JSON.stringify({ persona }),
      });
      if (data && data.id) {
        const authUser: AuthUser = {
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
        };
        setUser(authUser);
        return authUser;
      }
      return null;
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
      setStoredActiveStudioId(null);
      setActiveStudioIdHeader(null);
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
      setStoredActiveStudioId(null);
      setActiveStudioIdHeader(null);
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
        switchStudio,
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
  switchStudio: async () => {},
  loginDevPersona: async () => {},
  logout: async () => {},
  revokeAllSessions: async () => {},
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  return context || defaultAuthContext;
}
