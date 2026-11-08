package com.boilerplate.infrastructure.http.middleware;

import com.boilerplate.application.api.ApiClientService;
import com.boilerplate.application.api.ApiTokenPrincipal;
import com.boilerplate.infrastructure.security.ApiClientAuthentication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiClientAuthFilter extends OncePerRequestFilter {

    private final ApiClientService apiClientService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String rawToken = header.substring(7);
            try {
                ApiTokenPrincipal principal = apiClientService.validateAccessToken(rawToken);
                SecurityContextHolder.getContext()
                        .setAuthentication(new ApiClientAuthentication(principal.clientId(), principal.scopes()));
            } catch (Exception e) {
                log.trace("API client token validation failed: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}
