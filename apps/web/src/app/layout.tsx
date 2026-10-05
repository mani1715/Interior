import type { Metadata, Viewport } from 'next';
import './globals.css';
import './platform-refinement.css';

export const metadata: Metadata = {
  title: 'Interior Design Platform',
  description: 'Portfolio, SEO Discovery & AI Visualization Platform for Interior Professionals',
  robots: {
    index: true,
    follow: true,
  },
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  viewportFit: 'cover',
};

import { AuthProvider } from '@/lib/auth/auth-context';
import { RealtimeProvider } from '@/lib/realtime/RealtimeProvider';
import { UnsavedChangesProvider } from '@/lib/workspace/unsaved-changes-context';

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <UnsavedChangesProvider>
            <RealtimeProvider>
              <div id="root-shell">{children}</div>
            </RealtimeProvider>
          </UnsavedChangesProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
