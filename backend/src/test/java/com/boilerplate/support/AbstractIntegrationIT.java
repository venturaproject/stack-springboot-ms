package com.boilerplate.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Full-context integration test against a real PostgreSQL, with {@code MockMvc} wired. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainer.class)
public abstract class AbstractIntegrationIT {

    protected MockHttpServletRequestBuilder withCsrf(MockMvc mvc, MockHttpServletRequestBuilder request)
            throws Exception {
        MvcResult result =
                mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        return request.cookie(cookie).header("X-XSRF-TOKEN", token);
    }
}
