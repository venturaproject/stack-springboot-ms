package com.boilerplate.application.auth;

import com.boilerplate.config.AppProperties;
import com.boilerplate.domain.auth.Email;
import com.boilerplate.domain.auth.RefreshToken;
import com.boilerplate.domain.auth.RefreshTokenRepository;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final CompromisedRefreshTokenHandler compromisedRefreshTokenHandler;
    private final AuthTokenIssuer tokenIssuer;
    private final AppProperties appProperties;

    @Transactional
    public LoginResult login(LoginCommand command) {
        credentialsAuthenticator.authenticate(command.email(), command.password());

        User user = userRepository
                .findDetailedByEmail(new Email(command.email()))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String access = tokenIssuer.generateAccessToken(user);
        String refresh = tokenIssuer.generateRefreshToken(user);
        refreshTokenRepository.save(RefreshToken.startFamily(refresh, user, refreshExpiry()));

        return new LoginResult(UserResponse.from(user), tokens(access, refresh));
    }

    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        RefreshToken presented = refreshTokenRepository
                .findByToken(rawRefreshToken)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid refresh token"));

        if (presented.isRevoked()) {
            // A revoked token was replayed — assume the whole family leaked. The breach
            // response commits in its own transaction; this call still fails with 401.
            compromisedRefreshTokenHandler.revokeFamilyAndFlagUser(
                    presented.getFamilyId(), presented.getUser().getId());
            log.warn("refresh-token reuse detected for family {}", presented.getFamilyId());
            throw new AuthenticationFailedException("Refresh token reuse detected");
        }
        if (presented.isExpired()) {
            throw new AuthenticationFailedException("Refresh token expired");
        }

        User user = userRepository
                .findDetailedById(presented.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        presented.revoke();
        String access = tokenIssuer.generateAccessToken(user);
        String refresh = tokenIssuer.generateRefreshToken(user);
        refreshTokenRepository.save(presented.rotate(refresh, refreshExpiry()));

        return new LoginResult(UserResponse.from(user), tokens(access, refresh));
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
