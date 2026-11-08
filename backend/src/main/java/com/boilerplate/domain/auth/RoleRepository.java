package com.boilerplate.domain.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface RoleRepository extends Repository<Role, UUID> {

    Optional<Role> findById(UUID id);

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    List<Role> findAll();

    Role save(Role role);
}
