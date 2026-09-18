'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth/auth-context';

export default function SignUpPage() {
  const router = useRouter();
  const { loginDevPersona } = useAuth();
  const [selectedIntent, setSelectedIntent] = useState<'CUSTOMER' | 'DESIGNER'>('CUSTOMER');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  const handleRegister = async () => {
    setIsSubmitting(true);
    try {
      // In dev/test environment, route directly to the respective persona
      const persona = selectedIntent === 'DESIGNER' ? 'designer_owner' : 'customer';
      await loginDevPersona(persona);
      router.push('/account');
    } catch {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12 bg-sand-50/50">
      <div className="w-full max-w-lg bg-white border border-sand-200 rounded-2xl p-6 sm:p-8 shadow-sm">
        <div className="text-center mb-8">
          <span className="text-xs font-semibold tracking-widest uppercase text-bronze-700 mb-2 block">
            Account Registration
          </span>
          <h1 className="font-serif text-3xl text-charcoal-900 tracking-tight">
            Join the Interior Platform
          </h1>
          <p className="text-sm text-charcoal-600 mt-2">
            Select your primary objective on the platform to personalize your workspace.
          </p>
        </div>

        {/* Role Intent Selection */}
        <div className="space-y-3 mb-6">
          <label
            onClick={() => setSelectedIntent('CUSTOMER')}
            className={`block p-4 rounded-xl border-2 cursor-pointer transition-all ${
              selectedIntent === 'CUSTOMER'
                ? 'border-bronze-600 bg-sand-50/50 shadow-sm'
                : 'border-sand-200 hover:border-sand-300'
            }`}
          >
            <div className="flex items-center justify-between mb-1">
              <span className="font-medium text-charcoal-900">Client / Homeowner</span>
              <span className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${
                selectedIntent === 'CUSTOMER' ? 'border-bronze-600 bg-bronze-600' : 'border-charcoal-300'
              }`}>
                {selectedIntent === 'CUSTOMER' && <span className="w-1.5 h-1.5 bg-white rounded-full" />}
              </span>
            </div>
            <p className="text-xs text-charcoal-600">
              Browse curated architectural projects, save personal moodboards, and request consultations from verified studios.
            </p>
          </label>

          <label
            onClick={() => setSelectedIntent('DESIGNER')}
            className={`block p-4 rounded-xl border-2 cursor-pointer transition-all ${
              selectedIntent === 'DESIGNER'
                ? 'border-bronze-600 bg-sand-50/50 shadow-sm'
                : 'border-sand-200 hover:border-sand-300'
            }`}
          >
            <div className="flex items-center justify-between mb-1">
              <span className="font-medium text-charcoal-900">Interior Designer / Studio</span>
              <span className={`w-4 h-4 rounded-full border-2 flex items-center justify-center ${
                selectedIntent === 'DESIGNER' ? 'border-bronze-600 bg-bronze-600' : 'border-charcoal-300'
              }`}>
                {selectedIntent === 'DESIGNER' && <span className="w-1.5 h-1.5 bg-white rounded-full" />}
              </span>
            </div>
            <p className="text-xs text-charcoal-600">
              Publish project portfolios, manage studio team collaboration, and receive verified client project leads.
            </p>
          </label>
        </div>

        {/* Security & Principle of Least Privilege Notice */}
        <div className="p-4 rounded-xl bg-sand-50 border border-sand-200 text-xs text-charcoal-600 mb-6 space-y-1">
          <p className="font-semibold text-charcoal-800">Security Architecture Notice</p>
          <p>
            Under the principle of least privilege, initial account registration grants Customer access. Designer studio verification, team member assignments, and elevated studio permissions are provisioned during Phase 08 Studio Onboarding.
          </p>
        </div>

        <button
          onClick={handleRegister}
          disabled={isSubmitting}
          className="w-full py-3 px-4 rounded-xl bg-charcoal-900 hover:bg-charcoal-800 text-white font-medium text-sm transition-colors shadow-sm disabled:opacity-50"
        >
          {isSubmitting ? 'Creating Account...' : `Continue as ${selectedIntent === 'DESIGNER' ? 'Interior Designer' : 'Client'}`}
        </button>

        <div className="mt-8 pt-6 border-t border-sand-200 text-center">
          <p className="text-sm text-charcoal-600">
            Already have an account?{' '}
            <Link href="/sign-in" className="text-bronze-700 font-medium hover:underline">
              Sign In
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
