package com.diddycart.service;

import com.diddycart.dto.vendor.VendorRegistrationRequest;
import com.diddycart.dto.vendor.VendorResponse;
import com.diddycart.enums.UserRole;
import com.diddycart.models.User;
import com.diddycart.models.Vendor;
import com.diddycart.repository.UserRepository;
import com.diddycart.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private UserRepository userRepository;

    // Register user as vendor
    @Transactional
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
        userRepository.save(user);

        // Return response
        return mapToResponse(savedVendor);
    }

    // Get vendor profile by user ID
    public VendorResponse getVendorByUserId(Long userId) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Vendor profile not found"));
        return mapToResponse(vendor);
    }

    // Get vendor profile by vendor ID
    public VendorResponse getVendorById(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        return mapToResponse(vendor);
    }

    // Update vendor profile
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
