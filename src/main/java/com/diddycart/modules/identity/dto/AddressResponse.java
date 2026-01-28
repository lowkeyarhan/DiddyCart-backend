package com.diddycart.modules.identity.dto;

import java.io.Serializable;

import com.diddycart.modules.identity.models.AddressLabel;
import lombok.Data;

// Data Transfer Object for address responses
// What the backend sends to the frontend regarding address information.

@Data
public class AddressResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private AddressLabel label;
    private String street;
    private String landmark;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String phone;
    private String alternatePhone;
}
