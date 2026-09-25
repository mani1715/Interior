'use client';

import React, { useEffect, useState } from 'react';
import { useAuth } from '@/lib/auth/auth-context';
import {
  StudioVerificationDto,
  VerificationDocumentType,
} from '@/lib/verification/types';
import {
  getStudioVerification,
  registerVerificationDocument,
  submitVerification,
} from '@/lib/verification/api';

export default function WorkspaceVerificationPage() {
  const { user } = useAuth();
  const [data, setData] = useState<StudioVerificationDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Submission Form State
  const [businessName, setBusinessName] = useState('');
  const [registrationNumber, setRegistrationNumber] = useState('');
  const [taxIdentifier, setTaxIdentifier] = useState('');
  const [registeredAddress, setRegisteredAddress] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // File Upload State
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [docType, setDocType] = useState<VerificationDocumentType>('BUSINESS_REGISTRATION');
  const [isUploading, setIsUploading] = useState(false);
  const [uploadedDocIds, setUploadedDocIds] = useState<string[]>([]);

  const fetchVerification = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await getStudioVerification();
      setData(res);
      setBusinessName(res.businessName || '');
      setRegistrationNumber(res.registrationNumber || '');
      setTaxIdentifier(res.taxIdentifier || '');
      setRegisteredAddress(res.registeredAddress || '');
      if (res.documents) {
        setUploadedDocIds(res.documents.map((d) => d.id));
      }
    } catch {
      setError('Failed to load studio verification status.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVerification();
  }, []);

  const handleUploadDocument = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) return;

    // Client-side file size check (10MB limit)
    if (selectedFile.size > 10 * 1024 * 1024) {
      setError('File exceeds 10MB limit.');
      return;
    }

    setIsUploading(true);
    setError(null);
    try {
      const doc = await registerVerificationDocument(selectedFile, docType);
      setUploadedDocIds((prev) => [...prev, doc.id]);
      setSelectedFile(null);
      setSuccess('Document uploaded securely to platform evidence vault.');
      fetchVerification();
    } catch {
      setError('Failed to upload document. Only PDF, JPG, and PNG are supported.');
    } finally {
      setIsUploading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!businessName.trim()) {
      setError('Official Registered Business Name is required.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      const updated = await submitVerification({
        businessName: businessName.trim(),
        registrationNumber: registrationNumber.trim() || null,
        taxIdentifier: taxIdentifier.trim() || null,
        registeredAddress: registeredAddress.trim() || null,
        documentIds: uploadedDocIds,
      });
      setData(updated);
      setSuccess('Verification application submitted for platform review.');
    } catch {
      setError('Submission failed. Please check the fields and try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="py-20 flex justify-center">
        <div className="w-8 h-8 border-4 border-zinc-200 border-t-zinc-900 rounded-full animate-spin" />
      </div>
    );
  }

  const status = data?.status || 'NOT_SUBMITTED';

  const statusBadges = {
    NOT_SUBMITTED: { label: 'Not Submitted', color: 'bg-zinc-100 text-zinc-700 border-zinc-200' },
    PENDING: { label: 'Pending Platform Review', color: 'bg-amber-50 text-amber-800 border-amber-200' },
    NEEDS_MORE_INFO: { label: 'Needs Additional Info', color: 'bg-orange-50 text-orange-800 border-orange-200' },
    VERIFIED: { label: 'Verified Business Entity', color: 'bg-emerald-50 text-emerald-800 border-emerald-200' },
    REJECTED: { label: 'Verification Declined', color: 'bg-red-50 text-red-800 border-red-200' },
    REVERIFY_REQUIRED: { label: 'Re-verification Required', color: 'bg-yellow-50 text-yellow-800 border-yellow-200' },
    EXPIRED: { label: 'Verification Expired', color: 'bg-zinc-100 text-zinc-700 border-zinc-300' },
  }[status];

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-8 space-y-8">
      {/* Title */}
      <div>
        <span className="text-xs font-semibold tracking-widest uppercase text-emerald-700 block mb-1">
          Trust & Authenticity
        </span>
        <h1 className="text-3xl font-extrabold text-zinc-900 tracking-tight">
          Business Verification
        </h1>
        <p className="text-sm text-zinc-500 mt-1">
          Verify your studio identity with legal business registration records to build genuine trust with potential clients.
        </p>
      </div>

      {error && (
        <div className="p-4 bg-red-50 text-red-700 rounded-xl text-xs flex justify-between items-center">
          <span>{error}</span>
          <button type="button" onClick={() => setError(null)} className="font-bold">✕</button>
        </div>
      )}

      {success && (
        <div className="p-4 bg-emerald-50 text-emerald-700 rounded-xl text-xs flex justify-between items-center">
          <span>{success}</span>
          <button type="button" onClick={() => setSuccess(null)} className="font-bold">✕</button>
        </div>
      )}

      {/* Current Status Card */}
      <div className="p-6 bg-white rounded-2xl border border-zinc-200/80 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-medium text-zinc-500">Current Status</p>
          <div className="flex items-center gap-2">
            <span
              className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold border ${statusBadges.color}`}
            >
              {statusBadges.label}
            </span>
            {data?.verifiedAt && (
              <span className="text-xs text-zinc-500">
                Verified on {new Date(data.verifiedAt).toLocaleDateString('en-IN')}
              </span>
            )}
          </div>
          {status === 'REVERIFY_REQUIRED' && (
            <p className="text-xs text-amber-700 pt-1">
              Critical studio profile attributes were updated. Please confirm your registration details to reinstate the verified badge.
            </p>
          )}
          {data?.rejectionReason && (
            <p className="text-xs text-red-600 pt-1">
              Feedback: {data.rejectionReason}
            </p>
          )}
        </div>

        {/* Truthful Disclaimer Notice */}
        <div className="sm:max-w-xs p-3 bg-zinc-50 rounded-xl border border-zinc-200/60 text-[11px] text-zinc-500 leading-relaxed">
          <span className="font-semibold text-zinc-700 block mb-0.5">Platform Policy</span>
          Verification validates government registration and business entity records. It does not rank studios or guarantee project outcomes.
        </div>
      </div>

      {/* Strict PII Warning */}
      <div className="p-4 bg-blue-50/70 border border-blue-200/80 rounded-xl text-xs text-blue-900 space-y-1">
        <p className="font-semibold flex items-center gap-1.5">
          <span>🔒</span> Private Verification Evidence Vault
        </p>
        <p className="text-blue-800 leading-relaxed">
          Upload only business entity documents (GST Registration, Certificate of Incorporation, Trade License, or Council of Architecture License).
          <span className="font-bold"> Strictly DO NOT upload Aadhaar cards or personal PAN cards.</span> All evidence is encrypted and restricted to administrative review.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Verification Form */}
        <div className="p-6 bg-white rounded-2xl border border-zinc-200/80 shadow-xs space-y-4">
          <h2 className="text-base font-bold text-zinc-900">Business Details</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-zinc-700 mb-1">
                Registered Legal Business Name <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                required
                value={businessName}
                onChange={(e) => setBusinessName(e.target.value)}
                placeholder="e.g. Apex Studio Design Private Limited"
                className="w-full text-xs p-2.5 border border-zinc-300 rounded-lg focus:ring-1 focus:ring-zinc-900"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-zinc-700 mb-1">
                Company Registration / CIN / LLPIN
              </label>
              <input
                type="text"
                value={registrationNumber}
                onChange={(e) => setRegistrationNumber(e.target.value)}
                placeholder="e.g. U74999KA2020PTC123456"
                className="w-full text-xs p-2.5 border border-zinc-300 rounded-lg focus:ring-1 focus:ring-zinc-900"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-zinc-700 mb-1">
                Business GSTIN
              </label>
              <input
                type="text"
                value={taxIdentifier}
                onChange={(e) => setTaxIdentifier(e.target.value)}
                placeholder="e.g. 29ABCDE1234F1Z5"
                className="w-full text-xs p-2.5 border border-zinc-300 rounded-lg focus:ring-1 focus:ring-zinc-900"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-zinc-700 mb-1">
                Registered Office Address
              </label>
              <textarea
                rows={3}
                value={registeredAddress}
                onChange={(e) => setRegisteredAddress(e.target.value)}
                placeholder="Official corporate address as recorded in registration documents..."
                className="w-full text-xs p-2.5 border border-zinc-300 rounded-lg focus:ring-1 focus:ring-zinc-900"
              />
            </div>

            <div className="pt-2">
              <button
                type="submit"
                disabled={isSubmitting || !businessName.trim()}
                className="w-full py-2.5 text-xs font-semibold text-white bg-zinc-900 hover:bg-zinc-800 rounded-lg disabled:opacity-50 transition-colors"
              >
                {isSubmitting ? 'Submitting...' : 'Submit Verification Application'}
              </button>
            </div>
          </form>
        </div>

        {/* Evidence Documents Vault */}
        <div className="space-y-6">
          <div className="p-6 bg-white rounded-2xl border border-zinc-200/80 shadow-xs space-y-4">
            <h2 className="text-base font-bold text-zinc-900">Upload Evidence Document</h2>
            <form onSubmit={handleUploadDocument} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-zinc-700 mb-1">
                  Document Type
                </label>
                <select
                  value={docType}
                  onChange={(e) => setDocType(e.target.value as VerificationDocumentType)}
                  className="w-full text-xs p-2.5 border border-zinc-300 rounded-lg focus:ring-1 focus:ring-zinc-900"
                >
                  <option value="BUSINESS_REGISTRATION">Certificate of Business Registration</option>
                  <option value="GST_CERTIFICATE">GST Registration Certificate</option>
                  <option value="TRADE_LICENSE">Municipal Trade License</option>
                  <option value="COA_ARCHITECT_REGISTRATION">Council of Architecture (CoA) Registration</option>
                  <option value="INCORPORATION_CERTIFICATE">Certificate of Incorporation (ROC)</option>
                  <option value="OTHER_GOVERNMENT_IDENTIFIER">Other Business Entity Proof</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-zinc-700 mb-1">
                  File (PDF, JPEG, or PNG up to 10MB)
                </label>
                <input
                  type="file"
                  accept="application/pdf,image/jpeg,image/png"
                  onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
                  className="w-full text-xs p-2 border border-zinc-300 rounded-lg file:mr-3 file:py-1 file:px-2.5 file:rounded-md file:border-0 file:text-xs file:font-semibold file:bg-zinc-100 file:text-zinc-700 hover:file:bg-zinc-200"
                />
              </div>

              <button
                type="submit"
                disabled={isUploading || !selectedFile}
                className="w-full py-2 text-xs font-semibold text-zinc-900 bg-zinc-100 hover:bg-zinc-200 rounded-lg disabled:opacity-50 transition-colors"
              >
                {isUploading ? 'Uploading to Vault...' : 'Upload Document'}
              </button>
            </form>
          </div>

          {/* Uploaded Documents List */}
          <div className="p-6 bg-white rounded-2xl border border-zinc-200/80 shadow-xs space-y-3">
            <h2 className="text-base font-bold text-zinc-900">Evidence Vault Files</h2>
            {data?.documents && data.documents.length > 0 ? (
              <div className="space-y-2">
                {data.documents.map((doc) => (
                  <div
                    key={doc.id}
                    className="p-3 bg-zinc-50 rounded-xl border border-zinc-200/60 flex items-center justify-between text-xs"
                  >
                    <div>
                      <p className="font-semibold text-zinc-800">{doc.fileName}</p>
                      <p className="text-[11px] text-zinc-500">
                        {doc.documentType.replace(/_/g, ' ')} • {(doc.fileSizeBytes / 1024).toFixed(0)} KB
                      </p>
                    </div>
                    <span className="text-[11px] text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200/50">
                      Encrypted
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-zinc-500 italic">
                No documents uploaded yet. Upload official business certificates to support verification.
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Append-Only Event Timeline */}
      {data?.events && data.events.length > 0 && (
        <div className="p-6 bg-white rounded-2xl border border-zinc-200/80 shadow-xs space-y-4">
          <h2 className="text-base font-bold text-zinc-900">Audit History Timeline</h2>
          <div className="space-y-3">
            {data.events.map((evt) => (
              <div
                key={evt.id}
                className="p-3 bg-zinc-50 rounded-xl border border-zinc-200/60 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs"
              >
                <div>
                  <span className="font-semibold text-zinc-800">
                    {evt.eventType.replace(/_/g, ' ')}
                  </span>
                  <span className="text-zinc-500 ml-2">
                    Status: <span className="font-medium text-zinc-700">{evt.newStatus}</span>
                  </span>
                  {evt.reason && (
                    <p className="text-[11px] text-zinc-600 mt-1">
                      Reason: {evt.reason}
                    </p>
                  )}
                </div>
                <time dateTime={evt.createdAt} className="text-[11px] text-zinc-400 shrink-0">
                  {new Date(evt.createdAt).toLocaleString('en-IN')}
                </time>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
