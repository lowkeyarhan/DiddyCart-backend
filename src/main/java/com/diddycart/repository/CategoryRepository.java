package com.diddycart.repository;

import com.diddycart.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Check if a category type already exists
    boolean existsByType(String type);

    Optional<Category> findByType(String type);
}