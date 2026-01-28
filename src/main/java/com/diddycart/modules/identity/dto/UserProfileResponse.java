package com.diddycart.modules.identity.dto;

import java.io.Serializable;

import com.diddycart.modules.identity.models.UserRole;
import lombok.Data;

// Data Transfer Object for user profile responses
// What the backend sends to the frontend when user profile information is requested.

@Data
public class UserProfileResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
}