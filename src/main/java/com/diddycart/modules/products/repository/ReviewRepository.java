package com.diddycart.modules.products.repository;

import com.diddycart.modules.products.models.Review;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Find reviews by product ID with pagination
    Page<Review> findByProductId(Long productId, Pageable pageable);

    // Check if a review exists by user ID and product ID
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // Pessimistic Lock for safe Like toggling
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Review r WHERE r.id = :id")
    Optional<Review> findByIdForUpdate(@Param("id") Long id);

    // Calculate actual average rating from DB
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    // Count actual reviews
    Long countByProductId(Long productId);
}