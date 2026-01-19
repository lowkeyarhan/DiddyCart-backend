package com.diddycart.controller;

import com.diddycart.dto.payment.PaymentResponse;
import com.diddycart.enums.PaymentMode;
import com.diddycart.service.OrderService;
import com.diddycart.service.PaymentService;
import com.diddycart.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

    // Process Payment for an Order
    @PostMapping("/process/{orderId}")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable Long orderId,
            @RequestParam PaymentMode mode) {

        return ResponseEntity.ok(paymentService.processPayment(orderId, mode));
    }

    // Get Payment Details by Order ID
    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentDetails(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String token) {

        Long userId = jwtUtil.extractUserId(token.substring(7));
        orderService.getOrderById(orderId, userId);

        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }
}
