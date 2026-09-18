# Portfolio Template Contract & Engine Specification

This specification defines the formal contract between the shared **Portfolio Engine** (built in Phase 10) and the **Visual Portfolio Themes** to be created by Astra in subsequent phases:
- **Phase 11**: Basic Clean Editorial
- **Phase 12**: Modern Minimalist
- **Phase 13**: Luxury Atelier
- **Phase 14**: Architectural Monograph
- **Phase 15**: Warm & Natural (Biophilic)
- **Phase 16**: Dark Cinematic
- **Phase 17**: Antigravity Integration, Theme Engine Consistency, Responsive QA & Regression

---

## 1. Fundamental Principle: Content != Template

The platform strictly decouples **persisted portfolio content and structure** from **presentation templates**.

1. An interior designer or studio can switch between any of the 6 aesthetic themes at any time.
2. Switching themes updates only `portfolios.template_key`.
3. **No section data, text, configuration, or order is destroyed, mutated, or lost during a template switch.**
4. When a designer switches from `BASIC` to `LUXURY` and then back to `BASIC`, the entire content tree remains identical.

---

## 2. Component Interface: `PortfolioTemplateProps`

All theme components implemented by Astra must implement this exact React contract (defined in `apps/web/src/lib/portfolio/template-contract.ts`):

```typescript
export interface PortfolioTemplateProps {
  portfolioId: string;
  studioId: string;
  studioName: string;
  studioSlug: string;
  professionalType?: string;
  professionalTitle?: string;
  studioCity?: string;
  studioState?: string;
  templateKey: PortfolioTemplateKey;
  headline?: string | null;
  subheadline?: string | null;
  bio?: string | null;
  designPhilosophy?: string | null;
  yearsOfExperience?: number | null;
  primaryColor?: string | null;
  secondaryColor?: string | null;
  accentColor?: string | null;
  fontPairing?: FontPairing;
  publicContacts: PreviewContactDto[];
  canonicalServices: PreviewServiceDto[];
  canonicalSpecialties: PreviewSpecialtyDto[];
  canonicalServiceAreas: PreviewServiceAreaDto[];
  visibleSections: PreviewSectionDto[];
  isMobilePreview?: boolean;
}
```

---

## 3. Section Handling Rules

1. **Persisted Order (`displayOrder`)**:
   - Themes must render sections according to their sorted `displayOrder`.
   - Invisible sections (`isVisible == false`) are already filtered out by the backend preview endpoint and are not present in `visibleSections`.

2. **Empty States**:
   - **Zero Projects**: Project CMS is implemented in Phase 18. Sections of type `FEATURED_PROJECTS` or `PROJECT_GALLERY` must render an elegant editorial empty state (e.g. "Projects currently in curation") and must never crash or throw.
   - **Zero Testimonials**: Empty structured testimonials must render gracefully or omit the quote carousel without breaking page flow.
   - **Privacy Filtering**: Only contacts where `publicConsent == true` are passed in `publicContacts`. Themes must never attempt to query or display private phone numbers or emails.

3. **Security Invariant**:
   - Section content is strictly validated JSON.
   - Themes must **NEVER** use `dangerouslySetInnerHTML` or execute unsanitized strings.

---

## 4. Theme Registration

When creating a new theme, Astra will register the component in `apps/web/src/lib/portfolio/template-registry.ts`:

```typescript
export const TEMPLATE_REGISTRY: Record<PortfolioTemplateKey, TemplateDefinition> = {
  // Update status from 'SCAFFOLD' to 'AVAILABLE' and replace the component:
  MODERN: {
    key: 'MODERN',
    name: 'Modern Minimalist',
    tagline: 'Asymmetric grids, bold headlines, and sleek contemporary lines',
    description: '...',
    status: 'AVAILABLE',
    designer: 'Astra (Phase 12)',
    phase: 'Phase 12',
    component: ModernThemeComponent,
  },
  // ...
};
```

---

## 5. Responsive & Mobile Preview Guarantee

- Every theme component must support the `isMobilePreview: boolean` flag.
- When `isMobilePreview === true`, the component should render responsively inside a mobile container (`375px` width) or apply compact mobile-optimized spacing.
