'use client';

import React, { useState, useEffect } from 'react';
import { X, DoorOpen, Check, Loader2 } from 'lucide-react';
import { RoomType, CANONICAL_ROOM_TYPES, ProjectRoomDto } from '@/lib/projects/types';

interface AddRoomModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (roomType: RoomType, displayName?: string) => Promise<void>;
  editingRoom?: ProjectRoomDto | null;
}

export function AddRoomModal({
  isOpen,
  onClose,
  onSave,
  editingRoom = null,
}: AddRoomModalProps) {
  const [roomType, setRoomType] = useState<RoomType>('LIVING_ROOM');
  const [displayName, setDisplayName] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      if (editingRoom) {
        setRoomType(editingRoom.roomType);
        setDisplayName(editingRoom.displayName);
      } else {
        setRoomType('LIVING_ROOM');
        setDisplayName('');
      }
      setError(null);
    }
  }, [isOpen, editingRoom]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      setError(null);
      await onSave(roomType, displayName.trim() || undefined);
      onClose();
    } catch (err: any) {
      setError(err?.message || 'Failed to save space');
    } finally {
      setSaving(false);
    }
  };

  if (!isOpen) return null;

  const activeOption = CANONICAL_ROOM_TYPES.find((opt) => opt.code === roomType);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="room-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-charcoal-950/70 backdrop-blur-sm animate-in fade-in duration-200"
    >
      <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl border border-sand-200 overflow-hidden flex flex-col">
        {/* Header */}
        <div className="px-6 py-4 border-b border-sand-100 flex items-center justify-between bg-sand-50/50">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-bronze-50 border border-bronze-200/60 text-bronze-700">
              <DoorOpen className="w-4 h-4" />
            </div>
            <div>
              <h3 id="room-modal-title" className="font-serif text-base font-semibold text-charcoal-900">
                {editingRoom ? 'Edit Space / Room' : 'Add Space / Room Group'}
              </h3>
              <p className="text-xs text-charcoal-500">
                Organize photos into architectural spaces and rooms.
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

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-xs">
              {error}
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-charcoal-800 mb-1">
              Space Archetype <span className="text-red-500">*</span>
            </label>
            <select
              value={roomType}
              onChange={(e) => {
                const nextType = e.target.value as RoomType;
                setRoomType(nextType);
                if (!displayName || !editingRoom) {
                  const found = CANONICAL_ROOM_TYPES.find((opt) => opt.code === nextType);
                  if (found) setDisplayName(found.label);
                }
              }}
              className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
            >
              {CANONICAL_ROOM_TYPES.map((opt) => (
                <option key={opt.code} value={opt.code}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="block text-xs font-semibold text-charcoal-800">
                Display Name (Customizable)
              </label>
              <span className="text-[11px] text-charcoal-400">
                e.g. Master Bedroom, Bedroom 2
              </span>
            </div>
            <input
              type="text"
              maxLength={128}
              value={displayName}
              placeholder={activeOption?.label || 'Space name'}
              onChange={(e) => setDisplayName(e.target.value)}
              className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 focus:outline-none focus:ring-1 focus:ring-bronze-700 bg-sand-50/40 text-charcoal-900"
            />
            <p className="text-[11px] text-charcoal-500 mt-1">
              Tip: You can create multiple rooms of the same type (e.g. Master Bedroom, Bedroom 1, Bedroom 2).
            </p>
          </div>

          {/* Footer */}
          <div className="pt-4 border-t border-sand-100 flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              disabled={saving}
              className="px-4 py-2 rounded-xl border border-sand-300 text-charcoal-700 hover:bg-sand-100 text-xs font-medium transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="inline-flex items-center gap-1.5 px-5 py-2 rounded-xl bg-charcoal-900 hover:bg-charcoal-800 text-white text-xs font-medium transition-colors shadow-sm disabled:opacity-50"
            >
              {saving ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
              ) : (
                <Check className="w-3.5 h-3.5 text-bronze-300" />
              )}
              <span>{saving ? 'Saving...' : editingRoom ? 'Update Space' : 'Create Space'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
