package com.diddycart.modules.identity.service;

import com.diddycart.modules.identity.dto.AddressRequest;
import com.diddycart.modules.identity.dto.AddressResponse;
import com.diddycart.modules.identity.dto.AddressSummaryResponse;
import com.diddycart.modules.identity.models.Address;
import com.diddycart.modules.identity.models.User;
import com.diddycart.modules.identity.repository.AddressRepository;
import com.diddycart.modules.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    // Fetch all addresses of a user
    @Cacheable(value = "user_addresses", key = "#userId")
    public List<AddressSummaryResponse> getUserAddresses(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return addressRepository.findByUser(user).stream()
                .map(this::mapToSummaryResponse)
                .collect(Collectors.toList());
    }

    // Fetch address of a user by ID
    // SECURITY: Include userId in the key so User B cannot fetch User A's cached address.
    @Cacheable(value = "address", key = "#userId + '_' + #addressId")
    public AddressResponse getAddressById(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));

        // Verify ownership
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to view this address");
        }

        return mapToResponse(address);
    }

    // Create new address
    // Delete old cache for user's address list and add new address to cache
    @Transactional
    @Caching(evict = { @CacheEvict(value = "user_addresses", key = "#userId") }, put = {
            @CachePut(value = "address", key = "#userId + '_' + #result.id") })
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Address address = new Address();
        address.setUser(user);
        address.setLabel(request.getLabel());
        address.setStreet(request.getStreet());
        address.setLandmark(request.getLandmark());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setPincode(request.getPincode());
        address.setPhone(request.getPhone());
        address.setAlternatePhone(request.getAlternatePhone());

        Address savedAddress = addressRepository.save(address);
        return mapToResponse(savedAddress);
    }

    // Update existing address
    // Update address cache and evict user's address list cache
    @Caching(put = { @CachePut(value = "address", key = "#userId + '_' + #addressId") }, evict = {
            @CacheEvict(value = "user_addresses", key = "#userId") })
    @Transactional
    public AddressResponse updateAddress(Long addressId, Long userId, AddressRequest request) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));

        // Verify ownership
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to update this address");
        }

        // Update fields
        address.setLabel(request.getLabel());
        address.setStreet(request.getStreet());
        address.setLandmark(request.getLandmark());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setPincode(request.getPincode());
        address.setPhone(request.getPhone());
        address.setAlternatePhone(request.getAlternatePhone());

        Address updatedAddress = addressRepository.save(address);
        return mapToResponse(updatedAddress);
    }

    // Delete address
    // Delete both address cache and user's address list cache
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "address", key = "#userId + '_' + #addressId"),
            @CacheEvict(value = "user_addresses", key = "#userId")
    })
    public void deleteAddress(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));

        // Verify ownership
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this address");
        }

        addressRepository.delete(address);
    }

    // Mapper to convert Address to AddressSummaryResponse used in see all addresses function
    private AddressSummaryResponse mapToSummaryResponse(Address address) {
        AddressSummaryResponse response = new AddressSummaryResponse();
        response.setId(address.getId());
        response.setLabel(address.getLabel());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setCountry(address.getCountry());
        response.setPincode(address.getPincode());
        return response;
    }

    // Helper method to map Address to AddressResponse
    private AddressResponse mapToResponse(Address address) {
        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setLabel(address.getLabel());
        response.setStreet(address.getStreet());
        response.setLandmark(address.getLandmark());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setCountry(address.getCountry());
        response.setPincode(address.getPincode());
        response.setPhone(address.getPhone());
        response.setAlternatePhone(address.getAlternatePhone());
        return response;
    }
}
