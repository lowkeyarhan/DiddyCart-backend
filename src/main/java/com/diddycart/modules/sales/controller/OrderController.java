package com.diddycart.modules.sales.controller;

import com.diddycart.modules.sales.dto.OrderRequest;
import com.diddycart.modules.sales.dto.OrderResponse;
import com.diddycart.modules.sales.models.OrderStatus;
import com.diddycart.modules.sales.service.OrderService;
import com.diddycart.common.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private JwtUtil jwtUtil;

    // Place Order
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody OrderRequest request) {

        // Extract userId from token
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(orderService.placeOrder(userId, request));
    }

    // Get My Orders (with pagination)
    @GetMapping("/my-orders")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @RequestHeader("Authorization") String token,
            Pageable pageable) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(orderService.getUserOrders(userId, pageable));
    }

    // Get Order by ID
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        // Extract userId from token
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(orderService.getOrderById(id, userId));
    }

    // Cancel Order
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        // Extract userId from token
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(orderService.cancelOrder(id, userId));
    }

    // ADMIN: Get All Orders (with pagination)
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    // ADMIN/VENDOR: Update Order Status
    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_VENDOR')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        // Update order status by id and status
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }
}