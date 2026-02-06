package com.diddycart.modules.products.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import com.diddycart.common.security.JwtUtil;
import com.diddycart.modules.products.dto.product.ProductRequest;
import com.diddycart.modules.products.dto.product.ProductResponse;
import com.diddycart.modules.products.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminProductControler {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ProductService productService;

    // Admin: Update Product
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid ProductRequest productRequest,
            @RequestHeader("Authorization") String token) throws IOException {

        // Extract Admin's userID from token
        String jwt = token.substring(7);
        Long vendorId = jwtUtil.extractUserId(jwt);

        return ResponseEntity.ok(productService.updateProduct(id, productRequest, null, vendorId));
    }

    // Admin: Delete Product
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) throws IOException {

        // Extract Admin's userID from token
        String jwt = token.substring(7);
        Long vendorId = jwtUtil.extractUserId(jwt);

        productService.deleteProduct(id, vendorId);
        return ResponseEntity.ok("Product deleted successfully");
    }
}
