package com.boilerplate.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.boilerplate.domain.customers.event.CustomerRegistered;
import com.boilerplate.domain.orders.event.OrderPlaced;
import com.boilerplate.domain.products.event.ProductCreated;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pins the public payload fields of representative cross-service events. */
class DomainEventWireContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void customer_registered_v1_keeps_its_wire_fields_and_values() throws Exception {
        UUID customerId = UUID.randomUUID();

        JsonNode payload = objectMapper.readTree(
                objectMapper.writeValueAsString(new CustomerRegistered(customerId, "buyer@example.com", "Buyer")));

        assertContract(payload, Set.of("customerId", "email", "name"));
        assertThat(payload.get("customerId").asText()).isEqualTo(customerId.toString());
        assertThat(payload.get("email").asText()).isEqualTo("buyer@example.com");
        assertThat(payload.get("name").asText()).isEqualTo("Buyer");
    }

    @Test
    void product_created_v1_keeps_its_wire_fields_and_values() throws Exception {
        UUID productId = UUID.randomUUID();

        JsonNode payload = objectMapper.readTree(
                objectMapper.writeValueAsString(new ProductCreated(productId, "SKU-100", "Widget")));

        assertContract(payload, Set.of("productId", "sku", "name"));
        assertThat(payload.get("productId").asText()).isEqualTo(productId.toString());
        assertThat(payload.get("sku").asText()).isEqualTo("SKU-100");
        assertThat(payload.get("name").asText()).isEqualTo("Widget");
    }

    @Test
    void order_placed_v1_keeps_its_wire_fields_and_values() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        JsonNode payload = objectMapper.readTree(
                objectMapper.writeValueAsString(new OrderPlaced(orderId, customerId, 1250, "USD")));

        assertContract(payload, Set.of("orderId", "customerId", "totalAmountMinorUnits", "currency"));
        assertThat(payload.get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(payload.get("customerId").asText()).isEqualTo(customerId.toString());
        assertThat(payload.get("totalAmountMinorUnits").asLong()).isEqualTo(1250);
        assertThat(payload.get("currency").asText()).isEqualTo("USD");
    }

    private static void assertContract(JsonNode payload, Set<String> expectedFields) {
        Set<String> actualFields = new HashSet<>();
        payload.fieldNames().forEachRemaining(actualFields::add);
        assertThat(actualFields).isEqualTo(expectedFields);
    }
}
