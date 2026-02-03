package com.diddycart.modules.identity.dto.address;

import com.diddycart.modules.identity.models.AddressLabel;
import lombok.Data;
import java.io.Serializable;

// Data Transfer Object for address summary responses
// What the backend sends to the frontend when user wants to view all their saved addresses.

@Data
public class AddressSummaryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private AddressLabel label;
    private String city;
    private String state;
    private String country;
    private String pincode;
}