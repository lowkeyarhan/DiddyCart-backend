package com.diddycart.modules.products.repository;

import com.diddycart.modules.products.models.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Find reviews by product ID with pagination
    Page<Review> findByProductId(Long productId, Pageable pageable);

    // Check if a review exists by user ID and product ID
    boolean existsByUserIdAndProductId(Long userId, Long productId);
}