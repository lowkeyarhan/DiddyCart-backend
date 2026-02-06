package com.diddycart.modules.identity.dto.admin;

import com.diddycart.modules.identity.models.UserRole;
import lombok.Data;
import java.io.Serializable;

// Data Transfer Object for admin user summary responses
// What the backend sends to the frontend when admin user summary information is requested.

@Data
public class AdminUserSummaryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String email;
    private UserRole role;
}