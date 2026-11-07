'use client';

import React, { useState, useEffect, useRef } from 'react';
import { Sparkles, RotateCcw, Check, Undo2, Layers, ShieldCheck, Box, Info, Palette, ChevronDown, ChevronUp } from 'lucide-react';
import { VoiceDictationButton } from './VoiceDictationButton';
import { enhancePrompt, PromptEnhanceResult } from '@/lib/ai/prompt-enhancer';

const CUPBOARD_COLORS = [
  { name: 'Matte Alabaster', hex: '#F8F6F0', bgClass: 'bg-[#F8F6F0]', borderClass: 'border-sand-300' },
  { name: 'Warm Cashmere', hex: '#E8DCC4', bgClass: 'bg-[#E8DCC4]', borderClass: 'border-sand-300' },
  { name: 'Muted Sage Green', hex: '#9EAC9C', bgClass: 'bg-[#9EAC9C]', borderClass: 'border-emerald-300' },
  { name: 'Forest Emerald', hex: '#2A4B3A', bgClass: 'bg-[#2A4B3A]', borderClass: 'border-emerald-900' },
  { name: 'Natural Walnut', hex: '#5C4033', bgClass: 'bg-[#5C4033]', borderClass: 'border-amber-950' },
  { name: 'Warm Teak Wood', hex: '#8B5A2B', bgClass: 'bg-[#8B5A2B]', borderClass: 'border-amber-800' },
  { name: 'Charcoal Slate', hex: '#2B2D2F', bgClass: 'bg-[#2B2D2F]', borderClass: 'border-charcoal-700' },
  { name: 'Royal Navy Blue', hex: '#1B2A4A', bgClass: 'bg-[#1B2A4A]', borderClass: 'border-indigo-950' },
  { name: 'Brushed Champagne', hex: '#D4AF37', bgClass: 'bg-[#D4AF37]', borderClass: 'border-amber-400' },
  { name: 'Terracotta Matte', hex: '#C87D55', bgClass: 'bg-[#C87D55]', borderClass: 'border-amber-600' },
];

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
  const [showColorPicker, setShowColorPicker] = useState(false);
  const [customColorText, setCustomColorText] = useState('');

  const basePromptBeforeVoiceRef = useRef<string | null>(null);

  // Voice transcript handler: maintains base text and accumulates speech continuously without duplicating words
  const handleVoiceTranscript = (transcriptText: string) => {
    if (!transcriptText.trim()) return;
    if (basePromptBeforeVoiceRef.current === null) {
      basePromptBeforeVoiceRef.current = prompt.trim();
    }
    const base = basePromptBeforeVoiceRef.current;
    const combined = base ? `${base} ${transcriptText.trim()}` : transcriptText.trim();
    // The speech service sends the entire session transcript, not a new chunk.
    onPromptChange(combined.slice(0, maxLength));
  };

  const applyCupboardColor = (colorName: string, hex?: string) => {
    const colorDesc = hex ? `${colorName} (${hex})` : colorName;
    const generatedPrompt = editingMode === 'PRECISION_MASK'
      ? `Recolour the selected cupboard doors ${colorDesc}.`
      : `Recolour the cupboard doors ${colorDesc}. Keep the room and existing cabinet structure.`;

    onPromptChange(generatedPrompt);
    setShowColorPicker(false);
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
      // Never truncate a spatial instruction or produce an unsendable prompt.
      onPromptChange(res.enhancedPrompt.length <= maxLength ? res.enhancedPrompt : prompt);
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
            {editingMode === 'PRECISION_MASK' ? 'Precision Mask (Inpainting)' : 'Source Photo Edit'}
          </span>

          {/* Structure Pill */}
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] font-medium bg-sand-100 text-charcoal-800 border border-sand-200">
            <ShieldCheck className="w-3 h-3 text-forest-700" />
            {preserveStructure ? 'Preserve Structure Requested' : 'Structure Flexible'}
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
              ? "Describe the selected surface: matte black cupboard doors, for example."
              : "Name one object and its new finish: recolour the kitchen cabinets sage green."
          }
          className="w-full bg-transparent p-3 sm:p-4 text-xs sm:text-sm text-charcoal-900 placeholder:text-charcoal-400 focus:outline-hidden leading-relaxed resize-y min-h-[90px]"
        />

        {/* Action Controls Bar inside Composer */}
        <div className="p-2 sm:p-3 bg-white/70 border-t border-sand-200/80 rounded-b-2xl flex items-center justify-between gap-2 flex-wrap">
          {/* Left: Voice Dictation Button */}
          <div className="flex items-center gap-2">
            <VoiceDictationButton
              onTranscript={handleVoiceTranscript}
              onSessionStart={() => { basePromptBeforeVoiceRef.current = prompt.trim(); }}
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
              {enhancementResult.enhancedPrompt.length <= maxLength ? 'Prompt enhancement applied' : 'Original instructions retained'}
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
            {enhancementResult.enhancedPrompt.length <= maxLength
              ? 'Your instructions are preserved with additional rendering guidance. Review the prompt before generating.'
              : 'The expanded version exceeded the character limit, so your original instructions were kept without truncation.'}
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

      {/* Primary Architectural Design Starter: Cupboard / Wardrobe Recolor */}
      <details className="pt-1 space-y-2 rounded-lg border border-sand-200 p-3">
        <summary className="cursor-pointer text-sm font-medium text-charcoal-700">Colour presets &amp; examples</summary>
        <div className="flex items-center justify-between flex-wrap gap-2">
          <span className="text-[10px] font-semibold text-charcoal-700 uppercase tracking-wider block">
            {editingMode === 'PRECISION_MASK' ? 'Targeted Edit Starters:' : 'Design Starters:'}
          </span>
          <button
            type="button"
            onClick={() => setShowColorPicker(!showColorPicker)}
            disabled={disabled || isEnhancing}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-bronze-50 border border-bronze-300 text-bronze-900 hover:bg-bronze-100 active:bg-bronze-200 text-xs font-semibold shadow-2xs transition-all"
          >
            <Palette className="w-3.5 h-3.5 text-bronze-700" />
            <span>Choose a finish</span>
            {showColorPicker ? <ChevronUp className="w-3 h-3 ml-0.5" /> : <ChevronDown className="w-3 h-3 ml-0.5" />}
          </button>
        </div>

        {/* Inline Colour Swatch Panel */}
        {showColorPicker && (
          <div className="p-3.5 rounded-2xl bg-white border border-bronze-200 shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-charcoal-800">
                Choose Furniture / Cabinet Finish
              </span>
              <span className="text-[10px] text-charcoal-500">
                Preserves doors, handles & geometry
              </span>
            </div>

            {/* Swatch Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
              {CUPBOARD_COLORS.map((col) => (
                <button
                  key={col.hex}
                  type="button"
                  onClick={() => applyCupboardColor(col.name, col.hex)}
                  className="flex items-center gap-2 p-2 rounded-xl border border-sand-200 hover:border-bronze-600 bg-sand-50/50 hover:bg-sand-50 transition-all text-left group"
                >
                  <span
                    className={`w-5 h-5 rounded-lg border shadow-2xs flex-shrink-0 ${col.bgClass} ${col.borderClass}`}
                  />
                  <div className="min-w-0">
                    <span className="text-[11px] font-medium text-charcoal-800 block truncate group-hover:text-bronze-900">
                      {col.name}
                    </span>
                    <span className="text-[9px] font-mono text-charcoal-400 block">
                      {col.hex}
                    </span>
                  </div>
                </button>
              ))}
            </div>

            {/* Custom Colour Input */}
            <div className="pt-2 border-t border-sand-200 flex items-center gap-2">
              <input
                type="text"
                value={customColorText}
                onChange={(e) => setCustomColorText(e.target.value)}
                placeholder="Or enter custom colour (e.g. Olive Drab, #3B4D3C, Nordic Oak)..."
                className="flex-1 px-3 py-1.5 rounded-lg border border-sand-300 text-xs bg-sand-50 focus:bg-white focus:outline-none focus:ring-1 focus:ring-bronze-700"
              />
              <button
                type="button"
                onClick={() => {
                  if (customColorText.trim()) {
                    applyCupboardColor(customColorText.trim());
                    setCustomColorText('');
                  }
                }}
                disabled={!customColorText.trim()}
                className="px-3 py-1.5 rounded-lg bg-charcoal-900 hover:bg-black text-white text-xs font-medium disabled:opacity-40 transition-colors"
              >
                Apply
              </button>
            </div>
          </div>
        )}

        {/* Other Presets */}
        {presets.length > 0 && (
          <div className="flex flex-wrap gap-1.5 pt-1">
            {presets.map((p, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => onPromptChange(p.prompt)}
                disabled={disabled || isEnhancing}
                className="text-[11px] min-h-[32px] px-2.5 py-1 rounded-lg bg-sand-100/70 hover:bg-sand-200/80 active:bg-sand-300 text-charcoal-700 text-left transition-colors border border-sand-200/50"
              >
                {p.label}
              </button>
            ))}
          </div>
        )}
      </details>
    </div>
  );
}
