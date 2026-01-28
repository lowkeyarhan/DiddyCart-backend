package com.diddycart.modules.products.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

// Data Transfer Object for product responses
// What the backend sends to the frontend when product information is requested during view product.

@Data
public class ProductResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String categoryName;
    private String vendorStoreName;
    private List<String> imageUrls; // List of URLs only
}