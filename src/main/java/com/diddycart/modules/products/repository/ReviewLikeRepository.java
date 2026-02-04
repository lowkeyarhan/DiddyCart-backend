package com.diddycart.modules.products.repository;

import com.diddycart.modules.products.models.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    // Check if a ReviewLike exists by review ID and user ID
    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);

    // Find a ReviewLike by review ID and user ID
    Optional<ReviewLike> findByReviewIdAndUserId(Long reviewId, Long userId);

    // Find IDs of reviews liked by a specific user from a list of review IDs
    @Query("SELECT rl.review.id FROM ReviewLike rl WHERE rl.user.id = :userId AND rl.review.id IN :reviewIds")
    Set<Long> findLikedReviewIds(@Param("userId") Long userId, @Param("reviewIds") List<Long> reviewIds);
}