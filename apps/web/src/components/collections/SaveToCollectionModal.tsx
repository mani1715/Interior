'use client';

import React, { useEffect, useState } from 'react';
import { UserCollectionDto } from '@/lib/collections/types';
import {
  createCollection,
  listCollections,
  saveProjectToCollection,
} from '@/lib/collections/api';

interface SaveToCollectionModalProps {
  projectId: string;
  isOpen: boolean;
  onClose: () => void;
  onSaved?: (collectionTitle: string) => void;
}

export function SaveToCollectionModal({
  projectId,
  isOpen,
  onClose,
  onSaved,
}: SaveToCollectionModalProps) {
  const [collections, setCollections] = useState<UserCollectionDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCollectionId, setSelectedCollectionId] = useState<string>('');
  const [note, setNote] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // New collection inline creation
  const [showCreateNew, setShowCreateNew] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [isCreating, setIsCreating] = useState(false);

  useEffect(() => {
    if (!isOpen) return;

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const list = await listCollections();
        setCollections(list);
        if (list.length > 0) {
          const defaultCol = list.find((c) => c.isDefault) || list[0];
          setSelectedCollectionId(defaultCol.id);
        }
      } catch {
        setError('Failed to load collections. Please sign in to save.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSave = async () => {
    if (!selectedCollectionId) return;
    setIsSaving(true);
    setError(null);
    try {
      await saveProjectToCollection({
        projectId,
        collectionId: selectedCollectionId,
        note: note.trim() || null,
      });
      const col = collections.find((c) => c.id === selectedCollectionId);
      onSaved?.(col?.title || 'Saved');
      onClose();
    } catch {
      setError('Unable to save project. Please try again.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleCreateCollection = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;
    setIsCreating(true);
    setError(null);
    try {
      const created = await createCollection({ title: newTitle.trim() });
      setCollections((prev) => [created, ...prev]);
      setSelectedCollectionId(created.id);
      setShowCreateNew(false);
      setNewTitle('');
    } catch {
      setError('Failed to create collection.');
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs">
      <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl space-y-4">
        <div className="flex items-center justify-between border-b border-zinc-100 pb-3">
          <h3 className="font-semibold text-zinc-900 text-base">Save to Collection</h3>
          <button
            type="button"
            onClick={onClose}
            className="text-zinc-400 hover:text-zinc-600 transition-colors"
          >
            ✕
          </button>
        </div>

        {error && (
          <div className="p-2.5 text-xs text-red-600 bg-red-50 rounded-lg">
            {error}
          </div>
        )}

        {loading ? (
          <div className="py-8 flex justify-center">
            <div className="w-5 h-5 border-2 border-zinc-900 border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          <div className="space-y-4">
            {/* Collection selection */}
            <div>
              <label className="block text-xs font-medium text-zinc-700 mb-1.5">
                Select Mood Board / Collection
              </label>
              <div className="space-y-1 max-h-44 overflow-y-auto pr-1">
                {collections.map((col) => (
                  <button
                    key={col.id}
                    type="button"
                    onClick={() => setSelectedCollectionId(col.id)}
                    className={`w-full text-left p-2.5 rounded-lg border text-xs flex items-center justify-between transition-colors ${
                      selectedCollectionId === col.id
                        ? 'border-zinc-900 bg-zinc-50 font-semibold text-zinc-900'
                        : 'border-zinc-200 hover:border-zinc-300 text-zinc-700'
                    }`}
                  >
                    <span>
                      {col.title} {col.isDefault && <span className="text-zinc-400 font-normal">(Default)</span>}
                    </span>
                    <span className="text-zinc-400 text-[11px]">
                      {col.itemCount} {col.itemCount === 1 ? 'item' : 'items'}
                    </span>
                  </button>
                ))}
              </div>
            </div>

            {/* Inline Create New Toggle */}
            {!showCreateNew ? (
              <button
                type="button"
                onClick={() => setShowCreateNew(true)}
                className="text-xs text-emerald-700 font-medium hover:underline flex items-center gap-1"
              >
                + Create new collection
              </button>
            ) : (
              <form onSubmit={handleCreateCollection} className="space-y-2 p-3 bg-zinc-50 rounded-lg border border-zinc-200">
                <input
                  type="text"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="Collection name, e.g. Living Room Ideas"
                  className="w-full text-xs p-2 border border-zinc-300 rounded focus:ring-1 focus:ring-zinc-900"
                  autoFocus
                />
                <div className="flex items-center justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => setShowCreateNew(false)}
                    className="text-xs text-zinc-500 hover:text-zinc-700 px-2 py-1"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isCreating || !newTitle.trim()}
                    className="text-xs bg-zinc-900 text-white px-3 py-1 rounded hover:bg-zinc-800 disabled:opacity-50"
                  >
                    {isCreating ? 'Creating...' : 'Create'}
                  </button>
                </div>
              </form>
            )}

            {/* Private Note (optional) */}
            <div>
              <label className="block text-xs font-medium text-zinc-700 mb-1">
                Private Note (optional)
              </label>
              <textarea
                rows={2}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Why you saved this, design elements you love..."
                className="w-full text-xs p-2 border border-zinc-300 rounded-lg focus:ring-1 focus:ring-zinc-900"
              />
              <p className="text-[10px] text-zinc-400 mt-0.5">
                Private to you. Designers and studios never see your notes or collections.
              </p>
            </div>

            {/* Actions */}
            <div className="flex items-center justify-end gap-2 pt-2 border-t border-zinc-100">
              <button
                type="button"
                onClick={onClose}
                className="px-3 py-1.5 text-xs text-zinc-600 hover:text-zinc-800"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSave}
                disabled={isSaving || !selectedCollectionId}
                className="px-4 py-1.5 text-xs font-medium text-white bg-zinc-900 hover:bg-zinc-800 rounded-lg disabled:opacity-50"
              >
                {isSaving ? 'Saving...' : 'Save to Collection'}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
