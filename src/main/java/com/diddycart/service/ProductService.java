package com.diddycart.service;

import com.diddycart.dto.product.ProductRequest;
import com.diddycart.dto.product.ProductResponse;
import com.diddycart.models.Category;
import com.diddycart.models.Product;
import com.diddycart.models.ProductImage;
import com.diddycart.models.Vendor;
import com.diddycart.repository.CategoryRepository;
import com.diddycart.repository.ProductRepository;
import com.diddycart.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private FileService fileService;

    // Add Product
    public Product addProduct(ProductRequest req, MultipartFile image, Long vendorUserId) throws IOException {
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Vendor vendor = vendorRepository.findByUserId(vendorUserId) // Assuming you added findByUserId in Repo
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));

        Product product = new Product();
        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setStockQuantity(req.getStockQuantity());
        product.setCategory(category);
        product.setVendor(vendor);

        // Handle Image
        if (image != null && !image.isEmpty()) {
            String imageUrl = fileService.uploadImage(image);

            ProductImage productImage = new ProductImage();
            productImage.setImageUrl(imageUrl);
            productImage.setProduct(product);

            // Initialize list if null
            if (product.getImages() == null)
                product.setImages(new ArrayList<>());
            product.getImages().add(productImage);
        }

        return productRepository.save(product);
    }

    // Get all products (Paginated)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::mapToResponse);
    }

    // Search products by name
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(keyword, pageable).map(this::mapToResponse);
    }

    // Helper: Map to DTO
    private ProductResponse mapToResponse(Product product) {
        ProductResponse res = new ProductResponse();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setDescription(product.getDescription());
        res.setPrice(product.getPrice());
        res.setStockQuantity(product.getStockQuantity());
        res.setCategoryName(product.getCategory().getType());
        res.setVendorStoreName(product.getVendor().getStoreName());

        // Map images
        if (product.getImages() != null) {
            res.setImageUrls(product.getImages().stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList()));
        }
        return res;
    }
}