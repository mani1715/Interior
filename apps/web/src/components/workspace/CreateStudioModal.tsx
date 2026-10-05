'use client';

import React, { useState } from 'react';
import { Building2, AlertCircle, Loader2 } from 'lucide-react';
import { Dialog } from '@/components/overlay/Dialog';
import { createStudio } from '@/lib/workspace/api';

interface CreateStudioModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (newStudioId: string, studioName: string) => Promise<void> | void;
}

const PROFESSIONAL_TYPES = [
  { code: 'INTERIOR_STUDIO', label: 'Interior Studio' },
  { code: 'INDIVIDUAL_DESIGNER', label: 'Individual Designer' },
  { code: 'ARCHITECT', label: 'Architect' },
  { code: 'ARCHITECTURE_STUDIO', label: 'Architecture Studio' },
  { code: 'CUSTOM_FURNITURE', label: 'Custom Furniture Studio' },
  { code: 'WOODWORK_CABINETRY', label: 'Woodwork / Cabinetry' },
  { code: 'TURNKEY_CONTRACTOR', label: 'Turnkey Contractor' },
];

export function CreateStudioModal({ isOpen, onClose, onSuccess }: CreateStudioModalProps) {
  const [name, setName] = useState('');
  const [professionalType, setProfessionalType] = useState('INTERIOR_STUDIO');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const resetForm = () => {
    setName('');
    setProfessionalType('INTERIOR_STUDIO');
    setCity('');
    setState('');
    setError(null);
    setIsSubmitting(false);
  };

  const handleClose = () => {
    if (isSubmitting) return;
    resetForm();
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSubmitting) return;

    const trimmedName = name.trim();
    const trimmedCity = city.trim();
    const trimmedState = state.trim();

    if (!trimmedName || trimmedName.length < 2) {
      setError('Please provide a studio or business name (at least 2 characters).');
      return;
    }
    if (!trimmedCity || trimmedCity.length < 2) {
      setError('Please provide a city location.');
      return;
    }
    if (!trimmedState || trimmedState.length < 2) {
      setError('Please provide a state location.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const response = await createStudio({
        name: trimmedName,
        professionalType,
        city: trimmedCity,
        state: trimmedState,
      });

      resetForm();
      await onSuccess(response.studioId, response.name);
    } catch (err: any) {
      const message =
        err?.envelope?.message ||
        err?.message ||
        'Failed to create studio. Please check details and try again.';
      setError(message);
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog
      isOpen={isOpen}
      onClose={handleClose}
      title="Create Another Professional Studio"
      description="Establish a new independent studio workspace with distinct team members, projects, portfolio, and settings."
      maxWidth="md"
    >
      <form onSubmit={handleSubmit} className="p-6 space-y-4">
        {error && (
          <div
            role="alert"
            className="flex items-start gap-2.5 p-3 rounded-xl bg-terracotta-50 border border-terracotta-200 text-terracotta-800 text-xs"
          >
            <AlertCircle className="w-4 h-4 text-terracotta-700 flex-shrink-0 mt-0.5" />
            <span className="leading-relaxed">{error}</span>
          </div>
        )}

        <div>
          <label htmlFor="studio-name-input" className="block text-xs font-semibold text-charcoal-700 mb-1">
            Studio or Practice Name <span className="text-terracotta-600">*</span>
          </label>
          <div className="relative">
            <Building2 className="w-4 h-4 absolute left-3 top-3 text-charcoal-400" />
            <input
              id="studio-name-input"
              type="text"
              required
              disabled={isSubmitting}
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Studio Atelier Design"
              className="w-full pl-9 pr-3 py-2 text-sm bg-white border border-sand-300 rounded-xl text-charcoal-900 placeholder-charcoal-400 focus:outline-none focus:ring-2 focus:ring-bronze-500 disabled:opacity-50"
            />
          </div>
        </div>

        <div>
          <label htmlFor="professional-type-select" className="block text-xs font-semibold text-charcoal-700 mb-1">
            Professional Practice Type <span className="text-terracotta-600">*</span>
          </label>
          <select
            id="professional-type-select"
            disabled={isSubmitting}
            value={professionalType}
            onChange={(e) => setProfessionalType(e.target.value)}
            className="w-full px-3 py-2 text-sm bg-white border border-sand-300 rounded-xl text-charcoal-900 focus:outline-none focus:ring-2 focus:ring-bronze-500 disabled:opacity-50"
          >
            {PROFESSIONAL_TYPES.map((type) => (
              <option key={type.code} value={type.code}>
                {type.label}
              </option>
            ))}
          </select>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="studio-city-input" className="block text-xs font-semibold text-charcoal-700 mb-1">
              City <span className="text-terracotta-600">*</span>
            </label>
            <input
              id="studio-city-input"
              type="text"
              required
              disabled={isSubmitting}
              value={city}
              onChange={(e) => setCity(e.target.value)}
              placeholder="e.g. Hyderabad"
              className="w-full px-3 py-2 text-sm bg-white border border-sand-300 rounded-xl text-charcoal-900 placeholder-charcoal-400 focus:outline-none focus:ring-2 focus:ring-bronze-500 disabled:opacity-50"
            />
          </div>

          <div>
            <label htmlFor="studio-state-input" className="block text-xs font-semibold text-charcoal-700 mb-1">
              State <span className="text-terracotta-600">*</span>
            </label>
            <input
              id="studio-state-input"
              type="text"
              required
              disabled={isSubmitting}
              value={state}
              onChange={(e) => setState(e.target.value)}
              placeholder="e.g. Telangana"
              className="w-full px-3 py-2 text-sm bg-white border border-sand-300 rounded-xl text-charcoal-900 placeholder-charcoal-400 focus:outline-none focus:ring-2 focus:ring-bronze-500 disabled:opacity-50"
            />
          </div>
        </div>

        <div className="pt-3 border-t border-sand-200 flex items-center justify-end gap-2">
          <button
            type="button"
            disabled={isSubmitting}
            onClick={handleClose}
            className="px-4 py-2 text-xs font-medium text-charcoal-600 hover:text-charcoal-900 hover:bg-sand-100 rounded-xl transition-colors disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex items-center gap-2 px-4 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-medium hover:bg-charcoal-800 transition-colors disabled:opacity-60 shadow-xs"
          >
            {isSubmitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            {isSubmitting ? 'Creating Studio...' : 'Create Studio'}
          </button>
        </div>
      </form>
    </Dialog>
  );
}
