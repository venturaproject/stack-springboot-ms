package com.boilerplate.infrastructure.messaging.upcast;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventUpcasterChainTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void a_type_with_no_upcaster_passes_straight_through() {
        var chain = new EventUpcasterChain(List.of(), objectMapper);

        EventUpcasterChain.Upcasted result = chain.upcast("orders.placed.v1", "{\"orderId\":\"x\"}");

        assertThat(result.type()).isEqualTo("orders.placed.v1");
        assertThat(result.payload().path("orderId").asText()).isEqualTo("x");
    }

    @Test
    void the_chain_walks_every_hop_to_the_latest_version() {
        var chain = new EventUpcasterChain(List.of(v1ToV2(), v2ToV3()), objectMapper);

        EventUpcasterChain.Upcasted result = chain.upcast("demo.thing.v1", "{\"a\":1}");

        assertThat(result.type()).isEqualTo("demo.thing.v3");
        assertThat(result.payload().path("a").asInt()).isEqualTo(1);
        assertThat(result.payload().path("b").asInt()).isEqualTo(2);
        assertThat(result.payload().path("c").asInt()).isEqualTo(3);
    }

    private EventUpcaster v1ToV2() {
        return upcaster("demo.thing.v1", "demo.thing.v2", "b", 2);
    }

    private EventUpcaster v2ToV3() {
        return upcaster("demo.thing.v2", "demo.thing.v3", "c", 3);
    }

    private EventUpcaster upcaster(String from, String to, String addField, int addValue) {
        return new EventUpcaster() {
            @Override
            public String fromType() {
                return from;
            }

            @Override
            public String toType() {
                return to;
            }

            @Override
            public JsonNode upcast(JsonNode payload) {
                ObjectNode next = payload.deepCopy();
                next.put(addField, addValue);
                return next;
            }
        };
    }
}
