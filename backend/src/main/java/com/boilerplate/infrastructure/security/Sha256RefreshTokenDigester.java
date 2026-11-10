package com.boilerplate.infrastructure.security;

import com.boilerplate.application.auth.RefreshTokenDigester;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** SHA-256 is appropriate here because refresh tokens are uniformly random 256-bit values. */
@Component
public class Sha256RefreshTokenDigester implements RefreshTokenDigester {

    @Override
    public String digest(String rawToken) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
