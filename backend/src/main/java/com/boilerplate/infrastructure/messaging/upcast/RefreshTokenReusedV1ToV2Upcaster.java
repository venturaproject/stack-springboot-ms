package com.boilerplate.infrastructure.messaging.upcast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Worked example of schema evolution. Suppose {@code auth.refresh_token_reused} grew a
 * {@code reusedTokenId} field in v2. Producers now emit v2, but v1 messages may still be on
 * the topic (or replayed from the outbox) — this lifts them: same data, plus the new field
 * defaulted to {@code null}. Consumers written against v2 never see a v1 payload.
 *
 * <p>Delete this class if you remove the v1 → v2 hop; add a sibling for each further hop.
 */
@Component
public class RefreshTokenReusedV1ToV2Upcaster implements EventUpcaster {

    @Override
    public String fromType() {
        return "auth.refresh_token_reused.v1";
    }

    @Override
    public String toType() {
        return "auth.refresh_token_reused.v2";
    }

    @Override
    public JsonNode upcast(JsonNode payload) {
        ObjectNode v2 = payload.deepCopy();
        if (!v2.has("reusedTokenId")) {
            v2.putNull("reusedTokenId");
        }
        return v2;
    }
}
