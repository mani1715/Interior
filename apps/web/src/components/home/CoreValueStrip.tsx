import React from 'react';
import { FolderKanban, Search, Sparkles, MessageSquareQuote } from 'lucide-react';

export function CoreValueStrip() {
  const pillars = [
    {
      icon: FolderKanban,
      title: 'Premium Portfolio',
      desc: 'Build a dedicated architectural home for your work with your branding and watermarks.',
    },
    {
      icon: Search,
      title: 'Google Discoverability',
      desc: 'Each project generates clean, indexable pages structured for local search intent.',
    },
    {
      icon: Sparkles,
      title: 'AI Visualization',
      desc: 'Explore materials, finishes, and layout alternatives directly on site photographs.',
    },
    {
      icon: MessageSquareQuote,
      title: 'Direct Client Leads',
      desc: 'Turn design admiration into direct WhatsApp chats and structured project briefs.',
    },
  ];

  return (
    <section className="w-full bg-[var(--surface-alt)] border-y border-[var(--border)] py-8 sm:py-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 sm:gap-8">
          {pillars.map((item, idx) => {
            const Icon = item.icon;
            return (
              <div key={idx} className="flex items-start gap-3.5">
                <div className="w-10 h-10 rounded-xl bg-[var(--surface)] border border-[var(--border)] flex items-center justify-center text-[var(--brand)] flex-shrink-0 shadow-sm">
                  <Icon className="w-5 h-5 stroke-[2]" />
                </div>
                <div>
                  <h3 className="font-serif text-sm sm:text-base font-semibold text-[var(--foreground)] mb-1">
                    {item.title}
                  </h3>
                  <p className="text-xs text-[var(--muted)] leading-relaxed">
                    {item.desc}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
