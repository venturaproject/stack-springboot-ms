package com.boilerplate.domain.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface PermissionRepository extends Repository<Permission, UUID> {

    Optional<Permission> findByName(String name);

    List<Permission> findAll();

    Permission save(Permission permission);
}
