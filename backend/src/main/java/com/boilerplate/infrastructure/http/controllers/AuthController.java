package com.boilerplate.infrastructure.http.controllers;

import com.boilerplate.application.auth.AuthResponse;
import com.boilerplate.application.auth.AuthService;
import com.boilerplate.application.auth.LoginCommand;
import com.boilerplate.application.auth.LoginResult;
import com.boilerplate.application.auth.UpdateProfileCommand;
import com.boilerplate.application.auth.UserResponse;
import com.boilerplate.application.auth.UserService;
import com.boilerplate.application.storage.ImageUpload;
import com.boilerplate.infrastructure.http.AuthCookieWriter;
import com.boilerplate.infrastructure.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final AuthCookieWriter authCookieWriter;

    @GetMapping("/csrf")
    @Operation(summary = "Get a CSRF token for browser-authenticated requests")
    public ResponseEntity<Map<String, String>> csrf(@RequestAttribute("_csrf") CsrfToken csrfToken) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("token", csrfToken.getToken()));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginCommand command, HttpServletResponse response) {
        LoginResult result = authService.login(command);
        authCookieWriter.writeAuthCookies(response, result.tokens());
        return ResponseEntity.ok(new AuthResponse(result.user()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current user")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        authService.logout(refreshToken);
        authCookieWriter.clearAuthCookies(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using cookie")
    public ResponseEntity<UserResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LoginResult result = authService.refresh(refreshToken);
        authCookieWriter.writeAuthCookies(response, result.tokens());
        return ResponseEntity.ok(result.user());
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(UserResponse.from(principal.domainUser()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal SecurityUser principal, @Valid @RequestBody UpdateProfileCommand command) {
        return ResponseEntity.ok(userService.updateMe(principal.id(), command));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload avatar for current user")
    public ResponseEntity<UserResponse> uploadAvatar(
            @AuthenticationPrincipal SecurityUser principal, @RequestPart("avatar") MultipartFile file) {
        return ResponseEntity.ok(userService.uploadAvatar(principal.id(), toImageUpload(file)));
    }

    private static ImageUpload toImageUpload(MultipartFile file) {
        try {
            return new ImageUpload(file.getBytes(), file.getOriginalFilename(), file.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read uploaded file", e);
        }
    }
}
