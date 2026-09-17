'use client';

import React, { useRef, useState } from 'react';
import { UploadCloud, Image as ImageIcon, Camera, X } from 'lucide-react';
import { Button } from '@/components/ui/Button';

export interface FileDropzoneProps {
  label?: string;
  helperText?: string;
  accept?: string;
  multiple?: boolean;
  maxFiles?: number;
  maxSizeMb?: number;
  allowCamera?: boolean;
  onFilesSelected?: (files: File[]) => void;
  disabled?: boolean;
  className?: string;
}

export function FileDropzone({
  label = 'Upload Assets',
  helperText = 'Supported formats: JPG, PNG, WEBP up to 25MB',
  accept = 'image/jpeg,image/png,image/webp',
  multiple = true,
  maxFiles = 10,
  maxSizeMb = 25,
  allowCamera = true,
  onFilesSelected,
  disabled = false,
  className = '',
}: FileDropzoneProps) {
  const [isDragOver, setIsDragOver] = useState(false);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [error, setError] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);

  const handleFiles = (incomingFiles: FileList | File[]) => {
    setError(null);
    const newFiles: File[] = [];
    const maxBytes = maxSizeMb * 1024 * 1024;

    for (let i = 0; i < incomingFiles.length; i++) {
      const file = incomingFiles[i];
      if (file.size > maxBytes) {
        setError(`File "${file.name}" exceeds the ${maxSizeMb}MB limit.`);
        return;
      }
      newFiles.push(file);
    }

    const updated = multiple
      ? [...selectedFiles, ...newFiles].slice(0, maxFiles)
      : newFiles.slice(0, 1);

    setSelectedFiles(updated);
    if (onFilesSelected) {
      onFilesSelected(updated);
    }
  };

  const removeFile = (index: number) => {
    const updated = selectedFiles.filter((_, i) => i !== index);
    setSelectedFiles(updated);
    if (onFilesSelected) {
      onFilesSelected(updated);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    if (!disabled) setIsDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    if (disabled || !e.dataTransfer.files) return;
    handleFiles(e.dataTransfer.files);
  };

  return (
    <div className={`w-full ${className}`}>
      {label && (
        <label className="block text-xs font-semibold uppercase tracking-wider text-[var(--muted)] mb-2">
          {label}
        </label>
      )}

      {/* Hidden file inputs */}
      <input
        ref={fileInputRef}
        type="file"
        accept={accept}
        multiple={multiple}
        disabled={disabled}
        onChange={(e) => e.target.files && handleFiles(e.target.files)}
        className="hidden"
        id="file-dropzone-input"
        aria-label={label}
      />

      {allowCamera && (
        <input
          ref={cameraInputRef}
          type="file"
          accept="image/*"
          capture="environment"
          disabled={disabled}
          onChange={(e) => e.target.files && handleFiles(e.target.files)}
          className="hidden"
          id="file-dropzone-camera-input"
          aria-label="Camera Capture"
        />
      )}

      {/* Drop area */}
      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => !disabled && fileInputRef.current?.click()}
        className={`relative flex flex-col items-center justify-center p-6 sm:p-8 rounded-xl border-2 border-dashed transition-all cursor-pointer min-h-[160px] text-center ${
          isDragOver
            ? 'border-[var(--brand)] bg-[var(--brand-muted)]'
            : disabled
            ? 'border-[var(--border)] bg-[var(--surface-sunken)] opacity-60 cursor-not-allowed'
            : 'border-[var(--border)] bg-[var(--surface)] hover:border-[var(--border-strong)] hover:bg-[var(--surface-raised)]'
        }`}
      >
        <div className="w-12 h-12 rounded-full bg-[var(--surface-raised)] border border-[var(--border)] flex items-center justify-center text-[var(--brand)] mb-3 shadow-sm">
          <UploadCloud className="w-6 h-6" />
        </div>

        <div className="space-y-1">
          <p className="text-sm font-medium text-[var(--foreground)]">
            <span className="text-[var(--brand)] font-semibold">Tap to upload</span> or drag and drop
          </p>
          <p className="text-xs text-[var(--muted)]">{helperText}</p>
        </div>

        {/* Mobile Camera Option */}
        {allowCamera && (
          <div className="mt-4 sm:hidden flex items-center">
            <Button
              type="button"
              variant="outline"
              size="sm"
              leftIcon={<Camera className="w-4 h-4 text-[var(--brand)]" />}
              onClick={(e) => {
                e.stopPropagation();
                cameraInputRef.current?.click();
              }}
            >
              Take Site Photo
            </Button>
          </div>
        )}
      </div>

      {error && (
        <p className="mt-2 text-xs font-medium text-[var(--danger)]" role="alert">
          {error}
        </p>
      )}

      {/* File Previews */}
      {selectedFiles.length > 0 && (
        <div className="mt-4 space-y-2">
          <p className="text-xs font-semibold uppercase tracking-wider text-[var(--muted)]">
            Selected Files ({selectedFiles.length}/{maxFiles})
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            {selectedFiles.map((file, idx) => (
              <div
                key={`${file.name}-${idx}`}
                className="flex items-center justify-between p-2.5 rounded-lg border border-[var(--border)] bg-[var(--surface)] text-sm"
              >
                <div className="flex items-center space-x-2.5 overflow-hidden">
                  <div className="w-8 h-8 rounded bg-[var(--surface-raised)] border border-[var(--border)] flex items-center justify-center text-[var(--muted)] flex-shrink-0">
                    <ImageIcon className="w-4 h-4" />
                  </div>
                  <div className="truncate">
                    <p className="truncate text-xs font-medium text-[var(--foreground)]">{file.name}</p>
                    <p className="text-[10px] text-[var(--muted)]">
                      {(file.size / (1024 * 1024)).toFixed(2)} MB
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => removeFile(idx)}
                  className="p-1 rounded-md text-[var(--muted)] hover:text-[var(--foreground)] hover:bg-[var(--surface-raised)] transition-colors min-h-[32px] min-w-[32px] flex items-center justify-center"
                  aria-label={`Remove ${file.name}`}
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
