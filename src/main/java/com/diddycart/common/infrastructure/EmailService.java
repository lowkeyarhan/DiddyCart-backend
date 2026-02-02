package com.diddycart.common.infrastructure;

import com.diddycart.modules.sales.events.OrderPlacedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendWelcomeEmail(String to, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Welcome to DiddyCart!");
        message.setText("Hello " + name + ",\n\nWelcome to DiddyCart! We are excited to have you.");
        mailSender.send(message);
        System.out.println("📧 Welcome email sent to " + to);
    }

    public void sendOrderConfirmation(String to, Long orderId, String amount, List<OrderPlacedEvent.ItemDetail> items) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Order Confirmation - #" + orderId);

        StringBuilder sb = new StringBuilder();
        sb.append("Thank you for your order!\n\n");
        sb.append("Order ID: ").append(orderId).append("\n");
        sb.append("Total: $").append(amount).append("\n\n");

        sb.append("--- Order Details ---\n");
        if (items != null) {
            for (OrderPlacedEvent.ItemDetail item : items) {
                sb.append(String.format("- %s (x%d) @ $%s\n", item.getName(), item.getQuantity(), item.getPrice()));
            }
        }
        sb.append("---------------------\n");

        message.setText(sb.toString());
        mailSender.send(message);
        System.out.println("📧 Order confirmation sent to " + to);
    }
}