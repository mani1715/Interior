'use client';

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { X, RotateCcw, Check, Crosshair, Smartphone, Monitor } from 'lucide-react';

interface FocalPointModalProps {
  isOpen: boolean;
  onClose: () => void;
  imageUrl: string;
  initialX?: number | null;
  initialY?: number | null;
  onSave: (focalX: number, focalY: number) => void;
}

export function FocalPointModal({
  isOpen,
  onClose,
  imageUrl,
  initialX = 50,
  initialY = 50,
  onSave,
}: FocalPointModalProps) {
  const [focalX, setFocalX] = useState<number>(initialX ?? 50);
  const [focalY, setFocalY] = useState<number>(initialY ?? 50);
  const [isDragging, setIsDragging] = useState(false);
  const imageContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isOpen) {
      setFocalX(initialX ?? 50);
      setFocalY(initialY ?? 50);
    }
  }, [isOpen, initialX, initialY]);

  const updateCoordinatesFromPointer = useCallback((clientX: number, clientY: number) => {
    if (!imageContainerRef.current) return;
    const rect = imageContainerRef.current.getBoundingClientRect();
    if (rect.width === 0 || rect.height === 0) return;

    let x = ((clientX - rect.left) / rect.width) * 100;
    let y = ((clientY - rect.top) / rect.height) * 100;

    x = Math.max(0, Math.min(100, Math.round(x * 10) / 10));
    y = Math.max(0, Math.min(100, Math.round(y * 10) / 10));

    setFocalX(x);
    setFocalY(y);
  }, []);

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    setIsDragging(true);
    e.currentTarget.setPointerCapture(e.pointerId);
    updateCoordinatesFromPointer(e.clientX, e.clientY);
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!isDragging) return;
    updateCoordinatesFromPointer(e.clientX, e.clientY);
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    if (isDragging) {
      setIsDragging(false);
      try {
        e.currentTarget.releasePointerCapture(e.pointerId);
      } catch {
        // pointer capture release fallback
      }
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    const step = e.shiftKey ? 5 : 1;
    let handled = true;

    if (e.key === 'ArrowLeft') {
      setFocalX((prev) => Math.max(0, Math.round((prev - step) * 10) / 10));
    } else if (e.key === 'ArrowRight') {
      setFocalX((prev) => Math.min(100, Math.round((prev + step) * 10) / 10));
    } else if (e.key === 'ArrowUp') {
      setFocalY((prev) => Math.max(0, Math.round((prev - step) * 10) / 10));
    } else if (e.key === 'ArrowDown') {
      setFocalY((prev) => Math.min(100, Math.round((prev + step) * 10) / 10));
    } else if (e.key === 'Escape') {
      onClose();
    } else {
      handled = false;
    }

    if (handled) {
      e.preventDefault();
    }
  };

  const handleReset = () => {
    setFocalX(50);
    setFocalY(50);
  };

  const handleSave = () => {
    onSave(focalX, focalY);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="focal-point-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-charcoal-950/70 backdrop-blur-sm animate-in fade-in duration-200"
      onKeyDown={handleKeyDown}
      tabIndex={-1}
    >
      <div className="bg-white rounded-2xl max-w-4xl w-full shadow-2xl border border-sand-200 overflow-hidden flex flex-col max-h-[92vh]">
        {/* Header */}
        <div className="px-6 py-4 border-b border-sand-100 flex items-center justify-between bg-sand-50/50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-bronze-50 border border-bronze-200/60 text-bronze-700">
              <Crosshair className="w-4 h-4" />
            </div>
            <div>
              <h3 id="focal-point-title" className="font-serif text-base font-semibold text-charcoal-900">
                Adjust Focal Point
              </h3>
              <p className="text-xs text-charcoal-500">
                Click or drag the reticle to anchor the primary subject across widescreen and mobile crops.
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-charcoal-400 hover:text-charcoal-700 hover:bg-sand-100 rounded-lg transition-colors"
            aria-label="Close dialog"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            {/* Primary Interactive Reticle Canvas (8 cols on desktop) */}
            <div className="lg:col-span-7 flex flex-col items-center">
              <div
                ref={imageContainerRef}
                onPointerDown={handlePointerDown}
                onPointerMove={handlePointerMove}
                onPointerUp={handlePointerUp}
                tabIndex={0}
                aria-label="Interactive image focal point canvas. Use arrow keys or click to reposition."
                className="relative select-none cursor-crosshair rounded-xl overflow-hidden shadow-inner border border-sand-300 bg-charcoal-900 max-h-[50vh] flex items-center justify-center group focus:outline-none focus:ring-2 focus:ring-bronze-500"
              >
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={imageUrl}
                  alt="Focal point calibration target"
                  className="max-h-[50vh] w-auto max-w-full object-contain pointer-events-none block"
                  draggable={false}
                />

                {/* Target Reticle Indicator */}
                <div
                  className="absolute pointer-events-none -translate-x-1/2 -translate-y-1/2 transition-transform duration-75"
                  style={{
                    left: `${focalX}%`,
                    top: `${focalY}%`,
                  }}
                >
                  <div className="relative flex items-center justify-center">
                    {/* Outer Ring */}
                    <div className="w-10 h-10 rounded-full border-2 border-white shadow-[0_0_8px_rgba(0,0,0,0.6)] animate-pulse" />
                    {/* Inner Target Cross */}
                    <div className="absolute w-2.5 h-2.5 rounded-full bg-bronze-500 border border-white" />
                    {/* Subtle Crosshairs */}
                    <div className="absolute -left-3 w-2.5 h-[1.5px] bg-white shadow-sm" />
                    <div className="absolute -right-3 w-2.5 h-[1.5px] bg-white shadow-sm" />
                    <div className="absolute -top-3 h-2.5 w-[1.5px] bg-white shadow-sm" />
                    <div className="absolute -bottom-3 h-2.5 w-[1.5px] bg-white shadow-sm" />
                  </div>
                </div>
              </div>

              {/* Controls bar beneath canvas */}
              <div className="w-full mt-3 flex items-center justify-between text-xs text-charcoal-600 px-1">
                <span className="font-mono text-[11px] text-charcoal-500">
                  Focal: {focalX.toFixed(0)}% × {focalY.toFixed(0)}%
                </span>
                <button
                  type="button"
                  onClick={handleReset}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-sand-300 hover:bg-sand-100 text-charcoal-700 transition-colors text-xs font-medium"
                >
                  <RotateCcw className="w-3.5 h-3.5 text-charcoal-500" />
                  <span>Reset Center</span>
                </button>
              </div>
            </div>

            {/* Live Cropping Preview Column (5 cols on desktop) */}
            <div className="lg:col-span-5 space-y-4">
              <h4 className="text-xs font-semibold text-charcoal-800 tracking-wide uppercase">
                Real-Time Aspect Crop Previews
              </h4>

              {/* Desktop 16:9 Landscape Card */}
              <div className="bg-sand-50/70 rounded-xl p-3 border border-sand-200 space-y-2">
                <div className="flex items-center gap-1.5 text-xs text-charcoal-700 font-medium">
                  <Monitor className="w-3.5 h-3.5 text-bronze-600" />
                  <span>Landscape Banner (16:9)</span>
                </div>
                <div className="w-full aspect-video rounded-lg overflow-hidden bg-charcoal-800 border border-sand-300 relative shadow-sm">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={imageUrl}
                    alt="Desktop preview"
                    className="w-full h-full object-cover transition-all duration-150"
                    style={{
                      objectPosition: `${focalX}% ${focalY}%`,
                    }}
                  />
                </div>
              </div>

              {/* Mobile 4:5 Portrait Card */}
              <div className="bg-sand-50/70 rounded-xl p-3 border border-sand-200 space-y-2">
                <div className="flex items-center gap-1.5 text-xs text-charcoal-700 font-medium">
                  <Smartphone className="w-3.5 h-3.5 text-bronze-600" />
                  <span>Mobile Portrait (4:5)</span>
                </div>
                <div className="w-28 mx-auto aspect-[4/5] rounded-lg overflow-hidden bg-charcoal-800 border border-sand-300 relative shadow-sm">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={imageUrl}
                    alt="Mobile preview"
                    className="w-full h-full object-cover transition-all duration-150"
                    style={{
                      objectPosition: `${focalX}% ${focalY}%`,
                    }}
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="px-6 py-3.5 border-t border-sand-200 bg-sand-50/60 flex items-center justify-between">
          <div className="text-[11px] text-charcoal-500 hidden sm:block">
            Tip: Use arrow keys to fine-tune (Shift + arrow for 5% jumps)
          </div>
          <div className="flex items-center gap-2 ml-auto">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-sand-300 text-charcoal-700 hover:bg-sand-100 text-xs font-medium transition-colors"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleSave}
              className="inline-flex items-center gap-1.5 px-5 py-2 rounded-xl bg-charcoal-900 hover:bg-charcoal-800 text-white text-xs font-medium transition-colors shadow-sm"
            >
              <Check className="w-3.5 h-3.5 text-bronze-300" />
              <span>Apply Focal Point</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
