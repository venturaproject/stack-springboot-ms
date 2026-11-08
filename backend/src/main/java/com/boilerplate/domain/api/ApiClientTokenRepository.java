package com.boilerplate.domain.api;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface ApiClientTokenRepository extends Repository<ApiClientToken, UUID> {

    Optional<ApiClientToken> findByAccessTokenHash(String accessTokenHash);

    Optional<ApiClientToken> findByRefreshTokenHash(String refreshTokenHash);

    ApiClientToken save(ApiClientToken token);

    void delete(ApiClientToken token);

    @Modifying
    @Query("DELETE FROM ApiClientToken t WHERE t.clientId = :clientId")
    void deleteByClientId(String clientId);
}
