package com.diddycart.service;

import com.diddycart.dto.payment.PaymentResponse;
import com.diddycart.enums.PaymentMode;
import com.diddycart.enums.PaymentStatus;
import com.diddycart.models.Order;
import com.diddycart.models.Payment;
import com.diddycart.repository.OrderRepository;
import com.diddycart.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    // Process Payment for an Order and Update Order Payment Status
    // EVICT CACHE: Order cache needs update after payment
    @Transactional
    @CacheEvict(value = "orders", key = "#result.userId + '_' + #orderId")
    public PaymentResponse processPayment(Long orderId, PaymentMode mode) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new RuntimeException("Order is already paid for");
        }

        // Simulate Payment Processing
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotal());
        payment.setMode(mode);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(UUID.randomUUID().toString());

        // Update Order Status
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        orderRepository.save(order);

        return mapToResponse(paymentRepository.save(payment));
    }

    @Cacheable(value = "payments", key = "#orderId")
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Payment details not found"));

        return mapToResponse(payment);
    }

    // Map Payment entity to PaymentResponse DTO
    private PaymentResponse mapToResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setOrderId(payment.getOrder().getId());
        response.setUserId(payment.getOrder().getUser().getId());
        response.setAmount(payment.getAmount());
        response.setMode(payment.getMode());
        response.setStatus(payment.getStatus());
        response.setTransactionId(payment.getTransactionId());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }
}