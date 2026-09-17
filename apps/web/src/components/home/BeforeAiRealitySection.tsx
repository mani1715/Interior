import React from 'react';
import { BeforeAiReality } from '@/components/media/BeforeAiReality';
import { SectionHeader, SectionTitle, SectionEyebrow, SectionDescription } from '@/components/layout/Section';
import { rawSiteSvg, aiConceptSvg, executedSpaceSvg } from '@/app/design-system/mockData';

export function BeforeAiRealitySection() {
  return (
    <section className="w-full py-12 sm:py-16 md:py-24 bg-[var(--background)] border-b border-[var(--border)]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeader>
          <div>
            <SectionEyebrow>05 / Signature Differentiator</SectionEyebrow>
            <SectionTitle>Site Condition → AI Concept → Handcrafted Reality</SectionTitle>
            <SectionDescription>
              Build unmatched credibility with clients by showcasing the full story: from the raw unfinished site to the initial photorealistic AI visualization, and finally the executed space delivered on-site.
            </SectionDescription>
          </div>
        </SectionHeader>

        <BeforeAiReality
          title="The Complete Architectural Journey"
          subtitle="Real execution matching the client-approved visualization."
          beforeImage={rawSiteSvg}
          aiImage={aiConceptSvg}
          realityImage={executedSpaceSvg}
          beforeDescription="Raw masonry with exposed PVC conduit rough-ins and laser datum markings."
          aiDescription="AI concept simulation exploring bookmatched marble, walnut fluting, and 3000K warm cove lighting."
          realityDescription="Final built reality completed on-site with Italian Statuario composite and natural American walnut."
        />
      </div>
    </section>
  );
}
