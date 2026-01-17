package com.diddycart.controller;

import com.diddycart.dto.address.AddressDTO;
import com.diddycart.models.Address;
import com.diddycart.service.AddressService;
import com.diddycart.util.JwtUtil;
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
    public ResponseEntity<List<Address>> getMyAddresses(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    // Get address by ID
    @GetMapping("/{id}")
    public ResponseEntity<Address> getAddressById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(addressService.getAddressById(id, userId));
    }

    // Create new address
    @PostMapping
    public ResponseEntity<Address> createAddress(
            @Valid @RequestBody AddressDTO addressDTO,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(addressService.createAddress(userId, addressDTO));
    }

    // Update existing address
    @PutMapping("/{id}")
    public ResponseEntity<Address> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressDTO addressDTO,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(addressService.updateAddress(id, userId, addressDTO));
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
