import { apiFetch } from '../api-client';
import {
  CollectionDetailDto,
  CollectionItemDto,
  CreateCollectionRequest,
  ReorderItemsRequest,
  SaveProjectRequest,
  UpdateCollectionRequest,
  UpdateItemNoteRequest,
  UserCollectionDto,
} from './types';

/**
 * User inspiration collections endpoints (authenticated)
 */
export async function listCollections(): Promise<UserCollectionDto[]> {
  return apiFetch<UserCollectionDto[]>('/account/collections');
}

export async function createCollection(
  data: CreateCollectionRequest
): Promise<UserCollectionDto> {
  return apiFetch<UserCollectionDto>('/account/collections', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function getCollectionDetail(
  collectionId: string
): Promise<CollectionDetailDto> {
  return apiFetch<CollectionDetailDto>(`/account/collections/${encodeURIComponent(collectionId)}`);
}

export async function updateCollection(
  collectionId: string,
  data: UpdateCollectionRequest
): Promise<UserCollectionDto> {
  return apiFetch<UserCollectionDto>(`/account/collections/${encodeURIComponent(collectionId)}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deleteCollection(collectionId: string): Promise<void> {
  return apiFetch<void>(`/account/collections/${encodeURIComponent(collectionId)}`, {
    method: 'DELETE',
  });
}

export async function saveProjectToCollection(
  data: SaveProjectRequest
): Promise<CollectionItemDto> {
  return apiFetch<CollectionItemDto>('/account/collections/save', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function removeCollectionItem(itemId: string): Promise<void> {
  return apiFetch<void>(`/account/collections/items/${encodeURIComponent(itemId)}`, {
    method: 'DELETE',
  });
}

export async function updateCollectionItemNote(
  itemId: string,
  data: UpdateItemNoteRequest
): Promise<CollectionItemDto> {
  return apiFetch<CollectionItemDto>(
    `/account/collections/items/${encodeURIComponent(itemId)}/note`,
    {
      method: 'PUT',
      body: JSON.stringify(data),
    }
  );
}

export async function reorderCollectionItems(
  collectionId: string,
  data: ReorderItemsRequest
): Promise<void> {
  return apiFetch<void>(
    `/account/collections/${encodeURIComponent(collectionId)}/reorder`,
    {
      method: 'PUT',
      body: JSON.stringify(data),
    }
  );
}
