package com.boilerplate.infrastructure.http.controllers;

import com.boilerplate.application.PagedResponse;
import com.boilerplate.application.auth.CreateUserCommand;
import com.boilerplate.application.auth.UpdateUserCommand;
import com.boilerplate.application.auth.UserResponse;
import com.boilerplate.application.auth.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    @Operation(summary = "List all users (paginated)")
    public ResponseEntity<PagedResponse<UserResponse>> list(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.list(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users:read')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('users:write')")
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(command));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('users:write')")
    @Operation(summary = "Update a user")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserCommand command) {
        return ResponseEntity.ok(userService.update(id, command));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('users:write')")
    public ResponseEntity<UserResponse> patch(@PathVariable UUID id, @Valid @RequestBody UpdateUserCommand command) {
        return ResponseEntity.ok(userService.update(id, command));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('users:delete')")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
