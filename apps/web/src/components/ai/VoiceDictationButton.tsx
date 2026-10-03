'use client';

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Mic, MicOff, Square, AlertCircle, Loader2 } from 'lucide-react';
import { voiceService, VoiceState, VoiceError } from '@/lib/ai/voice-service';

export interface VoiceDictationButtonProps {
  onTranscript: (text: string) => void;
  disabled?: boolean;
  className?: string;
}

export function VoiceDictationButton({
  onTranscript,
  disabled = false,
  className = '',
}: VoiceDictationButtonProps) {
  const [voiceState, setVoiceState] = useState<VoiceState>('idle');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // Check support on mount
  const [supported, setSupported] = useState(true);

  useEffect(() => {
    setSupported(voiceService.isSupported());
  }, []);

  const clearTimer = () => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  };

  const startTimer = () => {
    clearTimer();
    setRecordingSeconds(0);
    timerRef.current = setInterval(() => {
      setRecordingSeconds((prev) => prev + 1);
    }, 1000);
  };

  const handleStart = async () => {
    if (disabled || !supported) return;
    setErrorMsg(null);

    await voiceService.start({
      onStateChange: (state) => {
        setVoiceState(state);
        if (state === 'listening') {
          startTimer();
        } else {
          clearTimer();
        }
      },
      onTranscriptChange: (text) => {
        onTranscript(text);
      },
      onError: (err: VoiceError) => {
        clearTimer();
        setErrorMsg(err.message);
      },
    });
  };

  const handleStop = () => {
    clearTimer();
    voiceService.stop();
  };

  const handleCancel = () => {
    clearTimer();
    voiceService.cancel();
    setVoiceState('idle');
  };

  useEffect(() => {
    return () => {
      clearTimer();
      voiceService.cancel();
    };
  }, []);

  const formatSeconds = (sec: number) => {
    const mins = Math.floor(sec / 60);
    const remainder = sec % 60;
    return `${mins}:${remainder < 10 ? '0' : ''}${remainder}`;
  };

  if (!supported) {
    return (
      <div className="relative inline-flex items-center">
        <button
          type="button"
          disabled
          aria-label="Voice input not supported in this browser"
          title="Speech recognition is not supported in this browser. Please type your prompt."
          className={`min-h-[44px] min-w-[44px] p-2.5 rounded-xl border border-sand-200 bg-sand-100/60 text-charcoal-400 cursor-not-allowed inline-flex items-center justify-center ${className}`}
        >
          <MicOff className="w-4 h-4" />
        </button>
      </div>
    );
  }

  return (
    <div className="relative inline-flex items-center gap-2">
      {voiceState === 'listening' ? (
        <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-xl bg-terracotta-50 border border-terracotta-300 shadow-sm animate-pulse">
          <span className="w-2.5 h-2.5 rounded-full bg-terracotta-600 animate-ping" />
          <span className="text-xs font-medium text-terracotta-900 font-mono">
            Listening {formatSeconds(recordingSeconds)}
          </span>
          <button
            type="button"
            onClick={handleStop}
            aria-label="Stop recording speech"
            className="min-h-[36px] min-w-[36px] p-1.5 rounded-lg bg-terracotta-600 hover:bg-terracotta-700 text-white transition-colors flex items-center justify-center"
            title="Stop recording"
          >
            <Square className="w-3.5 h-3.5 fill-current" />
          </button>
          <button
            type="button"
            onClick={handleCancel}
            aria-label="Cancel voice input"
            className="text-[11px] text-charcoal-500 hover:text-charcoal-800 underline px-1"
          >
            Cancel
          </button>
        </div>
      ) : voiceState === 'processing' ? (
        <div className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-sand-100 border border-sand-300 text-xs text-charcoal-700">
          <Loader2 className="w-3.5 h-3.5 animate-spin text-bronze-700" />
          <span>Processing audio...</span>
        </div>
      ) : (
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleStart}
            disabled={disabled}
            aria-label="Start voice dictation"
            title="Describe your design transformation using your microphone"
            className={`min-h-[44px] min-w-[44px] p-2.5 rounded-xl border border-sand-300 bg-white hover:bg-sand-50 hover:border-bronze-600 active:bg-sand-100 text-charcoal-700 hover:text-bronze-800 transition-all shadow-2xs flex items-center justify-center gap-1.5 disabled:opacity-50 disabled:cursor-not-allowed ${className}`}
          >
            <Mic className="w-4 h-4 text-bronze-700" />
            <span className="text-xs font-medium hidden sm:inline">Voice Dictate</span>
          </button>
          <span className="text-[11px] text-charcoal-500 hidden md:inline">
            Speak your design idea <span className="text-charcoal-400">(e.g. &ldquo;Change wardrobe shutters to walnut...&rdquo;)</span>
          </span>
        </div>
      )}

      {/* Screen reader polite live region */}
      <div className="sr-only" aria-live="polite">
        {voiceState === 'listening' ? 'Microphone is active. Speak your interior design prompt now.' : ''}
        {voiceState === 'processing' ? 'Processing speech to text.' : ''}
        {errorMsg ? `Speech recognition error: ${errorMsg}` : ''}
      </div>

      {/* Error Popup */}
      {errorMsg && (
        <div
          role="alert"
          className="absolute left-0 bottom-full mb-2 z-30 p-2.5 rounded-xl bg-white border border-terracotta-300 shadow-md text-xs text-terracotta-800 w-64 space-y-1"
        >
          <div className="flex items-center justify-between font-semibold">
            <span className="flex items-center gap-1">
              <AlertCircle className="w-3.5 h-3.5 text-terracotta-600" /> Voice Input Notice
            </span>
            <button
              type="button"
              onClick={() => setErrorMsg(null)}
              className="text-charcoal-400 hover:text-charcoal-700 text-xs px-1"
              aria-label="Close error notice"
            >
              ✕
            </button>
          </div>
          <p className="text-[11px] leading-relaxed text-charcoal-600">{errorMsg}</p>
        </div>
      )}
    </div>
  );
}
