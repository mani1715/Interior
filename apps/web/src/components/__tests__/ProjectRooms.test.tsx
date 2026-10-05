import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { FocalPointModal } from '@/components/media/FocalPointModal';
import { AddRoomModal } from '@/components/media/AddRoomModal';
import { PhotoInspectorSlideover } from '@/components/media/PhotoInspectorSlideover';
import { ProjectRoomManager } from '@/components/media/ProjectRoomManager';
import * as projectApi from '@/lib/projects/api';
import * as mediaApi from '@/lib/media/api';
import { ProjectRoomDto } from '@/lib/projects/types';
import { MediaDetailResponse } from '@/lib/media/types';

const mockRooms: ProjectRoomDto[] = [
  {
    id: 'room-1',
    projectId: 'proj-123',
    studioId: 'studio-1',
    roomType: 'LIVING_ROOM',
    displayName: 'Living Room',
    sortOrder: 0,
    photoCount: 2,
    createdAt: '2026-10-05T10:00:00Z',
    updatedAt: '2026-10-05T10:00:00Z',
  },
  {
    id: 'room-2',
    projectId: 'proj-123',
    studioId: 'studio-1',
    roomType: 'BEDROOM',
    displayName: 'Master Bedroom',
    sortOrder: 1,
    photoCount: 1,
    createdAt: '2026-10-05T10:00:00Z',
    updatedAt: '2026-10-05T10:00:00Z',
  },
];

const mockPhotos: MediaDetailResponse[] = [
  {
    id: 'media-1',
    studioId: 'studio-1',
    projectId: 'proj-123',
    roomId: 'room-1',
    mediaType: 'REAL_PROJECT',
    mediaTypeDisplayName: 'Real Project Photo',
    visibility: 'PORTFOLIO',
    processingStatus: 'READY',
    originalStorageKey: 'studio/1/media-1.jpg',
    contentType: 'image/jpeg',
    fileSize: 1024000,
    width: 2400,
    height: 1600,
    sortOrder: 0,
    isCover: true,
    isRoomCover: true,
    altText: 'Living room seating area with walnut coffee table',
    caption: 'Custom Italian sofa and bespoke cabinetry',
    watermarkEnabled: true,
    focalX: 45,
    focalY: 55,
    motionEnabled: true,
    derivatives: [
      {
        id: 'd-1',
        variantName: 'THUMBNAIL',
        width: 400,
        height: 267,
        format: 'webp',
        fileSize: 25000,
        publicUrl: '/media/thumb1.webp',
        isWatermarked: false,
      },
    ],
    createdAt: '2026-10-05T10:00:00Z',
    updatedAt: '2026-10-05T10:00:00Z',
  },
  {
    id: 'media-2',
    studioId: 'studio-1',
    projectId: 'proj-123',
    roomId: 'room-1',
    mediaType: 'REAL_PROJECT',
    mediaTypeDisplayName: 'Real Project Photo',
    visibility: 'PORTFOLIO',
    processingStatus: 'READY',
    originalStorageKey: 'studio/1/media-2.jpg',
    contentType: 'image/jpeg',
    fileSize: 1500000,
    width: 1920,
    height: 1080,
    sortOrder: 1,
    isCover: false,
    isRoomCover: false,
    altText: 'Dining table view from living',
    caption: null,
    watermarkEnabled: true,
    focalX: 50,
    focalY: 50,
    motionEnabled: false,
    derivatives: [],
    createdAt: '2026-10-05T11:00:00Z',
    updatedAt: '2026-10-05T11:00:00Z',
  },
  {
    id: 'media-3',
    studioId: 'studio-1',
    projectId: 'proj-123',
    roomId: null,
    mediaType: 'REAL_PROJECT',
    mediaTypeDisplayName: 'Real Project Photo',
    visibility: 'PORTFOLIO',
    processingStatus: 'READY',
    originalStorageKey: 'studio/1/media-3.jpg',
    contentType: 'image/jpeg',
    fileSize: 2000000,
    width: 1920,
    height: 1080,
    sortOrder: 2,
    isCover: false,
    isRoomCover: false,
    altText: 'Unassigned terrace overview',
    caption: null,
    watermarkEnabled: true,
    focalX: 50,
    focalY: 50,
    motionEnabled: true,
    derivatives: [],
    createdAt: '2026-10-05T12:00:00Z',
    updatedAt: '2026-10-05T12:00:00Z',
  },
];

describe('Portfolio Evolution Phase 1: Room & Photo Foundation & Organizer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('FocalPointModal Component', () => {
    it('1. Renders focal modal with initial coordinates, preview cards and keyboard calibration', () => {
      const handleSave = vi.fn();
      const handleClose = vi.fn();

      render(
        <FocalPointModal
          isOpen={true}
          onClose={handleClose}
          imageUrl="/media/test.jpg"
          initialX={45}
          initialY={60}
          onSave={handleSave}
        />
      );

      expect(screen.getByText('Adjust Focal Point')).toBeDefined();
      expect(screen.getByText(/Landscape Banner/i)).toBeDefined();
      expect(screen.getByText(/Mobile Portrait/i)).toBeDefined();
      expect(screen.getByText('Focal: 45% × 60%')).toBeDefined();

      // Keyboard nudge
      const modalDialog = screen.getByRole('dialog');
      fireEvent.keyDown(modalDialog, { key: 'ArrowRight' });
      expect(screen.getByText('Focal: 46% × 60%')).toBeDefined();

      // Reset Center
      const resetBtn = screen.getByText('Reset Center');
      fireEvent.click(resetBtn);
      expect(screen.getByText('Focal: 50% × 50%')).toBeDefined();

      // Save
      const applyBtn = screen.getByText('Apply Focal Point');
      fireEvent.click(applyBtn);
      expect(handleSave).toHaveBeenCalledWith(50, 50);
      expect(handleClose).toHaveBeenCalled();
    });
  });

  describe('AddRoomModal Component', () => {
    it('2. Allows creating and customizing space archetype', async () => {
      const handleSave = vi.fn().mockResolvedValue(undefined);
      const handleClose = vi.fn();

      render(
        <AddRoomModal
          isOpen={true}
          onClose={handleClose}
          onSave={handleSave}
        />
      );

      expect(screen.getByText('Add Space / Room Group')).toBeDefined();

      // Change archetype to Modular Kitchen
      const select = screen.getByDisplayValue('Living Room');
      fireEvent.change(select, { target: { value: 'KITCHEN' } });

      // Custom display name
      const nameInput = screen.getByPlaceholderText('Modular Kitchen');
      fireEvent.change(nameInput, { target: { value: 'Bespoke Island Kitchen' } });

      const submitBtn = screen.getByText('Create Space');
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(handleSave).toHaveBeenCalledWith('KITCHEN', 'Bespoke Island Kitchen');
        expect(handleClose).toHaveBeenCalled();
      });
    });
  });

  describe('PhotoInspectorSlideover Component', () => {
    it('3. Renders full photo properties, room selector, dual covers, and motion opt-out', async () => {
      const handleUpdate = vi.fn().mockResolvedValue(undefined);
      const handleDelete = vi.fn().mockResolvedValue(undefined);
      const handleOpenFocal = vi.fn();
      const handleClose = vi.fn();

      render(
        <PhotoInspectorSlideover
          isOpen={true}
          onClose={handleClose}
          media={mockPhotos[0]}
          rooms={mockRooms}
          onUpdate={handleUpdate}
          onDelete={handleDelete}
          onOpenFocalModal={handleOpenFocal}
        />
      );

      expect(screen.getByText('Photo Inspector')).toBeDefined();
      expect(screen.getByDisplayValue('Custom Italian sofa and bespoke cabinetry')).toBeDefined();
      expect(screen.getByDisplayValue('Living room seating area with walnut coffee table')).toBeDefined();

      // Room cover & project cover indicators
      expect(screen.getByText('Project Primary Cover')).toBeDefined();
      expect(screen.getByText('Room Cover Photo')).toBeDefined();

      // Motion opt-out
      expect(screen.getByText('Enable Presentation Motion')).toBeDefined();

      // Calibrate focal button
      const focalBtn = screen.getByText(/Calibrate Focal/i);
      fireEvent.click(focalBtn);
      expect(handleOpenFocal).toHaveBeenCalledWith(mockPhotos[0]);

      // Save properties
      const saveBtn = screen.getByText('Save Properties');
      fireEvent.click(saveBtn);

      await waitFor(() => {
        expect(handleUpdate).toHaveBeenCalledWith(
          'media-1',
          expect.objectContaining({
            isCover: true,
            isRoomCover: true,
            motionEnabled: true,
            roomId: 'room-1',
          })
        );
      });
    });
  });

  describe('ProjectRoomManager Component', () => {
    it('4. Loads spatial index, filters photos per room, and toggles room covers', async () => {
      vi.spyOn(mediaApi, 'fetchProjectMedia').mockResolvedValue(mockPhotos);
      vi.spyOn(projectApi, 'fetchProjectRooms').mockResolvedValue(mockRooms);
      vi.spyOn(mediaApi, 'updateMedia').mockResolvedValue(mockPhotos[0]);
      vi.spyOn(projectApi, 'reorderProjectRooms').mockResolvedValue(undefined);

      render(<ProjectRoomManager projectId="proj-123" studioId="studio-1" />);

      await waitFor(() => {
        expect(screen.getByText('Project Photography & Media')).toBeDefined();
        expect(screen.getByText('3 Assets')).toBeDefined();
      });

      // Spatial index lists rooms
      expect(screen.getAllByText('Living Room').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('Master Bedroom').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Project Photos (Unassigned)')).toBeDefined();

      // Filter by Living Room
      const livingRoomBtns = screen.getAllByRole('button', { name: /living room/i });
      fireEvent.click(livingRoomBtns[0]);

      await waitFor(() => {
        expect(screen.getByText('Photographs grouped in Living Room.')).toBeDefined();
      });

      // Shows photo cards
      expect(screen.getByText('Living room seating area with walnut coffee table')).toBeDefined();
      expect(screen.getByText('Dining table view from living')).toBeDefined();

      // Photo 1 has Project Cover and Room Cover
      expect(screen.getByText('Cover')).toBeDefined();
      expect(screen.getByText('Room Cover')).toBeDefined();

      // Static motion badge on Photo 2
      expect(screen.getByText('Static')).toBeDefined();
    });
  });
});
