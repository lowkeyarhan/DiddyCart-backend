package com.diddycart.dto.orders;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private String productName;
    private BigDecimal price; // The price they PAID (Snapshot)
    private Integer quantity;
}