package com.diddycart.dto.orders;

import com.diddycart.enums.OrderStatus;
import com.diddycart.enums.PaymentStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class OrderDTO {
    private Long orderId;
    private Instant orderDate;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentStatus paymentStatus;

    // Address Snapshot
    private String shippingAddress; // Full address as a single string

    private List<OrderItemDTO> items;
}