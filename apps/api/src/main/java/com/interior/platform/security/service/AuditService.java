package com.interior.platform.security.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interior.platform.security.repository.SecurityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final SecurityRepository securityRepository;
    private final ObjectMapper objectMapper;

    public AuditService(SecurityRepository securityRepository, ObjectMapper objectMapper) {
        this.securityRepository = securityRepository;
        this.objectMapper = objectMapper;
    }

    public void record(
            UUID actorUserId,
            UUID studioId,
            String eventType,
            String resourceType,
            String resourceId,
            Map<String, Object> payload,
            String ipAddress,
            String userAgent
    ) {
        String payloadJson = "{}";
        if (payload != null && !payload.isEmpty()) {
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize audit payload: {}", e.getMessage());
                payloadJson = "{\"error\":\"serialization_failed\"}";
            }
        }

        try {
            UUID id = UUID.randomUUID();
            UUID resUuid = null;
            if (resourceId != null) {
                try {
                    resUuid = UUID.fromString(resourceId);
                } catch (IllegalArgumentException ignored) {
                }
            }
            securityRepository.recordAuditEvent(
                    id,
                    studioId,
                    actorUserId,
                    eventType,
                    resourceType,
                    resUuid,
                    "req-" + UUID.randomUUID().toString().substring(0, 8),
                    payloadJson,
                    java.time.Instant.now()
            );
        } catch (Exception e) {
            // Audit failures should not crash the request, but must be logged
            log.error("Failed to persist audit event [{}]: {}", eventType, e.getMessage(), e);
        }
    }
}
