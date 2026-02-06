package com.diddycart.modules.products.service;

import com.diddycart.modules.products.dto.category.CategoryRequest;
import com.diddycart.modules.products.dto.category.CategoryResponse;
import com.diddycart.modules.products.models.Category;
import com.diddycart.modules.products.repository.CategoryRepository;
import com.diddycart.modules.products.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// Service for category management
// Handles business logic for category operations

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    // Public: Get All Categories
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        // Find all categories and map to category response
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Public: Get Category By ID
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    // Admin: Create Category
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest req) {
        // Check if category type already exists
        if (categoryRepository.existsByType(req.getType())) {
            throw new RuntimeException("Category already exists: " + req.getType());
        }

        // Create new category
        Category category = new Category();
        category.setType(req.getType());
        category.setDescription(req.getDescription());

        // Save category to repository and map to category response
        return mapToResponse(categoryRepository.save(category));
    }

    // Admin: Update Category
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest req) {
        // Find category by id
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        // Check for duplicate type only if type is changing
        if (!category.getType().equals(req.getType()) && categoryRepository.existsByType(req.getType())) {
            throw new RuntimeException("Category name already exists: " + req.getType());
        }

        // Update category type and description
        category.setType(req.getType());
        category.setDescription(req.getDescription());

        return mapToResponse(categoryRepository.save(category));
    }

    // Admin: Delete Category
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        // Prevent deletion if products exist in this category by checking if any
        // product exists in the category
        if (productRepository.existsByCategoryId(id)) {
            throw new RuntimeException("Cannot delete category. It contains products.");
        }

        // Check if category exists by id
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found with id: " + id);
        }

        // Delete category by id
        categoryRepository.deleteById(id);
    }

    // Map category to category response
    private CategoryResponse mapToResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setType(category.getType());
        response.setDescription(category.getDescription());
        return response;
    }
}