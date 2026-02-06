package com.diddycart.modules.products.dto.category;

import lombok.Data;
import java.io.Serializable;

// Data Transfer Object for category responses
// What the backend sends to the frontend when category information is requested during view category.

@Data
public class CategoryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String type;
    private String description;
}