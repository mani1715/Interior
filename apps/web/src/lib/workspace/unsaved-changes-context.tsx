'use client';

import React, { createContext, useContext, useState, useCallback } from 'react';

interface DirtyRegistration {
  id: string;
  isDirty: boolean;
  message?: string;
}

interface UnsavedChangesContextType {
  isAnyDirty: boolean;
  registerDirty: (id: string, isDirty: boolean, message?: string) => () => void;
  setDirty: (id: string, isDirty: boolean, message?: string) => void;
  confirmDiscard: (action: () => void | Promise<void>) => void;
  modalOpen: boolean;
  cancelDiscard: () => void;
  proceedDiscard: () => void;
}

const UnsavedChangesContext = createContext<UnsavedChangesContextType | undefined>(undefined);

export function UnsavedChangesProvider({ children }: { children: React.ReactNode }) {
  const [dirtyMap, setDirtyMap] = useState<Map<string, DirtyRegistration>>(new Map());
  const [pendingAction, setPendingAction] = useState<(() => void | Promise<void>) | null>(null);
  const [modalOpen, setModalOpen] = useState(false);

  const setDirty = useCallback((id: string, isDirty: boolean, message?: string) => {
    setDirtyMap((prev) => {
      const next = new Map(prev);
      if (isDirty) {
        next.set(id, { id, isDirty: true, message });
      } else {
        next.delete(id);
      }
      return next;
    });
  }, []);

  const registerDirty = useCallback((id: string, isDirty: boolean, message?: string) => {
    setDirty(id, isDirty, message);
    return () => {
      setDirtyMap((prev) => {
        const next = new Map(prev);
        next.delete(id);
        return next;
      });
    };
  }, [setDirty]);

  const isAnyDirty = dirtyMap.size > 0;

  const confirmDiscard = useCallback((action: () => void | Promise<void>) => {
    if (dirtyMap.size === 0) {
      action();
      return;
    }
    setPendingAction(() => action);
    setModalOpen(true);
  }, [dirtyMap]);

  const cancelDiscard = useCallback(() => {
    setPendingAction(null);
    setModalOpen(false);
  }, []);

  const proceedDiscard = useCallback(() => {
    // Clear dirty state on user discard
    setDirtyMap(new Map());
    setModalOpen(false);
    if (pendingAction) {
      pendingAction();
      setPendingAction(null);
    }
  }, [pendingAction]);

  return (
    <UnsavedChangesContext.Provider
      value={{
        isAnyDirty,
        registerDirty,
        setDirty,
        confirmDiscard,
        modalOpen,
        cancelDiscard,
        proceedDiscard,
      }}
    >
      {children}
    </UnsavedChangesContext.Provider>
  );
}

export function useUnsavedChanges() {
  const context = useContext(UnsavedChangesContext);
  if (!context) {
    return {
      isAnyDirty: false,
      registerDirty: () => () => {},
      setDirty: () => {},
      confirmDiscard: (action: () => void | Promise<void>) => {
        action();
      },
      modalOpen: false,
      cancelDiscard: () => {},
      proceedDiscard: () => {},
    };
  }
  return context;
}
