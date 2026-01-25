package com.diddycart.dto.payment; // New package suggested

import com.diddycart.enums.PaymentMode;
import com.diddycart.enums.PaymentStatus;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class PaymentResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private PaymentMode mode;
    private PaymentStatus status;
    private String transactionId;
    private Instant createdAt;
    private String token; // JWT token for success page
}