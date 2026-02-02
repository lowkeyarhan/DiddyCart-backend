package com.diddycart.modules.payment.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentFailedEvent {
    private Long orderId;
    private Long userId;
    private String email;
    private BigDecimal amount;
    private String paymentMode;
}