package com.diddycart.modules.identity.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

// Data Transfer Object for user registration requests
// What the frontend sends to the backend when a user registers.

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Pattern(regexp = "^\\d{10}$", message = "Phone must be exactly 10 digits")
    private String phone;
}