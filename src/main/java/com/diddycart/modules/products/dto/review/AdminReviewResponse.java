package com.diddycart.modules.products.dto.review;

import lombok.Data;
import java.io.Serializable;

// Data Transfer Object for admin review responses
// What the backend sends to the frontend when providing admin review details.

@Data
public class AdminReviewResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String comment;
    private Integer rating;
    private String createdAt;

    // User Info
    private Long userId;
    private String userName;
    private String userEmail;

    // Product Info (Context for the admin)
    private Long productId;
    private String productName;
}