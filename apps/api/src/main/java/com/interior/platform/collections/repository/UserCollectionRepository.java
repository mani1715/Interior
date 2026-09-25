package com.interior.platform.collections.repository;

import com.interior.platform.collections.domain.CollectionItemRecord;
import com.interior.platform.collections.domain.UserCollectionRecord;
import com.interior.platform.collections.dto.CollectionItemDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserCollectionRepository {

    UserCollectionRecord save(UserCollectionRecord collection);

    Optional<UserCollectionRecord> findById(UUID id);

    Optional<UserCollectionRecord> findDefaultCollection(UUID ownerUserId);

    List<UserCollectionRecord> listCollections(UUID ownerUserId);

    int countCollections(UUID ownerUserId);

    void updateCollection(UUID id, UUID ownerUserId, String title, String description);

    void deleteCollection(UUID id, UUID ownerUserId);

    CollectionItemRecord saveItem(CollectionItemRecord item);

    Optional<CollectionItemRecord> findItemById(UUID itemId);

    Optional<CollectionItemRecord> findItemByProject(UUID collectionId, UUID projectId);

    List<CollectionItemRecord> listItems(UUID collectionId);

    List<CollectionItemDto> listItemsWithProjection(UUID collectionId);

    int countItems(UUID collectionId);

    void deleteItem(UUID itemId, UUID ownerUserId);

    void updateItemNote(UUID itemId, UUID ownerUserId, String note);

    void updateItemDisplayOrder(UUID itemId, UUID collectionId, int displayOrder);
}
