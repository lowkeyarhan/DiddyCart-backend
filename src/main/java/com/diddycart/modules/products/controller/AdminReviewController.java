package com.diddycart.modules.products.controller;

import com.diddycart.modules.products.dto.review.AdminReviewResponse;
import com.diddycart.modules.products.service.ReviewService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminReviewController {

    @Autowired
    private ReviewService reviewService;

    // Admin: Get All Reviews (Paginated)
    @GetMapping
    public ResponseEntity<Page<AdminReviewResponse>> getAllReviews(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getAllReviews(pageable));
    }

    // Admin: Delete a review by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok("Review deleted successfully");
    }
}