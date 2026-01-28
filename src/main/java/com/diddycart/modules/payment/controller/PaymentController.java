package com.diddycart.modules.payment.controller;

import com.diddycart.modules.payment.dto.PaymentResponse;
import com.diddycart.modules.sales.models.Order;
import com.diddycart.modules.sales.repository.OrderRepository;
import com.diddycart.modules.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    // Init Payment by orderId and token (Creates Order)
    @PostMapping("/init/{orderId}")
    public ResponseEntity<PaymentResponse> createOrder(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String token) {
        PaymentResponse response = paymentService.createRazorpayOrder(orderId);
        // Store token temporarily with orderId for callback by token
        response.setToken(token.substring(7));
        return ResponseEntity.ok(response);
    }

    // Payment Callback by razorpayOrderId, razorpayPaymentId, razorpaySignature
    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RedirectView paymentCallback(
            @RequestParam("razorpay_order_id") String razorpayOrderId,
            @RequestParam("razorpay_payment_id") String razorpayPaymentId,
            @RequestParam("razorpay_signature") String razorpaySignature) {

        try {
            Long orderId = paymentService.verifyPaymentCallbackAndGetOrderId(razorpayOrderId, razorpayPaymentId,
                    razorpaySignature);

            if (orderId != null) {
                // Get the order from OrderRepository by orderId
                Order order = orderRepository.findById(orderId).orElse(null);
                if (order != null) {
                    // Pass orderId to success page by orderId
                    return new RedirectView("/payment-success?orderId=" + orderId);
                }
                return new RedirectView("/payment-failure?reason=Order not found");
            } else {
                return new RedirectView("/payment-failure?reason=Payment verification failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new RedirectView("/payment-failure?error=" + e.getMessage());
        }
    }
}