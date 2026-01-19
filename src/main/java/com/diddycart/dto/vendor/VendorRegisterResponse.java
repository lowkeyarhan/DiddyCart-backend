package com.diddycart.dto.vendor;

import java.io.Serializable;

import lombok.Data;

// Data Transfer Object for vendor responses
// What the backend sends to the frontend regarding vendor information.

@Data
public class VendorRegisterResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String newToken;
    private String storeName;
    private String gstin;
    private String description;
    private String userEmail;
    private String userName;
}
