package com.diddycart.modules.products.controller;

import com.diddycart.common.security.JwtUtil;
import com.diddycart.modules.products.dto.review.ReviewRequest;
import com.diddycart.modules.products.dto.review.ReviewResponse;
import com.diddycart.modules.products.dto.review.LikeToggleResponse;
import com.diddycart.modules.products.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private JwtUtil jwtUtil;

    // Add a new review for a product
    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable Long productId,
            @RequestBody @Valid ReviewRequest request,
            @RequestHeader("Authorization") String token) {

        String jwt = token.substring(7);
        Long userId = jwtUtil.extractUserId(jwt);

        return ResponseEntity.ok(reviewService.addReview(userId, productId, request));
    }

    // Toggle like/unlike for a review
    @PostMapping("/{reviewId}/like")
    public ResponseEntity<LikeToggleResponse> toggleLike(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @RequestHeader("Authorization") String token) {

        String jwt = token.substring(7);
        Long userId = jwtUtil.extractUserId(jwt);

        LikeToggleResponse response = reviewService.toggleLike(userId, reviewId);
        return ResponseEntity.ok(response);
    }

    // Get paginated reviews for a product
    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @PathVariable Long productId,
            @RequestHeader(value = "Authorization", required = false) String token,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        // Extract userId from token if present
        Long userId = null;
        if (token != null && token.startsWith("Bearer ")) {
            try {
                String jwt = token.substring(7);
                userId = jwtUtil.extractUserId(jwt);
            } catch (Exception ignored) {
                userId = null;
            }
        }

        return ResponseEntity.ok(reviewService.getProductReviews(productId, userId, pageable));
    }
}