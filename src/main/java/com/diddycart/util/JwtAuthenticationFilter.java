package com.diddycart.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    // Main filter method
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Get Authorization header
        String authHeader = request.getHeader("Authorization");

        String token = null;
        Long userId = null;

        // 1. Check for Bearer token and extract userId
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                userId = jwtUtil.extractUserId(token);
            } catch (Exception e) {
                System.out.println("JWT Extraction Error: " + e.getMessage());
            }
        }

        // 2. If token is valid and no authentication exists yet, authenticate
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 3. Validate Token
            if (jwtUtil.validateToken(token)) {

                // Extract role and create authority
                String role = jwtUtil.extractRole(token);
                SimpleGrantedAuthority authority = role.startsWith("ROLE_")
                        ? new SimpleGrantedAuthority(role)
                        : new SimpleGrantedAuthority("ROLE_" + role);

                // Create authentication token with userId as principal
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(authority));

                // 4. Set the Authentication Context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}