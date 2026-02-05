package com.diddycart.common.security;

import com.diddycart.common.infrastructure.RateLimiterService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String key;
        boolean isAuthenticated = false;
        String authHeader = request.getHeader("Authorization");

        // Identify user via JWT or fallback to IP address
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    Long userId = jwtUtil.extractUserId(token);
                    key = "user:" + userId;
                    isAuthenticated = true;
                } else {
                    key = "ip:" + request.getRemoteAddr();
                }
            } catch (Exception e) {
                key = "ip:" + request.getRemoteAddr();
            }
        } else {
            key = "ip:" + request.getRemoteAddr();
        }

        Bucket bucket = rateLimiterService.resolveBucket(key, isAuthenticated);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers and proceed
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Block request and set retry-after header
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("X-Rate-Limit-Retry-After-Seconds",
                    String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            response.getWriter().write("Too many requests. Please slow down.");
        }
    }
}
