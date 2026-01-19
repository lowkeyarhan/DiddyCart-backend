package com.diddycart.dto.orders;

import com.diddycart.enums.OrderStatus;
import com.diddycart.enums.PaymentStatus;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Data Transfer Object for order responses
// What the backend sends to the frontend regarding order information.

@Data
public class OrderResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private Long userId;
    private Instant orderDate;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String shippingAddress;
    private List<OrderItemResponse> items;
}
