package com.diddycart.modules.identity.dto.admin;

import lombok.Data;
import java.io.Serializable;

// Data Transfer Object for admin vendor summary responses
// What the backend sends to the frontend when admin vendor summary information is requested.

@Data
public class AdminVendorSummaryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id; // Vendor ID
    private String name; // User Name
    private String email; // User Email
    private String storeName;
}