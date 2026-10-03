import React from 'react';
import type { Metadata } from 'next';
import { PublicHeader } from '@/components/navigation/PublicHeader';
import { Footer } from '@/components/home/Footer';

export const metadata: Metadata = {
  title: 'Account & Collections | Elégance Interior Platform',
  description: 'Manage your verified account profile, identity, and private inspiration mood boards.',
};

export default function AccountLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-[#FAF8F5] text-[#1F1F1F] flex flex-col selection:bg-[#B88A5A]/20 selection:text-[#1F1F1F]">
      <PublicHeader currentPath="/account" />
      <main id="main-content" className="flex-1 w-full">
        {children}
      </main>
      <Footer />
    </div>
  );
}
