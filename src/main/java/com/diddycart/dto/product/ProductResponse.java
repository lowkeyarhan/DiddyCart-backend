package com.diddycart.dto.product;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

// Data Transfer Object for product responses
// What the backend sends to the frontend when product information is requested during view product.

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String categoryName;
    private String vendorStoreName;
    private List<String> imageUrls; // List of URLs only
}