import React from 'react';
import { Metadata } from 'next';
import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';
import { WorkspaceShell } from '@/components/workspace/WorkspaceShell';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: 'Professional Workspace | Elégance Interior Platform',
  description: 'Operational command center for registered interior professionals, studios, and architects.',
  robots: {
    index: false,
    follow: false,
  },
};

export default async function WorkspaceLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const cookieStore = await cookies();
  const sessionToken = cookieStore.get('__Host-session')?.value || cookieStore.get('session')?.value;

  // Server-first authentication boundary: Unauthenticated requests never render private workspace content
  if (!sessionToken) {
    redirect('/sign-in?returnUrl=/workspace');
  }

  return (
    <WorkspaceShell>
      {children}
    </WorkspaceShell>
  );
}
