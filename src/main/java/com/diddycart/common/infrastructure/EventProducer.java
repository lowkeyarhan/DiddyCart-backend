package com.diddycart.common.infrastructure;

import com.diddycart.modules.identity.events.UserRegisteredEvent;
import com.diddycart.modules.payment.events.PaymentFailedEvent;
import com.diddycart.modules.sales.events.OrderPlacedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventProducer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // Send UserRegisteredEvent to Kafka
    public void sendUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send("user-registration", event);
    }

    // Send OrderPlacedEvent to Kafka
    public void sendOrderPlaced(OrderPlacedEvent event) {
        kafkaTemplate.send("order-placed", event);
    }

    // Send PaymentFailedEvent to Kafka
    public void sendPaymentFailed(PaymentFailedEvent event) {
        kafkaTemplate.send("payment-failed", event);
        System.out.println("📤 Produced PaymentFailedEvent for Order ID: " + event.getOrderId());
    }
}