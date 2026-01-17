package com.diddycart.service;

import com.diddycart.dto.user.AuthResponse;
import com.diddycart.dto.user.LoginRequest;
import com.diddycart.dto.user.RegisterRequest;
import com.diddycart.enums.UserRole;
import com.diddycart.models.User;
import com.diddycart.models.Cart;
import com.diddycart.repository.CartRepository;
import com.diddycart.repository.UserRepository;
import com.diddycart.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository; // To create an empty cart on register

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    // User Registration (Auto-login after registration)
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER); // Default role

        User savedUser = userRepository.save(user);

        Cart cart = new Cart();
        cart.setUser(savedUser);
        cartRepository.save(cart);

        // Generate JWT token for auto-login
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());

        // Return AuthResponse with token
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setName(savedUser.getName());
        response.setRole(savedUser.getRole());
        response.setUserId(savedUser.getId());

        return response;
    }

    // User Login
    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        // 2. Fetch User
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Generate Token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        // 4. Return Response
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setName(user.getName());
        response.setRole(user.getRole());
        response.setUserId(user.getId());

        return response;
    }
}