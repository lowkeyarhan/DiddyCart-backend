package com.diddycart.modules.products.controller;

import com.diddycart.modules.products.dto.product.ProductResponse;
import com.diddycart.modules.products.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.io.IOException;
import com.diddycart.modules.products.dto.product.ProductRequest;
import com.diddycart.common.security.JwtUtil;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private JwtUtil jwtUtil;

    // Public: Get All Products by pageable (Paginated)
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    // Public: Get Product by ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // Public: Search Products
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(@RequestParam String keyword, Pageable pageable) {
        return ResponseEntity.ok(productService.searchProducts(keyword, pageable));
    }

    // Vendor: Add Product
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public ResponseEntity<ProductResponse> addProduct(
            @RequestBody @Valid ProductRequest productRequest,
            @RequestHeader("Authorization") String token) throws IOException {

        // Extract Vendor's userID from token
        String jwt = token.substring(7);
        Long vendorId = jwtUtil.extractUserId(jwt);
        return ResponseEntity.ok(productService.addProduct(productRequest, null, vendorId));
    }

    // Vendor: Update Product
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid ProductRequest productRequest,
            @RequestHeader("Authorization") String token) throws IOException {

        // Extract Vendor's userID from token
        String jwt = token.substring(7);
        Long vendorId = jwtUtil.extractUserId(jwt);
        return ResponseEntity.ok(productService.updateProduct(id, productRequest, null, vendorId));
    }

    // Vendor: Delete Product
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_VENDOR')")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) throws IOException {

        // Extract Vendor's userID from token
        String jwt = token.substring(7);
        Long vendorId = jwtUtil.extractUserId(jwt);
        productService.deleteProduct(id, vendorId);
        return ResponseEntity.ok("Product deleted successfully");
    }
}