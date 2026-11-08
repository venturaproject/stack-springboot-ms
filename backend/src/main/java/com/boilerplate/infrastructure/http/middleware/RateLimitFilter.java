package com.boilerplate.infrastructure.http.middleware;

import com.boilerplate.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-IP token-bucket rate limiting on the login endpoints, ahead of the security chain so
 * a flood never reaches Argon2 or the database. Exceeded → {@code 429} with {@code Retry-After}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of("/api/auth/login", "/api/ext/auth/token");

    private final AppProperties appProperties;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && LIMITED_PATHS.contains(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        AppProperties.Security.RateLimit cfg = appProperties.security().rateLimit();
        String key = request.getRequestURI() + '|' + clientIp(request);
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(cfg.capacity(), cfg.refillPerMinute()));

        if (bucket.tryConsume()) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");
        response.getWriter().write("{\"message\":\"Too many requests\"}");
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    /** Evict idle buckets so the map does not grow without bound. */
    @Scheduled(fixedDelay = 600_000L)
    void sweepIdleBuckets() {
        buckets.values().removeIf(TokenBucket::isFull);
    }
}
