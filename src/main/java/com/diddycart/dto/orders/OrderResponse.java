package com.diddycart.dto.orders;

import com.diddycart.enums.OrderStatus;
import com.diddycart.enums.PaymentStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class OrderResponse {
    private Long orderId;
    private Instant orderDate;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String shippingAddress;
    private List<OrderItemResponse> items;
}
