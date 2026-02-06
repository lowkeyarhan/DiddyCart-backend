package com.diddycart.modules.identity.service;

import com.diddycart.modules.identity.dto.admin.AdminUserDetailResponse;
import com.diddycart.modules.identity.dto.admin.AdminUserSummaryResponse;
import com.diddycart.modules.identity.dto.admin.AdminVendorSummaryResponse;
import com.diddycart.modules.identity.models.User;
import com.diddycart.modules.identity.models.Vendor;
import com.diddycart.modules.identity.repository.UserRepository;
import com.diddycart.modules.identity.repository.VendorRepository;
import com.diddycart.modules.sales.dto.order.OrderItemResponse;
import com.diddycart.modules.sales.dto.order.OrderListResponse;
import com.diddycart.modules.sales.models.Order;
import com.diddycart.modules.sales.models.OrderItem;
import com.diddycart.modules.sales.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminIdentityService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private OrderRepository orderRepository;

    // Get all users (summary view)
    public Page<AdminUserSummaryResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToUserSummary);
    }

    // Get user details with their order history
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUserById(Long id) {
        // Find user by id
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        // Fetch orders for this user
        List<Order> userOrders = orderRepository.findByUser(user);

        // Map user to user detail response
        return mapToUserDetail(user, userOrders);
    }

    // Get all vendors (summary view)
    public Page<AdminVendorSummaryResponse> getAllVendors(Pageable pageable) {
        return vendorRepository.findAll(pageable)
                .map(this::mapToVendorSummary);
    }

    // Map user to user summary response
    private AdminUserSummaryResponse mapToUserSummary(User user) {
        AdminUserSummaryResponse res = new AdminUserSummaryResponse();
        res.setId(user.getId());
        res.setName(user.getName());
        res.setEmail(user.getEmail());
        res.setRole(user.getRole());
        return res;
    }

    // Map user to user detail response
    private AdminUserDetailResponse mapToUserDetail(User user, List<Order> orders) {
        AdminUserDetailResponse res = new AdminUserDetailResponse();
        res.setId(user.getId());
        res.setName(user.getName());
        res.setEmail(user.getEmail());
        res.setPhone(user.getPhone());
        res.setRole(user.getRole());
        res.setCreatedAt(user.getCreatedAt());

        // Map orders to order list response
        List<OrderListResponse> orderResponses = orders.stream()
                .map(this::mapToOrderListResponse)
                .collect(Collectors.toList());
        res.setOrders(orderResponses);

        return res;
    }

    // Map vendor to vendor summary response
    private AdminVendorSummaryResponse mapToVendorSummary(Vendor vendor) {
        AdminVendorSummaryResponse res = new AdminVendorSummaryResponse();
        res.setId(vendor.getId());
        res.setStoreName(vendor.getStoreName());

        if (vendor.getUser() != null) {
            res.setName(vendor.getUser().getName());
            res.setEmail(vendor.getUser().getEmail());
        }
        return res;
    }

    // Map order to order list response
    // Helper to map order entity to order list response
    private OrderListResponse mapToOrderListResponse(Order order) {
        OrderListResponse response = new OrderListResponse();
        response.setOrderId(order.getId());
        response.setOrderDate(order.getCreatedAt());
        response.setStatus(order.getStatus());
        response.setBill(order.getTotal());

        String shippingAddress = String.join(", ",
                order.getStreet() != null ? order.getStreet() : "",
                order.getCity() != null ? order.getCity() : "",
                order.getState() != null ? order.getState() : "",
                order.getPincode() != null ? order.getPincode() : "");
        response.setShippingAddress(shippingAddress);

        // Map items to order item response
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                OrderItemResponse itemResponse = new OrderItemResponse();
                if (item.getProduct() != null) {
                    itemResponse.setProductId(item.getProduct().getId());
                    itemResponse.setProductName(item.getProduct().getName());
                    itemResponse.setPrice(item.getPrice());
                    itemResponse.setQuantity(item.getQuantity());
                    // Skip images for the list view to keep it light
                } else {
                    itemResponse.setProductName("Product no longer available");
                }
                itemResponses.add(itemResponse);
            }
        }
        response.setItems(itemResponses);

        return response;
    }
}