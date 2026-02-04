package com.diddycart.modules.sales.dto.order;

import com.diddycart.modules.payment.models.PaymentStatus;
import com.diddycart.modules.payment.models.PaymentMode;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

// Data Transfer Object for detailed order responses
// What the backend sends to the frontend regarding detailed order information.

@Data
public class OrderDetailResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderDate;
    private String shippingAddress;
    private PaymentStatus paymentStatus;
    private PaymentMode paymentMode;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
}
