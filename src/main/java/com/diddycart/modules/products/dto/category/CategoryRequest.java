package com.diddycart.modules.products.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// Data Transfer Object for category creation or update requests
// What the frontend sends to the backend when a vendor/admin is creating or updating a category.

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name (type) is required")
    private String type;

    private String description;
}