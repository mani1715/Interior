package com.interior.platform.email.web;

import com.interior.platform.email.domain.CommunicationDeliveryRecord;
import com.interior.platform.email.service.CommunicationDeliveryService;
import com.interior.platform.security.domain.ActorContext;
import com.interior.platform.security.interceptor.SecurityInterceptor;
import com.interior.platform.security.service.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/workspace/deliveries", "/api/v1/workspace/deliveries"})
@Tag(name = "Communication Deliveries", description = "Truthful external communication delivery observability and audit logs")
public class CommunicationDeliveryController {

    private final CommunicationDeliveryService deliveryService;
    private final AuthorizationService authorizationService;

    public CommunicationDeliveryController(
            CommunicationDeliveryService deliveryService,
            AuthorizationService authorizationService
    ) {
        this.deliveryService = deliveryService;
        this.authorizationService = authorizationService;
    }

    private ActorContext getActor(HttpServletRequest request) {
        ActorContext actor = (ActorContext) request.getAttribute(SecurityInterceptor.ACTOR_ATTRIBUTE);
        return actor != null ? actor : ActorContext.anonymous();
    }

    @GetMapping
    @Operation(summary = "List studio communication deliveries", description = "Retrieves truthful delivery audit trail for email and messaging")
    public ResponseEntity<List<CommunicationDeliveryRecord>> listDeliveries(
            @RequestParam(required = false) UUID studioId,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest request
    ) {
        ActorContext actor = getActor(request);
        authorizationService.requireAuthenticated(actor);

        UUID effectiveStudioId = studioId != null ? studioId : actor.activeStudioId();
        if (effectiveStudioId == null) {
            return ResponseEntity.badRequest().build();
        }

        authorizationService.requireStudioAccess(actor, effectiveStudioId);

        List<CommunicationDeliveryRecord> records = deliveryService.listStudioDeliveries(effectiveStudioId, limit, offset);
        return ResponseEntity.ok(records);
    }
}
