'use client';

import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  Upload,
  Image as ImageIcon,
  CheckCircle,
  AlertCircle,
  Trash2,
  Star,
  Edit2,
  ArrowLeft,
  ArrowRight,
  Shield,
  Sparkles,
  Lock,
  Loader2,
  Eye,
  Sliders,
  X,
  Plus,
  Home,
  Crosshair,
  Film,
  Layers,
  ChevronUp,
  ChevronDown,
  FolderOpen,
} from 'lucide-react';
import {
  MediaDetailResponse,
  MediaType,
  MediaVisibility,
  MEDIA_TYPE_LABELS,
  UpdateMediaRequest,
  WatermarkSettings,
} from '@/lib/media/types';
import {
  fetchProjectMedia,
  uploadMediaFile,
  updateMedia,
  reorderProjectMedia,
  deleteMedia,
  fetchWatermarkSettings,
  updateWatermarkSettings,
} from '@/lib/media/api';
import {
  ProjectRoomDto,
  RoomType,
} from '@/lib/projects/types';
import {
  fetchProjectRooms,
  createProjectRoom,
  updateProjectRoom,
  deleteProjectRoom,
  reorderProjectRooms,
} from '@/lib/projects/api';
import { FocalPointModal } from './FocalPointModal';
import { AddRoomModal } from './AddRoomModal';
import { PhotoInspectorSlideover } from './PhotoInspectorSlideover';

interface ProjectRoomManagerProps {
  projectId: string;
  studioId?: string;
  isReadOnly?: boolean;
  onCoverChanged?: (mediaId: string) => void;
}

interface UploadQueueItem {
  id: string;
  file: File;
  progress: number;
  status: 'PENDING' | 'UPLOADING' | 'COMPLETED' | 'FAILED';
  error?: string;
}

export function ProjectRoomManager({
  projectId,
  studioId,
  isReadOnly = false,
  onCoverChanged,
}: ProjectRoomManagerProps) {
  // State: Data
  const [mediaList, setMediaList] = useState<MediaDetailResponse[]>([]);
  const [rooms, setRooms] = useState<ProjectRoomDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // State: Spatial Navigation
  // 'all' = All Spaces, 'unassigned' = Project Photos, or <room-uuid>
  const [activeSpaceId, setActiveSpaceId] = useState<string>('all');

  // State: Modals
  const [isAddRoomOpen, setIsAddRoomOpen] = useState(false);
  const [editingRoom, setEditingRoom] = useState<ProjectRoomDto | null>(null);

  const [inspectingMedia, setInspectingMedia] = useState<MediaDetailResponse | null>(null);

  const [focalModalMedia, setFocalModalMedia] = useState<MediaDetailResponse | null>(null);
  const [isFocalModalOpen, setIsFocalModalOpen] = useState(false);

  // State: Watermark Settings
  const [showWatermarkModal, setShowWatermarkModal] = useState(false);
  const [watermarkSettings, setWatermarkSettings] = useState<WatermarkSettings | null>(null);
  const [savingWatermark, setSavingWatermark] = useState(false);

  // State: Multi-upload
  const [uploadQueue, setUploadQueue] = useState<UploadQueueItem[]>([]);
  const [selectedMediaType, setSelectedMediaType] = useState<MediaType>('REAL_PROJECT');
  const [uploadWatermark, setUploadWatermark] = useState(true);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Load rooms and media
  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const mediaData = await fetchProjectMedia(projectId, studioId);
      let roomsData: ProjectRoomDto[] = [];
      try {
        roomsData = await fetchProjectRooms(projectId, studioId);
      } catch {
        roomsData = [];
      }
      setMediaList(mediaData);
      setRooms(roomsData);
    } catch (err: any) {
      setError(err?.message || 'Failed to load project media and spaces');
    } finally {
      setLoading(false);
    }
  }, [projectId, studioId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Spatial Filtering
  const unassignedCount = mediaList.filter((m) => !m.roomId).length;

  const filteredMedia = mediaList.filter((m) => {
    if (activeSpaceId === 'all') return true;
    if (activeSpaceId === 'unassigned') return !m.roomId;
    return m.roomId === activeSpaceId;
  });

  const activeRoom = rooms.find((r) => r.id === activeSpaceId);

  // Multi-upload processor with concurrency control (max 3 concurrent)
  useEffect(() => {
    const activeUploads = uploadQueue.filter((q) => q.status === 'UPLOADING').length;
    const pendingItem = uploadQueue.find((q) => q.status === 'PENDING');

    if (activeUploads < 3 && pendingItem) {
      setUploadQueue((prev) =>
        prev.map((item) => (item.id === pendingItem.id ? { ...item, status: 'UPLOADING' } : item))
      );

      const targetRoomId =
        activeSpaceId !== 'all' && activeSpaceId !== 'unassigned' ? activeSpaceId : undefined;

      uploadMediaFile(pendingItem.file, projectId, {
        mediaType: selectedMediaType,
        watermarkEnabled: uploadWatermark,
        studioId,
        roomId: targetRoomId,
        onProgress: (progress) => {
          setUploadQueue((prev) =>
            prev.map((item) => (item.id === pendingItem.id ? { ...item, progress } : item))
          );
        },
      })
        .then(() => {
          setUploadQueue((prev) =>
            prev.map((item) =>
              item.id === pendingItem.id ? { ...item, status: 'COMPLETED', progress: 100 } : item
            )
          );
          loadData();
        })
        .catch((uploadErr: any) => {
          setUploadQueue((prev) =>
            prev.map((item) =>
              item.id === pendingItem.id
                ? { ...item, status: 'FAILED', error: uploadErr?.message || 'Upload failed' }
                : item
            )
          );
        });
    }
  }, [uploadQueue, activeSpaceId, selectedMediaType, uploadWatermark, projectId, studioId, loadData]);

  const handleFilesSelected = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    const newItems: UploadQueueItem[] = Array.from(files).slice(0, 50).map((file) => ({
      id: `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
      file,
      progress: 0,
      status: 'PENDING',
    }));

    setUploadQueue((prev) => [...prev, ...newItems]);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  // Room Management Handlers
  const handleSaveRoom = async (roomType: RoomType, displayName?: string) => {
    if (editingRoom) {
      await updateProjectRoom(projectId, editingRoom.id, { roomType, displayName }, studioId);
    } else {
      const created = await createProjectRoom(projectId, { roomType, displayName }, studioId);
      setActiveSpaceId(created.id);
    }
    await loadData();
    setEditingRoom(null);
  };

  const handleDeleteRoom = async (room: ProjectRoomDto) => {
    const confirmMsg = `Delete "${room.displayName}"?\nPhotos assigned to this space will not be lost; they will be moved to Project Photos (unassigned).`;
    if (!window.confirm(confirmMsg)) return;

    try {
      await deleteProjectRoom(projectId, room.id, studioId);
      if (activeSpaceId === room.id) {
        setActiveSpaceId('all');
      }
      await loadData();
    } catch (err: any) {
      setError(err?.message || 'Failed to delete room');
    }
  };

  const handleMoveRoom = async (roomIndex: number, direction: 'UP' | 'DOWN') => {
    const targetIndex = direction === 'UP' ? roomIndex - 1 : roomIndex + 1;
    if (targetIndex < 0 || targetIndex >= rooms.length) return;

    const reordered = [...rooms];
    const [moved] = reordered.splice(roomIndex, 1);
    reordered.splice(targetIndex, 0, moved);

    try {
      await reorderProjectRooms(
        projectId,
        reordered.map((r) => r.id),
        studioId
      );
      await loadData();
    } catch (err: any) {
      setError(err?.message || 'Failed to reorder rooms');
    }
  };

  // Photo Action Handlers
  const handleQuickMoveRoom = async (mediaId: string, newRoomId: string) => {
    try {
      const isUnassigned = newRoomId === 'unassigned';
      await updateMedia(
        mediaId,
        {
          roomId: isUnassigned ? undefined : newRoomId,
          clearRoom: isUnassigned,
          isRoomCover: isUnassigned ? false : undefined,
        },
        studioId
      );
      await loadData();
    } catch (err: any) {
      setError(err?.message || 'Failed to move photo');
    }
  };

  const handleToggleProjectCover = async (media: MediaDetailResponse) => {
    try {
      await updateMedia(media.id, { isCover: !media.isCover }, studioId);
      if (!media.isCover) onCoverChanged?.(media.id);
      await loadData();
    } catch (err: any) {
      setError(err?.message || 'Failed to update project cover');
    }
  };

  const handleToggleRoomCover = async (media: MediaDetailResponse) => {
    if (!media.roomId) {
      alert('Assign this photograph to a space first before designating it as a room cover.');
      return;
    }
    try {
      await updateMedia(media.id, { isRoomCover: !media.isRoomCover, roomId: media.roomId }, studioId);
      await loadData();
    } catch (err: any) {
      setError(err?.message || 'Failed to update room cover');
    }
  };

  const handleReorderPhoto = async (index: number, direction: 'LEFT' | 'RIGHT') => {
    const targetIndex = direction === 'LEFT' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= filteredMedia.length) return;

    const listCopy = [...filteredMedia];
    const [moved] = listCopy.splice(index, 1);
    listCopy.splice(targetIndex, 0, moved);

    try {
      await reorderProjectMedia(
        projectId,
        listCopy.map((m) => m.id),
        studioId
      );
      await loadData();
    } catch (err: any) {
      setError(err?.message || 'Failed to reorder photos');
    }
  };

  const handleDeletePhoto = async (mediaId: string) => {
    try {
      await deleteMedia(mediaId, studioId);
      await loadData();
    } catch (err: any) {
      setError(err?.message || 'Failed to delete photo');
    }
  };

  const handleUpdatePhotoProps = async (mediaId: string, data: UpdateMediaRequest) => {
    await updateMedia(mediaId, data, studioId);
    if (data.isCover) onCoverChanged?.(mediaId);
    await loadData();
  };

  const handleSaveFocalPoint = async (focalX: number, focalY: number) => {
    if (!focalModalMedia) return;
    try {
      await updateMedia(focalModalMedia.id, { focalX, focalY }, studioId);
      await loadData();
    } catch (err: any) {
      setError(err?.message || 'Failed to update focal point');
    }
  };

  const handleOpenWatermarkSettings = async () => {
    try {
      const s = await fetchWatermarkSettings(studioId);
      setWatermarkSettings(s);
      setShowWatermarkModal(true);
    } catch {
      setShowWatermarkModal(true);
    }
  };

  const handleSaveWatermark = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!watermarkSettings) return;
    try {
      setSavingWatermark(true);
      await updateWatermarkSettings(watermarkSettings, studioId);
      setShowWatermarkModal(false);
    } catch (err: any) {
      setError(err?.message || 'Failed to save watermark settings');
    } finally {
      setSavingWatermark(false);
    }
  };

  return (
    <div className="bg-white border border-sand-200 rounded-2xl shadow-sm overflow-hidden">
      {/* Top Header Bar */}
      <div className="p-5 sm:p-6 border-b border-sand-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-sand-50/40">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="font-serif text-lg font-semibold text-charcoal-900">
              Project Photography & Media
            </h3>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-sand-200 text-charcoal-700">
              {mediaList.length} {mediaList.length === 1 ? 'Asset' : 'Assets'}
            </span>
          </div>
          <p className="text-xs text-charcoal-500 mt-0.5">
            Organize real project photographs into rooms and spaces. Set room covers and focal points.
          </p>
        </div>

        {/* Global Toolbar Actions */}
        <div className="flex items-center gap-2 flex-wrap">
          <button
            type="button"
            onClick={handleOpenWatermarkSettings}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-sand-300 hover:bg-sand-100 text-charcoal-700 text-xs font-medium transition-colors"
          >
            <Shield className="w-3.5 h-3.5 text-bronze-600" />
            <span>Watermark Rules</span>
          </button>

          {!isReadOnly && (
            <button
              type="button"
              onClick={() => {
                setEditingRoom(null);
                setIsAddRoomOpen(true);
              }}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-bronze-50 border border-bronze-200 text-bronze-800 hover:bg-bronze-100 text-xs font-medium transition-colors shadow-sm"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Add Space</span>
            </button>
          )}
        </div>
      </div>

      {/* Feedback Banner */}
      {error && (
        <div className="m-4 p-3 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{error}</span>
          </div>
          <button onClick={() => setError(null)} className="font-semibold underline">
            Dismiss
          </button>
        </div>
      )}

      {/* Main Two-Column Spatial Workspace */}
      <div className="grid grid-cols-1 md:grid-cols-12 min-h-[540px]">
        {/* Left Column: Spatial Index (Rooms & Groups) */}
        <div className="md:col-span-4 lg:col-span-3 border-b md:border-b-0 md:border-r border-sand-100 p-4 bg-sand-50/30 flex flex-col">
          <div className="flex items-center justify-between mb-3 px-1">
            <span className="text-[11px] font-bold tracking-wider text-charcoal-500 uppercase">
              Spaces & Rooms ({rooms.length})
            </span>
          </div>

          {/* Swipeable Pills for Mobile (< 768px) */}
          <div className="flex md:hidden overflow-x-auto gap-2 pb-2 mb-2 scrollbar-none">
            <button
              type="button"
              onClick={() => setActiveSpaceId('all')}
              className={`px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-colors flex items-center gap-1.5 ${
                activeSpaceId === 'all'
                  ? 'bg-charcoal-900 text-white shadow-sm'
                  : 'bg-white border border-sand-300 text-charcoal-700'
              }`}
            >
              <span>All Spaces</span>
              <span className="text-[10px] opacity-80">({mediaList.length})</span>
            </button>

            {rooms.map((room) => (
              <button
                key={room.id}
                type="button"
                onClick={() => setActiveSpaceId(room.id)}
                className={`px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-colors flex items-center gap-1.5 ${
                  activeSpaceId === room.id
                    ? 'bg-charcoal-900 text-white shadow-sm'
                    : 'bg-white border border-sand-300 text-charcoal-700'
                }`}
              >
                <span>{room.displayName}</span>
                <span className="text-[10px] opacity-80">({room.photoCount})</span>
              </button>
            ))}

            <button
              type="button"
              onClick={() => setActiveSpaceId('unassigned')}
              className={`px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-colors flex items-center gap-1.5 ${
                activeSpaceId === 'unassigned'
                  ? 'bg-charcoal-900 text-white shadow-sm'
                  : 'bg-white border border-sand-300 text-charcoal-700'
              }`}
            >
              <span>Project Photos</span>
              <span className="text-[10px] opacity-80">({unassignedCount})</span>
            </button>
          </div>

          {/* Vertical Spatial Navigation List for Desktop (>= 768px) */}
          <div className="hidden md:flex flex-col space-y-1.5 flex-1">
            {/* All Spaces Button */}
            <button
              type="button"
              onClick={() => setActiveSpaceId('all')}
              className={`w-full px-3 py-2.5 rounded-xl text-left text-xs font-medium flex items-center justify-between transition-colors ${
                activeSpaceId === 'all'
                  ? 'bg-charcoal-900 text-white shadow-sm'
                  : 'hover:bg-sand-100/70 text-charcoal-800'
              }`}
            >
              <div className="flex items-center gap-2">
                <Layers className={`w-3.5 h-3.5 ${activeSpaceId === 'all' ? 'text-bronze-300' : 'text-charcoal-400'}`} />
                <span>All Spaces</span>
              </div>
              <span
                className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${
                  activeSpaceId === 'all' ? 'bg-charcoal-800 text-bronze-200' : 'bg-sand-200 text-charcoal-700'
                }`}
              >
                {mediaList.length}
              </span>
            </button>

            {/* Room List with Reordering & Actions */}
            <div className="space-y-1 py-1">
              {rooms.map((room, idx) => (
                <div
                  key={room.id}
                  className={`group rounded-xl p-2 flex items-center justify-between transition-all ${
                    activeSpaceId === room.id
                      ? 'bg-charcoal-900 text-white shadow-sm'
                      : 'hover:bg-sand-100/70 text-charcoal-800'
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => setActiveSpaceId(room.id)}
                    className="flex-1 text-left flex items-center gap-2 min-w-0"
                  >
                    <Home
                      className={`w-3.5 h-3.5 flex-shrink-0 ${
                        activeSpaceId === room.id ? 'text-bronze-300' : 'text-charcoal-400'
                      }`}
                    />
                    <span className="truncate text-xs font-medium">{room.displayName}</span>
                  </button>

                  <div className="flex items-center gap-1 flex-shrink-0">
                    <span
                      className={`text-[10px] px-2 py-0.5 rounded-full font-semibold mr-1 ${
                        activeSpaceId === room.id
                          ? 'bg-charcoal-800 text-bronze-200'
                          : 'bg-sand-200 text-charcoal-700'
                      }`}
                    >
                      {room.photoCount}
                    </span>

                    {!isReadOnly && (
                      <div className="opacity-0 group-hover:opacity-100 flex items-center transition-opacity">
                        <button
                          type="button"
                          disabled={idx === 0}
                          onClick={() => handleMoveRoom(idx, 'UP')}
                          title="Move space up"
                          className={`p-1 rounded hover:bg-sand-200/50 disabled:opacity-30 ${
                            activeSpaceId === room.id ? 'text-sand-300 hover:bg-charcoal-800' : 'text-charcoal-500'
                          }`}
                        >
                          <ChevronUp className="w-3 h-3" />
                        </button>
                        <button
                          type="button"
                          disabled={idx === rooms.length - 1}
                          onClick={() => handleMoveRoom(idx, 'DOWN')}
                          title="Move space down"
                          className={`p-1 rounded hover:bg-sand-200/50 disabled:opacity-30 ${
                            activeSpaceId === room.id ? 'text-sand-300 hover:bg-charcoal-800' : 'text-charcoal-500'
                          }`}
                        >
                          <ChevronDown className="w-3 h-3" />
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            setEditingRoom(room);
                            setIsAddRoomOpen(true);
                          }}
                          title="Rename space"
                          className={`p-1 rounded hover:bg-sand-200/50 ${
                            activeSpaceId === room.id ? 'text-sand-300 hover:bg-charcoal-800' : 'text-charcoal-500'
                          }`}
                        >
                          <Edit2 className="w-3 h-3" />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDeleteRoom(room)}
                          title="Delete space"
                          className={`p-1 rounded hover:bg-red-100 text-red-500`}
                        >
                          <Trash2 className="w-3 h-3" />
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>

            {/* Unassigned / Project Photos Group */}
            <button
              type="button"
              onClick={() => setActiveSpaceId('unassigned')}
              className={`w-full px-3 py-2.5 rounded-xl text-left text-xs font-medium flex items-center justify-between transition-colors mt-2 ${
                activeSpaceId === 'unassigned'
                  ? 'bg-charcoal-900 text-white shadow-sm'
                  : 'hover:bg-sand-100/70 text-charcoal-700'
              }`}
            >
              <div className="flex items-center gap-2">
                <FolderOpen
                  className={`w-3.5 h-3.5 ${
                    activeSpaceId === 'unassigned' ? 'text-bronze-300' : 'text-charcoal-400'
                  }`}
                />
                <span>Project Photos (Unassigned)</span>
              </div>
              <span
                className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${
                  activeSpaceId === 'unassigned'
                    ? 'bg-charcoal-800 text-bronze-200'
                    : 'bg-sand-200 text-charcoal-700'
                }`}
              >
                {unassignedCount}
              </span>
            </button>

            {/* Add Space Button */}
            {!isReadOnly && (
              <button
                type="button"
                onClick={() => {
                  setEditingRoom(null);
                  setIsAddRoomOpen(true);
                }}
                className="w-full mt-3 py-2 px-3 rounded-xl border border-dashed border-sand-300 text-charcoal-600 hover:border-bronze-500 hover:text-bronze-800 hover:bg-bronze-50/30 text-xs font-medium flex items-center justify-center gap-1.5 transition-colors"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Space</span>
              </button>
            )}
          </div>
        </div>

        {/* Right Column: Active Stage & Photo Grid */}
        <div className="md:col-span-8 lg:col-span-9 p-5 sm:p-6 flex flex-col space-y-6">
          {/* Active Space Header & Multi-Upload Bar */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-sand-100">
            <div>
              <div className="flex items-center gap-2">
                <h4 className="font-serif text-base font-semibold text-charcoal-900">
                  {activeSpaceId === 'all'
                    ? 'All Spaces'
                    : activeSpaceId === 'unassigned'
                    ? 'Project Photos (Unassigned)'
                    : activeRoom?.displayName || 'Space Photos'}
                </h4>
                <span className="text-xs text-charcoal-400 font-medium">
                  ({filteredMedia.length} {filteredMedia.length === 1 ? 'photo' : 'photos'})
                </span>
              </div>
              <p className="text-xs text-charcoal-500 mt-0.5">
                {activeSpaceId === 'all'
                  ? 'Viewing all photographs across every architectural space in this project.'
                  : activeSpaceId === 'unassigned'
                  ? 'Photographs not yet assigned to a specific room or space.'
                  : `Photographs grouped in ${activeRoom?.displayName}.`}
              </p>
            </div>

            {/* Upload Photography Trigger */}
            {!isReadOnly && filteredMedia.length > 0 && (
              <div className="flex items-center gap-2">
                <input
                  ref={fileInputRef}
                  type="file"
                  multiple
                  accept="image/jpeg,image/png,image/webp"
                  onChange={handleFilesSelected}
                  className="hidden"
                />
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-charcoal-900 hover:bg-charcoal-800 text-white text-xs font-medium transition-colors shadow-sm"
                >
                  <Upload className="w-3.5 h-3.5 text-bronze-300" />
                  <span>Upload Photography</span>
                </button>
              </div>
            )}
          </div>

          {/* Multi-File Upload Queue Status */}
          {uploadQueue.length > 0 && (
            <div className="bg-sand-50/80 rounded-xl p-4 border border-sand-200 space-y-3">
              <div className="flex items-center justify-between text-xs font-medium text-charcoal-800">
                <span>
                  Uploading {uploadQueue.filter((q) => q.status === 'COMPLETED').length} / {uploadQueue.length} files...
                </span>
                {uploadQueue.every((q) => q.status === 'COMPLETED' || q.status === 'FAILED') && (
                  <button
                    type="button"
                    onClick={() => setUploadQueue([])}
                    className="text-charcoal-500 hover:text-charcoal-900 text-xs underline"
                  >
                    Clear Queue
                  </button>
                )}
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-32 overflow-y-auto">
                {uploadQueue.map((item) => (
                  <div
                    key={item.id}
                    className="flex items-center justify-between p-2 rounded-lg bg-white border border-sand-200 text-xs"
                  >
                    <span className="truncate max-w-[160px] text-charcoal-700">{item.file.name}</span>
                    <div className="flex items-center gap-2">
                      {item.status === 'UPLOADING' && (
                        <div className="w-16 bg-sand-200 rounded-full h-1.5 overflow-hidden">
                          <div
                            className="bg-bronze-600 h-1.5 rounded-full transition-all"
                            style={{ width: `${item.progress}%` }}
                          />
                        </div>
                      )}
                      {item.status === 'COMPLETED' && (
                        <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                      )}
                      {item.status === 'FAILED' && (
                        <span title={item.error}>
                          <AlertCircle className="w-3.5 h-3.5 text-red-600" />
                        </span>
                      )}
                      {item.status === 'PENDING' && (
                        <span className="text-[10px] text-charcoal-400">Waiting</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Photo Gallery Grid */}
          {loading ? (
            <div className="flex items-center justify-center py-20 text-charcoal-400">
              <Loader2 className="w-6 h-6 animate-spin text-bronze-600" />
            </div>
          ) : filteredMedia.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20 border-2 border-dashed border-sand-200 rounded-2xl bg-sand-50/30 text-center p-6">
              <div className="p-3.5 rounded-2xl bg-white border border-sand-200 text-charcoal-400 mb-3 shadow-sm">
                <ImageIcon className="w-6 h-6" />
              </div>
              <h4 className="font-serif text-sm font-semibold text-charcoal-800">
                No project media uploaded yet
              </h4>
              <p className="text-xs text-charcoal-500 max-w-sm mt-1">
                {activeSpaceId === 'all'
                  ? 'Upload photographs to showcase this project. You can organize them into rooms anytime.'
                  : `No photos currently assigned to this space. Upload photos directly here or reassign photos from All Spaces.`}
              </p>
              {!isReadOnly && (
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="mt-4 inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-charcoal-900 hover:bg-charcoal-800 text-white text-xs font-medium transition-colors shadow-sm"
                >
                  <Upload className="w-3.5 h-3.5 text-bronze-300" />
                  <span>Upload Photography</span>
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {filteredMedia.map((media, index) => {
                const thumbUrl =
                  media.derivatives?.find((d) => d.variantName === 'THUMBNAIL')?.publicUrl ||
                  media.originalStorageKey;
                const fx = media.focalX ?? 50;
                const fy = media.focalY ?? 50;
                const isFocalCalibrated = fx !== 50 || fy !== 50;

                return (
                  <div
                    key={media.id}
                    className="group bg-white rounded-xl border border-sand-200 overflow-hidden shadow-sm hover:shadow-md transition-shadow flex flex-col"
                  >
                    {/* Thumbnail Image Container */}
                    <div
                      onClick={() => setInspectingMedia(media)}
                      className="relative aspect-video bg-charcoal-900 overflow-hidden cursor-pointer"
                    >
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={thumbUrl}
                        alt={media.altText || 'Project photo'}
                        className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                        style={{
                          objectPosition: `${fx}% ${fy}%`,
                        }}
                      />

                      {/* Top Badges */}
                      <div className="absolute top-2 left-2 flex flex-wrap gap-1 pointer-events-none">
                        {media.isCover && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-500 text-white shadow-sm">
                            <Star className="w-3 h-3 fill-current" />
                            <span>Cover</span>
                          </span>
                        )}
                        {media.isRoomCover && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-indigo-600 text-white shadow-sm">
                            <Home className="w-3 h-3" />
                            <span>Room Cover</span>
                          </span>
                        )}
                        {media.mediaType === 'AI_CONCEPT' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-purple-600 text-white shadow-sm">
                            <Sparkles className="w-3 h-3" />
                            <span>AI Concept</span>
                          </span>
                        )}
                        {media.visibility === 'PRIVATE' && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-charcoal-800 text-white shadow-sm">
                            <Lock className="w-3 h-3" />
                            <span>Private</span>
                          </span>
                        )}
                      </div>

                      {/* Bottom-right Reticle & Motion indicators */}
                      <div className="absolute bottom-2 right-2 flex items-center gap-1 pointer-events-none">
                        {isFocalCalibrated && (
                          <span className="p-1 rounded-md bg-white/90 text-charcoal-800 text-[10px] font-mono shadow-sm backdrop-blur-sm" title={`Focal: ${fx}% ${fy}%`}>
                            <Crosshair className="w-3 h-3 text-bronze-600 inline" />
                          </span>
                        )}
                        {media.motionEnabled === false && (
                          <span className="px-1.5 py-0.5 rounded-md bg-charcoal-900/80 text-white text-[9px] font-medium shadow-sm backdrop-blur-sm" title="Motion disabled">
                            Static
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Card Content & Fast Controls */}
                    <div className="p-3 flex-1 flex flex-col justify-between space-y-2">
                      <div>
                        <p className="text-xs font-medium text-charcoal-900 truncate">
                          {media.altText || media.caption || 'Untitled Photograph'}
                        </p>
                        {media.caption && media.altText && (
                          <p className="text-[11px] text-charcoal-500 truncate mt-0.5">
                            {media.caption}
                          </p>
                        )}
                      </div>

                      {/* Fast Room Mover Dropdown */}
                      <div className="pt-1">
                        <select
                          disabled={isReadOnly}
                          value={media.roomId || 'unassigned'}
                          onChange={(e) => handleQuickMoveRoom(media.id, e.target.value)}
                          className="w-full text-[11px] px-2 py-1 rounded-lg border border-sand-200 bg-sand-50/50 hover:bg-sand-50 text-charcoal-800 focus:outline-none focus:ring-1 focus:ring-bronze-600 transition-colors"
                        >
                          <option value="unassigned">Move to: Project Photos (Unassigned)</option>
                          {rooms.map((room) => (
                            <option key={room.id} value={room.id}>
                              Move to: {room.displayName}
                            </option>
                          ))}
                        </select>
                      </div>

                      {/* Card Footer Toolbar */}
                      <div className="pt-2 border-t border-sand-100 flex items-center justify-between text-charcoal-500">
                        {/* Reordering Controls */}
                        <div className="flex items-center gap-0.5">
                          {!isReadOnly && (
                            <>
                              <button
                                type="button"
                                disabled={index === 0}
                                onClick={() => handleReorderPhoto(index, 'LEFT')}
                                title="Move Left"
                                className="p-1 rounded hover:bg-sand-100 text-charcoal-600 disabled:opacity-30"
                              >
                                <ArrowLeft className="w-3.5 h-3.5" />
                              </button>
                              <button
                                type="button"
                                disabled={index === filteredMedia.length - 1}
                                onClick={() => handleReorderPhoto(index, 'RIGHT')}
                                title="Move Right"
                                className="p-1 rounded hover:bg-sand-100 text-charcoal-600 disabled:opacity-30"
                              >
                                <ArrowRight className="w-3.5 h-3.5" />
                              </button>
                            </>
                          )}
                        </div>

                        {/* Badges / Toggles */}
                        <div className="flex items-center gap-1">
                          {!isReadOnly && (
                            <>
                              {/* Project Cover Button */}
                              {!media.isCover &&
                                media.visibility !== 'PRIVATE' &&
                                media.mediaType !== 'REFERENCE' &&
                                media.mediaType !== 'CLIENT_PRIVATE' && (
                                  <button
                                    type="button"
                                    onClick={() => handleToggleProjectCover(media)}
                                    title="Set as Project Cover"
                                    className="p-1 rounded transition-colors text-charcoal-400 hover:text-amber-500 hover:bg-sand-100"
                                  >
                                    <Star className="w-3.5 h-3.5 fill-current" />
                                  </button>
                                )}

                              {media.isCover && (
                                <button
                                  type="button"
                                  onClick={() => handleToggleProjectCover(media)}
                                  title="Remove Project Cover"
                                  className="p-1 rounded transition-colors text-amber-500 hover:text-amber-600 bg-amber-50"
                                >
                                  <Star className="w-3.5 h-3.5 fill-current" />
                                </button>
                              )}

                              {/* Room Cover Button */}
                              {media.roomId && (
                                <button
                                  type="button"
                                  onClick={() => handleToggleRoomCover(media)}
                                  title={media.isRoomCover ? 'Remove Room Cover' : 'Set as Room Cover'}
                                  className={`p-1 rounded transition-colors ${
                                    media.isRoomCover
                                      ? 'text-indigo-600 hover:text-indigo-700 bg-indigo-50'
                                      : 'text-charcoal-400 hover:text-indigo-600 hover:bg-sand-100'
                                  }`}
                                >
                                  <Home className="w-3.5 h-3.5" />
                                </button>
                              )}

                              {/* Focal Point Shortcut */}
                              <button
                                type="button"
                                onClick={() => {
                                  setFocalModalMedia(media);
                                  setIsFocalModalOpen(true);
                                }}
                                title="Calibrate focal point"
                                className="p-1 rounded text-charcoal-400 hover:text-bronze-700 hover:bg-sand-100"
                              >
                                <Crosshair className="w-3.5 h-3.5" />
                              </button>

                              {/* Inspect Properties */}
                              <button
                                type="button"
                                onClick={() => setInspectingMedia(media)}
                                title="Edit Details"
                                className="p-1 rounded text-charcoal-400 hover:text-charcoal-800 hover:bg-sand-100"
                              >
                                <Edit2 className="w-3.5 h-3.5" />
                              </button>

                              {/* Delete Photo */}
                              <button
                                type="button"
                                onClick={() => handleDeletePhoto(media.id)}
                                title="Delete Asset"
                                className="p-1 rounded text-charcoal-400 hover:text-red-600 hover:bg-red-50"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </button>
                            </>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Add / Edit Room Modal */}
      <AddRoomModal
        isOpen={isAddRoomOpen}
        onClose={() => {
          setIsAddRoomOpen(false);
          setEditingRoom(null);
        }}
        onSave={handleSaveRoom}
        editingRoom={editingRoom}
      />

      {/* Photo Inspector Slide-over */}
      <PhotoInspectorSlideover
        isOpen={Boolean(inspectingMedia)}
        onClose={() => setInspectingMedia(null)}
        media={inspectingMedia}
        rooms={rooms}
        onUpdate={handleUpdatePhotoProps}
        onDelete={handleDeletePhoto}
        onOpenFocalModal={(targetMedia) => {
          setFocalModalMedia(targetMedia);
          setIsFocalModalOpen(true);
        }}
        isReadOnly={isReadOnly}
      />

      {/* Focal Point Calibration Modal */}
      {focalModalMedia && (
        <FocalPointModal
          isOpen={isFocalModalOpen}
          onClose={() => {
            setIsFocalModalOpen(false);
            setFocalModalMedia(null);
          }}
          imageUrl={
            focalModalMedia.derivatives?.find((d) => d.variantName === 'LARGE')?.publicUrl ||
            focalModalMedia.derivatives?.find((d) => d.variantName === 'THUMBNAIL')?.publicUrl ||
            focalModalMedia.originalStorageKey
          }
          initialX={focalModalMedia.focalX}
          initialY={focalModalMedia.focalY}
          onSave={handleSaveFocalPoint}
        />
      )}

      {/* Studio Watermark Settings Modal */}
      {showWatermarkModal && watermarkSettings && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-charcoal-950/70 backdrop-blur-sm"
        >
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-sand-200">
            <div className="flex items-center justify-between pb-3 border-b border-sand-100">
              <h3 className="font-serif text-base font-semibold text-charcoal-900">
                Watermark Rules
              </h3>
              <button
                onClick={() => setShowWatermarkModal(false)}
                className="p-1 rounded hover:bg-sand-100 text-charcoal-400"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <form onSubmit={handleSaveWatermark} className="space-y-4 pt-4">
              <label className="flex items-center gap-2 cursor-pointer text-xs font-medium text-charcoal-800">
                <input
                  type="checkbox"
                  checked={watermarkSettings.enabled}
                  onChange={(e) =>
                    setWatermarkSettings({ ...watermarkSettings, enabled: e.target.checked })
                  }
                  className="rounded border-sand-300 text-bronze-600 focus:ring-bronze-500"
                />
                <span>Enable Watermarking for Portfolio Photographs</span>
              </label>

              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Watermark Position
                </label>
                <select
                  value={watermarkSettings.position}
                  onChange={(e) =>
                    setWatermarkSettings({
                      ...watermarkSettings,
                      position: e.target.value as any,
                    })
                  }
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 bg-sand-50/40"
                >
                  <option value="BOTTOM_RIGHT">Bottom Right (Standard)</option>
                  <option value="BOTTOM_LEFT">Bottom Left</option>
                  <option value="TOP_RIGHT">Top Right</option>
                  <option value="TOP_LEFT">Top Left</option>
                  <option value="CENTER">Center (Diagonal)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-charcoal-800 mb-1">
                  Fallback Brand Text
                </label>
                <input
                  type="text"
                  value={watermarkSettings.fallbackText || ''}
                  onChange={(e) =>
                    setWatermarkSettings({ ...watermarkSettings, fallbackText: e.target.value })
                  }
                  placeholder="Studio Brand Name"
                  className="w-full text-xs px-3 py-2 rounded-lg border border-sand-300 bg-sand-50/40"
                />
              </div>

              <div className="pt-3 border-t border-sand-100 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowWatermarkModal(false)}
                  className="px-4 py-2 rounded-xl border border-sand-300 text-xs font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={savingWatermark}
                  className="px-5 py-2 rounded-xl bg-charcoal-900 text-white text-xs font-medium shadow-sm"
                >
                  {savingWatermark ? 'Saving...' : 'Save Settings'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
