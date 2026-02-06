package com.diddycart.common.infrastructure;

import com.diddycart.modules.identity.events.PasswordResetEvent;
import com.diddycart.modules.identity.events.UserRegisteredEvent;
import com.diddycart.modules.payment.events.PaymentFailedEvent;
import com.diddycart.modules.payment.events.RefundRequestedEvent;
import com.diddycart.modules.payment.service.PaymentService;
import com.diddycart.modules.sales.events.OrderPlacedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class EventConsumer {

    @Autowired
    private EmailService emailService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    @Qualifier("kafkaWorkerPool")
    private Executor workerPool;

    // Listen for User Registered Event
    @KafkaListener(topics = "user-registration", groupId = "diddycart-group")
    public void handleUserRegistration(UserRegisteredEvent event) {
        CompletableFuture.runAsync(() -> {
            System.out.println("⚙️ Processing registration [Thread: " + Thread.currentThread().getName() + "]");
            emailService.sendWelcomeEmail(event.getEmail(), event.getName());
        }, workerPool);
    }

    // Listen for Order Placed Event
    @KafkaListener(topics = "order-placed", groupId = "diddycart-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        emailService.sendOrderConfirmation(
                event.getEmail(),
                event.getOrderId(),
                event.getAmount().toString(),
                event.getPaymentMode(), // Pass Mode
                event.getItems());
    }

    // Listen for Payment Failed Event
    @KafkaListener(topics = "payment-failed", groupId = "diddycart-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        emailService.sendPaymentFailedEmail(
                event.getEmail(),
                event.getOrderId(),
                event.getAmount().toString(),
                event.getPaymentMode());
    }

    // Listen for Password Reset Event
    @KafkaListener(topics = "identity.password-reset", groupId = "diddycart-group")
    public void handlePasswordReset(PasswordResetEvent event) {
        System.out.println("Consumed PasswordResetEvent: " + event.getEmail());
        emailService.sendPasswordResetEmail(event.getEmail(), event.getToken());
    }

    // Listen for Refund Requested Event
    @KafkaListener(topics = "refund-requested", groupId = "diddycart-group")
    public void handleRefundRequested(RefundRequestedEvent event) {
        CompletableFuture.runAsync(() -> {
            System.out.println("⚙️ Processing refund for Order ID: " + event.getOrderId() + " [Thread: "
                    + Thread.currentThread().getName() + "]");
            try {
                // Process the refund via Razorpay
                paymentService.processRefund(event.getOrderId());

                // Send refund confirmation email
                emailService.sendRefundConfirmationEmail(
                        event.getEmail(),
                        event.getOrderId(),
                        event.getAmount().toString(),
                        event.getPaymentMode());

                System.out.println("✅ Refund processed successfully for Order ID: " + event.getOrderId());
            } catch (Exception e) {
                System.err.println(
                        "❌ Refund processing failed for Order ID: " + event.getOrderId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, workerPool);
    }
}