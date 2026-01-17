package com.diddycart.service;

import com.diddycart.dto.orders.OrderRequest;
import com.diddycart.enums.OrderStatus;
import com.diddycart.enums.PaymentStatus;
import com.diddycart.models.*;
import com.diddycart.repository.OrderRepository;
import com.diddycart.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Transactional
    public Order placeOrder(Long userId, OrderRequest req) {

        // 1. Get Cart
        Cart cart = cartService.getCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot place order: Cart is empty");
        }

        // 2. Create Order
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        // 3. Snapshot Address (Copying from Request so history doesn't change)
        order.setStreet(req.getStreet());
        order.setCity(req.getCity());
        order.setState(req.getState());
        order.setPincode(req.getPincode());

        // 4. Process Items & Deduct Stock
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            // Check Stock
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Out of stock: " + product.getName());
            }

            // Deduct Stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            // Create Order Item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(product.getPrice()); // Snapshot Price

            orderItems.add(orderItem);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        order.setOrderItems(orderItems);
        order.setTotal(totalAmount);

        // 5. Save Order & Clear Cart
        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(userId);

        return savedOrder;
    }

    public List<Order> getUserOrders(Long userId) {
        // You need to add findByUserId in OrderRepository, or use User object
        // Assuming findByUserId exists or fetching via User entity
        return orderRepository.findAll(); // Simplified; Ideally: orderRepository.findByUserId(userId);
    }
}