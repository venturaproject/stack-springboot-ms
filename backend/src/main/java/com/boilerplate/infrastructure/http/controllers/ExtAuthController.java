package com.boilerplate.infrastructure.http.controllers;

import com.boilerplate.application.api.ApiClientService;
import com.boilerplate.application.api.IssueTokenCommand;
import com.boilerplate.application.api.RefreshTokenCommand;
import com.boilerplate.application.api.TokenResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ext/auth")
@RequiredArgsConstructor
@Tag(name = "External Auth", description = "Token endpoints for external API clients")
public class ExtAuthController {

    private final ApiClientService apiClientService;

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(@Valid @RequestBody IssueTokenCommand cmd) {
        return ResponseEntity.ok(apiClientService.issueToken(cmd));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenCommand cmd) {
        return ResponseEntity.ok(apiClientService.refreshToken(cmd));
    }
}
