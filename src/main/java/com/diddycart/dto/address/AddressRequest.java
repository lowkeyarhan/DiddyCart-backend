package com.diddycart.dto.address;

import com.diddycart.enums.AddressLabel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

// Data Transfer Object for address requests
// What the frontend sends to the backend when a user wants to add or update an address.

@Data
public class AddressRequest {

    @NotNull(message = "Address label is required")
    private AddressLabel label;

    @NotBlank(message = "Street is required")
    private String street;

    private String landmark;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^\\d{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    private String phone;

    private String alternatePhone;
}
