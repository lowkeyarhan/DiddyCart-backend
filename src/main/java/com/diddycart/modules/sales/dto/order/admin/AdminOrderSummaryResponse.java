package com.diddycart.modules.sales.dto.order.admin;

import com.diddycart.modules.payment.models.PaymentStatus;
import com.diddycart.modules.sales.models.OrderStatus;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

// Data Transfer Object for admin order summary responses
// What the backend sends to the frontend when admin order summary information is requested.

@Data
public class AdminOrderSummaryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String customerEmail;
    private String orderDate;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
}