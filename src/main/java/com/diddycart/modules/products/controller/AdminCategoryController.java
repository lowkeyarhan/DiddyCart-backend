package com.diddycart.modules.products.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.diddycart.modules.products.dto.category.CategoryRequest;
import com.diddycart.modules.products.dto.category.CategoryResponse;
import com.diddycart.modules.products.service.CategoryService;

import jakarta.validation.Valid;

// Controller for admin category management
// Handles requests for admin category management

@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminCategoryController {

    @Autowired
    private CategoryService categoryService;

    // Admin: Create Category
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.createCategory(request));
    }

    // Admin: Update Category
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    // Admin: Delete Category
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("Category deleted successfully");
    }
}
