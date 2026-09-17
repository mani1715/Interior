import type { Metadata } from 'next';
import { DesignSystemClient } from './DesignSystemClient';

export const metadata: Metadata = {
  title: 'Design System Showcase — Elégance Platform',
  description: 'Internal visual specification and component library showcase for Elégance Interior Design Platform.',
  robots: {
    index: false,
    follow: false,
  },
};

export default function DesignSystemPage() {
  return <DesignSystemClient />;
}
