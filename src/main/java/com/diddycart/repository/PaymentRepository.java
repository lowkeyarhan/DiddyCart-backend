package com.diddycart.repository;

import com.diddycart.models.Payment;
import com.diddycart.models.Order;
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