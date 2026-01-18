package com.diddycart.service;

import com.diddycart.dto.cart.CartItemResponse;
import com.diddycart.dto.cart.CartResponse;
import com.diddycart.models.*;
import com.diddycart.repository.CartItemRepository;
import com.diddycart.repository.CartRepository;
import com.diddycart.repository.ProductRepository;
import com.diddycart.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    // Retrieve or Create Cart for User (internal use)
    public Cart getOrCreateCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    // Get Cart Response
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToResponse(cart);
    }

    // Add Item to Cart
    @Transactional
    public CartResponse addToCart(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Not enough stock available");
        }

        Optional<CartItem> existing = cart.getItems() != null ? cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst() : Optional.empty();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cartItemRepository.save(newItem);
        }

        // Refresh cart and return
        Cart updatedCart = getOrCreateCart(userId);
        return mapToResponse(updatedCart);
    }

    // Remove Item from Cart
    @Transactional
    public void removeFromCart(Long cartItemId) {
        cartItemRepository.deleteById(cartItemId);
    }

    // Clear Cart
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        if (cart.getItems() != null) {
            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
        }
    }

    // Helper method to map Cart to CartResponse
    private CartResponse mapToResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setCartId(cart.getId());

        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                CartItemResponse itemResponse = new CartItemResponse();
                itemResponse.setId(item.getId());
                itemResponse.setProductId(item.getProduct().getId());
                itemResponse.setProductName(item.getProduct().getName());
                itemResponse.setPrice(item.getProduct().getPrice());
                itemResponse.setQuantity(item.getQuantity());

                // Get first image if available
                if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                    itemResponse.setProductImage(item.getProduct().getImages().get(0).getImageUrl());
                }

                BigDecimal subTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                itemResponse.setSubTotal(subTotal);
                totalAmount = totalAmount.add(subTotal);

                itemResponses.add(itemResponse);
            }
        }

        response.setItems(itemResponses);
        response.setTotalAmount(totalAmount);
        return response;
    }
}