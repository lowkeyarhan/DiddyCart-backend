package com.diddycart.modules.products.models;

import com.diddycart.modules.identity.models.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "product_id" })
})
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private String createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
}