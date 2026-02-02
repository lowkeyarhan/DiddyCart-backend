package com.diddycart.common.infrastructure;

import com.diddycart.modules.identity.events.UserRegisteredEvent;
import com.diddycart.modules.sales.events.OrderPlacedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventProducer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send("user-registration", event);
    }

    public void sendOrderPlaced(OrderPlacedEvent event) {
        kafkaTemplate.send("order-placed", event);
    }
}