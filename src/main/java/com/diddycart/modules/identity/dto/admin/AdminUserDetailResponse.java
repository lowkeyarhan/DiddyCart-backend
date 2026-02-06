package com.diddycart.modules.identity.dto.admin;

import com.diddycart.modules.identity.models.UserRole;
import com.diddycart.modules.sales.dto.order.OrderListResponse;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

// Data Transfer Object for admin user detail responses
// What the backend sends to the frontend when admin user detail information is requested.

@Data
public class AdminUserDetailResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    // User Details
    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private String createdAt;

    // Order History
    private List<OrderListResponse> orders;
}