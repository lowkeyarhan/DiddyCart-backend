package com.diddycart.repository;

import com.diddycart.enums.OrderStatus;
import com.diddycart.enums.PaymentStatus;
import com.diddycart.models.Order;
import com.diddycart.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // USER ORDERS: Fetch all orders for a specific user (with pagination)
    Page<Order> findByUser(User user, Pageable pageable);

    // USER ORDERS: Fetch all orders for a specific user (without pagination)
    List<Order> findByUser(User user);

    // ADMIN ANALYTICS: Calculate total revenue from completed orders
    @Query("SELECT SUM(o.total) FROM Order o WHERE o.paymentStatus = :paymentStatus")
    Double calculateTotalRevenue(PaymentStatus paymentStatus);

    // ADMIN ANALYTICS: Count orders by status
    Long countByStatus(OrderStatus status);

    // ADMIN ANALYTICS: Find orders placed after a specific date
    List<Order> findByCreatedAtAfter(Instant date);

    // Scheduled Task: Find orders with specific status created before a certain
    // timestamp
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant timestamp);
}