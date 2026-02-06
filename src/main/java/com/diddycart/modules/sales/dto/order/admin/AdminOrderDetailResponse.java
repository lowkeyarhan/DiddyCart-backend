package com.diddycart.modules.sales.dto.order.admin;

import com.diddycart.modules.payment.models.PaymentMode;
import com.diddycart.modules.payment.models.PaymentStatus;
import com.diddycart.modules.sales.dto.order.OrderItemResponse;
import com.diddycart.modules.sales.models.OrderStatus;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

// Data Transfer Object for admin order detail responses
// What the backend sends to the frontend when admin order detail information is requested.

@Data
public class AdminOrderDetailResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderDate;
    private OrderStatus status;

    // Customer Details
    private Long userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Payment Details
    private PaymentStatus paymentStatus;
    private PaymentMode paymentMode;
    private String transactionId;

    // Shipping
    private String shippingAddress;

    // Items
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
}