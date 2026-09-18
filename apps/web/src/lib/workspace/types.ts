export interface WorkspaceUser {
  displayName: string;
  email: string;
  roles: string[];
}

export interface WorkspaceStudio {
  id: string;
  name: string;
  slug: string;
  professionalType: string;
  professionalTitle: string;
  tagline: string | null;
  role: string;
  operationalStatus: string;
  publicationStatus: string;
  city: string | null;
  state: string | null;
  serviceCount: number;
  specialtyCount: number;
  serviceAreaCount: number;
  contactCount: number;
}

export interface StudioMembershipSummary {
  studioId: string;
  studioName: string;
  studioSlug: string;
  role: string;
}

export interface CompletenessBreakdown {
  identityScore: number;
  locationScore: number;
  servicesScore: number;
  specialtiesScore: number;
  contactsScore: number;
  portfolioScore: number;
  projectsScore: number;
}

export interface CompletenessData {
  profileCompletenessPercentage: number;
  platformReadinessPercentage: number;
  status: string;
  breakdown: CompletenessBreakdown;
}

export interface SetupChecklistItem {
  id: string;
  title: string;
  description: string;
  completed: boolean;
  actionLabel: string;
  actionRoute: string;
}

export interface ModuleReadinessItem {
  id: string;
  name: string;
  description: string;
  status: 'READY' | 'NOT_CONFIGURED' | 'NOT_STARTED' | 'COMING_SOON' | 'LOCKED';
  route: string;
  ctaLabel: string;
}

export interface ActivityItem {
  id: string;
  title: string;
  description: string;
  timestamp: string;
  type: string;
}

export interface WorkspaceSummary {
  user: WorkspaceUser;
  studio: WorkspaceStudio;
  availableStudios: StudioMembershipSummary[];
  completeness: CompletenessData;
  setupChecklist: SetupChecklistItem[];
  modules: ModuleReadinessItem[];
  activityFeed: ActivityItem[];
}

export interface BusinessContactItem {
  kind: string;
  value: string;
  publicConsent: boolean;
  visibilityLabel: string;
  sortOrder: number;
}

export interface BusinessServiceItem {
  code: string;
  name: string;
}

export interface BusinessSpecialtyItem {
  code: string;
  name: string;
}

export interface BusinessServiceAreaItem {
  city: string;
  locality: string | null;
}

export interface BusinessProfile {
  id: string;
  name: string;
  slug: string;
  professionalType: string;
  professionalTitle: string;
  tagline: string | null;
  experienceSinceYear: number | null;
  teamSize: string | null;
  budgetRange: string | null;
  addressLine: string | null;
  city: string | null;
  district: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  travelAvailable: boolean;
  gstRegistered: boolean;
  gstNumber: string | null;
  operationalStatus: string;
  publicationStatus: string;
  roleInStudio: string;
  onboardingCompletedAt: string | null;
  contacts: BusinessContactItem[];
  services: BusinessServiceItem[];
  specialties: BusinessSpecialtyItem[];
  serviceAreas: BusinessServiceAreaItem[];
}
