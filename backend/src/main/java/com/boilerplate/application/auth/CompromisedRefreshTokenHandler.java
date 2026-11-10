package com.boilerplate.application.auth;

import com.boilerplate.domain.auth.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The breach response to refresh-token reuse. In its own transaction ({@code REQUIRES_NEW})
 * so revoking the family and recording the {@code RefreshTokenReused} event commit even
 * though the refresh call then fails with 401.
 */
@Component
@RequiredArgsConstructor
class CompromisedRefreshTokenHandler {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void revokeFamilyAndFlagUser(UUID familyId, UUID userId) {
        userRepository.findDetailedById(userId).ifPresent(user -> {
            user.revokeRefreshFamily(familyId);
            user.reportRefreshTokenReuse();
            userRepository.save(user);
        });
    }
}
