import {
  PortfolioPreviewResponse,
  PreviewSectionDto,
  SectionType,
} from './types';
import {
  PortfolioTemplateProps,
  NavigationSettings,
  NavItem,
} from './template-contract';

function deriveNavLabel(type: SectionType, content: Record<string, any>): string {
  if (content?.sectionHeadline && typeof content.sectionHeadline === 'string') {
    return content.sectionHeadline;
  }
  switch (type) {
    case 'ABOUT':
      return 'About';
    case 'SERVICES':
      return 'Services';
    case 'FEATURED_PROJECTS':
    case 'PROJECT_GRID':
      return 'Projects';
    case 'DESIGN_PROCESS':
      return 'Process';
    case 'BEFORE_AFTER':
      return 'Transformations';
    case 'TESTIMONIALS':
      return 'Testimonials';
    case 'TEAM':
      return 'Team';
    case 'AWARDS':
      return 'Awards';
    case 'PRESS':
      return 'Press';
    case 'SERVICE_AREAS':
      return 'Areas';
    case 'FAQ':
      return 'FAQ';
    case 'CONTACT':
      return 'Contact';
    case 'CTA':
      return 'Inquire';
    case 'VIDEO':
      return 'Video';
    case 'CUSTOM_NOTE':
      return content?.title || 'Note';
    default:
      return type.replace(/_/g, ' ');
  }
}

function deriveNavAnchor(type: SectionType): string {
  switch (type) {
    case 'ABOUT':
      return '#about';
    case 'SERVICES':
      return '#services';
    case 'FEATURED_PROJECTS':
    case 'PROJECT_GRID':
      return '#projects';
    case 'DESIGN_PROCESS':
      return '#process';
    case 'BEFORE_AFTER':
      return '#transformations';
    case 'TESTIMONIALS':
      return '#testimonials';
    case 'TEAM':
      return '#team';
    case 'SERVICE_AREAS':
      return '#areas';
    case 'FAQ':
      return '#faq';
    case 'CONTACT':
    case 'CTA':
      return '#contact';
    default:
      return `#${type.toLowerCase().replace(/_/g, '-')}`;
  }
}

export function buildNavigationSettings(visibleSections: PreviewSectionDto[]): NavigationSettings {
  const sorted = [...visibleSections].sort((a, b) => a.displayOrder - b.displayOrder);

  // Navigable section types
  const navigableTypes = new Set<SectionType>([
    'ABOUT',
    'SERVICES',
    'FEATURED_PROJECTS',
    'PROJECT_GRID',
    'DESIGN_PROCESS',
    'BEFORE_AFTER',
    'TESTIMONIALS',
    'TEAM',
    'SERVICE_AREAS',
    'FAQ',
    'CONTACT',
  ]);

  const navItems: NavItem[] = [];
  const seenAnchors = new Set<string>();

  for (const s of sorted) {
    if (navigableTypes.has(s.sectionType)) {
      const anchor = deriveNavAnchor(s.sectionType);
      if (!seenAnchors.has(anchor)) {
        seenAnchors.add(anchor);
        navItems.push({
          sectionId: s.sectionId,
          sectionType: s.sectionType,
          label: deriveNavLabel(s.sectionType, s.content),
          anchor,
        });
      }
    }
  }

  return {
    sticky: true,
    navItems,
    showPrimaryCta: true,
    primaryCtaLabel: 'Request Consultation',
    primaryCtaAnchor: '#contact',
  };
}

export function normalizePortfolioProps(
  preview: PortfolioPreviewResponse,
  options: { isMobilePreview?: boolean } = {}
): PortfolioTemplateProps {
  const visibleSections = preview.visibleSections || [];
  const navigationSettings = buildNavigationSettings(visibleSections);

  return {
    portfolioId: preview.portfolioId,
    studioId: preview.studioId,
    studioName: preview.studioName || 'Interior Design Studio',
    studioSlug: preview.studioSlug || '',
    professionalType: preview.professionalType,
    professionalTitle: preview.professionalTitle,
    studioCity: preview.studioCity,
    studioState: preview.studioState,
    templateKey: preview.templateKey,
    headline: preview.headline,
    subheadline: preview.subheadline,
    bio: preview.bio,
    designPhilosophy: preview.designPhilosophy,
    yearsOfExperience: preview.yearsOfExperience,
    primaryColor: preview.primaryColor || '#1F2937',
    secondaryColor: preview.secondaryColor || '#F3F4F6',
    accentColor: preview.accentColor || '#C5A880',
    fontPairing: preview.fontPairing || 'SYSTEM_SANS',
    navigationSettings,
    publicContacts: preview.publicContacts || [],
    canonicalServices: preview.canonicalServices || [],
    canonicalSpecialties: preview.canonicalSpecialties || [],
    canonicalServiceAreas: preview.canonicalServiceAreas || [],
    visibleSections,
    portfolioProjects: preview.portfolioProjects || [],
    isMobilePreview: options.isMobilePreview ?? false,
  };
}
