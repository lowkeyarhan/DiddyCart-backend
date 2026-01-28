package com.diddycart.modules.sales.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

// Data Transfer Object for cart item responses
// What the backend sends to the frontend regarding individual items in a cart.

@Data
public class CartItemResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subTotal;
}
