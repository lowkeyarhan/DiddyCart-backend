package com.diddycart.service;

import com.diddycart.enums.PaymentMode;
import com.diddycart.enums.PaymentStatus;
import com.diddycart.models.Order;
import com.diddycart.models.Payment;
import com.diddycart.repository.OrderRepository;
import com.diddycart.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    // Process Payment for an Order
    @Transactional
    public Payment processPayment(Long orderId, String mode) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new RuntimeException("Order is already paid for");
        }

        // Simulate Payment Processing
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotal());
        payment.setMode(PaymentMode.valueOf(mode.toUpperCase())); // e.g. "CARD", "UPI"
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(UUID.randomUUID().toString());

        // Update Order Status
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        orderRepository.save(order);

        return paymentRepository.save(payment);
    }
}