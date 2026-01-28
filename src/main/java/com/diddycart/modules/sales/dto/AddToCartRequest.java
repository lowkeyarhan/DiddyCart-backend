package com.diddycart.modules.sales.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Data Transfer Object for add to cart requests
// What the frontend sends to the backend when a user wants to add an item to their cart

@Data
public class AddToCartRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
