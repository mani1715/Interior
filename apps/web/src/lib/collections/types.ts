export interface UserCollectionDto {
  id: string;
  ownerUserId: string;
  title: string;
  description?: string | null;
  isDefault: boolean;
  itemCount: number;
  coverImageUrl?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CollectionItemDto {
  id: string;
  collectionId: string;
  projectId: string;
  projectTitle: string;
  projectSlug?: string | null;
  coverImageUrl?: string | null;
  studioId?: string | null;
  studioName?: string | null;
  studioSlug?: string | null;
  categoryCode?: string | null;
  city?: string | null;
  displayOrder: number;
  note?: string | null;
  isAvailable: boolean;
  savedAt: string;
}

export interface CollectionDetailDto {
  collection: UserCollectionDto;
  items: CollectionItemDto[];
}

export interface CreateCollectionRequest {
  title: string;
  description?: string | null;
}

export interface UpdateCollectionRequest {
  title: string;
  description?: string | null;
}

export interface SaveProjectRequest {
  projectId: string;
  collectionId?: string | null;
  note?: string | null;
}

export interface UpdateItemNoteRequest {
  note?: string | null;
}

export interface ReorderItemsRequest {
  itemIds: string[];
}
