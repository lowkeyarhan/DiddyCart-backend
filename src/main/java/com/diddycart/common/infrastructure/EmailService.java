package com.diddycart.common.infrastructure;

import com.diddycart.modules.sales.events.OrderPlacedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Send welcome email
    @Async("kafkaWorkerPool")
    public void sendWelcomeEmail(String to, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Welcome to DiddyCart!");
        message.setText("Hello " + name + ",\n\nWelcome to DiddyCart! We are excited to have you.");
        mailSender.send(message);
        System.out.println("📧 Welcome email sent to " + to);
    }

    // Send order confirmation email
    @Async("kafkaWorkerPool")
    public void sendOrderConfirmation(String to, Long orderId, String amount, String paymentMode,
            List<OrderPlacedEvent.ItemDetail> items) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Order Confirmed! - #" + orderId);

        StringBuilder sb = new StringBuilder();
        sb.append("Your payment was successful!\n");
        sb.append("Order ID: ").append(orderId).append("\n");
        sb.append("Total Paid: $").append(amount).append("\n");
        sb.append("Payment Mode: ").append(paymentMode.toUpperCase()).append("\n\n"); // <--- Show Mode

        sb.append("--- Items ---\n");
        if (items != null) {
            for (OrderPlacedEvent.ItemDetail item : items) {
                sb.append(String.format("- %s (x%d) @ $%s\n", item.getName(), item.getQuantity(), item.getPrice()));
            }
        }

        message.setText(sb.toString());
        mailSender.send(message);
        System.out.println("📧 Sent Order Confirmation to " + to);
    }

    // send payment failure email
    @Async("kafkaWorkerPool")
    public void sendPaymentFailedEmail(String to, Long orderId, String amount, String paymentMode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Payment Failed - Order #" + orderId);

        String text = "We could not process your payment.\n\n" +
                "Order ID: " + orderId + "\n" +
                "Amount: $" + amount + "\n" +
                "Payment Mode: " + (paymentMode != null ? paymentMode.toUpperCase() : "Unknown") + "\n\n" +
                "Please try again via the application.";

        message.setText(text);
        mailSender.send(message);
        System.out.println("📧 Sent Payment Failure Email to " + to);
    }

    @Async("kafkaWorkerPool")
    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = "http://localhost:8080/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Reset your DiddyCart Password");
        message.setText(String.format(
                "Hello,\n\n" +
                        "You have requested to reset your password.\n" +
                        "Click the link below to change your password:\n\n" +
                        "%s\n\n" +
                        "This link will expire in 15 minutes.\n" +
                        "If you did not request this, please ignore this email.",
                resetUrl));

        mailSender.send(message);
        System.out.println("📧 Password reset email sent to " + to);
    }

    // Send refund confirmation email
    @Async("kafkaWorkerPool")
    public void sendRefundConfirmationEmail(String to, Long orderId, String amount, String paymentMode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Refund Processed - Order #" + orderId);

        String text = "Your refund has been processed successfully!\n\n" +
                "Order ID: " + orderId + "\n" +
                "Refund Amount: $" + amount + "\n" +
                "Original Payment Mode: " + (paymentMode != null ? paymentMode.toUpperCase() : "Unknown") + "\n\n" +
                "The refund will be credited to your original payment method within 5-7 business days.\n\n" +
                "Thank you for shopping with DiddyCart!";

        message.setText(text);
        mailSender.send(message);
        System.out.println("📧 Sent Refund Confirmation Email to " + to);
    }
}