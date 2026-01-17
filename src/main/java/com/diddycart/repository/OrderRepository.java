package com.diddycart.repository;

import com.diddycart.models.Order;
import com.diddycart.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 1. USER ORDERS: Fetch all orders for a specific user
    List<Order> findByUser(User user);

    // 2. ADMIN ANALYTICS: Calculate total revenue from completed orders
    @Query("SELECT SUM(o.total) FROM Order o WHERE o.paymentStatus = 'COMPLETED'")
    Double calculateTotalRevenue();

    // 3. ADMIN ANALYTICS: Count orders by status
    Long countByStatus(String status);

    // 4. ADMIN ANALYTICS: Find orders placed after a specific date
    List<Order> findByCreatedAtAfter(Instant date);
}