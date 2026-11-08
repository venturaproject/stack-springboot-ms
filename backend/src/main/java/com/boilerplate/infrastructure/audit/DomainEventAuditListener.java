package com.boilerplate.infrastructure.audit;

import com.boilerplate.domain.audit.AuditLog;
import com.boilerplate.domain.audit.AuditLogRepository;
import com.boilerplate.domain.shared.DomainEvent;
import com.boilerplate.infrastructure.security.SecurityUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Writes an {@code audit_logs} row for the domain events worth a permanent trail —
 * customer registration, product creation / price changes, order placement / cancellation.
 * Runs {@code BEFORE_COMMIT} so the audit row shares the originating transaction, exactly
 * like {@code OutboxWriter}. The actor is taken from the security context of the request
 * thread; background work (seeders, the outbox relay) records {@code "system"}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class DomainEventAuditListener {

    private static final Set<String> AUDITED = Set.of(
            "customers.registered.v1",
            "products.created.v1",
            "products.price_changed.v1",
            "orders.placed.v1",
            "orders.confirmed.v1",
            "orders.cancelled.v1");

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void on(DomainEvent event) {
        if (!AUDITED.contains(event.eventType())) {
            return;
        }
        String type = event.eventType();
        int firstDot = type.indexOf('.');
        int secondDot = firstDot < 0 ? -1 : type.indexOf('.', firstDot + 1);
        String entity = firstDot > 0 ? type.substring(0, firstDot) : "unknown";
        String action = firstDot >= 0 && secondDot > firstDot ? type.substring(firstDot + 1, secondDot) : "changed";

        String changes = null;
        String entityId = null;
        try {
            changes = objectMapper.writeValueAsString(event);
            entityId = firstIdValue(objectMapper.readTree(changes));
        } catch (JsonProcessingException e) {
            log.warn("Could not serialise audit event {}: {}", event.eventType(), e.getMessage());
        }
        auditLogRepository.save(AuditLog.record(actor(), action, entity, entityId, changes));
    }

    private static String actor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser user) {
            return user.id().toString();
        }
        return "system";
    }

    @Nullable
    private static String firstIdValue(JsonNode node) {
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            JsonNode value = node.get(name);
            if (value != null && name.toLowerCase(Locale.ROOT).endsWith("id")) {
                return value.asText();
            }
        }
        return null;
    }
}
