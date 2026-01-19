package com.diddycart.service;

import com.diddycart.dto.orders.OrderItemResponse;
import com.diddycart.dto.orders.OrderRequest;
import com.diddycart.dto.orders.OrderResponse;
import com.diddycart.enums.OrderStatus;
import com.diddycart.enums.PaymentStatus;
import com.diddycart.models.*;
import com.diddycart.repository.AddressRepository;
import com.diddycart.repository.OrderRepository;
import com.diddycart.repository.ProductRepository;
import com.diddycart.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Transactional
    public OrderResponse placeOrder(Long userId, OrderRequest req) {

        // 1. Get Cart (Existing code)
        Cart cart = cartService.getOrCreateCart(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot place order: Cart is empty");
        }

        // 2. Fetch & Validate Address (NEW LOGIC)
        Address address = addressRepository.findById(req.getAddressId())
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // Security Check: Ensure the address belongs to the logged-in user!
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access Denied: You cannot use this address");
        }

        // 3. Create Order
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        // 4. Snapshot Address (Copy from Address Entity -> Order Entity)
        // We copy the data so if the user changes their address later,
        // this specific order record remains unchanged.
        order.setStreet(address.getStreet());
        order.setCity(address.getCity());
        order.setState(address.getState());
        order.setPincode(address.getPincode());
        order.setLandmark(address.getLandmark());

        // 5. Process Items & Deduct Stock
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Out of stock: " + product.getName());
            }

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());

            orderItems.add(orderItem);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        order.setOrderItems(orderItems);
        order.setTotal(totalAmount);

        // 6. Save & Clear (Existing code)
        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(userId);

        return mapToResponse(savedOrder);
    }

    // Get Orders for User (with pagination)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUser(user, pageable).map(this::mapToResponse);
    }

    // Get Order by ID
    @Cacheable(value = "orders", key = "#userId + '_' + #orderId")
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Verify ownership
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to view this order");
        }

        return mapToResponse(order);
    }

    // Get All Orders (Admin only) - with pagination
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::mapToResponse);
    }

    // Update Order Status (Admin and vendor only)
    @Transactional
    @CachePut(value = "orders", key = "#result.userId + '_' + #result.orderId")
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.setStatus(status);
        Order savedOrder = orderRepository.save(order);

        // This mapper will now include the userId
        return mapToResponse(savedOrder);
    }

    // Cancel Order (User/Admin)
    @Transactional
    @CachePut(value = "orders", key = "#userId + '_' + #orderId")
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Verify ownership
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to cancel this order");
        }

        // Only allow cancellation if order is still PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Cannot cancel order. Current status: " + order.getStatus());
        }

        // Restore stock
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    // Helper method to map Order to OrderResponse
    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setUserId(order.getUser().getId());
        response.setOrderDate(order.getCreatedAt());
        response.setTotalAmount(order.getTotal());
        response.setStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());

        // Build shipping address string
        String shippingAddress = String.join(", ",
                order.getStreet() != null ? order.getStreet() : "",
                order.getCity() != null ? order.getCity() : "",
                order.getState() != null ? order.getState() : "",
                order.getPincode() != null ? order.getPincode() : "");
        response.setShippingAddress(shippingAddress);

        // Map order items
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                OrderItemResponse itemResponse = new OrderItemResponse();
                // Check if product still exists
                if (item.getProduct() != null) {
                    itemResponse.setProductId(item.getProduct().getId());
                    itemResponse.setProductName(item.getProduct().getName());
                    if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {

                    }
                } else {
                    // Handle deleted products gracefully
                    itemResponse.setProductId(null);
                    itemResponse.setProductName("Product no longer available");
                }
                // --- FIX ENDS HERE ---
            }
        }
        response.setItems(itemResponses);

        return response;
    }
}