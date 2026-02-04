package com.diddycart.modules.products.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Data Transfer Object for review creation requests
// What the frontend sends to the backend when a user is submitting a product review.

@Data
public class ReviewRequest {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    private String comment;
}