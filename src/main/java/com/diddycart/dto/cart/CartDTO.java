package com.diddycart.dto.cart;

import lombok.Data;
import java.util.List;
import java.math.BigDecimal;

@Data
public class CartDTO {
    private Long cartId;
    private List<CartItemDTO> items;
    private BigDecimal totalAmount; // Sum of all items
}