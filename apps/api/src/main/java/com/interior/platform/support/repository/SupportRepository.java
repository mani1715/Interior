package com.interior.platform.support.repository;

import com.interior.platform.support.domain.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportRepository {

    SupportRequestRecord createRequest(SupportRequestRecord request);

    Optional<SupportRequestRecord> findRequestById(UUID requestId);

    List<SupportRequestRecord> findRequestsByUserId(UUID userId, int limit, int offset);

    List<SupportRequestRecord> findRequestsByStudioId(UUID studioId, int limit, int offset);

    List<SupportRequestRecord> listAllRequests(int limit, int offset, SupportStatus status, SupportCategory category, SupportPriority priority);

    void updateRequestStatus(UUID requestId, SupportStatus status, SupportPriority priority, Instant resolvedAt);

    SupportMessageRecord createMessage(SupportMessageRecord message);

    List<SupportMessageRecord> findMessagesByRequestId(UUID requestId, boolean includeInternal);

    int countUserRequestsLastHour(UUID userId);

    int countRecentUserMessages(UUID userId, Instant since);
}
