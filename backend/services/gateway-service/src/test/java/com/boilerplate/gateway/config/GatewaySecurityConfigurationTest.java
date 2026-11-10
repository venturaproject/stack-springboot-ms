package com.boilerplate.gateway.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.boilerplate.gateway.GatewayServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = GatewayServiceApplication.class)
@AutoConfigureMockMvc
class GatewaySecurityConfigurationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void business_routes_fail_closed_until_gateway_identity_is_implemented() throws Exception {
        mvc.perform(get("/api/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    void health_probe_remains_available_to_the_orchestrator() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
