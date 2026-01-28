package com.diddycart.modules.identity.service;

import com.diddycart.modules.identity.dto.VendorProfileResponse;
import com.diddycart.modules.identity.dto.VendorRegisterResponse;
import com.diddycart.modules.identity.dto.VendorRegistrationRequest;
import com.diddycart.modules.identity.dto.VendorResponse;
import com.diddycart.modules.identity.models.UserRole;
import com.diddycart.modules.identity.models.User;
import com.diddycart.modules.identity.models.Vendor;
import com.diddycart.modules.identity.repository.UserRepository;
import com.diddycart.modules.identity.repository.VendorRepository;
import com.diddycart.common.security.JwtUtil;

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

    // Register user as vendor by userId and VendorRegistrationRequest
    @Transactional
    @CacheEvict(value = "user_profile", key = "#userId")
    public VendorRegisterResponse registerVendor(Long userId, VendorRegistrationRequest request) {
        // Get user details by userId
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

        // Create vendor object
        Vendor vendor = new Vendor();
        vendor.setUser(user);
        vendor.setStoreName(request.getStoreName());
        vendor.setGstin(request.getGstin());
        vendor.setDescription(request.getDescription());

        // Save vendor to VendorRepository
        Vendor savedVendor = vendorRepository.save(vendor);

        // Update user role to VENDOR by userId
        user.setRole(UserRole.VENDOR);
        User updatedUser = userRepository.save(user);

        // Generate new JWT token with updated role by userId and role
        String newToken = jwtUtil.generateToken(updatedUser.getId(), updatedUser.getRole().name());

        // Return response with new token by savedVendor
        VendorRegisterResponse response = mapToRegisterResponse(savedVendor);
        response.setNewToken(newToken);

        return response;
    }

    // Get vendor profile by user ID by userId checks cache first
    // Sents the vendor profile data without user details
    @Cacheable(value = "vendors_by_user", key = "#userId")
    public VendorProfileResponse getVendorByUserId(Long userId) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));

        return mapToProfileData(vendor); // Map Vendor to VendorProfileResponse
    }

    // Get vendor profile by vendorId checks cache first
    // Sents the full vendor details including user info by vendorId
    @Cacheable(value = "vendors", key = "#vendorId")
    public VendorResponse getVendorById(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        return mapToResponse(vendor);
    }

    // Update vendor profile by userId and VendorRegistrationRequest
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

        // Update vendor fields
        vendor.setStoreName(request.getStoreName());
        vendor.setGstin(request.getGstin());
        vendor.setDescription(request.getDescription());

        // Save vendor to VendorRepository
        Vendor updatedVendor = vendorRepository.save(vendor);

        // Map Vendor to VendorResponse
        return mapToResponse(updatedVendor);
    }

    // Map Vendor to VendorResponse
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

    // Map Vendor to VendorProfileResponse
    private VendorProfileResponse mapToProfileData(Vendor vendor) {
        VendorProfileResponse data = new VendorProfileResponse();
        data.setId(vendor.getId());
        data.setStoreName(vendor.getStoreName());
        data.setGstin(vendor.getGstin());
        data.setDescription(vendor.getDescription());
        return data;
    }

    // Map Vendor to VendorRegisterResponse
    private VendorRegisterResponse mapToRegisterResponse(Vendor vendor) {
        VendorRegisterResponse response = new VendorRegisterResponse();
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
