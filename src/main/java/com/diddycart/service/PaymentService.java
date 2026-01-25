package com.diddycart.service;

import com.diddycart.dto.payment.PaymentResponse;
import com.diddycart.enums.OrderStatus;
import com.diddycart.enums.PaymentMode;
import com.diddycart.enums.PaymentStatus;
import com.diddycart.models.Order;
import com.diddycart.models.Payment;
import com.diddycart.repository.OrderRepository;
import com.diddycart.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    // Create Razorpay Order
    public PaymentResponse createRazorpayOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new RuntimeException("Order is already paid for");
        }

        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", order.getTotal().multiply(new BigDecimal(100)).intValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + order.getId());

            // Add notes to help identify order in callback if needed
            JSONObject notes = new JSONObject();
            notes.put("internal_order_id", order.getId().toString());
            orderRequest.put("notes", notes);

            com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);

            PaymentResponse response = new PaymentResponse();
            response.setOrderId(order.getId());
            response.setTransactionId(razorpayOrder.get("id"));
            response.setAmount(order.getTotal());
            response.setStatus(PaymentStatus.PENDING);
            return response;

        } catch (Exception e) {
            throw new RuntimeException("Error creating Razorpay order", e);
        }
    }

    // 2. Verify Callback (The "Demo" Logic)
    @Transactional
    public boolean verifyPaymentCallback(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        return verifyPaymentCallbackAndGetOrderId(razorpayOrderId, razorpayPaymentId, razorpaySignature) != null;
    }

    // 2b. Verify Callback and Return Order ID
    @Transactional
    public Long verifyPaymentCallbackAndGetOrderId(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            // Manual Signature Construction (Like in your demo)
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            boolean isValid = Utils.verifySignature(payload, razorpaySignature, keySecret);

            if (isValid) {

                RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
                com.razorpay.Order rzpOrder = razorpay.orders.fetch(razorpayOrderId);
                String internalOrderIdStr = rzpOrder.get("notes").getClass().equals(JSONObject.class)
                        ? ((JSONObject) rzpOrder.get("notes")).getString("internal_order_id")
                        : null;

                if (internalOrderIdStr == null)
                    return null;

                Long internalOrderId = Long.parseLong(internalOrderIdStr);
                Order order = orderRepository.findById(internalOrderId).orElseThrow();

                if (order.getPaymentStatus() == PaymentStatus.COMPLETED)
                    return internalOrderId;

                // Update DB
                Payment payment = new Payment();
                payment.setOrder(order);
                payment.setAmount(order.getTotal());
                payment.setMode(PaymentMode.ONLINE);
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(razorpayPaymentId);
                paymentRepository.save(payment);

                order.setPaymentStatus(PaymentStatus.COMPLETED);
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);

                return internalOrderId;
            }
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}