package com.diddycart.modules.sales.service;

import com.diddycart.modules.sales.dto.OrderItemResponse;
import com.diddycart.modules.sales.dto.OrderRequest;
import com.diddycart.modules.sales.dto.OrderResponse;
import com.diddycart.modules.sales.models.OrderStatus;
import com.diddycart.modules.payment.models.PaymentStatus;
import com.diddycart.modules.products.models.Product;
import com.diddycart.modules.products.repository.ProductRepository;
import com.diddycart.modules.sales.models.Order;
import com.diddycart.modules.sales.models.OrderItem;
import com.diddycart.modules.sales.models.Cart;
import com.diddycart.modules.sales.models.CartItem;
import com.diddycart.modules.identity.models.User;
import com.diddycart.modules.identity.models.Address;
import com.diddycart.modules.identity.repository.AddressRepository;
import com.diddycart.modules.sales.repository.OrderRepository;
import com.diddycart.modules.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    // Place an Order
    @Transactional
    public OrderResponse placeOrder(Long userId, OrderRequest req) {

        // Get Cart of the user (Existing code)
        Cart cart = cartService.getOrCreateCart(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot place order: Cart is empty");
        }

        // Fetch & Validate Address
        Address address = addressRepository.findById(req.getAddressId())
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // Security Check: Ensure the address belongs to the logged-in user!
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access Denied: You cannot use this address");
        }

        // Create Order
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        // Snapshot Address (Copy from Address Entity -> Order Entity)
        order.setStreet(address.getStreet());
        order.setCity(address.getCity());
        order.setState(address.getState());
        order.setPincode(address.getPincode());
        order.setLandmark(address.getLandmark());

        // Process Items & Deduct Stock
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

        // Save & Clear (Existing code)
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

    // --- ADD THIS METHOD ---
    @Scheduled(fixedRate = 600000) // Runs every 10 minutes
    @Transactional
    public void cancelUnpaidOrders() {
        // Threshold: Orders older than 15 minutes
        Instant timeoutThreshold = Instant.now().minus(15, ChronoUnit.MINUTES);

        // You need to ensure this method exists in your OrderRepository (see Step 3)
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING,
                timeoutThreshold);

        for (Order order : expiredOrders) {
            System.out.println("Auto-cancelling expired order: " + order.getId());

            // Reuse your existing cancel logic to restore stock!
            // Note: Your cancelOrder takes (orderId, userId).
            // Since this is a system action, we can just copy the logic or refactor.
            // Here is the direct logic for safety:

            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }

            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.FAILED); // Explicitly mark as failed
            orderRepository.save(order);
        }
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
                    itemResponse.setProductId(null);
                    itemResponse.setProductName("Product no longer available");
                }
            }
        }
        response.setItems(itemResponses);

        return response;
    }
}