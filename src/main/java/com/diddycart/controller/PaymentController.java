package com.diddycart.controller;

import com.diddycart.dto.payment.PaymentResponse;
import com.diddycart.models.Order;
import com.diddycart.repository.OrderRepository;
import com.diddycart.service.PaymentService;
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

    // Init Payment (Creates Order)
    @PostMapping("/init/{orderId}")
    public ResponseEntity<PaymentResponse> createOrder(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String token) {
        PaymentResponse response = paymentService.createRazorpayOrder(orderId);
        // Store token temporarily with orderId for callback
        response.setToken(token.substring(7)); // Remove "Bearer " prefix
        return ResponseEntity.ok(response);
    }

    // Callback URL (The "Demo" approach)
    // Razorpay redirects here with form data. We verify -> Redirect user to HTML
    // page
    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RedirectView paymentCallback(
            @RequestParam("razorpay_order_id") String razorpayOrderId,
            @RequestParam("razorpay_payment_id") String razorpayPaymentId,
            @RequestParam("razorpay_signature") String razorpaySignature) {

        try {
            Long orderId = paymentService.verifyPaymentCallbackAndGetOrderId(razorpayOrderId, razorpayPaymentId,
                    razorpaySignature);

            if (orderId != null) {
                // Get the user ID from order to generate a token reference
                Order order = orderRepository.findById(orderId).orElse(null);
                if (order != null) {
                    // Pass orderId to success page - user can use their existing JWT from
                    // localStorage
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