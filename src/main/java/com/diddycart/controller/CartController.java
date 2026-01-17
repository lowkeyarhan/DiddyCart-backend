package com.diddycart.controller;

import com.diddycart.models.Cart;
import com.diddycart.service.CartService;
import com.diddycart.util.JwtUtil;
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
    public ResponseEntity<Cart> getMyCart(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    // Add Item to Cart
    @PostMapping("/add")
    public ResponseEntity<String> addToCart(
            @RequestHeader("Authorization") String token,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {

        Long userId = jwtUtil.extractUserId(token.substring(7));
        cartService.addToCart(userId, productId, quantity);
        return ResponseEntity.ok("Item added to cart");
    }

    // Remove Item from Cart
    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<String> removeFromCart(@PathVariable Long cartItemId) {
        cartService.removeFromCart(cartItemId);
        return ResponseEntity.ok("Item removed from cart");
    }

    // Clear Cart
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");
    }
}