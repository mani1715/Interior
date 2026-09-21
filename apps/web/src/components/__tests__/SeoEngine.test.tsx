import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  serializeJsonLd,
  SafeJsonLd,
  buildStudioJsonLd,
  buildProjectJsonLd,
  buildBreadcrumbJsonLd,
} from '@/lib/seo/structured-data';
import {
  buildStudioMetadata,
  buildProjectMetadata,
  buildCategoryMetadata,
  buildLocationMetadata,
} from '@/lib/seo/metadata';
import robots from '@/app/robots';
import sitemap from '@/app/sitemap';
import SeoWorkspacePage from '@/app/workspace/seo/page';
import * as seoApi from '@/lib/seo/api';
import { PublicProjectDetailDto, PublicStudioDto, SeoStatusResponse } from '@/lib/seo/types';

vi.mock('@/lib/seo/api');

describe('SEO Engine — Structured Data & JSON-LD Security', () => {
  it('escapes <, >, and & characters to prevent script injection and XSS', () => {
    const dangerousData = {
      title: '</script><script>alert("xss")</script>',
      desc: 'Design & Build <modern> interiors',
    };

    const serialized = serializeJsonLd(dangerousData);
    expect(serialized).not.toContain('</script>');
    expect(serialized).toContain('\\u003c/script\\u003e');
    expect(serialized).toContain('\\u0026');
    expect(serialized).toContain('\\u003cmodern\\u003e');
  });

  it('SafeJsonLd component renders safe script tag', () => {
    const data = {
      '@context': 'https://schema.org',
      '@type': 'ProfessionalService',
      name: 'Studio <Apex>',
    };

    const { container } = render(<SafeJsonLd data={data} />);
    const script = container.querySelector('script[type="application/ld+json"]');
    expect(script).toBeTruthy();
    expect(script?.innerHTML).toContain('\\u003cApex\\u003e');
  });

  it('builds valid schema.org LocalBusiness / ProfessionalService structure', () => {
    const mockStudio: PublicStudioDto = {
      id: 's-1',
      name: 'Apex Studio',
      slug: 'apex-studio',
      city: 'Bengaluru',
      state: 'Karnataka',
      country: 'IN',
      travelAvailable: true,
      publishedAt: '2026-01-01T00:00:00Z',
      canonicalUrl: '/professionals/apex-studio',
      metaTitle: 'Apex Studio | Modern Architects in Bengaluru',
      metaDescription: 'Refined spaces crafted with precision.',
      indexingEnabled: true,
      contacts: [{ channelType: 'PHONE', contactValue: '+919876543210', sortOrder: 0 }],
      services: ['Living Room Design'],
      specialties: ['Modern Minimalist'],
      serviceAreas: ['Indiranagar'],
      projects: [{ id: 'p-1', title: 'Penthouse', slug: 'penthouse', categoryCode: 'LIVING_ROOM', coverImageUrl: 'https://img.com/p.webp' }],
    };

    const jsonLd = buildStudioJsonLd(mockStudio, 'https://interior.com');
    expect(jsonLd['@context']).toBe('https://schema.org');
    expect(jsonLd['@type']).toBe('ProfessionalService');
    expect(jsonLd.name).toBe('Apex Studio');
    expect(jsonLd.telephone).toBe('+919876543210');
    expect(jsonLd.image).toBe('https://img.com/p.webp');
  });

  it('builds valid CreativeWork schema for projects', () => {
    const mockProject: PublicProjectDetailDto = {
      id: 'p-1',
      slug: 'modern-villa',
      title: 'Modern Villa',
      categoryCode: 'LIVING_ROOM',
      shortDescription: 'Contemporary luxury villa',
      fullDescription: 'Complete overhaul',
      city: 'Bengaluru',
      state: 'Karnataka',
      country: 'IN',
      completionYear: 2025,
      styleCodes: ['CONTEMPORARY'],
      studio: {
        studioId: 's-1',
        name: 'Apex Studio',
        slug: 'apex-studio',
        city: 'Bengaluru',
        state: 'Karnataka',
      },
      canonicalUrl: '/projects/modern-villa',
      metaTitle: 'Modern Villa | Apex Studio',
      metaDescription: 'Luxury living room project.',
      media: [
        {
          id: 'm-1',
          mediaType: 'REAL_PROJECT',
          isAiConcept: false,
          sortOrder: 0,
          isCover: true,
          altText: 'Living room panoramic',
          originalWidth: 1920,
          originalHeight: 1080,
          thumbnailUrl: 'https://img.com/thumb.webp',
          mediumUrl: 'https://img.com/med.webp',
          largeUrl: 'https://img.com/large.webp',
        },
      ],
    };

    const jsonLd = buildProjectJsonLd(mockProject, 'https://interior.com');
    expect(jsonLd['@context']).toBe('https://schema.org');
    expect(jsonLd['@type']).toBe('CreativeWork');
    expect(jsonLd.name).toBe('Modern Villa');
    expect((jsonLd.creator as any)?.name).toBe('Apex Studio');
    expect((jsonLd.creator as any)?.url).toBe('https://interior.com/professionals/apex-studio');
  });

  it('builds valid BreadcrumbList schema', () => {
    const breadcrumb = buildBreadcrumbJsonLd(
      [
        { name: 'Home', url: '/' },
        { name: 'Projects', url: '/projects' },
        { name: 'Modern Villa', url: '/projects/modern-villa' },
      ],
      'https://interior.com'
    );

    expect(breadcrumb['@type']).toBe('BreadcrumbList');
    expect(breadcrumb.itemListElement.length).toBe(3);
    expect(breadcrumb.itemListElement[0].item).toBe('https://interior.com/');
    expect(breadcrumb.itemListElement[2].item).toBe('https://interior.com/projects/modern-villa');
  });
});

describe('SEO Engine — Truthful Metadata Generation', () => {
  it('generates studio metadata with canonical URL and indexing controls', () => {
    const studio: PublicStudioDto = {
      id: 's-1',
      name: 'Studio Terra',
      slug: 'studio-terra',
      city: 'Mumbai',
      travelAvailable: true,
      publishedAt: '2026-01-01T00:00:00Z',
      canonicalUrl: '/professionals/studio-terra',
      metaTitle: 'Studio Terra | Sustainable Architecture in Mumbai',
      metaDescription: 'Sustainable residential interiors.',
      indexingEnabled: true,
      contacts: [],
      services: [],
      specialties: [],
      serviceAreas: [],
      projects: [],
    };

    const meta = buildStudioMetadata(studio);
    expect(meta.title).toBe('Studio Terra | Sustainable Architecture in Mumbai');
    expect(meta.alternates?.canonical).toContain('/professionals/studio-terra');
    expect(meta.robots).toEqual({ index: true, follow: true });

    // When indexing is disabled
    const nonIndexed = buildStudioMetadata({ ...studio, indexingEnabled: false });
    expect(nonIndexed.robots).toEqual({ index: false, follow: false });
  });

  it('generates category and location metadata', () => {
    const catMeta = buildCategoryMetadata('Living Room', 'living-room');
    expect(catMeta.title).toContain('Living Room');
    expect(catMeta.alternates?.canonical).toContain('/categories/living-room');

    const locMeta = buildLocationMetadata('Bengaluru');
    expect(locMeta.title).toContain('Bengaluru');
    expect(locMeta.alternates?.canonical).toContain('/locations/bengaluru');
  });
});

describe('SEO Engine — Robots & Sitemap Configuration', () => {
  it('robots config allows public exploration and strictly disallows private / workspace routes', () => {
    const conf = robots();
    expect(conf.rules).toBeDefined();
    const rule = (conf.rules as any)[0];
    expect(rule.allow).toContain('/');
    expect(rule.allow).toContain('/professionals/');
    expect(rule.allow).toContain('/projects/');
    expect(rule.disallow).toContain('/workspace/');
    expect(rule.disallow).toContain('/account/');
    expect(rule.disallow).toContain('/auth/');
    expect(conf.sitemap).toContain('/sitemap.xml');
  });

  it('sitemap generator combines static routes with live dynamic published entries', async () => {
    vi.mocked(seoApi.fetchSitemapEntries).mockResolvedValue([
      {
        path: '/professionals/apex-designs',
        priority: '0.9',
        changefreq: 'weekly',
        lastModified: '2026-03-01T00:00:00Z',
      },
    ]);

    const items = await sitemap();
    expect(items.some((i) => i.url.includes('/categories/living-room'))).toBe(true);
    expect(items.some((i) => i.url.includes('/locations/bengaluru'))).toBe(true);
    expect(items.some((i) => i.url.includes('/professionals/apex-designs'))).toBe(true);
  });
});

describe('SEO Center Workspace UI', () => {
  const mockStatus: SeoStatusResponse = {
    studioId: 'studio-1',
    studioName: 'Apex Designs',
    studioSlug: 'apex-designs',
    publicationStatus: 'UNPUBLISHED',
    publishedAt: null,
    canonicalUrl: '/professionals/apex-designs',
    metaTitle: 'Apex Designs | Interior Architecture in Bengaluru',
    metaDescription: 'Refined architectural interiors.',
    metaTitleOverride: null,
    metaDescriptionOverride: null,
    canonicalUrlOverride: null,
    indexingEnabled: true,
    isPublishable: true,
    checklist: [
      { key: 'PROFILE', label: 'Studio Profile Completeness', passed: true, message: 'Profile complete' },
      { key: 'PORTFOLIO', label: 'Portfolio Ready', passed: true, message: 'Portfolio ready' },
      { key: 'CONTACT', label: 'Public Contact Channel', passed: true, message: 'Contact public' },
      { key: 'PROJECTS', label: 'Published Projects', passed: true, message: '1 project ready' },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders SEO checklist diagnostics and allows publishing', async () => {
    vi.mocked(seoApi.fetchSeoStatus).mockResolvedValue(mockStatus);
    vi.mocked(seoApi.publishStudio).mockResolvedValue({
      ...mockStatus,
      publicationStatus: 'PUBLISHED',
      publishedAt: '2026-03-21T10:00:00Z',
    });

    render(<SeoWorkspacePage />);

    expect(await screen.findByText('SEO Center')).toBeDefined();
    expect(screen.getByText('Publication & SEO Readiness')).toBeDefined();
    expect(screen.getByText('Studio Profile Completeness')).toBeDefined();
    expect(screen.getByText('Unpublished (Draft)')).toBeDefined();

    const publishBtn = screen.getByRole('button', { name: /Publish Studio/i });
    expect(publishBtn).toBeDefined();

    fireEvent.click(publishBtn);

    await waitFor(() => {
      expect(seoApi.publishStudio).toHaveBeenCalled();
      expect(screen.getByText(/Published & Indexed/i)).toBeDefined();
    });
  });

  it('submits search appearance overrides', async () => {
    vi.mocked(seoApi.fetchSeoStatus).mockResolvedValue(mockStatus);
    vi.mocked(seoApi.updateSeoSettings).mockResolvedValue({
      ...mockStatus,
      metaTitleOverride: 'Custom SEO Title',
      metaDescriptionOverride: 'Custom SEO Description',
    });

    render(<SeoWorkspacePage />);

    expect(await screen.findByText('SEO Center')).toBeDefined();

    const titleInput = screen.getByPlaceholderText(/Apex Designs \| Interior Architecture in Bengaluru/i);
    fireEvent.change(titleInput, { target: { value: 'Custom SEO Title' } });

    const descInput = screen.getByPlaceholderText(/Refined architectural interiors/i);
    fireEvent.change(descInput, { target: { value: 'Custom SEO Description' } });

    const saveBtn = screen.getByRole('button', { name: /Save Appearance Settings/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(seoApi.updateSeoSettings).toHaveBeenCalledWith({
        metaTitleOverride: 'Custom SEO Title',
        metaDescriptionOverride: 'Custom SEO Description',
        indexingEnabled: true,
      });
      expect(screen.getByText(/SEO settings saved successfully/i)).toBeDefined();
    });
  });
});
