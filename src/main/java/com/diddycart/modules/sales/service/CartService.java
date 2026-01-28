package com.diddycart.modules.sales.service;

import com.diddycart.modules.sales.dto.CartItemResponse;
import com.diddycart.modules.sales.dto.CartResponse;
import com.diddycart.modules.sales.models.Cart;
import com.diddycart.modules.sales.models.CartItem;
import com.diddycart.modules.identity.models.User;
import com.diddycart.modules.sales.repository.CartItemRepository;
import com.diddycart.modules.sales.repository.CartRepository;
import com.diddycart.modules.identity.repository.UserRepository;
import com.diddycart.modules.products.models.Product;
import com.diddycart.modules.products.repository.ProductRepository;

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

    // Retrieve or Create Cart by userId
    public Cart getOrCreateCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Find cart by user or create new cart
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    // Create new cart
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    // Get my Cart by userId checks cache first
    @Cacheable(value = "cart", key = "#userId")
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);

        // Map Cart to CartResponse
        return mapToResponse(cart);
    }

    // Add Item to Cart by userId, productId, quantity and update cache
    @Transactional
    @CachePut(value = "cart", key = "#userId")
    public CartResponse addToCart(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);

        // Find product by productId
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check if product has enough stock
        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Not enough stock available");
        }

        // Initialize list if null (Important!)
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }

        // Find existing cart item by productId
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        // If existing cart item is found, update quantity
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);

            // Save new cart item
            cartItemRepository.save(newItem);

            // Add new cart item to cart
            cart.getItems().add(newItem);
        }

        // Map Cart to CartResponse
        return mapToResponse(cart);
    }

    // Remove Item from Cart by userId, cartItemId and update cache
    @Transactional
    @CachePut(value = "cart", key = "#userId")
    public CartResponse removeFromCart(Long userId, Long cartItemId) {
        // Find cart item by cartItemId
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // Security Check by userId and cartItemId
        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to remove this item");
        }

        // Remove cart item from cart
        if (item.getCart().getItems() != null) {
            item.getCart().getItems().remove(item);
        }

        // Delete cart item
        cartItemRepository.delete(item);

        // Map Cart to CartResponse
        return getCart(userId);
    }

    // Clear Cart by userId and update cache
    @Transactional
    @CacheEvict(value = "cart", key = "#userId")
    public void clearCart(Long userId) {
        // Find cart by userId
        Cart cart = getOrCreateCart(userId);

        // If cart has items, delete all cart items
        if (cart.getItems() != null) {
            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
        }
    }

    // Map Cart to CartResponse
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