'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { MessageSquare, ArrowLeft, CheckCircle2, AlertCircle, Send } from 'lucide-react';
import { Button } from '@/components/ui/Button';

export default function FeedbackPage() {
  const [category, setCategory] = useState('GENERAL');
  const [message, setMessage] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!message.trim()) return;

    setSubmitting(true);
    setError(null);
    try {
      const res = await fetch('/api/v1/account/feedback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          category,
          message: message.trim(),
          contactEmail: contactEmail.trim() || undefined,
        }),
        credentials: 'include',
      });
      if (res.ok) {
        setSuccess(true);
      } else {
        setError('Failed to submit feedback. Please try again.');
      }
    } catch {
      setError('A network error occurred. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-xl mx-auto px-4 sm:px-6 py-12 space-y-8">
      <div>
        <Link
          href="/"
          className="inline-flex items-center gap-1.5 text-xs font-medium text-charcoal-500 hover:text-charcoal-900 mb-3 transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back</span>
        </Link>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-sand-100 text-bronze-700 flex items-center justify-center">
            <MessageSquare className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs font-semibold uppercase tracking-widest text-bronze-700 block">
              Continuous Improvement
            </span>
            <h1 className="font-serif text-2xl sm:text-3xl text-charcoal-900 tracking-tight">
              Platform Feedback
            </h1>
          </div>
        </div>
        <p className="text-xs sm:text-sm text-charcoal-600 mt-2">
          Help us refine the experience. Report bugs, inaccurate studio info, or suggest new design tools.
        </p>
      </div>

      <div className="bg-white border border-sand-200 rounded-2xl p-6 sm:p-8 shadow-sm">
        {success ? (
          <div className="text-center py-6 space-y-3">
            <div className="w-12 h-12 rounded-full bg-forest-50 text-forest-700 flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <h2 className="font-serif text-xl text-charcoal-900 font-semibold">Thank You!</h2>
            <p className="text-xs text-charcoal-600 max-w-sm mx-auto leading-relaxed">
              Your feedback has been submitted to the product team. We appreciate your partnership in making Elégance better.
            </p>
            <div className="pt-3">
              <Link
                href="/"
                className="inline-flex items-center gap-1.5 px-4 py-2 bg-charcoal-900 text-white rounded-xl text-xs font-medium hover:bg-charcoal-800 transition-colors"
              >
                Return to Home
              </Link>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
              <div className="p-3 bg-red-50 border border-red-200 text-red-800 rounded-xl text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-red-600" />
                <span>{error}</span>
              </div>
            )}

            <div>
              <label className="text-xs font-semibold text-charcoal-700 uppercase tracking-wider block mb-1.5">
                Feedback Category
              </label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full text-xs bg-sand-50/50 border border-sand-300 rounded-xl px-3.5 py-2.5 text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-bronze-600"
              >
                <option value="GENERAL">General Feedback</option>
                <option value="BUG">Bug or Technical Issue</option>
                <option value="INCORRECT_INFO">Incorrect Studio or Project Information</option>
                <option value="PRIVACY_CONCERN">Privacy or Data Concern</option>
                <option value="FEATURE_REQUEST">Feature Suggestion</option>
              </select>
            </div>

            <div>
              <label className="text-xs font-semibold text-charcoal-700 uppercase tracking-wider block mb-1.5">
                Your Comments
              </label>
              <textarea
                rows={4}
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                required
                placeholder="Describe your feedback, issue, or suggested improvement..."
                className="w-full text-xs sm:text-sm bg-sand-50/50 border border-sand-300 rounded-xl p-3 text-charcoal-900 placeholder:text-charcoal-400 focus:outline-none focus:ring-1 focus:ring-bronze-600 resize-y"
              />
            </div>

            <div>
              <label className="text-xs font-semibold text-charcoal-700 uppercase tracking-wider block mb-1.5">
                Email Address (Optional, if you would like a reply)
              </label>
              <input
                type="email"
                value={contactEmail}
                onChange={(e) => setContactEmail(e.target.value)}
                placeholder="name@example.com"
                className="w-full text-xs sm:text-sm bg-sand-50/50 border border-sand-300 rounded-xl px-3.5 py-2.5 text-charcoal-900 focus:outline-none focus:ring-1 focus:ring-bronze-600"
              />
            </div>

            <div className="pt-2">
              <Button type="submit" variant="primary" size="md" className="w-full" disabled={submitting}>
                <Send className="w-4 h-4 mr-1.5" />
                <span>{submitting ? 'Submitting...' : 'Submit Feedback'}</span>
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
