package com.boilerplate.infrastructure.http.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Makes the security filter chain speak the same {@code application/problem+json} dialect as
 * {@link GlobalExceptionHandler}. Without this, an unauthenticated or forbidden request would
 * fall through to the servlet container's error page instead of a problem document.
 */
@Component
@RequiredArgsConstructor
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        write(
                response,
                HttpStatus.UNAUTHORIZED,
                "authentication-required",
                "Authentication required",
                "This endpoint requires a valid session or token");
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        write(
                response,
                HttpStatus.FORBIDDEN,
                "access-denied",
                "Access denied",
                "You do not have permission to perform this action");
    }

    private void write(HttpServletResponse response, HttpStatus status, String typeSlug, String title, String detail)
            throws IOException {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setType(URI.create("urn:problem-type:" + typeSlug));
        body.setTitle(title);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
