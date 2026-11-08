package com.boilerplate.infrastructure.messaging.upcast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Applies the registered {@link EventUpcaster}s in sequence until the payload is at its
 * latest version. A consumer calls {@link #upcast} right after it reads the raw message and
 * then works only against the current shape.
 */
@Component
@Slf4j
public class EventUpcasterChain {

    private static final int MAX_HOPS = 20;

    private final Map<String, EventUpcaster> byFromType;
    private final ObjectMapper objectMapper;

    public EventUpcasterChain(List<EventUpcaster> upcasters, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.byFromType =
                upcasters.stream().collect(Collectors.toUnmodifiableMap(EventUpcaster::fromType, Function.identity()));
    }

    /** Raw {@code (type, json)} in, latest {@code (type, payload)} out. */
    public Upcasted upcast(String type, String rawJson) {
        try {
            String currentType = type;
            JsonNode payload = objectMapper.readTree(rawJson);
            for (int hop = 0; hop < MAX_HOPS; hop++) {
                EventUpcaster next = byFromType.get(currentType);
                if (next == null) {
                    return new Upcasted(currentType, payload);
                }
                payload = next.upcast(payload);
                currentType = next.toType();
            }
            throw new IllegalStateException(
                    "Upcaster chain for " + type + " did not converge after " + MAX_HOPS + " hops");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Cannot parse event payload of type " + type, e);
        }
    }

    public record Upcasted(String type, JsonNode payload) {}
}
