package com.diddycart.modules.sales.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPlacedEvent {
    private Long orderId;
    private Long userId;
    private String email;
    private BigDecimal amount;
    private String paymentMode;
    private List<ItemDetail> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ItemDetail {
        private String name;
        private int quantity;
        private BigDecimal price;
    }
}