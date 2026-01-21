package com.diddycart.controller;

import com.diddycart.dto.product.ProductRequest;
import com.diddycart.dto.product.ProductResponse;
import com.diddycart.service.ProductService;
import com.diddycart.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private JwtUtil jwtUtil;

    // Get All Products (Paginated)
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    // Get Product by ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // Search Products
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(@RequestParam String keyword, Pageable pageable) {
        return ResponseEntity.ok(productService.searchProducts(keyword, pageable));
    }

    // Vendor/Admin: Add Product
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    public ResponseEntity<ProductResponse> addProduct(
            @RequestBody @Valid ProductRequest productRequest,
            @RequestHeader("Authorization") String token) throws IOException {

        // Extract Vendor's userID from token
        String jwt = token.substring(7);
        Long vendorId = jwtUtil.extractUserId(jwt);

        return ResponseEntity.ok(productService.addProduct(productRequest, null, vendorId));
    }

    // Vendor/Admin: Update Product
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid ProductRequest productRequest,
            @RequestHeader("Authorization") String token) throws IOException {

        String jwt = token.substring(7);
        Long vendorId = jwtUtil.extractUserId(jwt);

        return ResponseEntity.ok(productService.updateProduct(id, productRequest, null, vendorId));
    }

    // Vendor/Admin: Delete Product
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) throws IOException {

        String jwt = token.substring(7);
        Long vendorId = jwtUtil.extractUserId(jwt);

        productService.deleteProduct(id, vendorId);
        return ResponseEntity.ok("Product deleted successfully");
    }
}