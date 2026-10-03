'use client';

import React, { useState, useEffect } from 'react';
import { Sparkles, RotateCcw, Check, Undo2, Layers, ShieldCheck, Box, Info } from 'lucide-react';
import { VoiceDictationButton } from './VoiceDictationButton';
import { enhancePrompt, PromptEnhanceResult } from '@/lib/ai/prompt-enhancer';

export interface AiPromptComposerProps {
  prompt: string;
  onPromptChange: (newPrompt: string) => void;
  editingMode: 'FULL_IMAGE' | 'PRECISION_MASK';
  preserveStructure: boolean;
  roomType?: string;
  architecturalStyle?: string;
  referenceCount?: number;
  referencePurposes?: string[];
  disabled?: boolean;
  maxLength?: number;
  presets?: { label: string; prompt: string }[];
}

export function AiPromptComposer({
  prompt,
  onPromptChange,
  editingMode,
  preserveStructure,
  roomType,
  architecturalStyle,
  referenceCount = 0,
  referencePurposes = [],
  disabled = false,
  maxLength = 1000,
  presets = [],
}: AiPromptComposerProps) {
  const [originalPromptSnapshot, setOriginalPromptSnapshot] = useState<string | null>(null);
  const [enhancementResult, setEnhancementResult] = useState<PromptEnhanceResult | null>(null);
  const [isEnhancing, setIsEnhancing] = useState(false);
  const [showOptimizationNotice, setShowOptimizationNotice] = useState(false);

  // Voice transcript handler: appends or replaces text based on user state
  const handleVoiceTranscript = (transcriptText: string) => {
    if (!transcriptText.trim()) return;
    if (!prompt.trim()) {
      onPromptChange(transcriptText);
    } else {
      // Append smoothly with space
      onPromptChange(`${prompt.trim()} ${transcriptText}`);
    }
  };

  const handleImprovePrompt = async () => {
    if (!prompt.trim() || isEnhancing || disabled) return;

    setIsEnhancing(true);
    // Snapshot original user input so they can always revert
    if (!originalPromptSnapshot) {
      setOriginalPromptSnapshot(prompt);
    }

    try {
      const res = await enhancePrompt({
        prompt,
        roomType,
        editingMode,
        preserveStructure,
        architecturalStyle,
        referencePurposes,
      });
      setEnhancementResult(res);
      onPromptChange(res.enhancedPrompt);
      setShowOptimizationNotice(true);
    } finally {
      setIsEnhancing(false);
    }
  };

  const handleRevertOriginal = () => {
    if (originalPromptSnapshot) {
      onPromptChange(originalPromptSnapshot);
      setOriginalPromptSnapshot(null);
      setEnhancementResult(null);
      setShowOptimizationNotice(false);
    }
  };

  const handleReset = () => {
    onPromptChange('');
    setOriginalPromptSnapshot(null);
    setEnhancementResult(null);
    setShowOptimizationNotice(false);
  };

  return (
    <div className="space-y-3">
      {/* Context Pills Bar */}
      <div className="flex items-center justify-between flex-wrap gap-2 pt-1">
        <div className="flex items-center gap-1.5 flex-wrap">
          {/* Mode Pill */}
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-medium bg-sand-100 text-charcoal-800 border border-sand-200">
            <Layers className="w-3 h-3 text-bronze-700" />
            {editingMode === 'PRECISION_MASK' ? 'Precision Mask (Inpainting)' : 'Full Space Transform'}
          </span>

          {/* Structure Pill */}
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-medium bg-sand-100 text-charcoal-800 border border-sand-200">
            <ShieldCheck className="w-3 h-3 text-forest-700" />
            {preserveStructure ? 'Structure Locked' : 'Structure Flexible'}
          </span>

          {/* Room Pill */}
          {roomType && (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-medium bg-sand-100 text-charcoal-800 border border-sand-200">
              <Box className="w-3 h-3 text-bronze-700" />
              {roomType}
            </span>
          )}

          {/* References Pill */}
          {referenceCount > 0 && (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-medium bg-bronze-50 text-bronze-800 border border-bronze-200">
              <Sparkles className="w-3 h-3" />
              {referenceCount} {referenceCount === 1 ? 'Reference' : 'References'} attached
            </span>
          )}
        </div>

        {/* Character Counter */}
        <span
          className={`text-[11px] font-mono ${
            prompt.length > maxLength ? 'text-terracotta-600 font-bold' : 'text-charcoal-400'
          }`}
        >
          {prompt.length} / {maxLength}
        </span>
      </div>

      {/* Main Composer Box */}
      <div className="relative rounded-2xl border border-sand-300 bg-[#FAF8F5] focus-within:border-bronze-700 focus-within:ring-1 focus-within:ring-bronze-700 transition-all shadow-2xs">
        <textarea
          rows={3}
          value={prompt}
          onChange={(e) => onPromptChange(e.target.value)}
          disabled={disabled || isEnhancing}
          aria-label="AI Prompt Instruction"
          placeholder={
            editingMode === 'PRECISION_MASK'
              ? "Describe target changes to the selected area (e.g. 'Change wardrobe shutters to walnut wood with matte black handles')..."
              : "Describe your spatial concept (e.g. 'Transform to warm minimalist aesthetic with fluted oak cabinetry and brushed brass pulls')..."
          }
          className="w-full bg-transparent p-3 sm:p-4 text-xs sm:text-sm text-charcoal-900 placeholder:text-charcoal-400 focus:outline-hidden leading-relaxed resize-y min-h-[90px]"
        />

        {/* Action Controls Bar inside Composer */}
        <div className="p-2 sm:p-3 bg-white/70 border-t border-sand-200/80 rounded-b-2xl flex items-center justify-between gap-2 flex-wrap">
          {/* Left: Voice Dictation Button */}
          <div className="flex items-center gap-2">
            <VoiceDictationButton
              onTranscript={handleVoiceTranscript}
              disabled={disabled || isEnhancing}
            />
          </div>

          {/* Right: Enhancement & Reset Actions */}
          <div className="flex items-center gap-2 flex-wrap">
            {originalPromptSnapshot && (
              <button
                type="button"
                onClick={handleRevertOriginal}
                disabled={disabled || isEnhancing}
                className="min-h-[44px] px-3 py-2 rounded-xl border border-sand-300 bg-white hover:bg-sand-50 text-xs font-medium text-charcoal-700 transition-colors flex items-center gap-1.5"
                title="Restore original text before optimization"
              >
                <Undo2 className="w-3.5 h-3.5 text-charcoal-500" />
                <span>Use Original</span>
              </button>
            )}

            <button
              type="button"
              onClick={handleImprovePrompt}
              disabled={disabled || isEnhancing || !prompt.trim()}
              className="min-h-[44px] px-3.5 py-2 rounded-xl bg-bronze-700 hover:bg-bronze-800 active:bg-bronze-900 text-white text-xs font-medium transition-all shadow-xs flex items-center gap-1.5 disabled:opacity-50 disabled:cursor-not-allowed"
              title="Enhance prompt with structured architectural directives and negative preservation constraints"
            >
              <Sparkles className={`w-3.5 h-3.5 ${isEnhancing ? 'animate-spin' : ''}`} />
              <span>{isEnhancing ? 'Optimizing Prompt...' : 'Improve Prompt'}</span>
            </button>

            {prompt.trim().length > 0 && (
              <button
                type="button"
                onClick={handleReset}
                disabled={disabled || isEnhancing}
                className="min-h-[44px] min-w-[44px] p-2 rounded-xl border border-sand-200 hover:bg-sand-100 text-charcoal-400 hover:text-charcoal-700 transition-colors flex items-center justify-center"
                title="Clear prompt"
                aria-label="Clear prompt"
              >
                <RotateCcw className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Optimization Notice Banner */}
      {showOptimizationNotice && enhancementResult && (
        <div className="p-3 sm:p-4 rounded-xl bg-bronze-50/80 border border-bronze-200 text-xs text-charcoal-800 space-y-2">
          <div className="flex items-center justify-between font-semibold text-bronze-900">
            <span className="flex items-center gap-1.5">
              <Check className="w-4 h-4 text-forest-600" />
              Architectural Prompt Optimization Applied
            </span>
            <button
              type="button"
              onClick={() => setShowOptimizationNotice(false)}
              className="text-charcoal-400 hover:text-charcoal-700 text-xs"
              aria-label="Dismiss optimization notice"
            >
              ✕
            </button>
          </div>
          <p className="text-[11px] text-charcoal-600 leading-relaxed">
            Your informal prompt has been structured into professional architectural directives. You can edit any word in the text box above before generating.
          </p>
          <div className="flex flex-wrap gap-1.5 pt-1 text-[10px]">
            {enhancementResult.detectedElements.length > 0 && (
              <span className="px-2 py-0.5 rounded bg-white border border-bronze-200 text-charcoal-700">
                Elements: {enhancementResult.detectedElements.join(', ')}
              </span>
            )}
            {enhancementResult.detectedMaterials.length > 0 && (
              <span className="px-2 py-0.5 rounded bg-white border border-bronze-200 text-charcoal-700">
                Materials: {enhancementResult.detectedMaterials.join(', ')}
              </span>
            )}
            {enhancementResult.detectedColors.length > 0 && (
              <span className="px-2 py-0.5 rounded bg-white border border-bronze-200 text-charcoal-700">
                Colors: {enhancementResult.detectedColors.join(', ')}
              </span>
            )}
          </div>
        </div>
      )}

      {/* Design Starters / Presets */}
      {presets.length > 0 && (
        <div className="space-y-1 pt-1">
          <span className="text-[10px] text-charcoal-400 uppercase tracking-wider block">
            {editingMode === 'PRECISION_MASK' ? 'Targeted Edit Starters:' : 'Design Starters:'}
          </span>
          <div className="flex flex-wrap gap-1.5">
            {presets.map((p, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => onPromptChange(p.prompt)}
                disabled={disabled || isEnhancing}
                className="text-[11px] min-h-[36px] px-2.5 py-1 rounded-lg bg-sand-100/70 hover:bg-sand-200/80 active:bg-sand-300 text-charcoal-700 text-left transition-colors border border-sand-200/50"
              >
                {p.label}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
