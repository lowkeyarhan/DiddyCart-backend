package com.diddycart.modules.sales.dto;

import lombok.Data;
import java.util.List;
import java.io.Serializable;
import java.math.BigDecimal;

// Data Transfer Object for cart responses
// What the backend sends to the frontend regarding cart information.

@Data
public class CartResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long cartId;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
}
