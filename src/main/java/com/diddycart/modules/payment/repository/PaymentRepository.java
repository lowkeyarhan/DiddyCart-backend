package com.diddycart.modules.payment.repository;

import com.diddycart.modules.payment.models.Payment;
import com.diddycart.modules.sales.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payment by its associated order
    Optional<Payment> findByOrder(Order order);

    // Find payment by Transaction ID
    Optional<Payment> findByTransactionId(String transactionId);
}