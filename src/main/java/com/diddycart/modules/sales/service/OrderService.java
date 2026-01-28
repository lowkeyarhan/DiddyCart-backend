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

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
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
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUser(user, pageable).map(this::mapToResponse);
    }

    // Cancel Unpaid Orders by every 10 minutes
    @Scheduled(fixedRate = 600000)
    @Transactional
    public void cancelUnpaidOrders() {
        // Threshold: Orders older than 15 minutes
        Instant timeoutThreshold = Instant.now().minus(15, ChronoUnit.MINUTES);

        // Find Orders older than threshold
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING,
                timeoutThreshold);

        for (Order order : expiredOrders) {
            // Auto-cancel expired order
            System.out.println("Auto-cancelling expired order: " + order.getId());

            // Restore stock from Product
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }

            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.FAILED);

            // Save order
            orderRepository.save(order);
        }
    }

    // Get Order by ID by orderId and userId checks cache first
    @Cacheable(value = "orders", key = "#userId + '_' + #orderId")
    public OrderResponse getOrderById(Long orderId, Long userId) {
        // Find order by orderId
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Verify ownership by userId and orderId
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to view this order");
        }

        // Map Order to OrderResponse
        return mapToResponse(order);
    }

    // Get All Orders by Admin only with pagination
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::mapToResponse);
    }

    // Update Order Status by orderId and status by Admin and vendor only
    @Transactional
    @CachePut(value = "orders", key = "#result.userId + '_' + #result.orderId")
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
    @CachePut(value = "orders", key = "#userId + '_' + #orderId")
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Verify ownership by userId
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to cancel this order");
        }

        // Only allow cancellation if order is still PENDING (Pending payment)
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Cannot cancel order. Current status: " + order.getStatus());
        }

        // Restore stock from Product
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    // Map Order to OrderResponse
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
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                OrderItemResponse itemResponse = new OrderItemResponse();
                // Check if product still exists in ProductRepository
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