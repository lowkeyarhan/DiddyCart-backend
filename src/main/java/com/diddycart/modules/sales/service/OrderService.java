package com.diddycart.modules.sales.service;

import com.diddycart.modules.sales.models.OrderStatus;
import com.diddycart.modules.payment.models.PaymentStatus;
import com.diddycart.modules.products.models.Product;
import com.diddycart.modules.products.repository.ProductRepository;
import com.diddycart.modules.sales.models.Order;
import com.diddycart.modules.sales.models.OrderItem;
import com.diddycart.modules.sales.dto.order.OrderItemResponse;
import com.diddycart.modules.sales.dto.order.OrderRequest;
import com.diddycart.modules.sales.dto.order.OrderResponse;
import com.diddycart.modules.sales.dto.order.OrderListResponse;
import com.diddycart.modules.sales.dto.order.OrderDetailResponse;
import com.diddycart.modules.sales.models.Cart;
import com.diddycart.modules.sales.models.CartItem;
import com.diddycart.modules.identity.models.User;
import com.diddycart.modules.identity.models.Address;
import com.diddycart.modules.identity.repository.AddressRepository;
import com.diddycart.modules.sales.repository.OrderRepository;
import com.diddycart.modules.identity.repository.UserRepository;
import com.diddycart.modules.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.diddycart.modules.sales.dto.order.admin.AdminOrderSummaryResponse;
import com.diddycart.modules.sales.dto.order.admin.AdminOrderDetailResponse;
import com.diddycart.common.infrastructure.EventProducer;
import com.diddycart.modules.payment.events.RefundRequestedEvent;
import com.diddycart.modules.payment.models.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EventProducer eventProducer;

    // Place an Order
    @Transactional
    public OrderResponse placeOrder(Long userId, OrderRequest req) {

        // Get Cart of the user by userId
        Cart cart = cartService.getOrCreateCart(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot place order: Cart is empty");
        }

        // Fetch & Validate Address by addressId
        Address address = addressRepository.findById(req.getAddressId())
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // Security Check: Ensure the address belongs to the logged-in user
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access Denied: You cannot use this address");
        }

        // Create Order object
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        // Snapshot Address (Copy from Address Entity to Order Entity)
        order.setStreet(address.getStreet());
        order.setCity(address.getCity());
        order.setState(address.getState());
        order.setPincode(address.getPincode());
        order.setLandmark(address.getLandmark());

        // Process Items & Deduct Stock from Product
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // Sort cart items by product ID to prevent deadlocks
        List<CartItem> sortedItems = new ArrayList<>(cart.getItems());
        sortedItems.sort(Comparator.comparing(item -> item.getProduct().getId()));

        for (CartItem cartItem : sortedItems) {

            // Fetch product with PESSIMISTIC_WRITE lock to prevent overselling
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            // Check stock availability
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Out of stock: " + product.getName());
            }

            // Deduct stock from product
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            // Create OrderItem object
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice());

            // Add orderItem to orderItems
            orderItems.add(orderItem);

            // Calculate line total
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        order.setOrderItems(orderItems);
        order.setTotal(totalAmount);

        // Save & Clear Cart
        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(userId);

        return mapToResponse(savedOrder);
    }

    // Get Orders for User by userId (with pagination)
    public Page<OrderListResponse> getUserOrders(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUser(user, pageable).map(this::mapToListResponse);
    }

    // Cancel Unpaid Orders by every 10 minutes
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void cancelUnpaidOrders() {
        // Threshold: Orders older than 15 minutes
        String timeoutThreshold = LocalDateTime.now().minusMinutes(15).format(DateTimeFormatter.ISO_DATE_TIME);

        // Find Orders older than threshold
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING,
                timeoutThreshold);

        for (Order order : expiredOrders) {
            // Auto-cancel expired order
            System.out.println("Auto-cancelling expired order: " + order.getId());

            // Restore stock from Product
            for (OrderItem item : order.getOrderItems()) {

                // Fetch product with PESSIMISTIC_WRITE lock to prevent overselling
                Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));

                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }

            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.FAILED);

            // Save order
            orderRepository.save(order);
        }
    }

    // Get detailed Order by ID by orderId
    public OrderDetailResponse getOrderById(Long orderId, Long userId) {
        // Find order by orderId
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Verify ownership by userId and orderId
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to view this order");
        }

        // Map Order to OrderDetailResponse
        return mapToDetailResponse(order);
    }

    // Get All Orders by Admin only with pagination
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::mapToResponse);
    }

    // Update Order Status by orderId and status by Admin and vendor only
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.setStatus(status);
        Order savedOrder = orderRepository.save(order);

        // Map Order to OrderResponse
        return mapToResponse(savedOrder);
    }

    // Cancel Order by orderId and userId (User/Admin)
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        // Find order by orderId
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Verify ownership by userId
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to cancel this order");
        }

        // Allow cancellation only if order is NOT yet shipped
        if (order.getStatus() == OrderStatus.SHIPPED ||
                order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel order. Current status: " + order.getStatus());
        }

        // Restore stock from Product
        for (OrderItem item : order.getOrderItems()) {

            // Fetch product with PESSIMISTIC_WRITE lock to prevent overselling
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        // Publish refund event if payment was completed
        if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            // Get payment details for the event
            Payment payment = paymentRepository.findByOrder(order)
                    .orElse(null);

            if (payment != null) {
                RefundRequestedEvent refundEvent = new RefundRequestedEvent(
                        order.getId(),
                        order.getUser().getId(),
                        order.getUser().getEmail(),
                        order.getTotal(),
                        payment.getMode() != null ? payment.getMode().toString() : "UNKNOWN",
                        payment.getTransactionId());

                // Publish event to Kafka for async refund processing
                eventProducer.sendRefundRequested(refundEvent);
            }
        }

        // Update order status to CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        // Map Order to OrderResponse
        return mapToResponse(savedOrder);
    }

    // Admin: Get all orders (summary view)
    public Page<AdminOrderSummaryResponse> getAdminAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::mapToAdminSummary);
    }

    // Admin: Search orders by ID or Email
    public Page<AdminOrderSummaryResponse> searchOrders(String keyword, Pageable pageable) {
        return orderRepository.searchOrders(keyword, pageable).map(this::mapToAdminSummary);
    }

    // Admin: Filter orders by status
    public Page<AdminOrderSummaryResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable).map(this::mapToAdminSummary);
    }

    // Admin: Get order detail by order ID (bypasses ownership check)
    public AdminOrderDetailResponse getAdminOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        return mapToAdminDetail(order);
    }

    // Admin: Get all orders by userId (bypasses ownership check)
    public Page<AdminOrderSummaryResponse> getOrdersByUserId(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return orderRepository.findByUser(user, pageable).map(this::mapToAdminSummary);
    }

    // Map Order to AdminOrderSummaryResponse (for summary view)
    private AdminOrderSummaryResponse mapToAdminSummary(Order order) {
        AdminOrderSummaryResponse res = new AdminOrderSummaryResponse();
        res.setOrderId(order.getId());
        res.setCustomerEmail(order.getUser().getEmail());
        res.setOrderDate(order.getCreatedAt());
        res.setStatus(order.getStatus());
        res.setPaymentStatus(order.getPaymentStatus());
        res.setTotalAmount(order.getTotal());
        return res;
    }

    // Map Order to AdminOrderDetailResponse (for detail view)
    private AdminOrderDetailResponse mapToAdminDetail(Order order) {
        AdminOrderDetailResponse res = new AdminOrderDetailResponse();
        res.setOrderId(order.getId());
        res.setOrderDate(order.getCreatedAt());
        res.setStatus(order.getStatus());

        // Customer Info
        res.setUserId(order.getUser().getId());
        res.setCustomerName(order.getUser().getName());
        res.setCustomerEmail(order.getUser().getEmail());
        res.setCustomerPhone(order.getUser().getPhone());

        // Payment Info
        res.setPaymentStatus(order.getPaymentStatus());
        // Fetch payment details
        paymentRepository.findByOrder(order).ifPresent(payment -> {
            res.setPaymentMode(payment.getMode());
            res.setTransactionId(payment.getTransactionId());
        });

        // Shipping
        String address = String.join(", ",
                order.getStreet() != null ? order.getStreet() : "",
                order.getCity() != null ? order.getCity() : "",
                order.getState() != null ? order.getState() : "",
                order.getPincode() != null ? order.getPincode() : "");
        res.setShippingAddress(address);

        // Items (Reusing existing helper mapOrderItems)
        res.setItems(mapOrderItems(order));
        res.setTotalAmount(order.getTotal());

        return res;
    }

    // Map Order to OrderListResponse (for list view)
    private OrderListResponse mapToListResponse(Order order) {
        OrderListResponse response = new OrderListResponse();
        response.setOrderId(order.getId());
        response.setOrderDate(order.getCreatedAt());
        response.setStatus(order.getStatus());
        response.setBill(order.getTotal());

        // Build simplified shipping address
        String shippingAddress = String.join(", ",
                order.getStreet() != null ? order.getStreet() : "",
                order.getCity() != null ? order.getCity() : "",
                order.getState() != null ? order.getState() : "",
                order.getPincode() != null ? order.getPincode() : "");
        response.setShippingAddress(shippingAddress);

        // Map order items
        response.setItems(mapOrderItems(order));

        return response;
    }

    // Map Order to OrderDetailResponse (for detail view)
    private OrderDetailResponse mapToDetailResponse(Order order) {
        OrderDetailResponse response = new OrderDetailResponse();
        response.setOrderId(order.getId());
        response.setOrderDate(order.getCreatedAt());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setTotalAmount(order.getTotal());

        // Fetch payment mode from Payment entity
        paymentRepository.findByOrder(order).ifPresent(payment -> {
            response.setPaymentMode(payment.getMode());
        });

        // Build complete shipping address
        String shippingAddress = String.join(", ",
                order.getStreet() != null ? order.getStreet() : "",
                order.getLandmark() != null ? order.getLandmark() : "",
                order.getCity() != null ? order.getCity() : "",
                order.getState() != null ? order.getState() : "",
                order.getPincode() != null ? order.getPincode() : "");
        response.setShippingAddress(shippingAddress);

        // Map order items
        response.setItems(mapOrderItems(order));

        return response;
    }

    // Map Order to OrderResponse (legacy - still used by placeOrder,
    // updateOrderStatus, cancelOrder)
    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setUserId(order.getUser().getId());
        response.setOrderDate(order.getCreatedAt());
        response.setTotalAmount(order.getTotal());
        response.setStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());

        // Build shipping address string from Order Entity to OrderResponse
        String shippingAddress = String.join(", ",
                order.getStreet() != null ? order.getStreet() : "",
                order.getCity() != null ? order.getCity() : "",
                order.getState() != null ? order.getState() : "",
                order.getPincode() != null ? order.getPincode() : "");
        response.setShippingAddress(shippingAddress);

        // Map order items from OrderEntity to OrderItemResponse
        response.setItems(mapOrderItems(order));

        return response;
    }

    // Shared method to map order items
    private List<OrderItemResponse> mapOrderItems(Order order) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                OrderItemResponse itemResponse = new OrderItemResponse();
                // Check if product still exists in ProductRepository
                if (item.getProduct() != null) {
                    itemResponse.setProductId(item.getProduct().getId());
                    itemResponse.setProductName(item.getProduct().getName());
                    itemResponse.setQuantity(item.getQuantity());
                    itemResponse.setPrice(item.getPrice());
                    if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                        itemResponse.setProductImage(item.getProduct().getImages().get(0).getImageUrl());
                    }
                } else {
                    itemResponse.setProductId(null);
                    itemResponse.setProductName("Product no longer available");
                }
                itemResponses.add(itemResponse);
            }
        }
        return itemResponses;
    }
}