package com.diddycart.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        String token = null;
        String username = null;

        // 1. Check Bearer token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.extractEmail(token);
            } catch (Exception e) {
                System.out.println("JWT Extraction Error: " + e.getMessage());
            }
        }

        // 2. If token is valid and no authentication exists yet
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load user from DB
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 3. Validate Token
            if (jwtUtil.validateToken(token, userDetails.getUsername())) {

                String role = jwtUtil.extractRole(token);

                // Fix: Spring Security expects "ROLE_USER", but token has "USER"
                SimpleGrantedAuthority authority = role.startsWith("ROLE_")
                        ? new SimpleGrantedAuthority(role)
                        : new SimpleGrantedAuthority("ROLE_" + role);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        List.of(authority));

                // 4. Set the Authentication Context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}