package com.diddycart.controller;

import com.diddycart.enums.PaymentMode;
import com.diddycart.models.Payment;
import com.diddycart.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Process Payment for an Order
    @PostMapping("/process/{orderId}")
    public ResponseEntity<Payment> processPayment(
            @PathVariable Long orderId,
            @RequestParam PaymentMode mode) {

        return ResponseEntity.ok(paymentService.processPayment(orderId, mode));
    }
}
