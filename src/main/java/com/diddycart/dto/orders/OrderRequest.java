package com.diddycart.dto.orders;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Data Transfer Object for order requests
// What the frontend sends to the backend when placing an order.

@Data
public class OrderRequest {

    @NotNull(message = "Address ID is required")
    private Long addressId;
}