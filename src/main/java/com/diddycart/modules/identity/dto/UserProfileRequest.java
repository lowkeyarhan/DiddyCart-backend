package com.diddycart.modules.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Data Transfer Object for user profile update requests
// What the frontend sends to the backend when a user wants to update their profile.

@Data
public class UserProfileRequest {

    @Size(min = 2, message = "Name must be at least 2 characters")
    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Pattern(regexp = "^\\d{10}$", message = "Phone must be exactly 10 digits")
    private String phone;
}