package com.boilerplate.infrastructure.http;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.boilerplate.support.AbstractIntegrationIT;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

class ProductControllerIT extends AbstractIntegrationIT {

    @Autowired
    MockMvc mvc;

    @Test
    void an_anonymous_request_is_401() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "products:read")
    void read_authority_can_list_but_not_write() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isOk());
        mvc.perform(withCsrf(mvc, post("/api/products"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"TSH-9001\",\"name\":\"x\",\"priceAmountMinorUnits\":100,"
                                + "\"priceCurrency\":\"USD\",\"initialStock\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"products:read", "products:write"})
    void write_authority_can_create_a_product_and_a_bad_sku_is_422() throws Exception {
        String sku = "TSH-" + ThreadLocalRandom.current().nextInt(1000, 9999);
        mvc.perform(withCsrf(mvc, post("/api/products"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"" + sku + "\",\"name\":\"Test Tee\",\"priceAmountMinorUnits\":1999,"
                                + "\"priceCurrency\":\"USD\",\"initialStock\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value(sku));

        mvc.perform(withCsrf(mvc, post("/api/products"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"nope\",\"name\":\"Bad\",\"priceAmountMinorUnits\":1,"
                                + "\"priceCurrency\":\"USD\",\"initialStock\":1}"))
                .andExpect(status().isBadRequest());
    }
}
