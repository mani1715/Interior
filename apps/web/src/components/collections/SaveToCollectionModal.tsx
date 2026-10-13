'use client';

import React, { useEffect, useState } from 'react';
import { Dialog } from '@/components/overlay/Dialog';
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
    <Dialog isOpen={isOpen} onClose={onClose} title="Save to Collection">
      <div className="space-y-4">
        {error && (
          <div className="p-2.5 text-xs text-red-600 bg-red-50 rounded-lg">
            {error}
          </div>
        )}

        {loading ? (
          <div className="py-8 flex justify-center">
            <div className="w-5 h-5 border-2 border-charcoal-900 border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          <div className="space-y-4">
            {/* Collection selection */}
            <div>
              <label className="block text-xs font-medium text-charcoal-700 mb-1.5">
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
                        ? 'border-charcoal-900 bg-sand-50 font-semibold text-charcoal-900'
                        : 'border-sand-200 hover:border-sand-300 text-charcoal-700'
                    }`}
                  >
                    <span>
                      {col.title} {col.isDefault && <span className="text-charcoal-400 font-normal">(Default)</span>}
                    </span>
                    <span className="text-charcoal-400 text-[11px]">
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
                className="text-xs text-bronze-700 font-medium hover:underline flex items-center gap-1"
              >
                + Create new collection
              </button>
            ) : (
              <form onSubmit={handleCreateCollection} className="space-y-2 p-3 bg-sand-50 rounded-lg border border-sand-200">
                <input
                  type="text"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="Collection name, e.g. Living Room Ideas"
                  className="w-full text-xs p-2 border border-sand-300 rounded focus:ring-1 focus:ring-charcoal-900"
                  autoFocus
                />
                <div className="flex items-center justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => setShowCreateNew(false)}
                    className="text-xs text-charcoal-500 hover:text-charcoal-700 px-2 py-1"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={isCreating || !newTitle.trim()}
                    className="text-xs bg-charcoal-900 text-white px-3 py-1 rounded hover:bg-charcoal-800 disabled:opacity-50"
                  >
                    {isCreating ? 'Creating...' : 'Create'}
                  </button>
                </div>
              </form>
            )}

            {/* Private Note (optional) */}
            <div>
              <label className="block text-xs font-medium text-charcoal-700 mb-1">
                Private Note (optional)
              </label>
              <textarea
                rows={2}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Why you saved this, design elements you love..."
                className="w-full text-xs p-2 border border-sand-300 rounded-lg focus:ring-1 focus:ring-charcoal-900"
              />
              <p className="text-[10px] text-charcoal-400 mt-0.5">
                Private to you. Designers and studios never see your notes or collections.
              </p>
            </div>

            {/* Actions */}
            <div className="flex items-center justify-end gap-2 pt-2 border-t border-sand-100">
              <button
                type="button"
                onClick={onClose}
                className="px-3 py-1.5 text-xs text-charcoal-600 hover:text-charcoal-800"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSave}
                disabled={isSaving || !selectedCollectionId}
                className="px-4 py-1.5 text-xs font-medium text-white bg-charcoal-900 hover:bg-charcoal-800 rounded-lg disabled:opacity-50"
              >
                {isSaving ? 'Saving...' : 'Save to Collection'}
              </button>
            </div>
          </div>
        )}
      </div>
    </Dialog>
  );
}
