package com.diddycart.repository;

import com.diddycart.models.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

        // 1. Search by product name
        Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

        // 2. Find by Category
        Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

        // 3. Find by Vendor
        Page<Product> findByVendorId(Long vendorId, Pageable pageable);

        // 4. Search + Filter by Price Range
        @Query("SELECT p FROM Product p WHERE " +
                        "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "AND p.price BETWEEN :minPrice AND :maxPrice")
        Page<Product> searchProducts(@Param("keyword") String keyword,
                        @Param("minPrice") Double minPrice,
                        @Param("maxPrice") Double maxPrice,
                        Pageable pageable);
}