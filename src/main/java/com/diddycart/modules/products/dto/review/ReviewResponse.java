package com.diddycart.modules.products.dto.review;

import lombok.Data;

// Data Transfer Object for review responses
// What the backend sends to the frontend when providing review details.

@Data
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String userName; // Display name of reviewer
    private Integer rating;
    private String comment;
    private Integer likeCount;
    private boolean isLiked; // True if the current user liked this review
    private String createdAt;
}