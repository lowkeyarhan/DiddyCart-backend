package com.diddycart.modules.products.service;

import com.diddycart.modules.identity.models.Vendor;
import com.diddycart.modules.identity.repository.VendorRepository;
import com.diddycart.modules.products.models.Category;
import com.diddycart.modules.products.models.Product;
import com.diddycart.modules.products.models.ProductImage;
import com.diddycart.modules.products.dto.ProductRequest;
import com.diddycart.modules.products.dto.ProductResponse;
import com.diddycart.modules.products.repository.CategoryRepository;
import com.diddycart.modules.products.repository.ProductRepository;
import com.diddycart.common.infrastructure.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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

    // ADMIN/VENDOR Add a new Product
    public ProductResponse addProduct(ProductRequest req, MultipartFile image, Long vendorUserId) throws IOException {
        // Get category by categoryId
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Get vendor by vendorUserId
        Vendor vendor = vendorRepository.findByUserId(vendorUserId)
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));

        // Create Product object
        Product product = new Product();
        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setStockQuantity(req.getStockQuantity());
        product.setCategory(category);
        product.setVendor(vendor);

        // Handle Image
        if (image != null && !image.isEmpty()) {
            // Upload image to file service
            String imageUrl = fileService.uploadImage(image);

            // Create ProductImage object
            ProductImage productImage = new ProductImage();
            productImage.setImageUrl(imageUrl);
            productImage.setProduct(product);

            // Initialize list if null
            if (product.getImages() == null)
                product.setImages(new ArrayList<>());
            product.getImages().add(productImage);
        }

        // Save product to ProductRepository
        Product savedProduct = productRepository.save(product);

        // Map Product to ProductResponse
        return mapToResponse(savedProduct);
    }

    // Get all products by pageable (Paginated)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::mapToResponse);
    }

    // Get Product by id checks cache first
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Map Product to ProductResponse
        return mapToResponse(product);
    }

    // Search products by name by keyword (Paginated)
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(keyword, pageable).map(this::mapToResponse);
    }

    // ADMIN/VENDOR Update Product and update cache
    @CachePut(value = "products", key = "#id")
    public ProductResponse updateProduct(Long id, ProductRequest req, MultipartFile image, Long vendorUserId)
            throws IOException {
        // Get product by id
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Verify ownership by vendorUserId
        if (!product.getVendor().getUser().getId().equals(vendorUserId)) {
            throw new RuntimeException("You are not authorized to update this product");
        }

        // Update fields by req
        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setStockQuantity(req.getStockQuantity());

        // Get category by categoryId
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);

        // Handle Image Update by image
        if (image != null && !image.isEmpty()) {
            // Remove old images by product.getImages()
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                for (ProductImage oldImage : product.getImages()) {
                    try {
                        fileService.removeImage(oldImage.getImageUrl());
                    } catch (IOException e) {
                        // Log but don't fail the update
                    }
                }
                product.getImages().clear();
            }

            // Add new image
            String imageUrl = fileService.uploadImage(image);

            // Create ProductImage object
            ProductImage productImage = new ProductImage();
            productImage.setImageUrl(imageUrl);
            productImage.setProduct(product);

            // Initialize list if null
            if (product.getImages() == null)
                product.setImages(new ArrayList<>());

            // Add productImage to product.getImages()
            product.getImages().add(productImage);
        }

        // Save product to ProductRepository
        Product updatedProduct = productRepository.save(product);

        // Map Product to ProductResponse
        return mapToResponse(updatedProduct);
    }

    // ADMIN/VENDOR Delete Product and remove from cache
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id, Long vendorUserId) throws IOException {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Verify ownership
        if (!product.getVendor().getUser().getId().equals(vendorUserId)) {
            throw new RuntimeException("You are not authorized to delete this product");
        }

        // Remove images from filesystem
        if (product.getImages() != null) {
            for (ProductImage image : product.getImages()) {
                try {
                    fileService.removeImage(image.getImageUrl());
                } catch (IOException e) {
                    // Log but continue deletion
                }
            }
        }

        productRepository.delete(product);
    }

    // Restore stock when payment fails
    public void restoreStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }

    // Map Product to ProductResponse
    private ProductResponse mapToResponse(Product product) {
        ProductResponse res = new ProductResponse();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setDescription(product.getDescription());
        res.setPrice(product.getPrice());
        res.setStockQuantity(product.getStockQuantity());
        res.setCategoryName(product.getCategory().getType());
        res.setVendorStoreName(product.getVendor().getStoreName());

        // Map images by product.getImages()
        if (product.getImages() != null) {
            res.setImageUrls(product.getImages().stream()
                    .map(ProductImage::getImageUrl)
                    .collect(Collectors.toList()));
        }

        // Return ProductResponse
        return res;
    }
}