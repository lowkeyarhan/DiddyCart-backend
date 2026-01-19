package com.diddycart.service;

import com.diddycart.dto.cart.CartItemResponse;
import com.diddycart.dto.cart.CartResponse;
import com.diddycart.models.*;
import com.diddycart.repository.CartItemRepository;
import com.diddycart.repository.CartRepository;
import com.diddycart.repository.ProductRepository;
import com.diddycart.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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

    // Retrieve or Create Cart
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

    // Get my Cart check cache first
    @Cacheable(value = "cart", key = "#userId")
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToResponse(cart);
    }

    // Add Item to Cart and update cache
    @Transactional
    @CachePut(value = "cart", key = "#userId")
    public CartResponse addToCart(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Not enough stock available");
        }

        // Initialize list if null (Important!)
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

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
            cart.getItems().add(newItem);
        }

        // Now 'cart' has the new item in its list
        return mapToResponse(cart);
    }

    // Remove Item from Cart and update cache
    @Transactional
    @CachePut(value = "cart", key = "#userId")
    public CartResponse removeFromCart(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // Security Check
        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to remove this item");
        }

        if (item.getCart().getItems() != null) {
            item.getCart().getItems().remove(item);
        }

        cartItemRepository.delete(item);

        return getCart(userId);
    }

    // Clear Cart and update cache
    @Transactional
    @CacheEvict(value = "cart", key = "#userId")
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        if (cart.getItems() != null) {
            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
        }
    }

    // Cart response mapper function
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