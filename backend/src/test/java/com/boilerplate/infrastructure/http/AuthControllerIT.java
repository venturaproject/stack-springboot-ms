package com.boilerplate.infrastructure.http;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.boilerplate.application.auth.RefreshTokenDigester;
import com.boilerplate.domain.auth.UserRepository;
import com.boilerplate.support.AbstractIntegrationIT;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class AuthControllerIT extends AbstractIntegrationIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    RefreshTokenDigester refreshTokenDigester;

    @Autowired
    UserRepository userRepository;

    private static final String LOGIN = "/api/auth/login";

    @Test
    void a_valid_login_returns_the_user_and_sets_httponly_cookies() throws Exception {
        mvc.perform(withCsrf(mvc, post(LOGIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.user.roles").isArray())
                .andExpect(jsonPath("$.user.permissions").isArray())
                .andExpect(jsonPath("$.user.permissions").value(hasItem("users:read")))
                .andExpect(cookie().httpOnly("access_token", true))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(header().stringValues("Set-Cookie", hasItem(containsString("refresh_token="))));
    }

    @Test
    void csrf_endpoint_issues_a_non_cacheable_token_cookie_and_body() throws Exception {
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", true));
    }

    @Test
    void state_changing_auth_requests_without_a_csrf_token_are_rejected() throws Exception {
        mvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"admin123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void bad_credentials_are_401_without_leaking_which_factor_failed() throws Exception {
        mvc.perform(withCsrf(mvc, post(LOGIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void the_rate_limiter_returns_429_after_the_burst() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(withCsrf(mvc, post(LOGIN))
                    .header("X-Forwarded-For", "203.0.113.7")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"x@example.com\",\"password\":\"x\"}"));
        }
        mvc.perform(withCsrf(mvc, post(LOGIN))
                        .header("X-Forwarded-For", "203.0.113.7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@example.com\",\"password\":\"x\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void logout_revokes_the_server_side_refresh_family() throws Exception {
        var login = mvc.perform(withCsrf(mvc, post(LOGIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie = login.getResponse().getCookie("refresh_token");
        org.junit.jupiter.api.Assertions.assertNotNull(refreshCookie);

        mvc.perform(withCsrf(mvc, post("/api/auth/logout")).cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refresh_token", 0));

        var persisted = userRepository
                .findDetailedByRefreshTokenHash(refreshTokenDigester.digest(refreshCookie.getValue()))
                .orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(persisted
                .refreshToken(refreshTokenDigester.digest(refreshCookie.getValue()))
                .orElseThrow()
                .isRevoked());
    }
}
