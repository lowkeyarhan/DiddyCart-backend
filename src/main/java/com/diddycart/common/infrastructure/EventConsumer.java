package com.diddycart.common.infrastructure;

import com.diddycart.modules.identity.events.UserRegisteredEvent;
import com.diddycart.modules.payment.events.PaymentFailedEvent;
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

    // 2. Add New Listener for Failure
    @KafkaListener(topics = "payment-failed", groupId = "diddycart-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        emailService.sendPaymentFailedEmail(
                event.getEmail(),
                event.getOrderId(),
                event.getAmount().toString(),
                event.getPaymentMode());
    }
}