package com.diddycart.modules.identity.service;

import com.diddycart.modules.identity.dto.authentication.AuthResponse;
import com.diddycart.modules.identity.dto.authentication.ForgotPasswordRequest;
import com.diddycart.modules.identity.dto.authentication.LoginRequest;
import com.diddycart.modules.identity.dto.authentication.RegisterRequest;
import com.diddycart.modules.identity.dto.profile.UserProfileRequest;
import com.diddycart.modules.identity.dto.profile.UserProfileResponse;
import com.diddycart.modules.identity.events.PasswordResetEvent;
import com.diddycart.modules.identity.events.UserRegisteredEvent;
import com.diddycart.modules.identity.models.UserRole;
import com.diddycart.modules.identity.models.User;
import com.diddycart.modules.sales.models.Cart;
import com.diddycart.modules.sales.repository.CartRepository;

import jakarta.transaction.Transactional;

import com.diddycart.modules.identity.repository.UserRepository;
import com.diddycart.common.infrastructure.EventProducer;
import com.diddycart.common.security.JwtUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
    private EventProducer eventProducer;

    @Autowired
    private JwtUtil jwtUtil;

    // User Registration by RegisterRequest (Auto-login after registration)
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

        // Create empty cart for the user by userId
        Cart cart = new Cart();
        cart.setUser(savedUser);
        cartRepository.save(cart);

        // Create User Registered Event
        UserRegisteredEvent event = new UserRegisteredEvent(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName());

        // Send User Registered Event to Kafka
        eventProducer.sendUserRegistered(event);

        // Generate JWT token for auto-login by userId and role
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getRole().name());

        // Return AuthResponse with token by userId and name
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setName(savedUser.getName());
        response.setUserId(savedUser.getId());

        return response;
    }

    // User Login by LoginRequest
    public AuthResponse login(LoginRequest request) {
        // Authenticate
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        // Fetch User details by email from UserRepository
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate JWT token by userId and role
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());

        // Return Response by userId and name
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setName(user.getName());
        response.setUserId(user.getId());

        return response;
    }

    // Fetch User Profile by userId (view my profile)
    @Cacheable(value = "user_profile", key = "#userId")
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Map User to UserProfileResponse
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());

        return response;
    }

    // Update user profile by userId and UserProfileRequest
    @CachePut(value = "user_profile", key = "#userId")
    public UserProfileResponse updateUserProfile(Long userId, UserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update name if provided in UserProfileRequest
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }

        // Update phone if provided in UserProfileRequest
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone());
        }

        // Update email if provided in UserProfileRequest (Check for uniqueness)
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Only check if email is actually changing
            if (!request.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new RuntimeException("Email is already in use by another account");
                }
                user.setEmail(request.getEmail());
            }
        }

        // Update password if provided in UserProfileRequest
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Save and Return by updatedUser
        User updatedUser = userRepository.save(user);

        UserProfileResponse response = new UserProfileResponse();
        response.setId(updatedUser.getId());
        response.setName(updatedUser.getName());
        response.setEmail(updatedUser.getEmail());
        response.setPhone(updatedUser.getPhone());
        response.setRole(updatedUser.getRole());

        return response;
    }

    // Forgot Password - Generate reset token and send email
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // If user not found, silently return
        if (user == null) {
            return;
        }

        // Generate reset token and set expiry (15 minutes from now)
        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordExpiresAt(LocalDateTime.now().plusMinutes(15).format(DateTimeFormatter.ISO_DATE_TIME));
        userRepository.save(user);

        // Produce PasswordResetEvent to Kafka
        eventProducer.sendPasswordResetEvent(new PasswordResetEvent(user.getEmail(), token));
    }

    // Reset Password - Verify token and update password
    @Transactional
    public void resetPassword(com.diddycart.modules.identity.dto.authentication.ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset token"));

        // Check if token has expired
        if (user.getResetPasswordExpiresAt() != null &&
                LocalDateTime.parse(user.getResetPasswordExpiresAt(), DateTimeFormatter.ISO_DATE_TIME)
                        .isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token has expired");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // Clear the token fields so it can't be used again
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiresAt(null);

        userRepository.save(user);
    }
}