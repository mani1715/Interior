'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  Building2,
  MapPin,
  Briefcase,
  Palette,
  Phone,
  ArrowLeft,
  AlertCircle,
  FileCheck,
  Shield,
  Eye,
  EyeOff,
} from 'lucide-react';
import { fetchBusinessProfile } from '@/lib/workspace/api';
import { BusinessProfile } from '@/lib/workspace/types';

export default function BusinessProfilePage() {
  const [profile, setProfile] = useState<BusinessProfile | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchBusinessProfile()
      .then(setProfile)
      .catch((err) => setError(err.message || 'Failed to load business profile'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="p-6 md:p-10 max-w-5xl mx-auto space-y-6 animate-pulse">
        <div className="h-8 bg-sand-200 rounded-lg w-1/4" />
        <div className="h-64 bg-sand-200 rounded-2xl" />
        <div className="h-64 bg-sand-200 rounded-2xl" />
      </div>
    );
  }

  if (error || !profile) {
    return (
      <div className="p-6 md:p-10 max-w-3xl mx-auto">
        <div className="bg-white border border-sand-200 rounded-2xl p-8 text-center">
          <AlertCircle className="w-10 h-10 text-terracotta-600 mx-auto mb-3" />
          <h1 className="font-serif text-xl text-charcoal-900 mb-2">Unable to Load Business Profile</h1>
          <p className="text-sm text-charcoal-600 mb-4">{error || 'Profile could not be retrieved.'}</p>
          <Link
            href="/workspace"
            className="inline-flex items-center gap-2 px-4 py-2 bg-sand-200 text-charcoal-800 text-xs font-medium rounded-xl hover:bg-sand-300"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Workspace</span>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 lg:p-10 max-w-5xl mx-auto space-y-8">
      {/* Header */}
      <div>
        <Link
          href="/workspace"
          className="inline-flex items-center gap-1.5 text-xs font-medium text-charcoal-500 hover:text-charcoal-900 mb-3 transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Workspace Home</span>
        </Link>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block mb-1">
              Studio Records
            </span>
            <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
              Business Profile
            </h1>
            <p className="text-xs sm:text-sm text-charcoal-600 mt-1">
              Authoritative commercial registration, services, and privacy-protected contact channels.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <span className="px-3 py-1 rounded-full text-xs font-medium bg-forest-50 text-forest-700 border border-forest-200">
              Operational: {profile.operationalStatus}
            </span>
            <span className="px-3 py-1 rounded-full text-xs font-mono font-semibold bg-sand-200 text-charcoal-800">
              {profile.roleInStudio}
            </span>
          </div>
        </div>
      </div>

      {/* 1. Studio Identity & Commercial Details */}
      <div className="bg-white border border-sand-200 rounded-2xl p-6 sm:p-8 shadow-sm">
        <div className="flex items-center gap-3 pb-4 border-b border-sand-200 mb-6">
          <div className="w-9 h-9 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center">
            <Building2 className="w-5 h-5" />
          </div>
          <div>
            <h2 className="font-serif text-lg font-semibold text-charcoal-900">
              Studio Identity
            </h2>
            <p className="text-xs text-charcoal-500">Core business registration and credentials</p>
          </div>
        </div>

        <dl className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 text-xs sm:text-sm">
          <div>
            <dt className="text-charcoal-500 font-medium">Business / Studio Name</dt>
            <dd className="font-semibold text-charcoal-900 mt-1">{profile.name}</dd>
          </div>
          <div>
            <dt className="text-charcoal-500 font-medium">Platform Handle / Slug</dt>
            <dd className="font-mono text-bronze-800 mt-1">@{profile.slug}</dd>
          </div>
          <div>
            <dt className="text-charcoal-500 font-medium">Professional Classification</dt>
            <dd className="text-charcoal-900 mt-1 font-medium">{profile.professionalType.replace(/_/g, ' ')}</dd>
          </div>
          <div>
            <dt className="text-charcoal-500 font-medium">Professional Title</dt>
            <dd className="text-charcoal-900 mt-1">{profile.professionalTitle || 'None specified'}</dd>
          </div>
          <div>
            <dt className="text-charcoal-500 font-medium">Established Year</dt>
            <dd className="text-charcoal-900 mt-1">{profile.experienceSinceYear || 'Not specified'}</dd>
          </div>
          <div>
            <dt className="text-charcoal-500 font-medium">Team Size</dt>
            <dd className="text-charcoal-900 mt-1">{profile.teamSize || 'Individual practitioner'}</dd>
          </div>
          <div>
            <dt className="text-charcoal-500 font-medium">Typical Project Budget Range</dt>
            <dd className="text-charcoal-900 mt-1">{profile.budgetRange || 'Flexible'}</dd>
          </div>
          <div>
            <dt className="text-charcoal-500 font-medium">Travel for Projects</dt>
            <dd className="text-charcoal-900 mt-1">{profile.travelAvailable ? 'Yes (Available for travel)' : 'No (Local only)'}</dd>
          </div>
          <div>
            <dt className="text-charcoal-500 font-medium">GST Registered Status</dt>
            <dd className="text-charcoal-900 mt-1">{profile.gstRegistered ? 'Registered' : 'Not registered'}</dd>
          </div>
          {profile.gstNumber && (
            <div className="sm:col-span-2 lg:col-span-3 pt-4 border-t border-sand-100">
              <div className="flex items-center gap-2">
                <Shield className="w-4 h-4 text-bronze-700" />
                <dt className="text-charcoal-700 font-medium">Goods & Services Tax Identification Number (GSTIN)</dt>
              </div>
              <dd className="font-mono text-sm text-charcoal-900 mt-1 font-semibold">
                {profile.gstNumber}
                <span className="ml-2 text-[11px] font-normal text-charcoal-500 font-sans">
                  (Format-validated Indian GSTIN • Owner confidential)
                </span>
              </dd>
            </div>
          )}
        </dl>
      </div>

      {/* 2. Location & Service Areas */}
      <div className="bg-white border border-sand-200 rounded-2xl p-6 sm:p-8 shadow-sm">
        <div className="flex items-center gap-3 pb-4 border-b border-sand-200 mb-6">
          <div className="w-9 h-9 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center">
            <MapPin className="w-5 h-5" />
          </div>
          <div>
            <h2 className="font-serif text-lg font-semibold text-charcoal-900">
              Location & Coverage
            </h2>
            <p className="text-xs text-charcoal-500">Physical address and geographical service coverage</p>
          </div>
        </div>

        <div className="space-y-6 text-xs sm:text-sm">
          <div>
            <span className="text-charcoal-500 font-medium block mb-1">Primary Studio Address</span>
            <p className="text-charcoal-900 font-medium">
              {[profile.addressLine, profile.city, profile.district, profile.state, profile.postalCode, profile.country]
                .filter(Boolean)
                .join(', ') || 'No physical address configured'}
            </p>
          </div>

          <div>
            <span className="text-charcoal-500 font-medium block mb-2">Service Coverage Localities</span>
            {profile.serviceAreas && profile.serviceAreas.length > 0 ? (
              <div className="flex flex-wrap gap-2">
                {profile.serviceAreas.map((area, idx) => (
                  <span
                    key={idx}
                    className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-sand-100 border border-sand-200 text-charcoal-800 text-xs font-medium"
                  >
                    <MapPin className="w-3 h-3 text-bronze-700" />
                    <span>{area.city}{area.locality ? ` — ${area.locality}` : ''}</span>
                  </span>
                ))}
              </div>
            ) : (
              <p className="text-charcoal-400 italic">No specific service areas added.</p>
            )}
          </div>
        </div>
      </div>

      {/* 3. Services & Aesthetic Specialties */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Services */}
        <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm">
          <div className="flex items-center gap-3 pb-4 border-b border-sand-200 mb-5">
            <div className="w-8 h-8 rounded-lg bg-sand-100 text-bronze-700 flex items-center justify-center">
              <Briefcase className="w-4 h-4" />
            </div>
            <div>
              <h2 className="font-serif text-base font-semibold text-charcoal-900">
                Services Offered
              </h2>
              <p className="text-[11px] text-charcoal-500">Configured interior design services</p>
            </div>
          </div>

          {profile.services && profile.services.length > 0 ? (
            <ul className="space-y-2">
              {profile.services.map((s) => (
                <li key={s.code} className="flex items-center gap-2 text-xs font-medium text-charcoal-800">
                  <span className="w-1.5 h-1.5 rounded-full bg-bronze-600" />
                  <span>{s.name}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-xs text-charcoal-400 italic">No services registered.</p>
          )}
        </div>

        {/* Specialties */}
        <div className="bg-white border border-sand-200 rounded-2xl p-6 shadow-sm">
          <div className="flex items-center gap-3 pb-4 border-b border-sand-200 mb-5">
            <div className="w-8 h-8 rounded-lg bg-sand-100 text-bronze-700 flex items-center justify-center">
              <Palette className="w-4 h-4" />
            </div>
            <div>
              <h2 className="font-serif text-base font-semibold text-charcoal-900">
                Design Specialties
              </h2>
              <p className="text-[11px] text-charcoal-500">Aesthetic and architectural styles</p>
            </div>
          </div>

          {profile.specialties && profile.specialties.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {profile.specialties.map((sp) => (
                <span
                  key={sp.code}
                  className="px-3 py-1 rounded-lg bg-sand-50 border border-sand-200 text-charcoal-800 text-xs font-medium"
                >
                  {sp.name}
                </span>
              ))}
            </div>
          ) : (
            <p className="text-xs text-charcoal-400 italic">No specialties registered.</p>
          )}
        </div>
      </div>

      {/* 4. Contact Channels & Privacy Boundaries */}
      <div className="bg-white border border-sand-200 rounded-2xl p-6 sm:p-8 shadow-sm">
        <div className="flex items-center gap-3 pb-4 border-b border-sand-200 mb-6">
          <div className="w-9 h-9 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center">
            <Phone className="w-5 h-5" />
          </div>
          <div>
            <h2 className="font-serif text-lg font-semibold text-charcoal-900">
              Contact Channels & Privacy State
            </h2>
            <p className="text-xs text-charcoal-500">
              Stored communication channels with explicit public directory publication consent tracking.
            </p>
          </div>
        </div>

        {profile.contacts && profile.contacts.length > 0 ? (
          <div className="divide-y divide-sand-100">
            {profile.contacts.map((contact, idx) => (
              <div key={idx} className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <span className="text-xs font-semibold text-charcoal-500 uppercase tracking-wider block">
                    {contact.kind}
                  </span>
                  <p className="text-sm font-medium text-charcoal-900 mt-0.5 font-mono">
                    {contact.value}
                  </p>
                </div>

                <div>
                  {contact.publicConsent ? (
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-forest-50 text-forest-700 border border-forest-200">
                      <Eye className="w-3.5 h-3.5" />
                      <span>{contact.visibilityLabel}</span>
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-sand-100 text-charcoal-600 border border-sand-300">
                      <EyeOff className="w-3.5 h-3.5" />
                      <span>{contact.visibilityLabel}</span>
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-xs text-charcoal-400 italic">No contact channels recorded.</p>
        )}
      </div>
    </div>
  );
}
