package com.diddycart.dto.cart;

import lombok.Data;
import java.math.BigDecimal;

// Data Transfer Object for cart item representation
// What the backend sends to the frontend when displaying items in the shopping cart.

@Data
public class CartItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage; // Thumbnail
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subTotal; // (price * quantity) - Calculated
}