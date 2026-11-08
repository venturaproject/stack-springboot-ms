package com.boilerplate.application.auth;

/**
 * Freshly issued access/refresh token pair plus their lifetimes. How they are delivered
 * to the client (httpOnly cookies) is an infrastructure concern — see
 * {@code infrastructure.http.AuthCookieWriter}.
 */
public record IssuedTokens(String accessToken, long accessTtlSecs, String refreshToken, long refreshTtlSecs) {}
