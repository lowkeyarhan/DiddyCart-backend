package com.diddycart.modules.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// Data Transfer Object for user login requests
// What the frontend sends to the backend when a user logs in.

@Data
public class LoginRequest {
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}