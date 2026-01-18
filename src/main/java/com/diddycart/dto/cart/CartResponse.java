package com.diddycart.dto.cart;

import lombok.Data;
import java.util.List;
import java.math.BigDecimal;

@Data
public class CartResponse {
    private Long cartId;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
}
