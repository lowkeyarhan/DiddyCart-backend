package com.diddycart.modules.identity.service;

import com.diddycart.modules.identity.dto.AuthResponse;
import com.diddycart.modules.identity.dto.LoginRequest;
import com.diddycart.modules.identity.dto.RegisterRequest;
import com.diddycart.modules.identity.dto.UserProfileRequest;
import com.diddycart.modules.identity.dto.UserProfileResponse;
import com.diddycart.modules.identity.models.UserRole;
import com.diddycart.modules.identity.models.User;
import com.diddycart.modules.sales.models.Cart;
import com.diddycart.modules.sales.repository.CartRepository;
import com.diddycart.modules.identity.repository.UserRepository;
import com.diddycart.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

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
        user.setRole(UserRole.USER);

        User savedUser = userRepository.save(user);

        // Create empty cart for the user
        Cart cart = new Cart();
        cart.setUser(savedUser);
        cartRepository.save(cart);

        // Generate JWT token for auto-login
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getRole().name());

        // Return AuthResponse with token
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setName(savedUser.getName());
        response.setUserId(savedUser.getId());

        return response;
    }

    // User Login
    public AuthResponse login(LoginRequest request) {
        // Authenticate
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        // Fetch User details by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());

        // Return Response
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setName(user.getName());
        response.setUserId(user.getId());

        return response;
    }

    // Fetch User Profile (view my profile)
    @Cacheable(value = "user_profile", key = "#userId")
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());

        return response;
    }

    // Update user profile
    @CachePut(value = "user_profile", key = "#userId")
    public UserProfileResponse updateUserProfile(Long userId, UserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update name if provided
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }

        // Update phone if provided
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone());
        }

        // Update email if provided (Check for uniqueness)
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Only check if email is actually changing
            if (!request.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new RuntimeException("Email is already in use by another account");
                }
                user.setEmail(request.getEmail());
            }
        }

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Save and Return
        User updatedUser = userRepository.save(user);

        UserProfileResponse response = new UserProfileResponse();
        response.setId(updatedUser.getId());
        response.setName(updatedUser.getName());
        response.setEmail(updatedUser.getEmail());
        response.setPhone(updatedUser.getPhone());
        response.setRole(updatedUser.getRole());

        return response;
    }
}