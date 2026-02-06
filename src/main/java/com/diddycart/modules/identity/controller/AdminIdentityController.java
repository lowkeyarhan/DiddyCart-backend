package com.diddycart.modules.identity.controller;

import com.diddycart.modules.identity.dto.admin.AdminUserDetailResponse;
import com.diddycart.modules.identity.dto.admin.AdminUserSummaryResponse;
import com.diddycart.modules.identity.dto.admin.AdminVendorSummaryResponse;
import com.diddycart.modules.identity.dto.vendor.VendorResponse;
import com.diddycart.modules.identity.service.AdminIdentityService;
import com.diddycart.modules.identity.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Controller for admin identity management
// Handles requests for admin user and vendor management

@RestController
@RequestMapping("/api/admin/identity")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminIdentityController {

    @Autowired
    private AdminIdentityService adminIdentityService;

    @Autowired
    private VendorService vendorService;

    // Get all users (summary view)
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserSummaryResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(adminIdentityService.getAllUsers(pageable));
    }

    // Get user by id (detailed view)
    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDetailResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminIdentityService.getUserById(id));
    }

    // Get all vendors (summary view)
    @GetMapping("/vendors")
    public ResponseEntity<Page<AdminVendorSummaryResponse>> getAllVendors(Pageable pageable) {
        return ResponseEntity.ok(adminIdentityService.getAllVendors(pageable));
    }

    // Get vendor by id (uses existing service implementation)
    @GetMapping("/vendors/{id}")
    public ResponseEntity<VendorResponse> getVendorById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getVendorById(id));
    }
}