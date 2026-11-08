package com.boilerplate.domain.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface ApiClientRepository extends Repository<ApiClient, UUID> {

    Optional<ApiClient> findById(UUID id);

    Optional<ApiClient> findByClientId(String clientId);

    List<ApiClient> findAll();

    ApiClient save(ApiClient client);
}
