package com.diddycart.modules.products.service;

import com.diddycart.modules.identity.models.User;
import com.diddycart.modules.identity.repository.UserRepository;
import com.diddycart.modules.products.dto.review.ReviewRequest;
import com.diddycart.modules.products.dto.review.ReviewResponse;
import com.diddycart.modules.products.dto.review.LikeToggleResponse;
import com.diddycart.modules.products.models.Product;
import com.diddycart.modules.products.models.Review;
import com.diddycart.modules.products.models.ReviewLike;
import com.diddycart.modules.products.repository.ProductRepository;
import com.diddycart.modules.products.repository.ReviewLikeRepository;
import com.diddycart.modules.products.repository.ReviewRepository;
import com.diddycart.modules.sales.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewLikeRepository reviewLikeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    // Add a new review for a product
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", key = "#productId"),
            @CacheEvict(value = "product_reviews", allEntries = true)
    })
    public ReviewResponse addReview(Long userId, Long productId, ReviewRequest request) {

        // Check if the user has purchased the product
        boolean hasPurchased = orderRepository.existsDeliveredOrderForProduct(userId, productId);
        if (!hasPurchased) {
            throw new RuntimeException(
                    "Verified Purchase Required: You can only review products you have purchased and received.");
        }

        // Check if the user has already reviewed the product
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new RuntimeException("You have already reviewed this product.");
        }

        // Get the user and product
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Create a new review
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setLikeCount(0);
        Review savedReview = reviewRepository.save(review);

        // Update the product rating
        updateProductRating(product, request.getRating());

        return mapToResponse(savedReview, false);
    }

    // Toggle like/unlike for a review
    @Transactional
    public LikeToggleResponse toggleLike(Long userId, Long reviewId) {

        // Get the review
        Review review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
                
        // Get the user
        User user = userRepository.getReferenceById(userId);

        // Check if the user has already liked the review
        Optional<ReviewLike> existingLike = reviewLikeRepository.findByReviewIdAndUserId(reviewId, userId);

        String action;
        // If the user has already liked the review, remove the like
        if (existingLike.isPresent()) {
            reviewLikeRepository.delete(existingLike.get());
            review.setLikeCount(Math.max(0, review.getLikeCount() - 1));
            action = "unliked";
        } else {
            // If the user has not liked the review, create a new like
            ReviewLike newLike = new ReviewLike(review, user);
            reviewLikeRepository.save(newLike);
            review.setLikeCount(review.getLikeCount() + 1);
            action = "liked";
        }

        // Save the review
        reviewRepository.save(review);

        return new LikeToggleResponse(action, review.getLikeCount());
    }

    // Get paginated reviews for a product
    @Transactional
    public Page<ReviewResponse> getProductReviews(Long productId, Long currentUserId, Pageable pageable) {

        // Get the reviews
        Page<Review> reviews = reviewRepository.findByProductId(productId, pageable);

        Set<Long> likedReviewIds;

        // If the current user is not null and the reviews are not empty, get the liked
        // review ids
        if (currentUserId != null && !reviews.isEmpty()) {
            List<Long> reviewIds = reviews.getContent().stream()
                    .map(Review::getId)
                    .toList();
            likedReviewIds = reviewLikeRepository.findLikedReviewIds(currentUserId, reviewIds);
        } else {
            likedReviewIds = Set.of();
        }

        // Map the reviews to responses
        return reviews.map(review -> mapToResponse(review, likedReviewIds.contains(review.getId())));
    }

    // Update the product rating
    private void updateProductRating(Product product, int newRating) {

        // Get the current average rating and review count
        BigDecimal currentAvg = product.getAverageRating() != null ? product.getAverageRating() : BigDecimal.ZERO;
        int currentCount = product.getReviewCount() != null ? product.getReviewCount() : 0;
        int newCount = currentCount + 1;

        // Calculate the new average rating
        BigDecimal totalScore = currentAvg.multiply(BigDecimal.valueOf(currentCount))
                .add(BigDecimal.valueOf(newRating));
        BigDecimal newAvg = totalScore.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

        // Update the product rating and review count
        product.setReviewCount(newCount);
        product.setAverageRating(newAvg);
        productRepository.save(product);
    }

    // Map the review to a response
    private ReviewResponse mapToResponse(Review review, boolean isLiked) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUser().getId());
        response.setUserName(review.getUser().getName());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setLikeCount(review.getLikeCount());
        response.setCreatedAt(review.getCreatedAt());

        // This boolean controls the blue "Thumbs Up" on the UI
        response.setLiked(isLiked);

        return response;
    }
}