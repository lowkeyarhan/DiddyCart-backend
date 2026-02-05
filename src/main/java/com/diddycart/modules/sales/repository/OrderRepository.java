package com.diddycart.modules.sales.repository;

import com.diddycart.modules.sales.models.OrderStatus;

import jakarta.persistence.LockModeType;

import com.diddycart.modules.payment.models.PaymentStatus;
import com.diddycart.modules.sales.models.Order;
import com.diddycart.modules.identity.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // USER ORDERS: Fetch all orders for a specific user (with pagination)
    Page<Order> findByUser(User user, Pageable pageable);

    // USER ORDERS: Fetch all orders for a specific user (without pagination)
    List<Order> findByUser(User user);

    // USER ORDERS: Check if a delivered order exists for a specific product and
    // user
    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.orderItems oi WHERE o.user.id = :userId AND oi.product.id = :productId AND o.status = 'DELIVERED'")
    boolean existsDeliveredOrderForProduct(@Param("userId") Long userId,
            @Param("productId") Long productId);

    // ADMIN ANALYTICS: Calculate total revenue from completed orders
    @Query("SELECT SUM(o.total) FROM Order o WHERE o.paymentStatus = :paymentStatus")
    Double calculateTotalRevenue(PaymentStatus paymentStatus);

    // ADMIN ANALYTICS: Count orders by status
    Long countByStatus(OrderStatus status);

    // ADMIN ANALYTICS: Find orders placed after a specific date
    List<Order> findByCreatedAtAfter(String date);

    // Find orders with specific status created before a certain timestamp
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, String timestamp);

    // Pessimistic Lock for safe Payment verification and Order Cancellation
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}