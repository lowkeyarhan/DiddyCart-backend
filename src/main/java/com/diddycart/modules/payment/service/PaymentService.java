package com.diddycart.modules.payment.service;

import com.diddycart.modules.payment.dto.PaymentResponse;
import com.diddycart.modules.sales.models.OrderStatus;
import com.diddycart.modules.payment.models.PaymentMode;
import com.diddycart.modules.payment.models.PaymentStatus;
import com.diddycart.modules.sales.models.Order;
import com.diddycart.modules.payment.models.Payment;
import com.diddycart.modules.sales.repository.OrderRepository;
import com.diddycart.modules.payment.repository.PaymentRepository;
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

    // Create Razorpay Order by orderId
    public PaymentResponse createRazorpayOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Check if order is already paid for
        if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new RuntimeException("Order is already paid for");
        }

        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", order.getTotal().multiply(new BigDecimal(100)).intValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + order.getId());

            // Add notes to help identify order in callback by internal_order_id
            JSONObject notes = new JSONObject();
            notes.put("internal_order_id", order.getId().toString());
            orderRequest.put("notes", notes);

            com.razorpay.Order razorpayOrder = razorpay.orders.create(orderRequest);

            // Map Order to PaymentResponse
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

    // Verify Callback by razorpayOrderId, razorpayPaymentId, razorpaySignature
    @Transactional
    public boolean verifyPaymentCallback(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        return verifyPaymentCallbackAndGetOrderId(razorpayOrderId, razorpayPaymentId, razorpaySignature) != null;
    }

    // Verify Callback and Return Order ID by razorpayOrderId, razorpayPaymentId,
    // razorpaySignature
    @Transactional
    public Long verifyPaymentCallbackAndGetOrderId(String razorpayOrderId, String razorpayPaymentId,
            String razorpaySignature) {
        try {
            // Manual Signature Construction by razorpayOrderId, razorpayPaymentId
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            boolean isValid = Utils.verifySignature(payload, razorpaySignature, keySecret);

            if (isValid) {
                // Fetch Razorpay Order by razorpayOrderId
                RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
                com.razorpay.Order rzpOrder = razorpay.orders.fetch(razorpayOrderId);
                // Get internal_order_id from notes
                String internalOrderIdStr = rzpOrder.get("notes").getClass().equals(JSONObject.class)
                        ? ((JSONObject) rzpOrder.get("notes")).getString("internal_order_id")
                        : null;

                // Check if internal_order_id is null
                if (internalOrderIdStr == null)
                    return null;

                // Get order by internal_order_id
                Long internalOrderId = Long.parseLong(internalOrderIdStr);
                Order order = orderRepository.findById(internalOrderId).orElseThrow();

                if (order.getPaymentStatus() == PaymentStatus.COMPLETED)
                    return internalOrderId;

                // Create Payment object
                Payment payment = new Payment();
                payment.setOrder(order);
                payment.setAmount(order.getTotal());
                payment.setMode(PaymentMode.ONLINE);
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(razorpayPaymentId);
                paymentRepository.save(payment);

                // Update order status to CONFIRMED
                order.setPaymentStatus(PaymentStatus.COMPLETED);
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);

                // Return orderId by internalOrderId
                return internalOrderId;
            }
            // Return null if payment verification failed
            return null;

        } catch (Exception e) {
            // Return null if exception occurs
            e.printStackTrace();
            return null;
        }
    }
}