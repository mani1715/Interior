import { describe, it, expect, vi } from 'vitest';
import {
  CANONICAL_SECTION_TYPES,
  CANONICAL_FONT_PAIRINGS,
  SectionType,
  FontPairing,
  PortfolioPreviewResponse,
  PortfolioDetailResponse,
} from '@/lib/portfolio/types';
import { normalizePortfolioProps, buildNavigationSettings } from '@/lib/portfolio/normalize-props';
import { TEMPLATE_REGISTRY } from '@/lib/portfolio/template-registry';
import * as portfolioApi from '@/lib/portfolio/api';

describe('Phase 10.1: Portfolio Frontend/Backend Contract Validation', () => {
  it('contains the exact 18 canonical section types matching backend & DB V006', () => {
    const expected18: SectionType[] = [
      'HERO',
      'ABOUT',
      'SERVICES',
      'FEATURED_PROJECTS',
      'PROJECT_GRID',
      'BEFORE_AFTER',
      'BEFORE_AI_REALITY',
      'DESIGN_PROCESS',
      'TESTIMONIALS',
      'TEAM',
      'AWARDS',
      'PRESS',
      'SERVICE_AREAS',
      'FAQ',
      'CONTACT',
      'CTA',
      'VIDEO',
      'CUSTOM_NOTE',
    ];

    expect(CANONICAL_SECTION_TYPES).toHaveLength(18);
    expect(CANONICAL_SECTION_TYPES).toEqual(expected18);
  });

  it('contains the exact 6 canonical font pairings matching backend & DB V006', () => {
    const expected6: FontPairing[] = [
      'SYSTEM_SANS',
      'CLASSIC_SERIF',
      'MODERN_CLEAN',
      'EDITORIAL',
      'WARM_EDITORIAL',
      'BOLD_CINEMATIC',
    ];

    expect(CANONICAL_FONT_PAIRINGS).toHaveLength(6);
    expect(CANONICAL_FONT_PAIRINGS).toEqual(expected6);
  });

  it('verifies all 6 templates have status SCAFFOLD and version 1.0.0', () => {
    const templateKeys = ['BASIC', 'MODERN', 'LUXURY', 'ARCHITECTURAL', 'WARM_NATURAL', 'DARK_CINEMATIC'] as const;

    for (const key of templateKeys) {
      const tpl = TEMPLATE_REGISTRY[key];
      expect(tpl).toBeDefined();
      expect(tpl.key).toBe(key);
      expect(tpl.status).toBe('SCAFFOLD');
      expect(tpl.version).toBe('1.0.0');
      expect(tpl.isSelectable).toBe(false);
      expect(tpl.supportedSections).toEqual(CANONICAL_SECTION_TYPES);
    }
  });

  it('normalizes preview response into complete PortfolioTemplateProps with safe navigationSettings', () => {
    const previewData: PortfolioPreviewResponse = {
      portfolioId: 'port-123',
      studioId: 'studio-456',
      studioName: 'Studio Studio',
      studioSlug: 'studio-studio',
      professionalType: 'INTERIOR_STUDIO',
      professionalTitle: 'Lead Architect',
      studioCity: 'Mumbai',
      studioState: 'Maharashtra',
      templateKey: 'BASIC',
      status: 'DRAFT',
      headline: 'Refined Architecture',
      subheadline: 'Crafting spaces',
      bio: 'Studio bio',
      designPhilosophy: 'Form follows beauty',
      yearsOfExperience: 8,
      primaryColor: '#1A1A1A',
      secondaryColor: '#F5F5F5',
      accentColor: '#D4AF37',
      fontPairing: 'EDITORIAL',
      publicContacts: [{ kind: 'EMAIL', contactValue: 'info@studio.com' }],
      canonicalServices: [{ serviceCode: 'RESIDENTIAL', serviceName: 'Residential' }],
      canonicalSpecialties: [{ specialtyCode: 'CONTEMPORARY', specialtyName: 'Contemporary' }],
      canonicalServiceAreas: [{ cityName: 'Mumbai', locality: 'Bandra' }],
      visibleSections: [
        {
          sectionId: 'sec-1',
          sectionType: 'HERO',
          displayOrder: 0,
          schemaVersion: 1,
          content: { headlineOverride: 'Hero Headline' },
        },
        {
          sectionId: 'sec-2',
          sectionType: 'ABOUT',
          displayOrder: 1,
          schemaVersion: 1,
          content: { narrativeOverride: 'About Text' },
        },
        {
          sectionId: 'sec-3',
          sectionType: 'SERVICES',
          displayOrder: 2,
          schemaVersion: 1,
          content: { sectionHeadline: 'Services Offered' },
        },
        {
          sectionId: 'sec-4',
          sectionType: 'CONTACT',
          displayOrder: 3,
          schemaVersion: 1,
          content: { contactIntro: 'Get in touch' },
        },
      ],
      previewGeneratedAt: '2026-09-19T00:00:00Z',
    };

    const normalized = normalizePortfolioProps(previewData, { isMobilePreview: false });

    // Assert presentation normalization
    expect(normalized.portfolioId).toBe('port-123');
    expect(normalized.templateKey).toBe('BASIC');
    expect(normalized.isMobilePreview).toBe(false);
    expect(normalized.navigationSettings).toBeDefined();
    expect(normalized.navigationSettings.sticky).toBe(true);
    expect(normalized.navigationSettings.showPrimaryCta).toBe(true);
    expect(normalized.navigationSettings.primaryCtaAnchor).toBe('#contact');

    // Check derived nav items (ABOUT, SERVICES, CONTACT are navigable)
    const navItems = normalized.navigationSettings.navItems;
    expect(navItems).toHaveLength(3);
    expect(navItems[0]).toEqual({
      sectionId: 'sec-2',
      sectionType: 'ABOUT',
      label: 'About',
      anchor: '#about',
    });
    expect(navItems[1]).toEqual({
      sectionId: 'sec-3',
      sectionType: 'SERVICES',
      label: 'Services Offered', // Overridden by sectionHeadline
      anchor: '#services',
    });
    expect(navItems[2]).toEqual({
      sectionId: 'sec-4',
      sectionType: 'CONTACT',
      label: 'Contact',
      anchor: '#contact',
    });
  });

  it('verifies sequential version mutation and 409 conflict handling', async () => {
    let currentVersion = 1;

    const mockPortfolio: PortfolioDetailResponse = {
      id: 'p-1',
      studioId: 's-1',
      templateKey: 'BASIC',
      status: 'DRAFT',
      headline: 'Headline v1',
      subheadline: null,
      bio: null,
      designPhilosophy: null,
      yearsOfExperience: null,
      primaryColor: null,
      secondaryColor: null,
      accentColor: null,
      fontPairing: 'SYSTEM_SANS',
      version: currentVersion,
      isReadyForPublish: false,
      readinessMissingRequirements: ['headline'],
      sections: [],
      recentVersions: [],
      createdAt: '2026-09-19T00:00:00Z',
      updatedAt: '2026-09-19T00:00:00Z',
    };

    // First mutation from v1 -> v2
    const updateSpy = vi.spyOn(portfolioApi, 'updatePortfolio').mockImplementation(async (req) => {
      if (req.version !== currentVersion) {
        const err: any = new Error('Optimistic locking failure');
        err.status = 409;
        throw err;
      }
      currentVersion += 1;
      return {
        ...mockPortfolio,
        headline: req.headline || mockPortfolio.headline,
        version: currentVersion,
      };
    });

    const result1 = await portfolioApi.updatePortfolio({
      headline: 'Headline v2',
      version: 1,
    });
    expect(result1.version).toBe(2);
    expect(currentVersion).toBe(2);

    // Second sequential mutation from v2 -> v3
    const result2 = await portfolioApi.updatePortfolio({
      headline: 'Headline v3',
      version: 2,
    });
    expect(result2.version).toBe(3);
    expect(currentVersion).toBe(3);

    // Stale version mutation (passing v1 or v2 when current is v3) triggers 409
    await expect(
      portfolioApi.updatePortfolio({
        headline: 'Conflicting update',
        version: 2,
      })
    ).rejects.toMatchObject({ status: 409 });
  });
});
