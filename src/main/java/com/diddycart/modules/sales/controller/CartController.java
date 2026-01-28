package com.diddycart.modules.sales.controller;

import com.diddycart.modules.sales.dto.AddToCartRequest;
import com.diddycart.modules.sales.dto.CartResponse;
import com.diddycart.modules.sales.service.CartService;
import com.diddycart.common.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private JwtUtil jwtUtil;

    // Get My Cart
    @GetMapping
    public ResponseEntity<CartResponse> getMyCart(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    // Add Item to Cart
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody AddToCartRequest request) {

        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(cartService.addToCart(userId, request.getProductId(), request.getQuantity()));
    }

    // Remove Item from Cart
    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<CartResponse> removeFromCart(
            @PathVariable Long cartItemId,
            @RequestHeader("Authorization") String token) {

        // Extract userId from token
        Long userId = jwtUtil.extractUserId(token.substring(7));

        // Remove item from cart by userId and cartItemId
        return ResponseEntity.ok(cartService.removeFromCart(userId, cartItemId));
    }

    // Clear Cart
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart(@RequestHeader("Authorization") String token) {

        // Extract userId from token
        Long userId = jwtUtil.extractUserId(token.substring(7));

        // Clear cart by userId
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");
    }
}