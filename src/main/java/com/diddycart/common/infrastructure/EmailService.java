package com.diddycart.common.infrastructure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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

    public void sendOrderConfirmation(String to, Long orderId, String amount) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Order Confirmation - #" + orderId);
        message.setText("Thank you for your order!\n\nOrder ID: " + orderId + "\nTotal: $" + amount);
        mailSender.send(message);
        System.out.println("📧 Order confirmation sent to " + to);
    }
}