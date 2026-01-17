package com.diddycart.dto.user;

import com.diddycart.enums.UserRole;
import lombok.Data;

// Data Transfer Object for authentication responses
// What the backend sends to the frontend after a successful login or registration.

@Data
public class AuthResponse {
    private String token; // The JWT Token
    private String name;
    private UserRole role;
    private Long userId;
}