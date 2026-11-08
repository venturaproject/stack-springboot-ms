package com.boilerplate.application.auth;

import com.boilerplate.domain.auth.Role;
import com.boilerplate.domain.auth.RoleRepository;
import com.boilerplate.domain.exception.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roleRepository.findAll().stream().map(RoleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(UUID id) {
        return RoleResponse.from(findById(id));
    }

    private Role findById(UUID id) {
        return roleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Role not found: " + id));
    }
}
