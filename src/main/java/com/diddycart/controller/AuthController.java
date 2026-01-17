package com.diddycart.controller;

import com.diddycart.dto.user.AuthResponse;
import com.diddycart.dto.user.LoginRequest;
import com.diddycart.dto.user.RegisterRequest;
import com.diddycart.dto.user.UserProfileResponse;
import com.diddycart.service.AuthService;
import com.diddycart.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    // User Registration
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    // User Login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Get My Profile
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long userId = jwtUtil.extractUserId(jwt);
        return ResponseEntity.ok(authService.getUserProfile(userId));
    }
}