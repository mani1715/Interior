import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { SaveToCollectionButton } from '../collections/SaveToCollectionButton';
import { SaveToCollectionModal } from '../collections/SaveToCollectionModal';
import * as collectionsApi from '@/lib/collections/api';
import { UserCollectionDto } from '@/lib/collections/types';

vi.mock('@/lib/collections/api', () => ({
  listCollections: vi.fn(),
  createCollection: vi.fn(),
  saveProjectToCollection: vi.fn(),
  removeProjectFromCollection: vi.fn(),
}));

const mockCollections: UserCollectionDto[] = [
  {
    id: 'col-001',
    ownerUserId: 'user-001',
    title: 'Dream Living Rooms',
    description: 'Ideas for home renovation',
    isDefault: true,
    itemCount: 3,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-02T00:00:00Z',
  },
  {
    id: 'col-002',
    ownerUserId: 'user-001',
    title: 'Modern Kitchens',
    description: null,
    isDefault: false,
    itemCount: 1,
    createdAt: '2026-01-03T00:00:00Z',
    updatedAt: '2026-01-04T00:00:00Z',
  },
];

describe('Phase 27 — Collections Components', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('SaveToCollectionButton', () => {
    it('renders save button with variant="button"', () => {
      render(<SaveToCollectionButton projectId="proj-123" variant="button" />);
      expect(screen.getByText('Save to Collection')).toBeDefined();
    });

    it('opens SaveToCollectionModal when clicked', async () => {
      vi.mocked(collectionsApi.listCollections).mockResolvedValueOnce(mockCollections);
      render(<SaveToCollectionButton projectId="proj-123" variant="button" />);

      fireEvent.click(screen.getByText('Save to Collection'));

      await waitFor(() => {
        expect(screen.getByText('Select Mood Board / Collection')).toBeDefined();
        expect(screen.getByText('Dream Living Rooms')).toBeDefined();
      });
    });
  });

  describe('SaveToCollectionModal', () => {
    it('loads and displays user collections', async () => {
      vi.mocked(collectionsApi.listCollections).mockResolvedValueOnce(mockCollections);

      render(
        <SaveToCollectionModal
          projectId="proj-123"
          isOpen={true}
          onClose={() => {}}
        />
      );

      await waitFor(() => {
        expect(screen.getByText('Dream Living Rooms')).toBeDefined();
        expect(screen.getByText('Modern Kitchens')).toBeDefined();
      });
    });

    it('submits save request with chosen collection and private note', async () => {
      vi.mocked(collectionsApi.listCollections).mockResolvedValueOnce(mockCollections);
      vi.mocked(collectionsApi.saveProjectToCollection).mockResolvedValueOnce({
        id: 'item-001',
        collectionId: 'col-001',
        projectId: 'proj-123',
        projectTitle: 'Penthouse',
        displayOrder: 0,
        isAvailable: true,
        savedAt: '2026-01-01T00:00:00Z',
        note: 'Love the lighting in this shot',
      });

      const onSaved = vi.fn();
      const onClose = vi.fn();

      render(
        <SaveToCollectionModal
          projectId="proj-123"
          isOpen={true}
          onClose={onClose}
          onSaved={onSaved}
        />
      );

      await waitFor(() => {
        expect(screen.getByText('Dream Living Rooms')).toBeDefined();
      });

      // Fill in note
      const noteInput = screen.getByPlaceholderText(/Why you saved this/i);
      fireEvent.change(noteInput, { target: { value: 'Love the lighting in this shot' } });

      // Click save button
      const saveBtn = screen.getByRole('button', { name: /Save to Collection/i });
      fireEvent.click(saveBtn);

      await waitFor(() => {
        expect(collectionsApi.saveProjectToCollection).toHaveBeenCalledWith({
          projectId: 'proj-123',
          collectionId: 'col-001',
          note: 'Love the lighting in this shot',
        });
        expect(onSaved).toHaveBeenCalledWith('Dream Living Rooms');
        expect(onClose).toHaveBeenCalled();
      });
    });

    it('creates a new collection directly within modal', async () => {
      vi.mocked(collectionsApi.listCollections).mockResolvedValueOnce(mockCollections);
      vi.mocked(collectionsApi.createCollection).mockResolvedValueOnce({
        id: 'col-003',
        ownerUserId: 'user-001',
        title: 'Balcony Gardens',
        description: null,
        isDefault: false,
        itemCount: 0,
        createdAt: '2026-01-05T00:00:00Z',
        updatedAt: '2026-01-05T00:00:00Z',
      });

      render(
        <SaveToCollectionModal
          projectId="proj-123"
          isOpen={true}
          onClose={() => {}}
        />
      );

      await waitFor(() => {
        expect(screen.getByText('+ Create new collection')).toBeDefined();
      });

      fireEvent.click(screen.getByText('+ Create new collection'));

      const input = screen.getByPlaceholderText(/Living Room Ideas/i);
      fireEvent.change(input, { target: { value: 'Balcony Gardens' } });

      const createBtn = screen.getByRole('button', { name: /^Create$/i });
      fireEvent.click(createBtn);

      await waitFor(() => {
        expect(collectionsApi.createCollection).toHaveBeenCalledWith({
          title: 'Balcony Gardens',
        });
        expect(screen.getByText('Balcony Gardens')).toBeDefined();
      });
    });
  });
});
