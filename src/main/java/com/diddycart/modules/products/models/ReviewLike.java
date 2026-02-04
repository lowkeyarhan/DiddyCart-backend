package com.diddycart.modules.products.models;

import com.diddycart.modules.identity.models.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Entity
@NoArgsConstructor
@Table(name = "review_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "review_id", "user_id" })
})
public class ReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private String createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

    public ReviewLike(Review review, User user) {
        this.review = review;
        this.user = user;
    }
}