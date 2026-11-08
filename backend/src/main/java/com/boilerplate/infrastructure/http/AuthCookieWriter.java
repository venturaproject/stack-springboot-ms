package com.boilerplate.infrastructure.http;

import com.boilerplate.application.auth.IssuedTokens;
import com.boilerplate.config.AppProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Turns an application-layer {@link IssuedTokens} into httpOnly cookies. This is the only
 * place that knows tokens travel in cookies — the application layer never touches the
 * servlet response (ARCHITECTURE.md).
 */
@Component
@RequiredArgsConstructor
public class AuthCookieWriter {

    private static final String ACCESS_COOKIE = "access_token";
    private static final String REFRESH_COOKIE = "refresh_token";

    private final AppProperties appProperties;

    public void writeAuthCookies(HttpServletResponse response, IssuedTokens tokens) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                accessCookie(tokens.accessToken(), tokens.accessTtlSecs()).toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshCookie(tokens.refreshToken(), tokens.refreshTtlSecs()).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie("", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString());
    }

    private ResponseCookie accessCookie(String value, long maxAge) {
        return ResponseCookie.from(ACCESS_COOKIE, value)
                .httpOnly(true)
                .secure(appProperties.cookie().secure())
                .path("/")
                .maxAge(maxAge)
                .sameSite(appProperties.cookie().sameSite())
                .build();
    }

    private ResponseCookie refreshCookie(String value, long maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(appProperties.cookie().secure())
                .path("/api/auth/refresh")
                .maxAge(maxAge)
                .sameSite(appProperties.cookie().sameSite())
                .build();
    }
}
