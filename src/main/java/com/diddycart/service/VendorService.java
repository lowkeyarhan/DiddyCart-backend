package com.diddycart.service;

import com.diddycart.dto.vendor.VendorRegistrationRequest;
import com.diddycart.dto.vendor.VendorResponse;
import com.diddycart.enums.UserRole;
import com.diddycart.models.User;
import com.diddycart.models.Vendor;
import com.diddycart.repository.UserRepository;
import com.diddycart.repository.VendorRepository;
import com.diddycart.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // Register user as vendor
    // EVICT CACHE: User profile cache needs update after role change
    @Transactional
    @CacheEvict(value = "user_profile", key = "#userId")
    public VendorResponse registerVendor(Long userId, VendorRegistrationRequest request) {
        // Get user details
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user is already a vendor
        if (user.getRole() == UserRole.VENDOR) {
            throw new RuntimeException("User is already a vendor");
        }

        // Check if GSTIN already exists
        if (vendorRepository.existsByGstin(request.getGstin())) {
            throw new RuntimeException("GSTIN already registered");
        }

        // Create vendor profile
        Vendor vendor = new Vendor();
        vendor.setUser(user);
        vendor.setStoreName(request.getStoreName());
        vendor.setGstin(request.getGstin());
        vendor.setDescription(request.getDescription());

        // Save vendor
        Vendor savedVendor = vendorRepository.save(vendor);

        // Update user role to VENDOR
        user.setRole(UserRole.VENDOR);
        User updatedUser = userRepository.save(user);

        // Generate new JWT token with updated role
        String newToken = jwtUtil.generateToken(updatedUser.getId(), updatedUser.getRole().name());

        // Return response with new token
        VendorResponse response = mapToResponse(savedVendor);
        response.setNewToken(newToken);

        return response;
    }

    // Get vendor profile by user ID checks cache first
    @Cacheable(value = "vendors_by_user", key = "#userId")
    public VendorResponse getVendorByUserId(Long userId) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));
        return mapToResponse(vendor);
    }

    // Get vendor profile by vendor ID checks cache first
    @Cacheable(value = "vendors", key = "#vendorId")
    public VendorResponse getVendorById(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        return mapToResponse(vendor);
    }

    // Update vendor profile and update both caches
    @Caching(put = {
            @CachePut(value = "vendors_by_user", key = "#userId"),
            @CachePut(value = "vendors", key = "#result.id")
    })
    @Transactional
    public VendorResponse updateVendor(Long userId, VendorRegistrationRequest request) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));

        // Check if GSTIN is being changed and if it already exists
        if (!vendor.getGstin().equals(request.getGstin()) &&
                vendorRepository.existsByGstin(request.getGstin())) {
            throw new RuntimeException("GSTIN already registered");
        }

        vendor.setStoreName(request.getStoreName());
        vendor.setGstin(request.getGstin());
        vendor.setDescription(request.getDescription());

        Vendor updatedVendor = vendorRepository.save(vendor);
        return mapToResponse(updatedVendor);
    }

    // Helper method to map Vendor to VendorResponse
    private VendorResponse mapToResponse(Vendor vendor) {
        VendorResponse response = new VendorResponse();
        response.setId(vendor.getId());
        response.setUserId(vendor.getUser().getId());
        response.setStoreName(vendor.getStoreName());
        response.setGstin(vendor.getGstin());
        response.setDescription(vendor.getDescription());
        response.setUserEmail(vendor.getUser().getEmail());
        response.setUserName(vendor.getUser().getName());
        return response;
    }
}
