package com.boilerplate.application.auth;

import com.boilerplate.domain.auth.User;

/**
 * Outbound port: mints the opaque access/refresh tokens the SPA carries in cookies.
 * The JWT implementation lives in {@code infrastructure.security} — the application layer
 * never sees a JWT library. Pattern to copy for any other third-party capability.
 */
public interface AuthTokenIssuer {

    String generateAccessToken(User user);

    String generateRefreshToken(User user);
}
