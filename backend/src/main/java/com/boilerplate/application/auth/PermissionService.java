package com.boilerplate.application.auth;

import com.boilerplate.domain.auth.PermissionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public List<PermissionResponse> list() {
        return permissionRepository.findAll().stream()
                .map(PermissionResponse::from)
                .toList();
    }
}
