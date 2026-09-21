import type { PortfolioTemplateProps } from '@/lib/portfolio/template-contract';
import type { PreviewSectionDto } from '@/lib/portfolio/types';
import { buildNavigationSettings } from '@/lib/portfolio/normalize-props';

// Synthetic QA-only content. Never imported by the application or registry.
export const basicSections: PreviewSectionDto[] = [
  { sectionId: 'hero', sectionType: 'HERO', displayOrder: 0, schemaVersion: 1, content: {} },
  { sectionId: 'about', sectionType: 'ABOUT', displayOrder: 1, schemaVersion: 1, content: {} },
  { sectionId: 'services', sectionType: 'SERVICES', displayOrder: 2, schemaVersion: 1, content: {} },
  { sectionId: 'projects', sectionType: 'FEATURED_PROJECTS', displayOrder: 3, schemaVersion: 1, content: {} },
  { sectionId: 'process', sectionType: 'DESIGN_PROCESS', displayOrder: 4, schemaVersion: 1, content: { steps: [
    { step: '01', title: 'Listen', desc: 'A conversation about the way you live and what your space needs.' },
    { step: '02', title: 'Develop', desc: 'Plans, materials and details considered together.' },
    { step: '03', title: 'Make', desc: 'A clear process from agreed design to installation.' },
  ] } },
  { sectionId: 'areas', sectionType: 'SERVICE_AREAS', displayOrder: 5, schemaVersion: 1, content: {} },
  { sectionId: 'faq', sectionType: 'FAQ', displayOrder: 6, schemaVersion: 1, content: { items: [
    { question: 'How does a project begin?', answer: 'Send a brief introduction to your space and the work you are considering.' },
    { question: 'Can we discuss a single room?', answer: 'We discuss the scope together before developing a proposal.' },
  ] } },
  { sectionId: 'contact', sectionType: 'CONTACT', displayOrder: 7, schemaVersion: 1, content: { contactIntro: 'Tell us about the space you have in mind.' } },
];

export function basicFixture(overrides: Partial<PortfolioTemplateProps> = {}): PortfolioTemplateProps {
  const visibleSections = overrides.visibleSections || basicSections;
  return {
    portfolioId: 'qa-portfolio', studioId: 'qa-studio', studioName: 'Form & Field', studioSlug: 'qa-only',
    templateKey: 'BASIC', headline: 'Considered spaces. Everyday living.',
    subheadline: 'Interiors, furniture and thoughtful details, designed around the way you live.',
    professionalTitle: 'Interior design & furniture', studioCity: 'Bengaluru', studioState: 'Karnataka',
    bio: 'We approach each space with a simple question: how should it feel to live here? Our work brings together practical planning, honest materials and a quiet attention to detail.',
    designPhilosophy: 'Spaces should feel personal, useful and at ease.', fontPairing: 'CLASSIC_SERIF',
    publicContacts: [{ kind: 'EMAIL', contactValue: 'hello@example.com' }],
    canonicalServices: [{ serviceCode: 'interior', serviceName: 'Interior design' }, { serviceCode: 'furniture', serviceName: 'Custom furniture' }, { serviceCode: 'joinery', serviceName: 'Cabinetry & joinery' }],
    canonicalSpecialties: [{ specialtyCode: 'home', specialtyName: 'Residential spaces' }],
    canonicalServiceAreas: [{ cityName: 'Bengaluru', locality: '' }, { cityName: 'Mysuru', locality: '' }],
    visibleSections, navigationSettings: buildNavigationSettings(visibleSections), ...overrides,
  };
}
