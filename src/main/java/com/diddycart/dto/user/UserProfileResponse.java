package com.diddycart.dto.user;

import java.io.Serializable;

import com.diddycart.enums.UserRole;
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