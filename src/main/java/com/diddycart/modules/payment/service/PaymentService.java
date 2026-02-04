package com.diddycart.modules.payment.service;

import com.diddycart.common.infrastructure.EventProducer;
import com.diddycart.modules.payment.dto.PaymentResponse;
import com.diddycart.modules.payment.events.PaymentFailedEvent;
import com.diddycart.modules.sales.models.OrderStatus;
import com.diddycart.modules.payment.models.PaymentMode;
import com.diddycart.modules.payment.models.PaymentStatus;
import com.diddycart.modules.sales.events.OrderPlacedEvent;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EventProducer eventProducer;

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

    // Verify Callback and return Internal Order ID
    @Transactional
    public Long verifyPaymentCallbackAndGetOrderId(String razorpayOrderId, String razorpayPaymentId,
            String razorpaySignature) {
        try {
            // Verify Signature
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            boolean isValid = Utils.verifySignature(payload, razorpaySignature, keySecret);

            if (!isValid)
                return null;

            // Fetch Payment Details from Razorpay to get the MODE (card, upi, etc.)
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            com.razorpay.Payment rzpPayment = razorpay.payments.fetch(razorpayPaymentId);

            // Razorpay returns "card", "upi", "netbanking", "wallet" etc.
            String rzpMethod = rzpPayment.get("method");
            String rzpStatus = rzpPayment.get("status");

            // Map Razorpay String -> Java Enum
            PaymentMode paymentMode = mapToPaymentMode(rzpMethod);

            // Get Internal Order
            com.razorpay.Order rzpOrder = razorpay.orders.fetch(razorpayOrderId);
            String internalOrderIdStr = rzpOrder.get("notes").getClass().equals(JSONObject.class)
                    ? ((JSONObject) rzpOrder.get("notes")).getString("internal_order_id")
                    : null;

            if (internalOrderIdStr == null)
                return null;
            Long internalOrderId = Long.parseLong(internalOrderIdStr);
            Order order = orderRepository.findById(internalOrderId).orElseThrow();

            // Handle Failure
            if ("failed".equalsIgnoreCase(rzpStatus)) {
                PaymentFailedEvent failEvent = new PaymentFailedEvent(
                        order.getId(),
                        order.getUser().getId(),
                        order.getUser().getEmail(),
                        order.getTotal(),
                        paymentMode.toString() // Pass Enum as String to Event
                );
                eventProducer.sendPaymentFailed(failEvent);
                return null;
            }

            // Handle Success
            if ("captured".equalsIgnoreCase(rzpStatus) || "authorized".equalsIgnoreCase(rzpStatus)) {
                if (order.getPaymentStatus() == PaymentStatus.COMPLETED)
                    return internalOrderId;

                Payment payment = new Payment();
                payment.setOrder(order);
                payment.setAmount(order.getTotal());
                payment.setMode(paymentMode);
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(razorpayPaymentId);
                paymentRepository.save(payment);

                order.setPaymentStatus(PaymentStatus.COMPLETED);
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);

                // Build Items List (for Event)
                List<OrderPlacedEvent.ItemDetail> itemDetails = order.getOrderItems().stream()
                        .map(item -> new OrderPlacedEvent.ItemDetail(
                                item.getProduct().getName(),
                                item.getQuantity(),
                                item.getPrice()))
                        .collect(Collectors.toList());

                // Trigger Success Event
                OrderPlacedEvent successEvent = new OrderPlacedEvent(
                        order.getId(),
                        order.getUser().getId(),
                        order.getUser().getEmail(),
                        order.getTotal(),
                        paymentMode.toString(),
                        itemDetails);

                // Send Event to Event Producer
                eventProducer.sendOrderPlaced(successEvent);

                return internalOrderId;
            }
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Helper: Maps Razorpay's lowercase strings to your Enum
    private PaymentMode mapToPaymentMode(String rzpMethod) {
        if (rzpMethod == null)
            return PaymentMode.ONLINE;

        switch (rzpMethod.toLowerCase()) {
            case "card":
                return PaymentMode.CARD;
            case "upi":
                return PaymentMode.UPI;
            case "netbanking":
                return PaymentMode.NET_BANKING;
            case "cod":
                return PaymentMode.CASH_ON_DELIVERY;
            default:
                return PaymentMode.ONLINE;
        }
    }
}