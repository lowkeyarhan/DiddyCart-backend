package com.diddycart.modules.identity.dto;

import lombok.Data;

// Data Transfer Object for vendor responses
// What the backend sends to the frontend regarding vendor information.

@Data
public class VendorResponse {
    private Long id;
    private Long userId;
    private String storeName;
    private String gstin;
    private String description;
    private String userEmail;
    private String userName;
}
