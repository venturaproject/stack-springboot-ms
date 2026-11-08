package com.boilerplate.infrastructure.security;

import com.boilerplate.application.auth.AuthTokenIssuer;
import com.boilerplate.config.AppProperties;
import com.boilerplate.domain.auth.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Access tokens are short-lived signed JWTs (stateless auth on every request). Refresh
 * tokens are <em>opaque</em> random strings — they are only ever a DB lookup key, so they
 * carry no claims, can't be forged, and are trivially revocable.
 */
@Service
@RequiredArgsConstructor
public class JwtService implements AuthTokenIssuer {

    private static final SecureRandom RNG = new SecureRandom();

    private final AppProperties appProperties;

    @Override
    public String generateAccessToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + appProperties.jwt().accessTtlSecs() * 1000L))
                .signWith(getSignKey())
                .compact();
    }

    @Override
    public String generateRefreshToken(User user) {
        byte[] buffer = new byte[32];
        RNG.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaims(token).getSubject());
    }

    @Nullable
    public String extractTokenType(String token) {
        return (String) extractClaims(token).get("type");
    }

    public boolean isTokenValid(String token) {
        try {
            return !extractClaims(token).getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }
}
