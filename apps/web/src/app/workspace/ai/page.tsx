import React, { Suspense } from 'react';
import { AiVisualizerClient } from '@/components/ai/AiVisualizerClient';
import { Loader2 } from 'lucide-react';

export default function AiWorkspacePage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[#FAF8F5] flex items-center justify-center">
          <Loader2 className="w-8 h-8 animate-spin text-[#B88A5A]" />
        </div>
      }
    >
      <AiVisualizerClient />
    </Suspense>
  );
}
