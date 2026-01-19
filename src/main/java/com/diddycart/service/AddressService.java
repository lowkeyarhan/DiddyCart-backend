package com.diddycart.service;

import com.diddycart.dto.address.AddressRequest;
import com.diddycart.dto.address.AddressResponse;
import com.diddycart.models.Address;
import com.diddycart.models.User;
import com.diddycart.repository.AddressRepository;
import com.diddycart.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<AddressResponse> getUserAddresses(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return addressRepository.findByUser(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Fetch address of a user by ID
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
    @Transactional
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
    @Transactional
    public void deleteAddress(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));

        // Verify ownership
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this address");
        }

        addressRepository.delete(address);
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
