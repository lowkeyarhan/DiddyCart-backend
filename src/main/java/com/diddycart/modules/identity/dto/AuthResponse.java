package com.diddycart.modules.identity.dto;

import lombok.Data;

// Data Transfer Object for authentication responses
// What the backend sends to the frontend after a successful login or registration.

@Data
public class AuthResponse {
    private String token; // The JWT Token
    private String name;
    private Long userId;
}