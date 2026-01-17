package com.diddycart.dto.user;

import com.diddycart.enums.UserRole;
import lombok.Data;

// Data Transfer Object for user profile responses
// What the backend sends to the frontend when user profile information is requested.

@Data
public class UserProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
}