package com.diddycart.modules.products.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.diddycart.modules.products.models.Product;

import jakarta.persistence.LockModeType;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

        // Search product by product name
        Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

        // Find by product Category
        Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

        // Check if any product exists in a category
        boolean existsByCategoryId(Long categoryId);

        // Search + Filter by Price Range
        @Query("SELECT p FROM Product p WHERE " +
                        "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "AND p.price BETWEEN :minPrice AND :maxPrice")
        Page<Product> searchProducts(@Param("keyword") String keyword,
                        @Param("minPrice") Double minPrice,
                        @Param("maxPrice") Double maxPrice,
                        Pageable pageable);

        // Pessimistic Lock for atomic updates
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT p FROM Product p WHERE p.id = :id")
        Optional<Product> findByIdForUpdate(@Param("id") Long id);
}