package com.diddycart.modules.identity.dto;

import lombok.Data;
import java.io.Serializable;

// Data Transfer Object for vendor profile responses
// What the backend sends to the frontend regarding vendor profile information.

@Data
public class VendorProfileResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String storeName;
    private String gstin;
    private String description;
}