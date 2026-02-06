package com.diddycart.modules.payment.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundRequestedEvent {
    private Long orderId;
    private Long userId;
    private String email;
    private BigDecimal amount;
    private String paymentMode;
    private String transactionId;
}
