package com.diddycart.modules.identity.controller;

import com.diddycart.modules.identity.dto.VendorProfileResponse;
import com.diddycart.modules.identity.dto.VendorRegisterResponse;
import com.diddycart.modules.identity.dto.VendorRegistrationRequest;
import com.diddycart.modules.identity.dto.VendorResponse;
import com.diddycart.modules.identity.service.VendorService;
import com.diddycart.common.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor")
public class VendorController {

    @Autowired
    private VendorService vendorService;

    @Autowired
    private JwtUtil jwtUtil;

    // Register a user as vendor (USER role required)
    @PostMapping("/register")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<VendorRegisterResponse> registerVendor(
            @RequestBody @Valid VendorRegistrationRequest request,
            @RequestHeader("Authorization") String token) {

        String jwt = token.substring(7);
        Long userId = jwtUtil.extractUserId(jwt);

        VendorRegisterResponse response = vendorService.registerVendor(userId, request);
        return ResponseEntity.ok(response);
    }

    // Get own vendor profile (Restricted View)
    @GetMapping("/profile")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    public ResponseEntity<VendorProfileResponse> getVendorProfile(@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        Long userId = jwtUtil.extractUserId(jwt);
        VendorProfileResponse response = vendorService.getVendorByUserId(userId);
        return ResponseEntity.ok(response);
    }

    // Update vendor profile
    @PutMapping("/profile")
    @PreAuthorize("hasAnyAuthority('ROLE_VENDOR', 'ROLE_ADMIN')")
    public ResponseEntity<VendorResponse> updateVendorProfile(
            @RequestBody @Valid VendorRegistrationRequest request,
            @RequestHeader("Authorization") String token) {

        String jwt = token.substring(7);
        Long userId = jwtUtil.extractUserId(jwt);

        VendorResponse response = vendorService.updateVendor(userId, request);
        return ResponseEntity.ok(response);
    }

    // Get vendor complete details by ID (public)
    @GetMapping("/{vendorId}")
    public ResponseEntity<VendorResponse> getVendorById(@PathVariable Long vendorId) {
        VendorResponse response = vendorService.getVendorById(vendorId);
        return ResponseEntity.ok(response);
    }
}
