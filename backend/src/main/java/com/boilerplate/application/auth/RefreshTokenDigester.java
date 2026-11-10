package com.boilerplate.application.auth;

/** Outbound port for hashing opaque refresh tokens before persistence or lookup. */
public interface RefreshTokenDigester {

    String digest(String rawToken);
}
