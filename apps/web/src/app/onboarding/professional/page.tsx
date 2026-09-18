'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import {
  Building2,
  User,
  Compass,
  Hammer,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  ArrowLeft,
  Loader2,
  ShieldCheck,
  ExternalLink,
  MapPin,
  Briefcase,
  Layers,
  Phone,
  Check,
  ChevronRight,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/lib/auth/auth-context';
import { apiFetch, setCachedCsrfToken } from '@/lib/api-client';

export type ProfessionalType =
  | 'INDIVIDUAL_DESIGNER'
  | 'INTERIOR_STUDIO'
  | 'ARCHITECT'
  | 'ARCHITECTURE_STUDIO'
  | 'CUSTOM_FURNITURE'
  | 'WOODWORK_CABINETRY'
  | 'TURNKEY_CONTRACTOR';

interface ProfessionalTypeOption {
  type: ProfessionalType;
  title: string;
  badge: string;
  description: string;
  icon: typeof User;
}

const PROFESSIONAL_TYPES: ProfessionalTypeOption[] = [
  {
    type: 'INDIVIDUAL_DESIGNER',
    title: 'Individual Interior Designer',
    badge: 'Solo Practitioner',
    description: 'Independent residential or commercial interior designer managing bespoke design projects.',
    icon: User,
  },
  {
    type: 'INTERIOR_STUDIO',
    title: 'Interior Design Studio',
    badge: 'Design Firm',
    description: 'Boutique or full-scale design practice with a team handling concept-to-execution projects.',
    icon: Building2,
  },
  {
    type: 'ARCHITECT',
    title: 'Individual Architect',
    badge: 'Architectural Design',
    description: 'Licensed architect offering residential, spatial planning, and interior architectural services.',
    icon: Compass,
  },
  {
    type: 'ARCHITECTURE_STUDIO',
    title: 'Architecture & Design Firm',
    badge: 'Multi-Disciplinary',
    description: 'Comprehensive architectural studio providing integrated structural and interior architecture.',
    icon: Building2,
  },
  {
    type: 'CUSTOM_FURNITURE',
    title: 'Custom Furniture Studio',
    badge: 'Artisan Workshop',
    description: 'Custom furniture designer, manufacturer, or bespoke luxury joinery atelier.',
    icon: Layers,
  },
  {
    type: 'WOODWORK_CABINETRY',
    title: 'Woodwork & Cabinetry Specialist',
    badge: 'Millwork Expert',
    description: 'Specialist in modular kitchens, wardrobes, architectural paneling, and fine woodwork.',
    icon: Hammer,
  },
  {
    type: 'TURNKEY_CONTRACTOR',
    title: 'Turnkey Execution Contractor',
    badge: 'Full Execution',
    description: 'General contractor or turnkey execution partner providing end-to-end site delivery.',
    icon: Briefcase,
  },
];

const AVAILABLE_SERVICES = [
  'Modular Kitchen',
  'Full Home Interior',
  'Living Room & Spatial Design',
  'Wardrobe & Modular Storage',
  'False Ceiling & Architectural Lighting',
  'Bathroom & Wet Area Renovation',
  'Commercial & Office Interiors',
  '3D Architectural Visualization',
  'Turnkey Site Execution',
  'Custom Furniture Fabrication',
];

const AVAILABLE_SPECIALTIES = [
  'Modern Minimalist',
  'Warm Contemporary',
  'Indian Traditional / Chettinad',
  'Neo-Classical & Heritage',
  'Scandinavian Natural',
  'Industrial & Loft',
  'Luxury Eclectic',
  'Biophilic & Sustainable',
];

interface FormData {
  // Step 1
  professionalType: ProfessionalType;
  // Step 2
  studioName: string;
  slug: string;
  professionalTitle: string;
  tagline: string;
  experienceSinceYear: number;
  // Step 3
  addressLine: string;
  city: string;
  district: string;
  state: string;
  postalCode: string;
  country: string;
  serviceAreas: string[];
  newServiceAreaInput: string;
  // Step 4
  services: string[];
  specialties: string[];
  // Step 5
  teamSize: string;
  budgetRange: string;
  travelAvailable: boolean;
  gstRegistered: boolean;
  gstNumber: string;
  // Step 6
  phone: string;
  whatsapp: string;
  sameAsPhone: boolean;
  email: string;
  websiteUrl: string;
  instagramUrl: string;
  // Step 7
  confirmedAccuracy: boolean;
  confirmedContentOwnership: boolean;
}

const INITIAL_DATA: FormData = {
  professionalType: 'INTERIOR_STUDIO',
  studioName: '',
  slug: '',
  professionalTitle: '',
  tagline: '',
  experienceSinceYear: new Date().getFullYear(),
  addressLine: '',
  city: '',
  district: '',
  state: 'Andhra Pradesh',
  postalCode: '',
  country: 'IN',
  serviceAreas: [],
  newServiceAreaInput: '',
  services: [],
  specialties: [],
  teamSize: '2-5',
  budgetRange: '15L-35L',
  travelAvailable: true,
  gstRegistered: false,
  gstNumber: '',
  phone: '',
  whatsapp: '',
  sameAsPhone: true,
  email: '',
  websiteUrl: '',
  instagramUrl: '',
  confirmedAccuracy: false,
  confirmedContentOwnership: false,
};

export default function ProfessionalOnboardingPage() {
  const router = useRouter();
  const { user, isAuthenticated, isLoading: authLoading, refreshUser } = useAuth();

  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState<FormData>(INITIAL_DATA);
  const [loadingStatus, setLoadingStatus] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorBanner, setErrorBanner] = useState<string | null>(null);

  // Slug check state
  const [slugChecking, setSlugChecking] = useState(false);
  const [slugStatus, setSlugStatus] = useState<{
    checkedSlug: string;
    available: boolean;
    reason?: string;
    suggestedSlug?: string;
  } | null>(null);

  // Completed studio state
  const [completedStudio, setCompletedStudio] = useState<{
    id: string;
    name: string;
    slug: string;
    professionalType: string;
    status: string;
    publicationStatus: string;
    role: string;
  } | null>(null);

  // Initial load: check auth & onboarding status / draft
  useEffect(() => {
    if (authLoading) return;
    if (!isAuthenticated) {
      router.push('/sign-in?returnUrl=/onboarding/professional');
      return;
    }

    async function loadStatus() {
      try {
        setLoadingStatus(true);
        const res = await apiFetch<{
          onboardingCompleted: boolean;
          currentStep?: number;
          draftPayload?: Partial<FormData>;
          studio?: any;
        }>('/designers/onboarding/status');

        if (res.onboardingCompleted && res.studio) {
          setCompletedStudio(res.studio);
          setCurrentStep(8);
        } else {
          if (res.draftPayload) {
            setFormData((prev) => ({
              ...prev,
              ...res.draftPayload,
              email: res.draftPayload?.email || user?.email || '',
            }));
            if (res.currentStep && res.currentStep >= 1 && res.currentStep <= 7) {
              setCurrentStep(res.currentStep);
            }
          } else if (user?.email) {
            setFormData((prev) => ({ ...prev, email: user.email || '' }));
          }
        }
      } catch (err: any) {
        // Continue with fresh draft if status fails
      } finally {
        setLoadingStatus(false);
      }
    }

    loadStatus();
  }, [authLoading, isAuthenticated, router, user]);

  // Slug check debouncer
  const checkSlugAvailability = useCallback(async (slugToCheck: string) => {
    if (!slugToCheck || slugToCheck.length < 3) {
      setSlugStatus(null);
      return;
    }
    try {
      setSlugChecking(true);
      const res = await apiFetch<{
        slug: string;
        available: boolean;
        reason?: string;
        suggestedSlug?: string;
      }>(`/designers/onboarding/check-slug?slug=${encodeURIComponent(slugToCheck)}`);
      setSlugStatus({
        checkedSlug: res.slug,
        available: res.available,
        reason: res.reason,
        suggestedSlug: res.suggestedSlug,
      });
    } catch {
      setSlugStatus(null);
    } finally {
      setSlugChecking(false);
    }
  }, []);

  // Save draft to backend
  const saveDraft = useCallback(
    async (stepToSave: number, dataToSave: FormData) => {
      try {
        await apiFetch('/designers/onboarding/draft', {
          method: 'POST',
          body: JSON.stringify({
            step: stepToSave,
            draftPayload: dataToSave,
          }),
        });
      } catch {
        // Non-blocking draft save failure
      }
    },
    []
  );

  // Field updates
  const updateField = <K extends keyof FormData>(field: K, value: FormData[K]) => {
    setFormData((prev) => {
      const updated = { ...prev, [field]: value };
      if (field === 'phone' && prev.sameAsPhone) {
        updated.whatsapp = value as string;
      }
      return updated;
    });
    setErrorBanner(null);
  };

  // Studio Name change: auto-generate slug
  const handleStudioNameChange = (name: string) => {
    const autoSlug = name
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9\s-]/g, '')
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-');
    setFormData((prev) => ({
      ...prev,
      studioName: name,
      slug: autoSlug,
    }));
    checkSlugAvailability(autoSlug);
  };

  // Service Area Chip Add/Remove
  const addServiceArea = () => {
    const area = formData.newServiceAreaInput.trim();
    if (area && !formData.serviceAreas.includes(area)) {
      setFormData((prev) => ({
        ...prev,
        serviceAreas: [...prev.serviceAreas, area],
        newServiceAreaInput: '',
      }));
    }
  };

  const removeServiceArea = (areaToRemove: string) => {
    setFormData((prev) => ({
      ...prev,
      serviceAreas: prev.serviceAreas.filter((a) => a !== areaToRemove),
    }));
  };

  // Service toggle
  const toggleService = (service: string) => {
    setFormData((prev) => ({
      ...prev,
      services: prev.services.includes(service)
        ? prev.services.filter((s) => s !== service)
        : [...prev.services, service],
    }));
  };

  // Specialty toggle
  const toggleSpecialty = (specialty: string) => {
    setFormData((prev) => ({
      ...prev,
      specialties: prev.specialties.includes(specialty)
        ? prev.specialties.filter((s) => s !== specialty)
        : [...prev.specialties, specialty],
    }));
  };

  // Step Validation
  const validateStep = (step: number): boolean => {
    setErrorBanner(null);

    if (step === 1) {
      if (!formData.professionalType) {
        setErrorBanner('Please select a professional type.');
        return false;
      }
      return true;
    }

    if (step === 2) {
      if (!formData.studioName || formData.studioName.trim().length < 2) {
        setErrorBanner('Studio / Business Name must be at least 2 characters.');
        return false;
      }
      if (!formData.slug || formData.slug.length < 3) {
        setErrorBanner('Studio URL slug must be at least 3 characters.');
        return false;
      }
      if (slugStatus && !slugStatus.available) {
        setErrorBanner(slugStatus.reason || 'This studio URL is not available.');
        return false;
      }
      return true;
    }

    if (step === 3) {
      if (!formData.city || formData.city.trim().length < 2) {
        setErrorBanner('Please provide your primary city.');
        return false;
      }
      if (!formData.state || formData.state.trim().length < 2) {
        setErrorBanner('Please provide your state.');
        return false;
      }
      return true;
    }

    if (step === 4) {
      if (formData.services.length === 0) {
        setErrorBanner('Please select at least one service offered.');
        return false;
      }
      return true;
    }

    if (step === 5) {
      if (formData.gstRegistered) {
        const gstPattern = /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/;
        if (!formData.gstNumber || !gstPattern.test(formData.gstNumber.trim().toUpperCase())) {
          setErrorBanner('Please enter a valid 15-character Indian GSTIN (e.g. 37AAAAA0000A1Z5) or turn off GST registration.');
          return false;
        }
      }
      return true;
    }

    if (step === 6) {
      if (!formData.phone || formData.phone.trim().length < 7) {
        setErrorBanner('Please provide a valid contact phone number.');
        return false;
      }
      if (!formData.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
        setErrorBanner('Please provide a valid business contact email address.');
        return false;
      }
      return true;
    }

    if (step === 7) {
      if (!formData.confirmedAccuracy) {
        setErrorBanner('You must confirm that all information provided is accurate.');
        return false;
      }
      if (!formData.confirmedContentOwnership) {
        setErrorBanner('You must acknowledge ownership or publication rights to your work.');
        return false;
      }
      return true;
    }

    return true;
  };

  // Next Step handler
  const handleNext = async () => {
    if (!validateStep(currentStep)) return;
    const nextStep = currentStep + 1;
    setCurrentStep(nextStep);
    window.scrollTo({ top: 0, behavior: 'smooth' });
    await saveDraft(nextStep, formData);
  };

  // Prev Step handler
  const handleBack = () => {
    if (currentStep > 1) {
      setCurrentStep((prev) => prev - 1);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  // Jump to step from review
  const handleJumpToStep = (step: number) => {
    setCurrentStep(step);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Final Submit
  const handleSubmit = async () => {
    if (!validateStep(7)) return;

    try {
      setSubmitting(true);
      setErrorBanner(null);

      const payload = {
        professionalType: formData.professionalType,
        studioName: formData.studioName.trim(),
        slug: formData.slug.trim(),
        professionalTitle: formData.professionalTitle.trim() || null,
        tagline: formData.tagline.trim() || null,
        experienceSinceYear: Number(formData.experienceSinceYear) || null,
        teamSize: formData.teamSize,
        budgetRange: formData.budgetRange,
        addressLine: formData.addressLine.trim() || null,
        city: formData.city.trim(),
        district: formData.district.trim() || null,
        state: formData.state.trim(),
        postalCode: formData.postalCode.trim() || null,
        country: formData.country || 'IN',
        travelAvailable: formData.travelAvailable,
        gstRegistered: formData.gstRegistered,
        gstNumber: formData.gstRegistered ? formData.gstNumber.trim().toUpperCase() : null,
        services: formData.services,
        specialties: formData.specialties,
        serviceAreas: formData.serviceAreas,
        phone: formData.phone.trim(),
        whatsapp: formData.sameAsPhone ? formData.phone.trim() : formData.whatsapp.trim() || null,
        email: formData.email.trim(),
        websiteUrl: formData.websiteUrl.trim() || null,
        instagramUrl: formData.instagramUrl.trim() || null,
        confirmedAccuracy: formData.confirmedAccuracy,
        confirmedContentOwnership: formData.confirmedContentOwnership,
      };

      const result = await apiFetch<{
        studio: any;
        newCsrfToken?: string;
        message: string;
      }>('/designers/onboarding/complete', {
        method: 'POST',
        body: JSON.stringify(payload),
      });

      if (result.newCsrfToken) {
        setCachedCsrfToken(result.newCsrfToken);
      }

      setCompletedStudio(result.studio);
      setCurrentStep(8);
      // Immediately refresh user session in auth context to reflect DESIGNER role & studio membership
      await refreshUser();
    } catch (err: any) {
      setErrorBanner(err.message || 'Failed to complete professional onboarding. Please review details.');
    } finally {
      setSubmitting(false);
    }
  };

  if (authLoading || loadingStatus) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center gap-3">
        <Loader2 className="w-8 h-8 animate-spin text-bronze-600" />
        <p className="text-sm text-charcoal-600 font-sans">Loading onboarding session...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[var(--surface-base)] text-charcoal-900 pb-20">
      {/* Top Banner Header */}
      <div className="bg-white border-b border-sand-200 sticky top-16 z-30 shadow-sm">
        <div className="max-w-3xl mx-auto px-4 sm:px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Link
              href="/account"
              className="text-charcoal-500 hover:text-charcoal-900 transition-colors p-1 -ml-1 rounded-lg"
              title="Return to Account"
            >
              <ArrowLeft className="w-5 h-5" />
            </Link>
            <div>
              <span className="text-[11px] font-semibold uppercase tracking-wider text-bronze-700">
                Professional Onboarding
              </span>
              <h1 className="font-serif text-lg sm:text-xl font-bold text-charcoal-900 leading-tight">
                {currentStep === 8 ? 'Registration Successful' : 'Create Your Studio & Profile'}
              </h1>
            </div>
          </div>
          {currentStep < 8 && (
            <div className="flex items-center gap-2 text-xs font-mono text-charcoal-500">
              <span className="font-bold text-bronze-800">Step {currentStep}</span> of 7
            </div>
          )}
        </div>

        {/* Stepper Progress Bar */}
        {currentStep < 8 && (
          <div className="w-full bg-sand-100 h-1">
            <div
              className="bg-bronze-600 h-1 transition-all duration-300 ease-out"
              style={{ width: `${(currentStep / 7) * 100}%` }}
            />
          </div>
        )}
      </div>

      <main className="max-w-3xl mx-auto px-4 sm:px-6 pt-8">
        {/* Error Alert */}
        {errorBanner && (
          <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 flex items-start gap-3 animate-fadeIn">
            <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5 text-red-600" />
            <div className="text-sm font-medium">{errorBanner}</div>
          </div>
        )}

        {/* ============================================================ */}
        {/* STEP 1: PROFESSIONAL TYPE SELECTION                         */}
        {/* ============================================================ */}
        {currentStep === 1 && (
          <div className="space-y-6">
            <div>
              <h2 className="font-serif text-2xl font-bold text-charcoal-900">
                Select Your Professional Category
              </h2>
              <p className="text-sm text-charcoal-600 mt-1">
                Choose the primary classification that best reflects your architectural or interior practice.
              </p>
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {PROFESSIONAL_TYPES.map((item) => {
                const isSelected = formData.professionalType === item.type;
                const Icon = item.icon;
                return (
                  <button
                    key={item.type}
                    type="button"
                    onClick={() => updateField('professionalType', item.type)}
                    className={`p-5 rounded-2xl border text-left transition-all relative flex flex-col justify-between ${
                      isSelected
                        ? 'border-bronze-600 bg-bronze-50/40 shadow-sm ring-1 ring-bronze-600'
                        : 'border-sand-200 bg-white hover:border-sand-300 hover:bg-sand-50/40'
                    }`}
                  >
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <span className="p-2 rounded-xl bg-sand-100 text-charcoal-700">
                          <Icon className="w-5 h-5" />
                        </span>
                        <span className="text-[10px] font-mono uppercase tracking-wider px-2 py-0.5 rounded-full bg-sand-100 text-charcoal-600">
                          {item.badge}
                        </span>
                      </div>
                      <h3 className="font-serif font-bold text-base text-charcoal-900 mb-1">
                        {item.title}
                      </h3>
                      <p className="text-xs text-charcoal-600 leading-relaxed">
                        {item.description}
                      </p>
                    </div>
                    {isSelected && (
                      <div className="mt-3 flex items-center gap-1.5 text-xs font-semibold text-bronze-800">
                        <CheckCircle2 className="w-4 h-4 text-bronze-700" />
                        <span>Selected Category</span>
                      </div>
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* STEP 2: STUDIO IDENTITY                                      */}
        {/* ============================================================ */}
        {currentStep === 2 && (
          <div className="space-y-6">
            <div>
              <h2 className="font-serif text-2xl font-bold text-charcoal-900">
                Studio Identity & Platform Handle
              </h2>
              <p className="text-sm text-charcoal-600 mt-1">
                Define how your business appears across project briefs and client interactions.
              </p>
            </div>

            <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-5">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                  Studio or Business Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.studioName}
                  onChange={(e) => handleStudioNameChange(e.target.value)}
                  placeholder="e.g. Srinivasa Interior Atelier"
                  className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600 focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                  Public Handle / Studio Slug <span className="text-red-500">*</span>
                </label>
                <div className="flex items-center rounded-xl border border-sand-300 bg-sand-50/50 px-3 py-1.5 focus-within:ring-2 focus-within:ring-bronze-600 focus-within:border-transparent">
                  <span className="text-xs text-charcoal-500 font-mono select-none">
                    elegance.design/@
                  </span>
                  <input
                    type="text"
                    value={formData.slug}
                    onChange={(e) => {
                      const clean = e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '');
                      updateField('slug', clean);
                      checkSlugAvailability(clean);
                    }}
                    placeholder="srinivasa-interiors"
                    className="flex-1 bg-transparent px-2 py-1 text-sm font-mono focus:outline-none text-charcoal-900"
                  />
                  {slugChecking ? (
                    <Loader2 className="w-4 h-4 animate-spin text-charcoal-400" />
                  ) : slugStatus ? (
                    slugStatus.available ? (
                      <span className="text-xs font-semibold text-forest-700 flex items-center gap-1">
                        <Check className="w-3.5 h-3.5" /> Available
                      </span>
                    ) : (
                      <span className="text-xs font-semibold text-red-600 flex items-center gap-1">
                        Unavailable
                      </span>
                    )
                  ) : null}
                </div>
                {slugStatus && !slugStatus.available && (
                  <div className="mt-1.5 text-xs text-red-600 flex items-center justify-between">
                    <span>{slugStatus.reason}</span>
                    {slugStatus.suggestedSlug && (
                      <button
                        type="button"
                        onClick={() => {
                          updateField('slug', slugStatus.suggestedSlug!);
                          checkSlugAvailability(slugStatus.suggestedSlug!);
                        }}
                        className="text-bronze-700 underline font-medium ml-2"
                      >
                        Use &ldquo;{slugStatus.suggestedSlug}&rdquo;
                      </button>
                    )}
                  </div>
                )}
                <p className="text-[11px] text-charcoal-500 mt-1">
                  Unique handle (3–60 chars, lowercase letters, numbers, and hyphens).
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                    Your Professional Title
                  </label>
                  <input
                    type="text"
                    value={formData.professionalTitle}
                    onChange={(e) => updateField('professionalTitle', e.target.value)}
                    placeholder="e.g. Principal Architect / Design Director"
                    className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                    Founded / Experience Since Year
                  </label>
                  <input
                    type="number"
                    min="1950"
                    max={new Date().getFullYear()}
                    value={formData.experienceSinceYear}
                    onChange={(e) => updateField('experienceSinceYear', Number(e.target.value))}
                    className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                  Studio Tagline / Philosophy (Max 255 chars)
                </label>
                <textarea
                  rows={2}
                  maxLength={255}
                  value={formData.tagline}
                  onChange={(e) => updateField('tagline', e.target.value)}
                  placeholder="Crafting climate-responsive, soulful living spaces across South India."
                  className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600"
                />
                <div className="text-[11px] text-charcoal-500 text-right mt-0.5">
                  {formData.tagline.length}/255
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* STEP 3: LOCATION & SERVICE AREAS                             */}
        {/* ============================================================ */}
        {currentStep === 3 && (
          <div className="space-y-6">
            <div>
              <h2 className="font-serif text-2xl font-bold text-charcoal-900">
                Location & Coverage
              </h2>
              <p className="text-sm text-charcoal-600 mt-1">
                Where your primary office or studio operates and the territories you service.
              </p>
            </div>

            <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-5">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                  Street / Studio Address
                </label>
                <input
                  type="text"
                  value={formData.addressLine}
                  onChange={(e) => updateField('addressLine', e.target.value)}
                  placeholder="e.g. 4th Line, Brodipet"
                  className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                    Primary City <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={formData.city}
                    onChange={(e) => updateField('city', e.target.value)}
                    placeholder="e.g. Guntur"
                    className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                    District
                  </label>
                  <input
                    type="text"
                    value={formData.district}
                    onChange={(e) => updateField('district', e.target.value)}
                    placeholder="e.g. Guntur District"
                    className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                    State <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={formData.state}
                    onChange={(e) => updateField('state', e.target.value)}
                    placeholder="e.g. Andhra Pradesh"
                    className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                    PIN / Postal Code
                  </label>
                  <input
                    type="text"
                    value={formData.postalCode}
                    onChange={(e) => updateField('postalCode', e.target.value)}
                    placeholder="e.g. 522002"
                    className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                    Country
                  </label>
                  <input
                    type="text"
                    disabled
                    value="India (IN)"
                    className="w-full px-4 py-2.5 rounded-xl border border-sand-200 bg-sand-50 text-sm text-charcoal-600"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                  Service Areas / Localities Served
                </label>
                <div className="flex gap-2 mb-2">
                  <input
                    type="text"
                    value={formData.newServiceAreaInput}
                    onChange={(e) => updateField('newServiceAreaInput', e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        addServiceArea();
                      }
                    }}
                    placeholder="Type city or locality (e.g. Amaravati, Vijayawada) and press Add"
                    className="flex-1 px-4 py-2 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600"
                  />
                  <Button type="button" variant="outline" size="sm" onClick={addServiceArea}>
                    Add Area
                  </Button>
                </div>
                <div className="flex flex-wrap gap-1.5 min-h-[32px]">
                  {formData.serviceAreas.map((area) => (
                    <span
                      key={area}
                      className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-sand-100 text-xs font-medium text-charcoal-800"
                    >
                      <MapPin className="w-3 h-3 text-bronze-700" />
                      {area}
                      <button
                        type="button"
                        onClick={() => removeServiceArea(area)}
                        className="text-charcoal-400 hover:text-red-600 font-bold ml-1"
                      >
                        ×
                      </button>
                    </span>
                  ))}
                  {formData.serviceAreas.length === 0 && (
                    <span className="text-xs text-charcoal-400 italic">
                      No additional service areas added yet. Primary city will be used as default.
                    </span>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* STEP 4: SERVICES & SPECIALTIES                               */}
        {/* ============================================================ */}
        {currentStep === 4 && (
          <div className="space-y-6">
            <div>
              <h2 className="font-serif text-2xl font-bold text-charcoal-900">
                Services Offered & Aesthetic Styles
              </h2>
              <p className="text-sm text-charcoal-600 mt-1">
                Select the capabilities and aesthetic disciplines your studio executes.
              </p>
            </div>

            <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-6">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-2">
                  Services Provided <span className="text-red-500">*</span>
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  {AVAILABLE_SERVICES.map((service) => {
                    const isChecked = formData.services.includes(service);
                    return (
                      <label
                        key={service}
                        className={`flex items-center gap-3 p-3 rounded-xl border cursor-pointer transition-all ${
                          isChecked
                            ? 'border-bronze-600 bg-bronze-50/40 text-charcoal-900 font-medium'
                            : 'border-sand-200 hover:border-sand-300 text-charcoal-700'
                        }`}
                      >
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => toggleService(service)}
                          className="rounded border-sand-300 text-bronze-600 focus:ring-bronze-500 w-4 h-4"
                        />
                        <span className="text-xs sm:text-sm">{service}</span>
                      </label>
                    );
                  })}
                </div>
              </div>

              <div className="pt-4 border-t border-sand-200">
                <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-2">
                  Design Aesthetics & Specialties
                </label>
                <div className="flex flex-wrap gap-2">
                  {AVAILABLE_SPECIALTIES.map((spec) => {
                    const isSelected = formData.specialties.includes(spec);
                    return (
                      <button
                        key={spec}
                        type="button"
                        onClick={() => toggleSpecialty(spec)}
                        className={`px-3.5 py-1.5 rounded-full text-xs font-medium border transition-all ${
                          isSelected
                            ? 'border-bronze-700 bg-bronze-700 text-white shadow-sm'
                            : 'border-sand-300 bg-white text-charcoal-700 hover:border-bronze-500 hover:text-bronze-800'
                        }`}
                      >
                        {spec}
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* STEP 5: BUSINESS DETAILS & GST                               */}
        {/* ============================================================ */}
        {currentStep === 5 && (
          <div className="space-y-6">
            <div>
              <h2 className="font-serif text-2xl font-bold text-charcoal-900">
                Business Scale & Project Fit
              </h2>
              <p className="text-sm text-charcoal-600 mt-1">
                Help clients understand your typical project scope and commercial structure.
              </p>
            </div>

            <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-6">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                    Team Size
                  </label>
                  <select
                    value={formData.teamSize}
                    onChange={(e) => updateField('teamSize', e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600 bg-white"
                  >
                    <option value="1">Solo Practitioner (1)</option>
                    <option value="2-5">Boutique Team (2 - 5)</option>
                    <option value="5-10">Medium Studio (5 - 10)</option>
                    <option value="10+">Large Practice (10+)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                    Typical Project Budget Range
                  </label>
                  <select
                    value={formData.budgetRange}
                    onChange={(e) => updateField('budgetRange', e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600 bg-white"
                  >
                    <option value="UNDER_5L">Under ₹5 Lakhs</option>
                    <option value="5L_15L">₹5 Lakhs – ₹15 Lakhs</option>
                    <option value="15L_35L">₹15 Lakhs – ₹35 Lakhs</option>
                    <option value="35L_75L">₹35 Lakhs – ₹75 Lakhs</option>
                    <option value="ABOVE_75L">₹75 Lakhs + (Luxury / Estate)</option>
                  </select>
                </div>
              </div>

              {/* Travel Toggle */}
              <div className="flex items-center justify-between p-4 rounded-xl bg-sand-50/70 border border-sand-200">
                <div>
                  <h4 className="text-sm font-semibold text-charcoal-900">
                    Willing to Travel for On-Site Projects
                  </h4>
                  <p className="text-xs text-charcoal-600 mt-0.5">
                    Indicate if you accept commissions outside your immediate metropolitan area.
                  </p>
                </div>
                <input
                  type="checkbox"
                  checked={formData.travelAvailable}
                  onChange={(e) => updateField('travelAvailable', e.target.checked)}
                  className="rounded border-sand-300 text-bronze-600 focus:ring-bronze-500 w-5 h-5"
                />
              </div>

              {/* GST Section */}
              <div className="p-4 rounded-xl bg-sand-50/70 border border-sand-200 space-y-3">
                <div className="flex items-center justify-between">
                  <div>
                    <h4 className="text-sm font-semibold text-charcoal-900">
                      GST Registered Entity
                    </h4>
                    <p className="text-xs text-charcoal-600 mt-0.5">
                      Enable if your studio operates with an active Indian GSTIN tax registration.
                    </p>
                  </div>
                  <input
                    type="checkbox"
                    checked={formData.gstRegistered}
                    onChange={(e) => updateField('gstRegistered', e.target.checked)}
                    className="rounded border-sand-300 text-bronze-600 focus:ring-bronze-500 w-5 h-5"
                  />
                </div>

                {formData.gstRegistered && (
                  <div className="pt-2">
                    <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                      15-Digit GST Identification Number (GSTIN) <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      maxLength={15}
                      value={formData.gstNumber}
                      onChange={(e) => updateField('gstNumber', e.target.value.toUpperCase())}
                      placeholder="e.g. 37AAAAA0000A1Z5"
                      className="w-full px-4 py-2 rounded-xl border border-sand-300 text-sm font-mono tracking-wider focus:outline-none focus:ring-2 focus:ring-bronze-600 bg-white"
                    />
                    <p className="text-[11px] text-charcoal-500 mt-1">
                      Format: 2 digits state code + 10 chars PAN + 1 entity code + &lsquo;Z&rsquo; + 1 checksum char.
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* STEP 6: CONTACT & PRESENCE                                   */}
        {/* ============================================================ */}
        {currentStep === 6 && (
          <div className="space-y-6">
            <div>
              <h2 className="font-serif text-2xl font-bold text-charcoal-900">
                Contact & Studio Channels
              </h2>
              <p className="text-sm text-charcoal-600 mt-1">
                Channels through which prospective clients can contact your office.
              </p>
            </div>

            <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm space-y-5">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                    Business Phone <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="tel"
                    value={formData.phone}
                    onChange={(e) => updateField('phone', e.target.value)}
                    placeholder="+91 98765 43210"
                    className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600"
                  />
                </div>

                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700">
                      WhatsApp Business
                    </label>
                    <label className="flex items-center gap-1.5 text-xs text-charcoal-600 cursor-pointer">
                      <input
                        type="checkbox"
                        checked={formData.sameAsPhone}
                        onChange={(e) => {
                          const checked = e.target.checked;
                          updateField('sameAsPhone', checked);
                          if (checked) {
                            updateField('whatsapp', formData.phone);
                          }
                        }}
                        className="rounded border-sand-300 text-bronze-600 focus:ring-bronze-500 w-3.5 h-3.5"
                      />
                      <span>Same as Phone</span>
                    </label>
                  </div>
                  <input
                    type="tel"
                    disabled={formData.sameAsPhone}
                    value={formData.sameAsPhone ? formData.phone : formData.whatsapp}
                    onChange={(e) => updateField('whatsapp', e.target.value)}
                    placeholder="+91 98765 43210"
                    className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600 disabled:bg-sand-50 disabled:text-charcoal-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                  Public Contact Email <span className="text-red-500">*</span>
                </label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => updateField('email', e.target.value)}
                  placeholder="contact@studio.com"
                  className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                    Website URL (Optional)
                  </label>
                  <input
                    type="url"
                    value={formData.websiteUrl}
                    onChange={(e) => updateField('websiteUrl', e.target.value)}
                    placeholder="https://yourstudio.com"
                    className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-charcoal-700 mb-1.5">
                    Instagram Handle or URL (Optional)
                  </label>
                  <input
                    type="text"
                    value={formData.instagramUrl}
                    onChange={(e) => updateField('instagramUrl', e.target.value)}
                    placeholder="https://instagram.com/yourstudio"
                    className="w-full px-4 py-2.5 rounded-xl border border-sand-300 text-sm focus:outline-none focus:ring-2 focus:ring-bronze-600"
                  />
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* STEP 7: REVIEW & CONFIRM                                     */}
        {/* ============================================================ */}
        {currentStep === 7 && (
          <div className="space-y-6">
            <div>
              <h2 className="font-serif text-2xl font-bold text-charcoal-900">
                Review & Complete Registration
              </h2>
              <p className="text-sm text-charcoal-600 mt-1">
                Please verify your studio details. You can jump directly to any step to make corrections.
              </p>
            </div>

            {/* Summary Cards */}
            <div className="bg-white border border-sand-200 rounded-2xl divide-y divide-sand-200 shadow-sm overflow-hidden">
              {/* Category & Identity */}
              <div className="p-5 flex items-start justify-between gap-4">
                <div>
                  <span className="text-[10px] font-mono uppercase text-bronze-700 tracking-wider">
                    Studio Identity
                  </span>
                  <h3 className="font-serif font-bold text-lg text-charcoal-900 mt-0.5">
                    {formData.studioName}
                  </h3>
                  <p className="text-xs font-mono text-charcoal-500">
                    Handle: @{formData.slug}
                  </p>
                  <p className="text-xs text-charcoal-700 mt-1">
                    {formData.professionalTitle && `${formData.professionalTitle} • `}
                    {PROFESSIONAL_TYPES.find((t) => t.type === formData.professionalType)?.title}
                  </p>
                  {formData.tagline && (
                    <p className="text-xs italic text-charcoal-600 mt-1">&ldquo;{formData.tagline}&rdquo;</p>
                  )}
                </div>
                <Button variant="ghost" size="sm" onClick={() => handleJumpToStep(2)}>
                  Edit
                </Button>
              </div>

              {/* Location & Areas */}
              <div className="p-5 flex items-start justify-between gap-4">
                <div>
                  <span className="text-[10px] font-mono uppercase text-bronze-700 tracking-wider">
                    Location & Coverage
                  </span>
                  <p className="text-xs text-charcoal-800 font-medium mt-1">
                    {formData.addressLine && `${formData.addressLine}, `}
                    {formData.city}, {formData.district && `${formData.district}, `}{formData.state} {formData.postalCode}
                  </p>
                  <div className="flex flex-wrap gap-1 mt-2">
                    {formData.serviceAreas.map((area) => (
                      <span key={area} className="px-2 py-0.5 rounded bg-sand-100 text-[11px] text-charcoal-700">
                        {area}
                      </span>
                    ))}
                  </div>
                </div>
                <Button variant="ghost" size="sm" onClick={() => handleJumpToStep(3)}>
                  Edit
                </Button>
              </div>

              {/* Services & Specialties */}
              <div className="p-5 flex items-start justify-between gap-4">
                <div>
                  <span className="text-[10px] font-mono uppercase text-bronze-700 tracking-wider">
                    Services & Specialties
                  </span>
                  <div className="flex flex-wrap gap-1.5 mt-1.5">
                    {formData.services.map((s) => (
                      <span key={s} className="px-2 py-0.5 rounded bg-bronze-50 text-bronze-800 border border-bronze-200 text-xs font-medium">
                        {s}
                      </span>
                    ))}
                  </div>
                  {formData.specialties.length > 0 && (
                    <div className="flex flex-wrap gap-1 mt-2">
                      {formData.specialties.map((sp) => (
                        <span key={sp} className="px-2 py-0.5 rounded bg-sand-100 text-charcoal-600 text-[11px]">
                          {sp}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
                <Button variant="ghost" size="sm" onClick={() => handleJumpToStep(4)}>
                  Edit
                </Button>
              </div>

              {/* Scale & Contact */}
              <div className="p-5 flex items-start justify-between gap-4">
                <div className="text-xs space-y-1">
                  <span className="text-[10px] font-mono uppercase text-bronze-700 tracking-wider block mb-1">
                    Business Scale & Contact
                  </span>
                  <p><span className="text-charcoal-500">Team Size:</span> {formData.teamSize} | <span className="text-charcoal-500">Budget:</span> {formData.budgetRange}</p>
                  <p><span className="text-charcoal-500">Phone:</span> {formData.phone} | <span className="text-charcoal-500">WhatsApp:</span> {formData.whatsapp || formData.phone}</p>
                  <p><span className="text-charcoal-500">Email:</span> {formData.email}</p>
                  {formData.gstRegistered && (
                    <p><span className="text-charcoal-500">GSTIN:</span> <span className="font-mono">{formData.gstNumber}</span></p>
                  )}
                </div>
                <Button variant="ghost" size="sm" onClick={() => handleJumpToStep(6)}>
                  Edit
                </Button>
              </div>
            </div>

            {/* Truthful Platform Transparency Notice */}
            <div className="p-4 rounded-xl bg-sand-100/80 border border-sand-200 text-xs text-charcoal-700 space-y-1">
              <div className="flex items-center gap-1.5 font-semibold text-charcoal-900">
                <ShieldCheck className="w-4 h-4 text-bronze-700" />
                <span>Truthful Platform Status Notice</span>
              </div>
              <p>
                Upon completion, your account will be upgraded to <strong>DESIGNER</strong> role with operational status <strong>ACTIVE</strong>.
                Your studio publication status will remain <strong>UNPUBLISHED</strong> until you upload project works in later phases.
                Your studio will not be indexed or publicly listed in discovery search until portfolio publishing is unlocked.
              </p>
            </div>

            {/* Mandatory Declarations */}
            <div className="bg-white border border-sand-200 rounded-2xl p-5 shadow-sm space-y-3">
              <label className="flex items-start gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.confirmedAccuracy}
                  onChange={(e) => updateField('confirmedAccuracy', e.target.checked)}
                  className="rounded border-sand-300 text-bronze-600 focus:ring-bronze-500 w-4 h-4 mt-0.5"
                />
                <span className="text-xs text-charcoal-800 leading-normal">
                  I confirm that all business details, credentials, and contact records provided above are authentic and accurate.
                </span>
              </label>

              <label className="flex items-start gap-3 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.confirmedContentOwnership}
                  onChange={(e) => updateField('confirmedContentOwnership', e.target.checked)}
                  className="rounded border-sand-300 text-bronze-600 focus:ring-bronze-500 w-4 h-4 mt-0.5"
                />
                <span className="text-xs text-charcoal-800 leading-normal">
                  I confirm that I hold legitimate rights or ownership to represent this studio brand and publish subsequent design materials on this platform.
                </span>
              </label>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* STEP 8: SUCCESS VIEW                                         */}
        {/* ============================================================ */}
        {currentStep === 8 && completedStudio && (
          <div className="space-y-6 animate-fadeIn text-center py-6">
            <div className="w-16 h-16 bg-forest-50 border border-forest-200 rounded-full flex items-center justify-center mx-auto text-forest-700">
              <CheckCircle2 className="w-10 h-10" />
            </div>

            <div>
              <span className="text-xs font-mono uppercase tracking-widest text-bronze-700 font-semibold">
                Onboarding Complete
              </span>
              <h2 className="font-serif text-3xl font-bold text-charcoal-900 mt-1">
                Welcome to Elégance Professional
              </h2>
              <p className="text-sm text-charcoal-600 max-w-md mx-auto mt-2">
                Your professional studio workspace has been created and your account has been promoted to DESIGNER status.
              </p>
            </div>

            {/* Studio Summary Card */}
            <div className="bg-white border border-sand-200 rounded-2xl p-6 max-w-lg mx-auto text-left shadow-sm space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-sand-200">
                <div>
                  <h3 className="font-serif font-bold text-lg text-charcoal-900">
                    {completedStudio.name}
                  </h3>
                  <p className="text-xs font-mono text-bronze-800">
                    @{completedStudio.slug}
                  </p>
                </div>
                <span className="text-xs font-semibold px-2.5 py-1 bg-bronze-50 text-bronze-800 border border-bronze-200 rounded-lg">
                  Role: {completedStudio.role}
                </span>
              </div>

              <dl className="grid grid-cols-2 gap-3 text-xs">
                <div>
                  <dt className="text-charcoal-500">Operational Status</dt>
                  <dd className="font-semibold text-forest-700 mt-0.5 flex items-center gap-1">
                    <span className="w-2 h-2 rounded-full bg-forest-600" />
                    {completedStudio.status}
                  </dd>
                </div>
                <div>
                  <dt className="text-charcoal-500">Publication Status</dt>
                  <dd className="font-semibold text-charcoal-700 mt-0.5 flex items-center gap-1">
                    <span className="w-2 h-2 rounded-full bg-sand-400" />
                    {completedStudio.publicationStatus}
                  </dd>
                </div>
                <div>
                  <dt className="text-charcoal-500">Platform Role</dt>
                  <dd className="font-semibold text-charcoal-900 mt-0.5">
                    DESIGNER
                  </dd>
                </div>
                <div>
                  <dt className="text-charcoal-500">Next Phase</dt>
                  <dd className="font-semibold text-bronze-800 mt-0.5">
                    Phase 09: Studio Workspace
                  </dd>
                </div>
              </dl>

              <div className="p-3 bg-sand-50 rounded-xl text-[11px] text-charcoal-600 border border-sand-200">
                <p className="font-semibold text-charcoal-800 mb-0.5">Public Discovery Status:</p>
                <p>
                  Your studio is registered internally. Portfolio builder, project publishing, and discovery listing will activate in upcoming dashboard phases.
                </p>
              </div>
            </div>

            {/* CTAs */}
            <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
              <Link href="/account">
                <Button variant="primary" size="lg">
                  View Account Profile
                </Button>
              </Link>
              <Link href="/projects">
                <Button variant="outline" size="lg">
                  Explore Public Discovery
                </Button>
              </Link>
            </div>
          </div>
        )}

        {/* Wizard Controls (Steps 1 to 7) */}
        {currentStep < 8 && (
          <div className="mt-8 flex items-center justify-between pt-6 border-t border-sand-200">
            {currentStep > 1 ? (
              <Button type="button" variant="outline" onClick={handleBack} disabled={submitting}>
                <ArrowLeft className="w-4 h-4 mr-2" />
                Back
              </Button>
            ) : (
              <div />
            )}

            {currentStep < 7 ? (
              <Button type="button" variant="primary" onClick={handleNext}>
                Continue
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
            ) : (
              <Button
                type="button"
                variant="primary"
                onClick={handleSubmit}
                disabled={submitting}
                className="bg-forest-700 hover:bg-forest-800 text-white"
              >
                {submitting ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    Registering Studio...
                  </>
                ) : (
                  <>
                    <ShieldCheck className="w-4 h-4 mr-2" />
                    Complete Registration & Promote Account
                  </>
                )}
              </Button>
            )}
          </div>
        )}
      </main>
    </div>
  );
}
