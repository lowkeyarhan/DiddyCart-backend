package com.diddycart.modules.products.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

// Data Transfer Object for product creation or update requests
// What the frontend sends to the backend when a vendor/admin is creating or updating a product.

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Available stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Category ID is required")
    private Long categoryId;
}