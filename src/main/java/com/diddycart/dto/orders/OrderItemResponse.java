package com.diddycart.dto.orders;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

// Data Transfer Object for order item responses
// What the backend sends to the frontend regarding individual items in an order.

@Data
public class OrderItemResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subTotal;
}
