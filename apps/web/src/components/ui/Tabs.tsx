'use client';

import React, { useState } from 'react';

export interface TabItem {
  id: string;
  label: string;
  badge?: string | number;
}

export interface TabsProps {
  tabs: TabItem[];
  activeTab?: string;
  onChange?: (tabId: string) => void;
  className?: string;
}

export const Tabs: React.FC<TabsProps> = ({
  tabs,
  activeTab: controlledActiveTab,
  onChange,
  className = '',
}) => {
  const [internalActiveTab, setInternalActiveTab] = useState(tabs[0]?.id || '');
  const activeId = controlledActiveTab !== undefined ? controlledActiveTab : internalActiveTab;

  const handleTabClick = (id: string) => {
    if (controlledActiveTab === undefined) {
      setInternalActiveTab(id);
    }
    onChange?.(id);
  };

  return (
    <div
      role="tablist"
      style={{
        display: 'flex',
        borderBottom: '1px solid var(--border)',
        gap: 'var(--space-8)',
        overflowX: 'auto',
        WebkitOverflowScrolling: 'touch',
        scrollbarWidth: 'none',
        maxWidth: '100%',
        paddingBottom: '2px',
      }}
      className={`tabs-bar ${className}`}
    >
      {tabs.map((tab) => {
        const isActive = tab.id === activeId;
        return (
          <button
            key={tab.id}
            role="tab"
            aria-selected={isActive}
            aria-controls={`panel-${tab.id}`}
            id={`tab-${tab.id}`}
            onClick={() => handleTabClick(tab.id)}
            style={{
              padding: '10px 16px',
              fontFamily: 'var(--font-sans)',
              fontSize: 'var(--text-body-small)',
              fontWeight: isActive ? 600 : 500,
              color: isActive ? 'var(--brand)' : 'var(--text-secondary)',
              backgroundColor: 'transparent',
              border: 'none',
              borderBottom: isActive ? '2px solid var(--brand)' : '2px solid transparent',
              cursor: 'pointer',
              whiteSpace: 'nowrap',
              minHeight: 'var(--min-touch-target)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              transition: 'color var(--duration-fast) var(--ease-standard), border-color var(--duration-fast) var(--ease-standard)',
            }}
            className={`tab-btn ${isActive ? 'active' : ''}`}
          >
            {tab.label}
            {tab.badge !== undefined && (
              <span
                style={{
                  fontSize: 'var(--text-caption)',
                  padding: '2px 6px',
                  borderRadius: 'var(--radius-pill)',
                  backgroundColor: isActive ? 'var(--color-sage-neutral)' : 'var(--surface-alt)',
                  color: 'var(--text-primary)',
                }}
              >
                {tab.badge}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
};
