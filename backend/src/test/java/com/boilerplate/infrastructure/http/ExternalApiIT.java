package com.boilerplate.infrastructure.http;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.boilerplate.application.api.ApiClientService;
import com.boilerplate.application.api.CreateApiClientCommand;
import com.boilerplate.application.api.CreateApiClientResult;
import com.boilerplate.support.AbstractIntegrationIT;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class ExternalApiIT extends AbstractIntegrationIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    ApiClientService apiClientService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void external_products_require_a_bearer_token_and_read_scope() throws Exception {
        mvc.perform(get("/api/ext/products")).andExpect(status().isUnauthorized());

        CreateApiClientResult client = apiClientService.create(
                new CreateApiClientCommand("external-api-it-" + UUID.randomUUID(), List.of("products:read")));
        MvcResult tokenResponse = mvc.perform(post("/api/ext/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("client_id", client.client().clientId(), "client_secret", client.secret()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.scopes[0]").value("products:read"))
                .andReturn();

        String accessToken = objectMapper
                .readTree(tokenResponse.getResponse().getContentAsString())
                .get("access_token")
                .asText();
        mvc.perform(get("/api/ext/products").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void external_token_endpoint_rejects_invalid_client_credentials() throws Exception {
        mvc.perform(post("/api/ext/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"client_id\":\"missing\",\"client_secret\":\"invalid\"}"))
                .andExpect(status().isUnauthorized());
    }
}
