'use client';

import React, { useRef, useState, useEffect, useCallback } from 'react';
import {
  scaleCanvasToSource,
  calculateCoverageRatio,
  Point,
} from '@/lib/ai/coordinates';

export interface PrecisionMaskEditorProps {
  sourceImageUrl: string;
  sourceWidth?: number;
  sourceHeight?: number;
  onMaskChange?: (hasMask: boolean, coverageRatio: number) => void;
  onExportReady?: (exportFn: () => Promise<Blob | null>) => void;
  disabled?: boolean;
}

export type BrushSize = 'small' | 'medium' | 'large';

const BRUSH_SIZES: Record<BrushSize, { label: string; px: number }> = {
  small: { label: 'Small', px: 12 },
  medium: { label: 'Medium', px: 24 },
  large: { label: 'Large', px: 48 },
};

const BRONZE_COLOR = 'rgba(184, 138, 90, 0.65)';

export function PrecisionMaskEditor({
  sourceImageUrl,
  sourceWidth = 1024,
  sourceHeight = 1024,
  onMaskChange,
  onExportReady,
  disabled = false,
}: PrecisionMaskEditorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const imageRef = useRef<HTMLImageElement>(null);

  const [tool, setTool] = useState<'brush' | 'eraser'>('brush');
  const [brushSize, setBrushSize] = useState<BrushSize>('medium');
  const [isDrawing, setIsDrawing] = useState(false);
  const [lastPoint, setLastPoint] = useState<Point | null>(null);
  const [undoStack, setUndoStack] = useState<ImageData[]>([]);
  const [coverageRatio, setCoverageRatio] = useState<number>(0);
  const [statusMessage, setStatusMessage] = useState<string>('Ready to select region.');

  // Initialize canvas size matching image display container
  const updateCanvasDimensions = useCallback(() => {
    const canvas = canvasRef.current;
    const img = imageRef.current;
    if (!canvas || !img) return;

    const rect = img.getBoundingClientRect();
    if (rect.width > 0 && rect.height > 0) {
      // Save current content before resize
      const prevWidth = canvas.width;
      const prevHeight = canvas.height;
      let prevImgData: ImageData | null = null;
      if (prevWidth > 0 && prevHeight > 0) {
        const ctx = canvas.getContext('2d');
        if (ctx) {
          try {
            prevImgData = ctx.getImageData(0, 0, prevWidth, prevHeight);
          } catch {
            // ignore
          }
        }
      }

      canvas.width = rect.width;
      canvas.height = rect.height;

      // Restore if same dimensions or redraw
      const ctx = canvas.getContext('2d');
      if (ctx && prevImgData && prevWidth === rect.width && prevHeight === rect.height) {
        ctx.putImageData(prevImgData, 0, 0);
      }
    }
  }, []);

  useEffect(() => {
    const img = imageRef.current;
    if (!img) return;

    if (img.complete) {
      updateCanvasDimensions();
    } else {
      img.onload = () => updateCanvasDimensions();
    }

    window.addEventListener('resize', updateCanvasDimensions);
    return () => window.removeEventListener('resize', updateCanvasDimensions);
  }, [updateCanvasDimensions, sourceImageUrl]);

  const saveUndoState = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    try {
      const state = ctx.getImageData(0, 0, canvas.width, canvas.height);
      setUndoStack((prev) => {
        const next = [...prev, state];
        if (next.length > 20) next.shift(); // Bound history to 20 states
        return next;
      });
    } catch {
      // ignore
    }
  }, []);

  const handleUndo = useCallback(() => {
    if (undoStack.length === 0) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const nextStack = [...undoStack];
    const previousState = nextStack.pop();
    setUndoStack(nextStack);

    if (previousState) {
      ctx.putImageData(previousState, 0, 0);
    } else {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
    }

    const ratio = calculateCoverageRatio(canvas);
    setCoverageRatio(ratio);
    onMaskChange?.(ratio > 0, ratio);
    setStatusMessage(`Undo performed. ${(ratio * 100).toFixed(1)}% area selected.`);
  }, [undoStack, onMaskChange]);

  const handleClear = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    saveUndoState();
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    setCoverageRatio(0);
    onMaskChange?.(false, 0);
    setStatusMessage('Selection cleared.');
  }, [saveUndoState, onMaskChange]);

  const getCanvasCoords = (e: React.PointerEvent<HTMLCanvasElement>): Point => {
    const canvas = canvasRef.current;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    return {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
    };
  };

  const drawLine = useCallback(
    (start: Point, end: Point) => {
      const canvas = canvasRef.current;
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      const size = BRUSH_SIZES[brushSize].px;

      ctx.lineWidth = size;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';

      if (tool === 'eraser') {
        ctx.globalCompositeOperation = 'destination-out';
        ctx.strokeStyle = 'rgba(0,0,0,1)';
      } else {
        ctx.globalCompositeOperation = 'source-over';
        ctx.strokeStyle = BRONZE_COLOR;
      }

      ctx.beginPath();
      ctx.moveTo(start.x, start.y);
      ctx.lineTo(end.x, end.y);
      ctx.stroke();
    },
    [tool, brushSize]
  );

  const handlePointerDown = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (disabled) return;
    e.currentTarget.setPointerCapture(e.pointerId);
    saveUndoState();

    const pt = getCanvasCoords(e);
    setIsDrawing(true);
    setLastPoint(pt);

    // Draw single dot at click position
    drawLine(pt, pt);
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!isDrawing || disabled) return;
    const currentPoint = getCanvasCoords(e);
    if (lastPoint) {
      drawLine(lastPoint, currentPoint);
    }
    setLastPoint(currentPoint);
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!isDrawing) return;
    try {
      e.currentTarget.releasePointerCapture(e.pointerId);
    } catch {
      // pointer capture might have already been lost
    }
    setIsDrawing(false);
    setLastPoint(null);

    const canvas = canvasRef.current;
    if (canvas) {
      const ratio = calculateCoverageRatio(canvas);
      setCoverageRatio(ratio);
      onMaskChange?.(ratio > 0, ratio);
      setStatusMessage(`Region updated. ${(ratio * 100).toFixed(1)}% area selected.`);
    }
  };

  const exportMaskBlob = useCallback(async (): Promise<Blob | null> => {
    const canvas = canvasRef.current;
    const img = imageRef.current;
    if (!canvas) return null;

    const actualSourceWidth = img?.naturalWidth || sourceWidth;
    const actualSourceHeight = img?.naturalHeight || sourceHeight;

    const scaled = scaleCanvasToSource(canvas, actualSourceWidth, actualSourceHeight);

    return new Promise((resolve) => {
      scaled.toBlob((blob) => {
        resolve(blob);
      }, 'image/png');
    });
  }, [sourceWidth, sourceHeight]);

  useEffect(() => {
    if (onExportReady) {
      onExportReady(exportMaskBlob);
    }
  }, [onExportReady, exportMaskBlob]);

  const coveragePercent = Math.round(coverageRatio * 100);

  return (
    <div className="flex flex-col gap-3 w-full" ref={containerRef}>
      {/* Screen Reader Live Announcements */}
      <div role="status" aria-live="polite" className="sr-only">
        {statusMessage}
      </div>

      {/* Mask Editor Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-2 p-2.5 rounded-lg border border-[#E7E1D8] bg-[#FAF8F5]">
        {/* Tool Mode: Brush vs Eraser */}
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => {
              setTool('brush');
              setStatusMessage('Brush tool selected');
            }}
            disabled={disabled}
            aria-label="Brush tool: Paint area to edit"
            aria-pressed={tool === 'brush'}
            className={`min-h-[44px] min-w-[44px] px-3 py-2 rounded font-medium text-xs flex items-center gap-1.5 transition-colors ${
              tool === 'brush'
                ? 'bg-[#B88A5A] text-white shadow-sm'
                : 'bg-white text-[#1F1F1F] border border-[#E7E1D8] hover:bg-[#F3EFEA]'
            }`}
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"
              />
            </svg>
            <span>Brush</span>
          </button>

          <button
            type="button"
            onClick={() => {
              setTool('eraser');
              setStatusMessage('Eraser tool selected');
            }}
            disabled={disabled}
            aria-label="Eraser tool: Remove painted area"
            aria-pressed={tool === 'eraser'}
            className={`min-h-[44px] min-w-[44px] px-3 py-2 rounded font-medium text-xs flex items-center gap-1.5 transition-colors ${
              tool === 'eraser'
                ? 'bg-[#B88A5A] text-white shadow-sm'
                : 'bg-white text-[#1F1F1F] border border-[#E7E1D8] hover:bg-[#F3EFEA]'
            }`}
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
              />
            </svg>
            <span>Eraser</span>
          </button>
        </div>

        {/* Brush Sizes */}
        <div className="flex items-center gap-1">
          <span className="text-xs text-[#1F1F1F]/70 mr-1 hidden sm:inline">Size:</span>
          {(['small', 'medium', 'large'] as BrushSize[]).map((size) => (
            <button
              key={size}
              type="button"
              onClick={() => {
                setBrushSize(size);
                setStatusMessage(`${BRUSH_SIZES[size].label} brush size selected`);
              }}
              disabled={disabled}
              aria-label={`${BRUSH_SIZES[size].label} brush size (${BRUSH_SIZES[size].px} pixels)`}
              aria-pressed={brushSize === size}
              className={`min-h-[44px] min-w-[44px] px-2.5 py-1.5 rounded text-xs font-medium transition-colors ${
                brushSize === size
                  ? 'bg-[#1F1F1F] text-white'
                  : 'bg-white text-[#1F1F1F] border border-[#E7E1D8] hover:bg-[#F3EFEA]'
              }`}
            >
              {BRUSH_SIZES[size].label}
            </button>
          ))}
        </div>

        {/* Action Controls: Undo & Clear */}
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={handleUndo}
            disabled={disabled || undoStack.length === 0}
            aria-label="Undo last brush stroke"
            className="min-h-[44px] min-w-[44px] px-3 py-2 rounded bg-white text-[#1F1F1F] border border-[#E7E1D8] hover:bg-[#F3EFEA] disabled:opacity-40 disabled:cursor-not-allowed text-xs font-medium flex items-center gap-1 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a5 5 0 015 5v2M3 10l6 6m-6-6l6-6" />
            </svg>
            <span className="hidden sm:inline">Undo</span>
          </button>

          <button
            type="button"
            onClick={handleClear}
            disabled={disabled || coverageRatio === 0}
            aria-label="Clear painted selection"
            className="min-h-[44px] min-w-[44px] px-3 py-2 rounded bg-white text-red-700 border border-red-200 hover:bg-red-50 disabled:opacity-40 disabled:cursor-not-allowed text-xs font-medium transition-colors"
          >
            Clear
          </button>
        </div>
      </div>

      {/* Canvas Drawing Surface & Image Overlay */}
      <div className="relative overflow-hidden rounded-xl border border-[#E7E1D8] bg-[#1F1F1F] select-none flex items-center justify-center">
        {/* Source Image */}
        <img
          ref={imageRef}
          src={sourceImageUrl}
          alt="Source room image for precision editing selection"
          className="block max-h-[600px] w-auto max-w-full object-contain pointer-events-none"
        />

        {/* Precision Mask Drawing Canvas */}
        <canvas
          ref={canvasRef}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
          style={{ touchAction: 'none' }}
          className={`absolute inset-0 w-full h-full cursor-crosshair ${
            disabled ? 'pointer-events-none opacity-80' : ''
          }`}
          aria-label="Canvas to highlight specific region in the room image"
        />

        {/* Live Selection Coverage Badge */}
        <div className="absolute bottom-3 right-3 bg-[#1F1F1F]/90 backdrop-blur-md text-white text-[11px] font-medium px-2.5 py-1.5 rounded-full border border-white/20 shadow-sm flex items-center gap-1.5 pointer-events-none">
          <span
            className="inline-block w-2.5 h-2.5 rounded-full"
            style={{ backgroundColor: coveragePercent > 0 ? '#B88A5A' : '#6B7280' }}
          />
          <span>
            {coveragePercent > 0
              ? `${coveragePercent}% room area selected`
              : 'Paint over shutters, walls, or furniture'}
          </span>
        </div>
      </div>

      {/* Truthful Disclaimer Bar */}
      <div className="p-3 rounded-lg bg-[#FAF8F5] border border-[#E7E1D8] flex items-start gap-2.5 text-xs text-[#1F1F1F]/85">
        <svg
          className="w-4 h-4 text-[#B88A5A] flex-shrink-0 mt-0.5"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
        <div>
          <span className="font-medium text-[#1F1F1F]">Precision Edit Mode: </span>
          <span>
            AI will attempt to modify only the selected area. Minor changes outside the selection may occur.
          </span>
        </div>
      </div>
    </div>
  );
}
