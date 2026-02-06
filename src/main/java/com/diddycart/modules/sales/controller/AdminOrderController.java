package com.diddycart.modules.sales.controller;

import com.diddycart.modules.sales.dto.order.admin.AdminOrderDetailResponse;
import com.diddycart.modules.sales.dto.order.admin.AdminOrderSummaryResponse;
import com.diddycart.modules.sales.models.OrderStatus;
import com.diddycart.modules.sales.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    // Admin: Get all orders (summary view)
    @GetMapping
    public ResponseEntity<Page<AdminOrderSummaryResponse>> getAllOrders(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getAdminAllOrders(pageable));
    }

    // Admin: Search orders by ID or Email
    @GetMapping("/search")
    public ResponseEntity<Page<AdminOrderSummaryResponse>> searchOrders(
            @RequestParam String keyword,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.searchOrders(keyword, pageable));
    }

    // Admin: Filter orders by status
    @GetMapping("/filter")
    public ResponseEntity<Page<AdminOrderSummaryResponse>> filterOrders(
            @RequestParam OrderStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status, pageable));
    }

    // Admin: Get order detail by order ID
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderDetailResponse> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getAdminOrderById(orderId));
    }

    // Admin: Get all orders by user ID
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AdminOrderSummaryResponse>> getOrdersByUserId(
            @PathVariable Long userId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId, pageable));
    }
}