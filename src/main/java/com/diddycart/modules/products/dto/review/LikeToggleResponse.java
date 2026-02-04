package com.diddycart.modules.products.dto.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeToggleResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String action; // "liked" or "unliked"
    private Integer likeCount;
}
