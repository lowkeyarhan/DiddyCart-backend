package com.diddycart.dto.orders;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

// Data Transfer Object for order placement requests
// What the frontend sends to the backend when a user places an order.

@Data
public class OrderRequest {

    @NotBlank(message = "Street address is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^\\d{6}$", message = "Pincode must be 6 digits") // India specific
    private String pincode;

    @NotBlank(message = "Phone number is required")
    private String phone;
}