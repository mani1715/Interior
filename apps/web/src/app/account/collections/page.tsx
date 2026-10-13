'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth/auth-context';
import { CollectionDetailDto, UserCollectionDto } from '@/lib/collections/types';
import {
  createCollection,
  deleteCollection,
  getCollectionDetail,
  listCollections,
  removeCollectionItem,
  updateCollection,
  updateCollectionItemNote,
} from '@/lib/collections/api';

export default function CollectionsPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading: authLoading } = useAuth();

  const [collections, setCollections] = useState<UserCollectionDto[]>([]);
  const [selectedCollectionId, setSelectedCollectionId] = useState<string | null>(null);
  const [collectionDetail, setCollectionDetail] = useState<CollectionDetailDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Create Collection Modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [isCreating, setIsCreating] = useState(false);

  // Edit Collection Metadata
  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');

  // Edit Item Note
  const [editingItemId, setEditingItemId] = useState<string | null>(null);
  const [itemNoteText, setItemNoteText] = useState('');

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push('/sign-in?returnUrl=/account/collections');
    }
  }, [authLoading, isAuthenticated, router]);

  const fetchCollections = async () => {
    try {
      setLoading(true);
      setError(null);
      const list = await listCollections();
      setCollections(list);
      if (list.length > 0 && !selectedCollectionId) {
        const defaultCol = list.find((c) => c.isDefault) || list[0];
        setSelectedCollectionId(defaultCol.id);
      }
    } catch {
      setError('Unable to load your collections.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      fetchCollections();
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (!selectedCollectionId) return;
    const fetchDetail = async () => {
      try {
        setDetailLoading(true);
        const detail = await getCollectionDetail(selectedCollectionId);
        setCollectionDetail(detail);
        setEditTitle(detail.collection.title);
        setEditDescription(detail.collection.description || '');
      } catch {
        setError('Failed to load collection items.');
      } finally {
        setDetailLoading(false);
      }
    };
    fetchDetail();
  }, [selectedCollectionId]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;
    setIsCreating(true);
    try {
      const created = await createCollection({
        title: newTitle.trim(),
        description: newDescription.trim() || null,
      });
      setCollections((prev) => [created, ...prev]);
      setSelectedCollectionId(created.id);
      setShowCreateModal(false);
      setNewTitle('');
      setNewDescription('');
    } catch {
      setError('Could not create collection.');
    } finally {
      setIsCreating(false);
    }
  };

  const handleUpdateCollection = async () => {
    if (!selectedCollectionId || !editTitle.trim()) return;
    try {
      const updated = await updateCollection(selectedCollectionId, {
        title: editTitle.trim(),
        description: editDescription.trim() || null,
      });
      setCollections((prev) =>
        prev.map((c) => (c.id === updated.id ? { ...c, title: updated.title, description: updated.description } : c))
      );
      if (collectionDetail) {
        setCollectionDetail({
          ...collectionDetail,
          collection: updated,
        });
      }
      setIsEditingTitle(false);
    } catch {
      setError('Failed to update collection details.');
    }
  };

  const handleDeleteCollection = async (collectionId: string) => {
    if (!confirm('Are you sure you want to delete this collection and its saved items?')) return;
    try {
      await deleteCollection(collectionId);
      const remaining = collections.filter((c) => c.id !== collectionId);
      setCollections(remaining);
      if (selectedCollectionId === collectionId) {
        setSelectedCollectionId(remaining.length > 0 ? remaining[0].id : null);
      }
    } catch {
      setError('Could not delete collection.');
    }
  };

  const handleRemoveItem = async (itemId: string) => {
    try {
      await removeCollectionItem(itemId);
      if (collectionDetail) {
        setCollectionDetail({
          ...collectionDetail,
          items: collectionDetail.items.filter((i) => i.id !== itemId),
        });
      }
      setCollections((prev) =>
        prev.map((c) =>
          c.id === selectedCollectionId ? { ...c, itemCount: Math.max(0, c.itemCount - 1) } : c
        )
      );
    } catch {
      setError('Unable to remove item from collection.');
    }
  };

  const handleSaveNote = async (itemId: string) => {
    try {
      await updateCollectionItemNote(itemId, { note: itemNoteText.trim() || null });
      if (collectionDetail) {
        setCollectionDetail({
          ...collectionDetail,
          items: collectionDetail.items.map((i) =>
            i.id === itemId ? { ...i, note: itemNoteText.trim() || null } : i
          ),
        });
      }
      setEditingItemId(null);
    } catch {
      setError('Failed to update note.');
    }
  };

  if (authLoading || loading) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-sand-200 border-t-bronze-600 rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
        <div>
          <span className="text-xs font-semibold tracking-widest uppercase text-bronze-700 block mb-1">
            Private Mood Boards
          </span>
          <h1 className="text-3xl font-extrabold text-charcoal-900 tracking-tight">
            Inspiration Collections
          </h1>
          <p className="text-sm text-charcoal-500 mt-1">
            Curate design inspiration for your home. Completely private to you—designers and studios never see your saved items.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center gap-2 px-4 py-2 text-xs font-semibold text-white bg-charcoal-900 rounded-lg hover:bg-charcoal-800 transition-colors shadow-xs"
        >
          + New Collection
        </button>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-50 text-red-700 rounded-xl text-xs flex items-center justify-between">
          <span>{error}</span>
          <button type="button" onClick={() => setError(null)} className="text-red-500 hover:text-red-700 font-bold">
            ✕
          </button>
        </div>
      )}

      {/* Main Grid: Collections Sidebar + Items Canvas */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Collections List */}
        <div className="lg:col-span-1 space-y-2">
          <h2 className="text-xs font-semibold text-charcoal-400 uppercase tracking-wider px-2">
            Your Boards
          </h2>
          <div className="space-y-1">
            {collections.map((col) => (
              <div
                key={col.id}
                onClick={() => setSelectedCollectionId(col.id)}
                className={`group p-3 rounded-xl border text-xs cursor-pointer flex items-center justify-between transition-all ${
                  selectedCollectionId === col.id
                    ? 'border-bronze-700 bg-white font-semibold text-charcoal-900 shadow-xs'
                    : 'border-transparent hover:bg-sand-100 text-charcoal-600'
                }`}
              >
                <div className="min-w-0 pr-2">
                  <p className="truncate">
                    {col.title} {col.isDefault && <span className="text-charcoal-400 font-normal">(Default)</span>}
                  </p>
                  <p className="text-[11px] text-charcoal-400 font-normal mt-0.5">
                    {col.itemCount} {col.itemCount === 1 ? 'project' : 'projects'}
                  </p>
                </div>
                {!col.isDefault && (
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeleteCollection(col.id);
                    }}
                    title="Delete collection"
                    className="opacity-0 group-hover:opacity-100 text-charcoal-400 hover:text-red-600 transition-opacity p-1"
                  >
                    🗑
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Collection Content Canvas */}
        <div className="lg:col-span-3">
          {detailLoading ? (
            <div className="py-20 flex justify-center">
              <div className="w-8 h-8 border-2 border-bronze-700 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : collectionDetail ? (
            <div className="space-y-6">
              {/* Collection Header & Metadata */}
              <div className="p-6 bg-white rounded-2xl border border-sand-200 shadow-xs space-y-3">
                {!isEditingTitle ? (
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <h2 className="text-xl font-bold text-charcoal-900">
                        {collectionDetail.collection.title}
                      </h2>
                      {collectionDetail.collection.description && (
                        <p className="text-xs text-charcoal-600 mt-1">
                          {collectionDetail.collection.description}
                        </p>
                      )}
                    </div>
                    {!collectionDetail.collection.isDefault && (
                      <button
                        type="button"
                        onClick={() => setIsEditingTitle(true)}
                        className="text-xs text-charcoal-500 hover:text-charcoal-800 font-medium"
                      >
                        Edit Details
                      </button>
                    )}
                  </div>
                ) : (
                  <div className="space-y-3">
                    <input
                      type="text"
                      value={editTitle}
                      onChange={(e) => setEditTitle(e.target.value)}
                      className="w-full text-sm font-semibold p-2 border border-sand-300 rounded-lg focus:ring-1 focus:ring-bronze-500"
                    />
                    <textarea
                      rows={2}
                      value={editDescription}
                      onChange={(e) => setEditDescription(e.target.value)}
                      placeholder="Add an optional description for this board..."
                      className="w-full text-xs p-2 border border-sand-300 rounded-lg focus:ring-1 focus:ring-bronze-500"
                    />
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={handleUpdateCollection}
                        className="px-3 py-1 text-xs font-medium text-white bg-charcoal-900 rounded-md hover:bg-charcoal-800"
                      >
                        Save
                      </button>
                      <button
                        type="button"
                        onClick={() => setIsEditingTitle(false)}
                        className="px-3 py-1 text-xs text-charcoal-600 hover:text-charcoal-800"
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* Items Grid */}
              {collectionDetail.items.length === 0 ? (
                <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-sand-300 space-y-2">
                  <p className="text-sm font-medium text-charcoal-700">No saved projects in this collection</p>
                  <p className="text-xs text-charcoal-500 max-w-sm mx-auto">
                    Explore design portfolios and bookmark your favorite projects to organize ideas here.
                  </p>
                  <div className="pt-2">
                    <Link
                      href="/projects"
                      className="inline-flex px-4 py-2 text-xs font-semibold text-charcoal-900 bg-sand-100 rounded-lg hover:bg-sand-200 transition-colors"
                    >
                      Browse Projects
                    </Link>
                  </div>
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
                  {collectionDetail.items.map((item) => (
                    <div
                      key={item.id}
                      className="bg-white rounded-xl border border-sand-200 shadow-xs overflow-hidden flex flex-col justify-between"
                    >
                      {/* Image / Tombstone Area */}
                      {item.isAvailable ? (
                        <div className="relative aspect-[4/3] bg-sand-100 overflow-hidden">
                          {item.coverImageUrl ? (
                            <img
                              src={item.coverImageUrl}
                              alt={item.projectTitle}
                              className="w-full h-full object-cover transition-transform duration-300 hover:scale-105"
                            />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-charcoal-400 text-xs">
                              No Cover Image
                            </div>
                          )}
                        </div>
                      ) : (
                        /* Strict Tombstone Presentation */
                        <div className="aspect-[4/3] bg-sand-100 flex flex-col items-center justify-center p-4 text-center border-b border-sand-200">
                          <span className="text-charcoal-400 text-2xl mb-1">🔒</span>
                          <span className="text-xs font-semibold text-charcoal-700">
                            Project Unavailable
                          </span>
                          <p className="text-[11px] text-charcoal-500 mt-1 max-w-[200px] leading-tight">
                            This project was made private or removed by the designer.
                          </p>
                        </div>
                      )}

                      {/* Content Card Body */}
                      <div className="p-4 space-y-2 flex-1 flex flex-col justify-between">
                        <div>
                          {item.isAvailable ? (
                            <>
                              <Link
                                href={`/projects/${item.projectSlug || item.projectId}`}
                                className="font-semibold text-xs text-charcoal-900 hover:underline block line-clamp-1"
                              >
                                {item.projectTitle}
                              </Link>
                              {item.studioName && (
                                <Link
                                  href={`/professionals/${item.studioSlug}`}
                                  className="text-[11px] text-charcoal-500 hover:text-charcoal-700 block mt-0.5 line-clamp-1"
                                >
                                  By {item.studioName}
                                </Link>
                              )}
                            </>
                          ) : (
                            <p className="text-xs font-medium text-charcoal-500 italic">
                              This project is no longer available
                            </p>
                          )}

                          {/* Private Note Area */}
                          <div className="mt-3 pt-2 border-t border-sand-100">
                            {editingItemId === item.id ? (
                              <div className="space-y-1.5">
                                <textarea
                                  rows={2}
                                  value={itemNoteText}
                                  onChange={(e) => setItemNoteText(e.target.value)}
                                  placeholder="Private note..."
                                  className="w-full text-xs p-1.5 border border-sand-300 rounded focus:ring-1 focus:ring-bronze-500"
                                />
                                <div className="flex items-center justify-end gap-1.5">
                                  <button
                                    type="button"
                                    onClick={() => handleSaveNote(item.id)}
                                    className="px-2 py-0.5 text-[11px] bg-charcoal-900 text-white rounded hover:bg-charcoal-800"
                                  >
                                    Save
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => setEditingItemId(null)}
                                    className="px-2 py-0.5 text-[11px] text-charcoal-500 hover:text-charcoal-700"
                                  >
                                    Cancel
                                  </button>
                                </div>
                              </div>
                            ) : (
                              <div
                                onClick={() => {
                                  setEditingItemId(item.id);
                                  setItemNoteText(item.note || '');
                                }}
                                className="cursor-pointer group/note"
                              >
                                {item.note ? (
                                  <p className="text-[11px] text-charcoal-600 bg-sand-50 p-2 rounded border border-sand-200 leading-tight">
                                    💬 {item.note}
                                  </p>
                                ) : (
                                  <p className="text-[11px] text-charcoal-400 group-hover/note:text-charcoal-600">
                                    + Add private note
                                  </p>
                                )}
                              </div>
                            )}
                          </div>
                        </div>

                        {/* Footer Remove Action */}
                        <div className="pt-2 flex items-center justify-end">
                          <button
                            type="button"
                            onClick={() => handleRemoveItem(item.id)}
                            className="text-[11px] text-charcoal-400 hover:text-red-600 transition-colors"
                          >
                            Remove
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ) : null}
        </div>
      </div>

      {/* New Collection Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-sand-100 pb-3">
              <h3 className="font-semibold text-charcoal-900 text-base">New Collection</h3>
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="text-charcoal-400 hover:text-charcoal-600"
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-charcoal-700 mb-1">
                  Collection Title <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="e.g. Master Bedroom Ideas"
                  className="w-full text-xs p-2.5 border border-sand-300 rounded-lg focus:ring-1 focus:ring-bronze-500"
                  autoFocus
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-charcoal-700 mb-1">
                  Description (optional)
                </label>
                <textarea
                  rows={2}
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  placeholder="What is this collection for..."
                  className="w-full text-xs p-2.5 border border-sand-300 rounded-lg focus:ring-1 focus:ring-bronze-500"
                />
              </div>
              <div className="flex items-center justify-end gap-2 pt-2 border-t border-sand-100">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-3 py-1.5 text-xs text-charcoal-600 hover:text-charcoal-800"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isCreating || !newTitle.trim()}
                  className="px-4 py-1.5 text-xs font-medium text-white bg-charcoal-900 hover:bg-charcoal-800 rounded-lg disabled:opacity-50"
                >
                  {isCreating ? 'Creating...' : 'Create Collection'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
