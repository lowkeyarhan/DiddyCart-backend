package com.diddycart.modules.sales.dto.order;

import com.diddycart.modules.sales.models.OrderStatus;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

// DTO for listing orders (used in getOrders for user/admin)
// What the backend sends to the frontend when request all my orders

@Data
public class OrderListResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private String orderDate;
    private String shippingAddress;
    private OrderStatus status;
    private BigDecimal bill;
    private List<OrderItemResponse> items;
}
