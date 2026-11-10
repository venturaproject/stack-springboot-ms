package com.boilerplate.application.auth;

import com.boilerplate.config.AppProperties;
import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.auth.RefreshToken;
import com.boilerplate.domain.auth.User;
import com.boilerplate.domain.auth.UserRepository;
import com.boilerplate.domain.exception.AuthenticationFailedException;
import com.boilerplate.domain.exception.EntityNotFoundException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final CredentialsAuthenticator credentialsAuthenticator;
    private final UserRepository userRepository;
    private final CompromisedRefreshTokenHandler compromisedRefreshTokenHandler;
    private final AuthTokenIssuer tokenIssuer;
    private final RefreshTokenDigester refreshTokenDigester;
    private final AppProperties appProperties;

    @Transactional
    public LoginResult login(LoginCommand command) {
        credentialsAuthenticator.authenticate(command.email(), command.password());

        User user = userRepository
                .findDetailedByEmail(new Email(command.email()))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String access = tokenIssuer.generateAccessToken(user);
        String refresh = tokenIssuer.generateRefreshToken(user);
        user.startRefreshFamily(refreshTokenDigester.digest(refresh), refreshExpiry());
        userRepository.save(user);

        return new LoginResult(UserResponse.from(user), tokens(access, refresh));
    }

    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        String tokenHash = refreshTokenDigester.digest(rawRefreshToken);
        User user = userRepository
                .findDetailedByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid refresh token"));
        RefreshToken presented = user.refreshToken(tokenHash)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid refresh token"));

        if (presented.isRevoked()) {
            // A revoked token was replayed — assume the whole family leaked. The breach
            // response commits in its own transaction; this call still fails with 401.
            compromisedRefreshTokenHandler.revokeFamilyAndFlagUser(presented.getFamilyId(), user.getId());
            log.warn("refresh-token reuse detected for family {}", presented.getFamilyId());
            throw new AuthenticationFailedException("Refresh token reuse detected");
        }
        if (presented.isExpired()) {
            throw new AuthenticationFailedException("Refresh token expired");
        }

        if (!user.isActive() || user.isLocked()) {
            throw new AuthenticationFailedException("Account is not available");
        }

        String access = tokenIssuer.generateAccessToken(user);
        String refresh = tokenIssuer.generateRefreshToken(user);
        user.rotateRefreshToken(presented, refreshTokenDigester.digest(refresh), refreshExpiry());
        userRepository.save(user);

        return new LoginResult(UserResponse.from(user), tokens(access, refresh));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String tokenHash = refreshTokenDigester.digest(rawRefreshToken);
        userRepository.findDetailedByRefreshTokenHash(tokenHash).ifPresent(user -> {
            user.refreshToken(tokenHash).ifPresent(token -> {
                user.revokeRefreshFamily(token.getFamilyId());
                userRepository.save(user);
            });
        });
    }

    private Instant refreshExpiry() {
        return Instant.now().plusSeconds(appProperties.jwt().refreshTtlSecs());
    }

    private IssuedTokens tokens(String access, String refresh) {
        return new IssuedTokens(
                access, appProperties.jwt().accessTtlSecs(),
                refresh, appProperties.jwt().refreshTtlSecs());
    }
}
