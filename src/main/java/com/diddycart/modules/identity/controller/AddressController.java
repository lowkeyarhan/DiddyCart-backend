package com.diddycart.modules.identity.controller;

import com.diddycart.modules.identity.dto.AddressRequest;
import com.diddycart.modules.identity.dto.AddressResponse;
import com.diddycart.modules.identity.dto.AddressSummaryResponse;
import com.diddycart.modules.identity.service.AddressService;
import com.diddycart.common.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @Autowired
    private JwtUtil jwtUtil;

    // Get all my addresses
    @GetMapping
    public ResponseEntity<List<AddressSummaryResponse>> getMyAddresses(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    // Get address by ID
    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddressById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(addressService.getAddressById(id, userId));
    }

    // Create new address
    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @Valid @RequestBody AddressRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(addressService.createAddress(userId, request));
    }

    // Update existing address
    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(addressService.updateAddress(id, userId, request));
    }

    // Delete address
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAddress(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        addressService.deleteAddress(id, userId);
        return ResponseEntity.ok("Address deleted successfully");
    }
}
