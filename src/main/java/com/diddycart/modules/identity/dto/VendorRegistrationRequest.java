package com.diddycart.modules.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

// Data Transfer Object for vendor registration requests
// What the frontend sends to the backend when a user wants to register as a vendor.

@Data
public class VendorRegistrationRequest {

    @NotBlank(message = "Store name is required")
    private String storeName;

    @NotBlank(message = "GSTIN is required")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
    private String gstin;

    private String description;
}
